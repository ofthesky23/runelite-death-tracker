package com.deathtracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.client.RuneLite;

/** Local, per-character JSON storage. Screenshots live in the same directory. */
final class DeathStore
{
    private static final Type RECORDS_TYPE = new TypeToken<List<DeathRecord>>() {}.getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path directory;
    private final List<DeathRecord> records = new ArrayList<>();

    synchronized void open(String player) throws IOException
    {
        String safe = player == null ? "unknown" : player.replaceAll("[^A-Za-z0-9 _-]", "_").trim();
        if (safe.isEmpty())
        {
            safe = "unknown";
        }
        directory = RuneLite.PLUGIN_DATA.resolve("death-tracker").resolve(safe.toLowerCase(java.util.Locale.ROOT));
        Files.createDirectories(directory);
        records.clear();
        Path journal = directory.resolve("deaths.json");
        if (Files.exists(journal))
        {
            try (Reader reader = Files.newBufferedReader(journal, StandardCharsets.UTF_8))
            {
                List<DeathRecord> loaded = gson.fromJson(reader, RECORDS_TYPE);
                if (loaded != null)
                {
                    records.addAll(loaded);
                }
            }
        }
    }

    synchronized void close()
    {
        directory = null;
        records.clear();
    }

    synchronized void add(DeathRecord record) throws IOException
    {
        records.add(0, record);
        save();
    }

    synchronized void save() throws IOException
    {
        if (directory == null)
        {
            return;
        }
        Path journal = directory.resolve("deaths.json");
        Path pending = directory.resolve("deaths.json.tmp");
        try (Writer writer = Files.newBufferedWriter(pending, StandardCharsets.UTF_8))
        {
            gson.toJson(records, RECORDS_TYPE, writer);
        }
        try
        {
            Files.move(pending, journal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (java.nio.file.AtomicMoveNotSupportedException ex)
        {
            Files.move(pending, journal, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    synchronized List<DeathRecord> records()
    {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    synchronized Path imagePath(DeathRecord record)
    {
        return directory.resolve(record.id + ".png");
    }

    synchronized Path directory()
    {
        return directory;
    }
}
