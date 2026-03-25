import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.*;
import com.drew.metadata.exif.GpsDirectory;

public class DatasetGenerator {

    // ================= METADATA =================
    static class Meta {
        double hasMake, hasModel, hasDateTime, hasGPS, hasSoftware, softwareIsEditor, metadataMissing;
    }

    static Meta extractMeta(String path) {
        Meta m = new Meta();

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(path));

            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                String make = safe(ifd0.getString(ExifIFD0Directory.TAG_MAKE));
                String model = safe(ifd0.getString(ExifIFD0Directory.TAG_MODEL));
                String software = safe(ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE));
                String date = safe(ifd0.getString(ExifIFD0Directory.TAG_DATETIME));

                if (!make.isEmpty()) m.hasMake = 1;
                if (!model.isEmpty()) m.hasModel = 1;
                if (!software.isEmpty()) m.hasSoftware = 1;
                if (!date.isEmpty()) m.hasDateTime = 1;

                if (!software.isEmpty() && looksLikeEditor(software))
                    m.softwareIsEditor = 1;
            }

            ExifSubIFDDirectory sub = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (sub != null) {
                String dt = safe(sub.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
                if (!dt.isEmpty()) m.hasDateTime = 1;
            }

            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null) m.hasGPS = 1;

        } catch (Exception e) {
            System.out.println("Metadata error for " + path + ": " + e.getMessage());
        }

        double total = m.hasMake + m.hasModel + m.hasDateTime + m.hasGPS + m.hasSoftware;
        m.metadataMissing = (total == 0) ? 1 : 0;

        return m;
    }

    static boolean looksLikeEditor(String s) {
        s = s.toLowerCase();
        return s.contains("photoshop") || s.contains("gimp") ||
               s.contains("lightroom") || s.contains("canva");
    }

    static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    // ================= PIXEL =================
    static class Pixel {
        double entropy, lap, edge, meanR, meanG, meanB, varR, varG, varB;
    }

    static Pixel extractPixel(String path) {
        Pixel p = new Pixel();

        try {
            BufferedImage img = ImageIO.read(new File(path));
            int w = img.getWidth(), h = img.getHeight();

            int[] hist = new int[256];
            double sr=0, sg=0, sb=0;

            for(int y=0;y<h;y++){
                for(int x=0;x<w;x++){
                    int px = img.getRGB(x,y);
                    int r=(px>>16)&255, g=(px>>8)&255, b=px&255;

                    sr+=r; sg+=g; sb+=b;
                    hist[(r+g+b)/3]++;
                }
            }

            double total = w*h;
            p.meanR = sr/total;
            p.meanG = sg/total;
            p.meanB = sb/total;

            double vr=0, vg=0, vb=0;

            for(int y=0;y<h;y++){
                for(int x=0;x<w;x++){
                    int px = img.getRGB(x,y);
                    int r=(px>>16)&255, g=(px>>8)&255, b=px&255;

                    vr += Math.pow(r-p.meanR,2);
                    vg += Math.pow(g-p.meanG,2);
                    vb += Math.pow(b-p.meanB,2);
                }
            }

            p.varR = vr/total;
            p.varG = vg/total;
            p.varB = vb/total;

            for(int i=0;i<256;i++){
                if(hist[i]==0) continue;
                double prob = hist[i]/total;
                p.entropy -= prob * (Math.log(prob)/Math.log(2));
            }

            int[][] lapK = {{0,-1,0},{-1,4,-1},{0,-1,0}};
            double lapSum=0;

            for(int y=1;y<h-1;y++){
                for(int x=1;x<w-1;x++){
                    double val=0;
                    for(int ky=-1;ky<=1;ky++){
                        for(int kx=-1;kx<=1;kx++){
                            int px=img.getRGB(x+kx,y+ky);
                            int gray=((px>>16)&255 + (px>>8)&255 + (px&255))/3;
                            val += gray * lapK[ky+1][kx+1];
                        }
                    }
                    lapSum += Math.abs(val);
                }
            }

            p.lap = lapSum/total;

            int edgeCount=0;
            for(int y=1;y<h-1;y++){
                for(int x=1;x<w-1;x++){
                    int px=img.getRGB(x,y);
                    int gray=((px>>16)&255 + (px>>8)&255 + (px&255))/3;
                    if(gray>100) edgeCount++;
                }
            }

            p.edge = (double)edgeCount/total;

        } catch(Exception e){
            System.out.println("Pixel error for " + path + ": " + e.getMessage());
        }

        return p;
    }

    // ================= MAIN =================
    public static void main(String[] args) throws Exception {

        DBHelper.createTable();  // ✅ create SQLite table

        FileWriter fw = new FileWriter("mpj_data.csv");

        fw.write("image_id,hash,hasMake,hasModel,hasDateTime,hasGPS,hasSoftware,softwareIsEditor,metadataMissing,entropy,lap,edge,meanR,meanG,meanB,varR,varG,varB,label\n");

        processFolder("au", 0, fw);
        processFolder("tp", 1, fw);

        fw.close();

        System.out.println("Dataset created + stored in DB");
    }

    static void processFolder(String folderPath, int label, FileWriter fw) throws Exception {

        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files == null) {
            System.out.println("Folder not found or empty: " + folderPath);
            return;
        }

        for(File file : files){

            if(!file.getName().endsWith(".jpg") && !file.getName().endsWith(".png")) continue;

            String path = file.getAbsolutePath();

            Meta m = extractMeta(path);
            Pixel p = extractPixel(path);

            String hash = ImageHash.computeHash(path);

            try {
                // Insert into DB
                DBInsert.insert(file.getName(), hash, m, p, label);

                // Save CSV
                fw.write(file.getName()+","+hash+","+ 
                        m.hasMake+","+m.hasModel+","+m.hasDateTime+","+m.hasGPS+","+m.hasSoftware+","+m.softwareIsEditor+","+m.metadataMissing+","+ 
                        p.entropy+","+p.lap+","+p.edge+","+ 
                        p.meanR+","+p.meanG+","+p.meanB+","+ 
                        p.varR+","+p.varG+","+p.varB+","+ 
                        label+"\n");

                System.out.println("Processed: "+file.getName());

            } catch(Exception e) {
                System.out.println("Failed to insert/write " + file.getName() + ": " + e.getMessage());
            }
        }
    }
}
