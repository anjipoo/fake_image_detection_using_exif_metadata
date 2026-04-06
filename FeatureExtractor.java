//extract metadata and pixel stats

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.Directory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class FeatureExtractor {

    public static double[] extract(File file) throws Exception {
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new Exception("Cannot read image: " + file.getName());

        double entropy     = calcEntropy(img);
        double laplacian   = calcLaplacian(img);
        double edgeDensity = calcEdgeDensity(img);
        double[] meanRGB   = calcMeanRGB(img);
        double[] varRGB    = calcVarRGB(img, meanRGB);
        double blockiness  = calcBlockiness(img);
        double missing     = metadataMissing(file) ? 1.0 : 0.0;

        return new double[]{
            entropy, laplacian, edgeDensity,
            meanRGB[0], meanRGB[1], meanRGB[2],
            varRGB[0],  varRGB[1],  varRGB[2],
            blockiness, missing
        };
    }

    // Names matching CSV header order
    public static String[] featureNames() {
        return new String[]{
            "entropy","laplacian","edgeDensity",
            "meanR","meanG","meanB",
            "varR","varG","varB",
            "blockiness","metadataMissing"
        };
    }

    // Shannon entropy on grayscale histogram
    static double calcEntropy(BufferedImage img) {
        int[] hist = new int[256];
        int w = img.getWidth(), h = img.getHeight();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int gray = (((rgb>>16)&0xFF) + ((rgb>>8)&0xFF) + (rgb&0xFF)) / 3;
                hist[gray]++;
            }
        double total = w * h, e = 0;
        for (int v : hist) {
            if (v > 0) { double p = v / total; e -= p * Math.log(p) / Math.log(2); }
        }
        return e;
    }

    // Average Laplacian magnitude (sharpness)
    static double calcLaplacian(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[][] gray = toGray(img);
        double sum = 0; int cnt = 0;
        for (int y = 1; y < h-1; y++)
            for (int x = 1; x < w-1; x++) {
                double lap = -gray[y-1][x] - gray[y+1][x] - gray[y][x-1] - gray[y][x+1] + 4*gray[y][x];
                sum += Math.abs(lap); cnt++;
            }
        return cnt > 0 ? sum / cnt : 0;
    }

    // Fraction of pixels that are edges (simple Sobel threshold)
    static double calcEdgeDensity(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[][] gray = toGray(img);
        int edges = 0, total = 0;
        for (int y = 1; y < h-1; y++)
            for (int x = 1; x < w-1; x++) {
                int gx = -gray[y-1][x-1] + gray[y-1][x+1] - 2*gray[y][x-1] + 2*gray[y][x+1] - gray[y+1][x-1] + gray[y+1][x+1];
                int gy = -gray[y-1][x-1] - 2*gray[y-1][x] - gray[y-1][x+1] + gray[y+1][x-1] + 2*gray[y+1][x] + gray[y+1][x+1];
                if (Math.sqrt(gx*gx + gy*gy) > 30) edges++;
                total++;
            }
        return total > 0 ? (double) edges / total : 0;
    }

    static double[] calcMeanRGB(BufferedImage img) {
        long r=0,g=0,b=0; int n = img.getWidth()*img.getHeight();
        for (int y=0;y<img.getHeight();y++) for (int x=0;x<img.getWidth();x++) {
            int rgb=img.getRGB(x,y); r+=(rgb>>16)&0xFF; g+=(rgb>>8)&0xFF; b+=rgb&0xFF;
        }
        return new double[]{(double)r/n,(double)g/n,(double)b/n};
    }

    static double[] calcVarRGB(BufferedImage img, double[] mean) {
        double vr=0,vg=0,vb=0; int n=img.getWidth()*img.getHeight();
        for (int y=0;y<img.getHeight();y++) for (int x=0;x<img.getWidth();x++) {
            int rgb=img.getRGB(x,y);
            double dr=((rgb>>16)&0xFF)-mean[0], dg=((rgb>>8)&0xFF)-mean[1], db=(rgb&0xFF)-mean[2];
            vr+=dr*dr; vg+=dg*dg; vb+=db*db;
        }
        return new double[]{vr/n,vg/n,vb/n};
    }

    // JPEG blockiness: average absolute difference at 8-pixel boundaries
    static double calcBlockiness(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[][] gray = toGray(img);
        double sum = 0; int cnt = 0;
        for (int y = 0; y < h; y++)
            for (int x = 8; x < w; x += 8) {
                sum += Math.abs(gray[y][x] - gray[y][x-1]); cnt++;
            }
        for (int x = 0; x < w; x++)
            for (int y = 8; y < h; y += 8) {
                sum += Math.abs(gray[y][x] - gray[y-1][x]); cnt++;
            }
        return cnt > 0 ? sum / cnt : 0;
    }

    static boolean metadataMissing(File file) {
        try {
            Metadata meta = ImageMetadataReader.readMetadata(file);
            boolean hasMake  = meta.getFirstDirectoryOfType(ExifIFD0Directory.class) != null &&
                               meta.getFirstDirectoryOfType(ExifIFD0Directory.class).containsTag(ExifIFD0Directory.TAG_MAKE);
            boolean hasModel = meta.getFirstDirectoryOfType(ExifIFD0Directory.class) != null &&
                               meta.getFirstDirectoryOfType(ExifIFD0Directory.class).containsTag(ExifIFD0Directory.TAG_MODEL);
            boolean hasDate  = meta.getFirstDirectoryOfType(ExifSubIFDDirectory.class) != null;
            return !(hasMake || hasModel || hasDate);
        } catch (Exception e) { return true; }
    }

    static int[][] toGray(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[][] g = new int[h][w];
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            int rgb=img.getRGB(x,y);
            g[y][x]=(((rgb>>16)&0xFF)+((rgb>>8)&0xFF)+(rgb&0xFF))/3;
        }
        return g;
    }
}