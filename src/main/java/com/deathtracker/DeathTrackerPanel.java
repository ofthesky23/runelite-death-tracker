package com.deathtracker;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

final class DeathTrackerPanel extends PluginPanel
{
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a");
    private final DeathStore store;

    DeathTrackerPanel(DeathStore store)
    {
        this.store = store;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        refresh();
    }

    void refresh()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::refresh);
            return;
        }
        removeAll();
        List<DeathRecord> records = store.records();
        long total = 0;
        int completed = 0;
        for (DeathRecord record : records)
        {
            Long actual = record.actualTotal();
            if (actual != null)
            {
                total += actual;
                completed++;
            }
        }

        sectionLabel("DEATH TRACKER");
        line(records.size() + " deaths  •  " + completed + " costed");
        line("Total actual cost: " + gp(total));
        line("Awaiting actual cost: " + (records.size() - completed));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        actions.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton export = new JButton("Export CSV");
        export.addActionListener(e -> exportCsv(records));
        JButton folder = new JButton("Open data folder");
        folder.addActionListener(e -> open(store.directory()));
        actions.add(export);
        actions.add(folder);
        add(actions);

        if (records.isEmpty())
        {
            line("Deaths will appear here while you play.");
        }
        for (DeathRecord record : records)
        {
            addCard(record);
        }
        revalidate();
        repaint();
    }

    private void addCard(DeathRecord record)
    {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(BorderFactory.createEmptyBorder(6, 5, 6, 5));
        String when = DATE.format(Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault()));
        String actual = record.actualTotal() == null ? "pending" : gp(record.actualTotal());
        JToggleButton toggle = new JToggleButton("▸ " + when + "  |  " + actual);
        toggle.setHorizontalAlignment(JToggleButton.LEFT);
        toggle.addActionListener(e -> {
            if (toggle.isSelected())
            {
                toggle.setText("▾ " + when + "  |  " + actual);
                card.add(details(record), BorderLayout.CENTER);
            }
            else
            {
                toggle.setText("▸ " + when + "  |  " + actual);
                if (card.getComponentCount() > 1)
                {
                    card.remove(1);
                }
            }
            card.revalidate();
            card.repaint();
        });
        card.add(toggle, BorderLayout.NORTH);
        add(card);
    }

    private JPanel details(DeathRecord record)
    {
        JPanel detail = new JPanel();
        detail.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        addText(detail, "World " + record.world + "  •  Region " + record.region);
        addText(detail, "Tile " + record.x + ", " + record.y + ", plane " + record.plane + (record.instance ? " (instance)" : ""));
        addText(detail, "Opponent: " + (record.opponent == null ? "unknown" : record.opponent));
        addText(detail, "Carried value: " + gp(record.carriedValue));
        addText(detail, "Estimated grave fee: " + (record.estimatedGraveFee == null ? "unknown" : gp(record.estimatedGraveFee)));
        addText(detail, record.estimateNote == null ? "" : record.estimateNote);
        addText(detail, "Actual reclaim: " + optional(record.actualReclaimFee));
        addText(detail, "Unrecovered items: " + optional(record.actualItemLoss));
        addText(detail, "Other costs: " + optional(record.actualOtherCost));
        addText(detail, "Actual total: " + (record.actualTotal() == null ? "pending" : gp(record.actualTotal())));
        addText(detail, "Method: " + (record.reclaimMethod == null ? "not recorded" : record.reclaimMethod));
        if (record.notes != null && !record.notes.isEmpty())
        {
            addText(detail, "Notes: " + record.notes);
        }
        items(detail, "WORN EQUIPMENT", record.equipment);
        items(detail, "INVENTORY", record.inventory);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JButton actual = new JButton("Record actual cost");
        actual.addActionListener(e -> editActual(record));
        buttons.add(actual);
        if (record.screenshot != null)
        {
            JButton shot = new JButton("Open screenshot");
            shot.addActionListener(e -> open(store.imagePath(record)));
            buttons.add(shot);
        }
        detail.add(buttons);
        return detail;
    }

    private void editActual(DeathRecord record)
    {
        JTextField reclaim = new JTextField(value(record.actualReclaimFee));
        JTextField loss = new JTextField(value(record.actualItemLoss));
        JTextField other = new JTextField(value(record.actualOtherCost));
        JComboBox<String> method = new JComboBox<>(new String[]{"Gravestone", "Death's Office", "Item retrieval service", "PvP / Wilderness", "Safe death", "Other"});
        if (record.reclaimMethod != null)
        {
            method.setSelectedItem(record.reclaimMethod);
        }
        JTextField notes = new JTextField(record.notes == null ? "" : record.notes);
        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.add(new JLabel("Reclaim fee (GP)")); form.add(reclaim);
        form.add(new JLabel("Lost items (GP)")); form.add(loss);
        form.add(new JLabel("Other costs (GP)")); form.add(other);
        form.add(new JLabel("Reclaim method")); form.add(method);
        form.add(new JLabel("Notes")); form.add(notes);
        int answer = JOptionPane.showConfirmDialog(this, form, "Record actual death cost", JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION)
        {
            return;
        }
        try
        {
            Long feeValue = parse(reclaim.getText());
            Long lossValue = parse(loss.getText());
            Long otherValue = parse(other.getText());
            record.actualReclaimFee = feeValue;
            record.actualItemLoss = lossValue;
            record.actualOtherCost = otherValue;
            record.reclaimMethod = (String) method.getSelectedItem();
            record.notes = notes.getText().trim();
            store.save();
            refresh();
        }
        catch (NumberFormatException ex)
        {
            JOptionPane.showMessageDialog(this, "Enter whole, nonnegative GP amounts (or leave blank).", "Invalid cost", JOptionPane.ERROR_MESSAGE);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, "Unable to save death journal: " + ex.getMessage(), "Save error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Long parse(String text)
    {
        String cleaned = text.replace(",", "").trim();
        if (cleaned.isEmpty())
        {
            return null;
        }
        long value = Long.parseLong(cleaned);
        if (value < 0)
        {
            throw new NumberFormatException();
        }
        return value;
    }

    private void items(JPanel parent, String title, List<TrackedItem> items)
    {
        addText(parent, title);
        if (items == null || items.isEmpty())
        {
            addText(parent, "  None captured");
            return;
        }
        for (TrackedItem item : items)
        {
            addText(parent, "  " + item.name + " × " + item.quantity + "  (" + gp(item.value()) + ")");
        }
    }

    private void sectionLabel(String value)
    {
        JLabel label = new JLabel(value);
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        add(label);
    }

    private void line(String value)
    {
        addText(this, value);
    }

    private static void addText(JPanel parent, String text)
    {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        parent.add(area);
    }

    private static String gp(long amount)
    {
        return String.format(Locale.US, "%,d GP", amount);
    }

    private static String optional(Long amount)
    {
        return amount == null ? "not recorded" : gp(amount);
    }

    private static String value(Long amount)
    {
        return amount == null ? "" : Long.toString(amount);
    }

    private void open(Path path)
    {
        try
        {
            if (path == null || !Files.exists(path) || !Desktop.isDesktopSupported())
            {
                throw new IOException("File is not available yet");
            }
            Desktop.getDesktop().open(path.toFile());
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Open failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv(List<DeathRecord> records)
    {
        Path directory = store.directory();
        if (directory == null)
        {
            return;
        }
        StringBuilder csv = new StringBuilder("date,player,world,x,y,plane,region,opponent,carried_value,estimated_grave_fee,actual_reclaim_fee,actual_item_loss,actual_other_cost,actual_total,reclaim_method,notes\n");
        for (DeathRecord r : records)
        {
            String[] columns = {Instant.ofEpochMilli(r.timestamp).toString(), r.player,
                Integer.toString(r.world), Integer.toString(r.x), Integer.toString(r.y), Integer.toString(r.plane),
                Integer.toString(r.region), r.opponent, Long.toString(r.carriedValue), value(r.estimatedGraveFee),
                value(r.actualReclaimFee), value(r.actualItemLoss), value(r.actualOtherCost), value(r.actualTotal()),
                r.reclaimMethod, r.notes};
            for (int i = 0; i < columns.length; i++)
            {
                if (i > 0) csv.append(',');
                csv.append('"').append(columns[i] == null ? "" : columns[i].replace("\"", "\"\"")).append('"');
            }
            csv.append('\n');
        }
        Path target = directory.resolve("deaths.csv");
        try
        {
            Files.write(target, csv.toString().getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this, "Exported to " + target);
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
