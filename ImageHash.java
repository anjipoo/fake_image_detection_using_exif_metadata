//create hash of images using pixels

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageHash {

    private static final int SIZE = 32;

    public static long hash(File file) throws Exception {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new Exception("Cannot read: " + file.getName());
        return hash(img);
    }

    public static long hash(BufferedImage orig) {
        // Resize to 32x32
        BufferedImage small = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.drawImage(orig.getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();

        // Grayscale pixels
        int[] pixels = new int[SIZE * SIZE];
        long sum = 0;
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++) {
                int rgb = small.getRGB(x, y);
                int gray = (((rgb>>16)&0xFF) + ((rgb>>8)&0xFF) + (rgb&0xFF)) / 3;
                pixels[y*SIZE+x] = gray;
                sum += gray;
            }

        // Average hash: 1 if pixel >= mean
        long avg = sum / (SIZE * SIZE);
        long hash = 0;
        for (int i = 0; i < 64; i++) {   // use first 64 pixels for a 64-bit hash
            if (pixels[i] >= avg) hash |= (1L << i);
        }
        return hash;
    }

    // Hamming distance between two 64-bit hashes
    public static int hamming(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    // Similarity 0–100%
    public static double similarity(long a, long b) {
        return (1.0 - hamming(a, b) / 64.0) * 100.0;
    }
}