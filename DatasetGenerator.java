//make dataset from au/ and tp/

import java.io.*;
import java.util.*;

public class DatasetGenerator {

    private static final String[] EXTS={".jpg",".jpeg",".png",".JPG",".JPEG",".PNG"};
    public static final String CSV="mpj_data.csv";

    public static void generate(String auDir, String tpDir) throws Exception {
        List<String> rows = new ArrayList<>();
        rows.add("image_name,entropy,laplacian,edgeDensity,meanR,meanG,meanB,varR,varG,varB,blockiness,metadataMissing,label");

        processFolder(new File(auDir), 0, rows);
        processFolder(new File(tpDir), 1, rows);

        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV))) {
            for (String r : rows) pw.println(r);
        }
        System.out.println("Dataset saved: " + CSV + " (" + (rows.size()-1) + " images)");
    }

    static void processFolder(File dir, int label, List<String> rows) {
        if (!dir.exists()) { System.out.println("Folder not found: " + dir); return; }
        for (File f : Objects.requireNonNull(dir.listFiles())) {
            if (!isImage(f.getName())) continue;
            try {
                double[] feat = FeatureExtractor.extract(f);
                StringBuilder sb = new StringBuilder(f.getName());
                for (double v : feat) sb.append(",").append(String.format("%.6f", v));
                sb.append(",").append(label);
                rows.add(sb.toString());
                System.out.println("Processed: " + f.getName());
            } catch (Exception e) {
                System.out.println("Skipped " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    static boolean isImage(String name) {
        String lc = name.toLowerCase();
        return lc.endsWith(".jpg") || lc.endsWith(".jpeg") || lc.endsWith(".png");
    }

    //Load CSV → double[][] features, int[] labels
    public static Object[] load() throws Exception {
        List<double[]> feats = new ArrayList<>();
        List<Integer>  labels = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(CSV))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length < 13) continue;
                double[] f = new double[11];
                for (int i = 0; i < 11; i++) f[i] = Double.parseDouble(p[i+1]);
                feats.add(f);
                labels.add(Integer.parseInt(p[12]));
            }
        }
        double[][] X = feats.toArray(new double[0][]);
        int[] y = labels.stream().mapToInt(Integer::intValue).toArray();
        return new Object[]{X, y};
    }
}