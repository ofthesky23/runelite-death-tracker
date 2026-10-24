package com.deathtracker;

import java.io.IOException;
import net.runelite.client.util.Filepath;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Assert;
import org.junit.Test;

public class DeathStoreTest extends PluginDataTestSupport
{
    @Test
    public void readsExistingJsonArrayAndPreservesSnapshotsOnSave() throws IOException
    {
        Filepath alice = root.joinSegment("alice");
        alice.createDirectories();
        alice.joinSegment("deaths.json").write("["
            + "{\"id\":\"legacy-death\",\"timestamp\":1750000000000,\"player\":\"Alice\","
            + "\"world\":301,\"x\":3200,\"y\":3201,\"plane\":1,\"region\":12850,"
            + "\"instance\":true,\"opponent\":\"Goblin\",\"accountType\":\"IRONMAN\","
            + "\"skulled\":false,\"protectItem\":true,"
            + "\"equipment\":[{\"slot\":3,\"id\":4151,\"quantity\":1,\"name\":\"Abyssal whip\","
            + "\"unitPrice\":1500000,\"tradeable\":true}],"
            + "\"inventory\":[{\"slot\":7,\"id\":995,\"quantity\":1500,\"name\":\"Coins\","
            + "\"unitPrice\":1,\"tradeable\":true}],"
            + "\"carriedValue\":1501500,\"estimatedGraveFee\":null,"
            + "\"screenshot\":\"legacy-death.png\",\"actualReclaimFee\":null,"
            + "\"actualItemLoss\":250,\"actualOtherCost\":null,\"notes\":\"Existing journal\"},"
            + "{\"id\":\"pending-death\",\"timestamp\":1749999999999,\"player\":\"Alice\","
            + "\"equipment\":[],\"inventory\":[],\"actualReclaimFee\":null,"
            + "\"actualItemLoss\":null,\"actualOtherCost\":null}]"
        );

        DeathStore store = new DeathStore(RuneLiteAPI.GSON, root);
        store.open("Alice");
        Assert.assertEquals(2, store.records().size());
        assertLegacySnapshot(store.records().get(0));
        Assert.assertNull(store.records().get(1).actualTotal());
        store.records().get(0).actualReclaimFee = 10_000L;
        store.save();
        Assert.assertFalse(alice.joinSegment("deaths.json.tmp").exists());
        store.close();

        DeathStore reopened = new DeathStore(RuneLiteAPI.GSON, root);
        reopened.open("Alice");
        Assert.assertEquals(2, reopened.records().size());
        assertLegacySnapshot(reopened.records().get(0));
        Assert.assertEquals(Long.valueOf(10_000), reopened.records().get(0).actualReclaimFee);
        Assert.assertEquals(Long.valueOf(10_250), reopened.records().get(0).actualTotal());
        Assert.assertNull(reopened.records().get(1).actualTotal());
    }

    @Test
    public void keepsCharactersSeparateAndNewDeathsFirst() throws IOException
    {
        DeathStore store = new DeathStore(RuneLiteAPI.GSON, root);
        store.open("Alice");
        store.add(death("alice-first", "Alice"));
        store.add(death("alice-second", "Alice"));
        store.open("Bob");
        Assert.assertTrue(store.records().isEmpty());
        store.add(death("bob-only", "Bob"));
        store.open("ALICE");
        Assert.assertEquals(2, store.records().size());
        Assert.assertEquals("alice-second", store.records().get(0).id);
        Assert.assertEquals("alice-first", store.records().get(1).id);
        store.close();
        Assert.assertNull(store.directory());
        Assert.assertTrue(store.records().isEmpty());
        store.open("Bob");
        Assert.assertEquals(1, store.records().size());
        Assert.assertEquals("bob-only", store.records().get(0).id);
    }

    @Test
    public void rejectsScreenshotPathsThatEscapeTheCharacterDirectory() throws IOException
    {
        DeathStore store = new DeathStore(RuneLiteAPI.GSON, root);
        store.open("Alice");
        for (String invalid : new String[]{"../outside", "..\\outside", "/absolute", "C:\\outside", "nested/file"})
        {
            try
            {
                store.imagePath(death(invalid, "Alice"));
                Assert.fail("Accepted invalid screenshot path: " + invalid);
            }
            catch (IllegalArgumentException expected)
            {
                // Invalid saved IDs must not become filesystem paths.
            }
        }
    }

    private static DeathRecord death(String id, String player)
    {
        DeathRecord record = new DeathRecord();
        record.id = id;
        record.player = player;
        record.timestamp = 1750000000000L;
        return record;
    }

    private static void assertLegacySnapshot(DeathRecord record)
    {
        Assert.assertEquals("legacy-death", record.id);
        Assert.assertEquals(1750000000000L, record.timestamp);
        Assert.assertEquals("Alice", record.player);
        Assert.assertEquals(301, record.world);
        Assert.assertEquals(3200, record.x);
        Assert.assertEquals(3201, record.y);
        Assert.assertEquals(1, record.plane);
        Assert.assertEquals(12850, record.region);
        Assert.assertTrue(record.instance);
        Assert.assertEquals("Goblin", record.opponent);
        Assert.assertEquals("IRONMAN", record.accountType);
        Assert.assertFalse(record.skulled);
        Assert.assertTrue(record.protectItem);
        Assert.assertEquals(1501500L, record.carriedValue);
        Assert.assertNull(record.estimatedGraveFee);
        Assert.assertEquals(Long.valueOf(250), record.actualItemLoss);
        Assert.assertNull(record.actualOtherCost);
        Assert.assertEquals("Existing journal", record.notes);
        Assert.assertEquals("legacy-death.png", record.screenshot);
        Assert.assertEquals(1, record.equipment.size());
        TrackedItem weapon = record.equipment.get(0);
        Assert.assertEquals(3, weapon.slot);
        Assert.assertEquals(4151, weapon.id);
        Assert.assertEquals(1, weapon.quantity);
        Assert.assertEquals("Abyssal whip", weapon.name);
        Assert.assertEquals(1500000L, weapon.unitPrice);
        Assert.assertTrue(weapon.tradeable);
        Assert.assertEquals(1, record.inventory.size());
        TrackedItem coins = record.inventory.get(0);
        Assert.assertEquals(7, coins.slot);
        Assert.assertEquals(995, coins.id);
        Assert.assertEquals(1500, coins.quantity);
        Assert.assertEquals("Coins", coins.name);
        Assert.assertEquals(1L, coins.unitPrice);
        Assert.assertTrue(coins.tradeable);
    }
}
