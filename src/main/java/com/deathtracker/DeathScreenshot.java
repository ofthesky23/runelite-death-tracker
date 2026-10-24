package com.deathtracker;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import net.runelite.client.util.Filepath;

/** PNG access stays within the plugin directory; ImageIO never uses a disk cache. */
final class DeathScreenshot
{
    private DeathScreenshot()
    {
    }

    static void save(Image image, Filepath path) throws IOException
    {
        BufferedImage copy = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        try
        {
            graphics.drawImage(image, 0, 0, null);
        }
        finally
        {
            graphics.dispose();
        }
        try (OutputStream output = path.openOutputStream();
             MemoryCacheImageOutputStream png = new MemoryCacheImageOutputStream(output))
        {
            if (!ImageIO.write(copy, "png", png))
            {
                throw new IOException("PNG encoder is unavailable");
            }
        }
    }

    static BufferedImage read(Filepath path) throws IOException
    {
        if (path == null || !path.isFile())
        {
            throw new IOException("Screenshot is not available yet");
        }
        try (InputStream input = path.openInputStream();
             MemoryCacheImageInputStream png = new MemoryCacheImageInputStream(input))
        {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("png");
            if (!readers.hasNext())
            {
                throw new IOException("PNG decoder is unavailable");
            }
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(png, true, true);
                return reader.read(0);
            }
            finally
            {
                reader.dispose();
            }
        }
    }
}
