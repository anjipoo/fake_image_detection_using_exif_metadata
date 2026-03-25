import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Color;

public class Processing {
    public static BufferedImage load(String path) throws IOException {
        File file=new File(path);
        BufferedImage img=ImageIO.read(file);
        if (img==null) throw new IOException("imvalid image");
        return img;
    }

    public static BufferedImage resize(BufferedImage og, int maxwidth) {
        if (og.getWidth()<=maxwidth) {
            return og;
        }

        int newwidth=maxwidth;
        int newheight=(og.getHeight()*maxwidth)/og.getWidth();

        Image temp=og.getScaledInstance(newwidth, newheight, Image.SCALE_SMOOTH);
        BufferedImage resized=new BufferedImage(newwidth, newheight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d=resized.createGraphics();
        g2d.drawImage(temp, 0, 0, null);
        g2d.dispose();

        return resized;
    }

    public static BufferedImage gray(BufferedImage og) {

        int width=og.getWidth();
        int height=og.getHeight();

        BufferedImage grayimg=new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {

                Color color=new Color(og.getRGB(x, y));

                int red=color.getRed();
                int green=color.getGreen();
                int blue=color.getBlue();

                int gray=(int)(0.299 * red + 0.587 * green + 0.114 * blue);

                int newPixel=new Color(gray, gray, gray).getRGB();
                grayimg.setRGB(x, y, newPixel);
            }
        }

        return grayimg;
    }

    public static void main(String[] args) throws IOException {

        BufferedImage img=load("numberplate.jpg");

        System.out.println("original width: " + img.getWidth());
        System.out.println("original height: " + img.getHeight());

        img = resize(img, 800);

        BufferedImage grayimg=gray(img);

        System.out.println("preprocessing completed.");
    }
}