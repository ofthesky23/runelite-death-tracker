package com.deathtracker;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(name = "Death Tracker", description = "Records local deaths, gear, inventory, screenshots, and costs", tags = {"death", "grave", "cost"})
public class DeathTrackerPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientToolbar toolbar;
    @Inject private ItemManager itemManager;
    @Inject private DrawManager drawManager;
    @Inject private DeathTrackerConfig config;

    private final DeathStore store = new DeathStore();
    private ExecutorService imageWriter;
    private DeathTrackerPanel panel;
    private NavigationButton navigation;
    private String currentPlayer;
    private Item[] lastEquipment;
    private Item[] lastInventory;
    private String recentOpponent;
    private long opponentSeenAt;
    private boolean lastSkulled;
    private boolean lastProtectItem;
    private long lastDeathAt;

    @Provides
    DeathTrackerConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(DeathTrackerConfig.class);
    }

    @Override
    protected void startUp()
    {
        imageWriter = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "death-tracker-image-writer");
            thread.setDaemon(true);
            return thread;
        });
        panel = new DeathTrackerPanel(store);
        navigation = NavigationButton.builder().tooltip("Death Tracker").icon(icon()).priority(7).panel(panel).build();
        toolbar.addNavigation(navigation);
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            initializeCharacter();
        }
    }

    @Override
    protected void shutDown()
    {
        toolbar.removeNavigation(navigation);
        imageWriter.shutdown();
        store.close();
        currentPlayer = null;
        lastEquipment = null;
        lastInventory = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            initializeCharacter();
        }
        else if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            store.close();
            currentPlayer = null;
            lastEquipment = null;
            lastInventory = null;
            recentOpponent = null;
            SwingUtilities.invokeLater(() -> panel.refresh());
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (currentPlayer == null || (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM))
        {
            return;
        }
        Long fee = ReclaimMessageParser.actualFee(Text.removeTags(event.getMessage()));
        if (fee == null)
        {
            return;
        }
        long now = System.currentTimeMillis();
        for (DeathRecord death : store.records())
        {
            if (now - death.timestamp > 2 * 60 * 60 * 1000L)
            {
                break;
            }
            if (death.actualReclaimFee == null)
            {
                death.actualReclaimFee = fee;
                death.notes = (death.notes == null || death.notes.isEmpty() ? "" : death.notes + "\n") +
                    "Reclaim fee detected from game chat.";
                try
                {
                    store.save();
                    SwingUtilities.invokeLater(() -> panel.refresh());
                }
                catch (IOException ex)
                {
                    log.error("Unable to save detected reclaim fee", ex);
                }
                break;
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            return;
        }
        if (currentPlayer == null)
        {
            initializeCharacter();
        }
        Player player = client.getLocalPlayer();
        if (player.getInteracting() != null)
        {
            Actor opponent = player.getInteracting();
            recentOpponent = opponent.getName();
            opponentSeenAt = System.currentTimeMillis();
        }
        lastSkulled = player.getSkullIcon() >= 0;
        lastProtectItem = client.isPrayerActive(Prayer.PROTECT_ITEM);
        lastEquipment = copy(client.getItemContainer(InventoryID.EQUIPMENT));
        lastInventory = copy(client.getItemContainer(InventoryID.INVENTORY));
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        if (event.getActor() != client.getLocalPlayer() || currentPlayer == null)
        {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDeathAt < 4000)
        {
            return;
        }
        lastDeathAt = now;

        Player player = client.getLocalPlayer();
        DeathRecord death = new DeathRecord();
        death.id = UUID.randomUUID().toString();
        death.timestamp = Instant.now().toEpochMilli();
        death.player = currentPlayer;
        death.world = client.getWorld();
        WorldPoint point = player.getWorldLocation();
        death.x = point.getX();
        death.y = point.getY();
        death.plane = point.getPlane();
        death.region = point.getRegionID();
        death.instance = client.isInInstancedRegion();
        death.opponent = now - opponentSeenAt < 10_000 ? recentOpponent : null;
        death.accountType = String.valueOf(client.getAccountType());
        death.skulled = lastSkulled;
        death.protectItem = lastProtectItem;
        death.equipment = describe(lastEquipment);
        death.inventory = describe(lastInventory);
        death.carriedValue = total(death.equipment) + total(death.inventory);
        boolean ironman = death.accountType.contains("IRONMAN");
        death.estimatedGraveFee = FeeEstimator.estimate(death.equipment, death.inventory,
            death.skulled, death.protectItem, ironman);
        death.estimateNote = "Standard PvM grave approximation using current item prices. " +
            "Special death rules, protected/untradeable items, discounts and actual reclaim choices can change the fee.";
        if (config.screenshot())
        {
            death.screenshot = death.id + ".png";
        }

        try
        {
            store.add(death);
            SwingUtilities.invokeLater(() -> panel.refresh());
            if (config.screenshot())
            {
                Path path = store.imagePath(death);
                ExecutorService writer = imageWriter;
                drawManager.requestNextFrameListener(image -> {
                    if (!writer.isShutdown())
                    {
                        writer.submit(() -> saveImage(image, path));
                    }
                });
            }
        }
        catch (IOException ex)
        {
            log.error("Unable to save death journal", ex);
        }
    }

    private void initializeCharacter()
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getName() == null)
        {
            return;
        }
        String name = player.getName();
        if (name.equals(currentPlayer))
        {
            return;
        }
        try
        {
            store.open(name);
            currentPlayer = name;
            SwingUtilities.invokeLater(() -> panel.refresh());
        }
        catch (IOException | RuntimeException ex)
        {
            log.error("Unable to open death journal for {}", name, ex);
        }
    }

    private static Item[] copy(ItemContainer container)
    {
        if (container == null)
        {
            return new Item[0];
        }
        Item[] source = container.getItems();
        Item[] result = new Item[source.length];
        for (int i = 0; i < source.length; i++)
        {
            Item item = source[i];
            result[i] = item == null ? null : new Item(item.getId(), item.getQuantity());
        }
        return result;
    }

    private List<TrackedItem> describe(Item[] snapshot)
    {
        List<TrackedItem> result = new ArrayList<>();
        if (snapshot == null)
        {
            return result;
        }
        for (int slot = 0; slot < snapshot.length; slot++)
        {
            Item item = snapshot[slot];
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
            {
                continue;
            }
            ItemComposition composition = itemManager.getItemComposition(item.getId());
            TrackedItem tracked = new TrackedItem();
            tracked.slot = slot;
            tracked.id = item.getId();
            tracked.quantity = item.getQuantity();
            tracked.name = composition.getMembersName();
            tracked.tradeable = composition.isTradeable();
            tracked.unitPrice = Math.max(0, itemManager.getItemPrice(item.getId()));
            if (tracked.unitPrice == 0)
            {
                tracked.unitPrice = Math.max(0, composition.getHaPrice());
            }
            result.add(tracked);
        }
        return result;
    }

    private static long total(List<TrackedItem> items)
    {
        long sum = 0;
        for (TrackedItem item : items)
        {
            sum += item.value();
        }
        return sum;
    }

    private static void saveImage(Image image, Path path)
    {
        try
        {
            BufferedImage copy = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = copy.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
            ImageIO.write(copy, "png", path.toFile());
        }
        catch (IOException ex)
        {
            log.error("Unable to save death screenshot", ex);
        }
    }

    private static BufferedImage icon()
    {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(165, 47, 47));
        g.fillOval(3, 2, 26, 26);
        g.setColor(Color.WHITE);
        g.fillOval(9, 10, 5, 6);
        g.fillOval(18, 10, 5, 6);
        g.fillRect(13, 20, 6, 5);
        g.dispose();
        return image;
    }
}
