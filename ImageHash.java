import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageHash {

    public static String computeHash(String path) throws Exception {

        BufferedImage img = ImageIO.read(new File(path));

        // Resize to 32x32
        Image tmp = img.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D g2 = resized.createGraphics();
        g2.drawImage(tmp, 0, 0, null);
        g2.dispose();

        int[] pixels = new int[32 * 32];
        resized.getRaster().getPixels(0, 0, 32, 32, pixels);

        double avg = 0;
        for (int p : pixels) avg += p;
        avg /= pixels.length;

        StringBuilder hash = new StringBuilder();
        for (int p : pixels) {
            hash.append(p > avg ? "1" : "0");
        }

        return hash.toString();
    }
}
