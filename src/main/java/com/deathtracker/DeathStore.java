package com.deathtracker;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.client.util.Filepath;

/** Local, per-character JSON storage. Screenshots live in the same directory. */
final class DeathStore
{
    private final Gson gson;
    private final Filepath root;
    private Filepath directory;
    private final List<DeathRecord> records = new ArrayList<>();

    DeathStore(Gson gson, Filepath root)
    {
        this.gson = gson;
        this.root = root;
    }

    synchronized void open(String player) throws IOException
    {
        String safe = player == null ? "unknown" : player.replaceAll("[^A-Za-z0-9 _-]", "_").trim();
        if (safe.isEmpty())
        {
            safe = "unknown";
        }
        Filepath nextDirectory = root.joinSegment(safe.toLowerCase(java.util.Locale.ROOT)).rooted();
        nextDirectory.createDirectories();
        List<DeathRecord> nextRecords = new ArrayList<>();
        Filepath journal = nextDirectory.joinSegment("deaths.json");
        if (journal.exists())
        {
            try (Reader reader = journal.openBufferedReader())
            {
                DeathRecord[] loaded = gson.fromJson(reader, DeathRecord[].class);
                if (loaded != null)
                {
                    Collections.addAll(nextRecords, loaded);
                }
            }
        }
        directory = nextDirectory;
        records.clear();
        records.addAll(nextRecords);
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
        Filepath journal = directory.joinSegment("deaths.json");
        Filepath pending = directory.joinSegment("deaths.json.tmp");
        try (Writer writer = pending.openBufferedWriter())
        {
            gson.toJson(records.toArray(new DeathRecord[0]), DeathRecord[].class, writer);
        }
        try
        {
            pending.moveTo(journal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (java.nio.file.AtomicMoveNotSupportedException ex)
        {
            pending.moveTo(journal, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    synchronized List<DeathRecord> records()
    {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    synchronized Filepath imagePath(DeathRecord record)
    {
        return directory == null ? null : directory.joinSegment(record.id + ".png");
    }

    synchronized Filepath directory()
    {
        return directory;
    }
}
