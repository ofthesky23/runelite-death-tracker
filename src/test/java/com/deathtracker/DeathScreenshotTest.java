package com.deathtracker;

import java.awt.image.BufferedImage;
import java.io.IOException;
import net.runelite.client.util.Filepath;
import org.junit.Assert;
import org.junit.Test;

public class DeathScreenshotTest extends PluginDataTestSupport
{
    @Test
    public void savesAndReadsPngPixels() throws IOException
    {
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xff223344);
        image.setRGB(2, 1, 0xffabcdef);
        Filepath path = root.joinSegment("screenshot.png");
        DeathScreenshot.save(image, path);
        Assert.assertTrue(path.isFile());

        BufferedImage read = DeathScreenshot.read(path);
        Assert.assertEquals(image.getWidth(), read.getWidth());
        Assert.assertEquals(image.getHeight(), read.getHeight());
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                Assert.assertEquals(image.getRGB(x, y), read.getRGB(x, y));
            }
        }
    }

    @Test(expected = IOException.class)
    public void rejectsMalformedImage() throws IOException
    {
        Filepath path = root.joinSegment("invalid.png");
        path.write("This is not an image.");
        DeathScreenshot.read(path);
    }

    @Test(expected = IOException.class)
    public void rejectsMissingImage() throws IOException
    {
        DeathScreenshot.read(root.joinSegment("missing.png"));
    }

    @Test(expected = IOException.class)
    public void rejectsUnavailablePath() throws IOException
    {
        DeathScreenshot.read(null);
    }

    @Test(expected = IOException.class)
    public void rejectsDirectoryAsImage() throws IOException
    {
        DeathScreenshot.read(root);
    }
}
