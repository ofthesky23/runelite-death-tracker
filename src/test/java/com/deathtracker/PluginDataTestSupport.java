package com.deathtracker;

import java.io.IOException;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Filepath;
import org.junit.After;
import org.junit.Before;

/** Uses a fresh test-only directory through the same supported API as the plugin. */
public abstract class PluginDataTestSupport
{
    private Filepath testArea;
    protected Filepath root;

    @Before
    public void createTestDirectory() throws IOException
    {
        testArea = new TestStoragePlugin().directory();
        testArea.createDirectories();
        root = testArea.createTempDir("test-").rooted();
    }

    @After
    public void removeTestDirectory() throws IOException
    {
        if (root != null)
        {
            // Never delete the shared test area or any existing player directory.
            if (root.equals(testArea) || !root.startsWith(testArea)
                || !root.getFileName().startsWith("test-"))
            {
                throw new IllegalStateException("Refusing to delete an unexpected test path");
            }
            root.deleteRecursively();
        }
    }

    @PluginDescriptor(name = "Death Tracker test storage", internalName = "death-tracker-regression-tests")
    private static final class TestStoragePlugin extends Plugin
    {
        Filepath directory() throws IOException
        {
            return getPluginDirectory();
        }
    }
}
