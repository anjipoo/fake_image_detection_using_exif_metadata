import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Usage:
 *   java Main train au tp          -- generate CSV + train model + index all images
 *   java Main predict <image_path> -- predict a single image
 */
public class Main {

    static final String MODEL_FILE = "model.csv";

    public static void main(String[] args) throws Exception {
        DBHelper.init();

        if (args.length == 0) {
            System.out.println("Usage: java Main train <au_dir> <tp_dir>");
            System.out.println("       java Main predict <image_path>");
            return;
        }

        if (args[0].equalsIgnoreCase("train")) {
            String auDir = args.length > 1 ? args[1] : "au";
            String tpDir = args.length > 2 ? args[2] : "tp";

            // 1. Generate dataset CSV
            DatasetGenerator.generate(auDir, tpDir);

            // 2. Load data
            Object[] data = DatasetGenerator.load();
            double[][] X = (double[][]) data[0];
            int[] y      = (int[])     data[1];

            if (X.length == 0) { System.out.println("No data found."); return; }

            // 3. Train model
            LogisticRegression model = new LogisticRegression(X[0].length);
            double[][] Xn = model.fitNormalize(X);
            model.train(Xn, y, 0.1, 1000);
            model.save(MODEL_FILE);
            System.out.println("Model saved: " + MODEL_FILE);

            // 4. Evaluate on training set
            int correct = 0;
            for (int i = 0; i < X.length; i++) {
                double prob = model.predict(Xn[i]);
                if ((prob >= 0.5 ? 1 : 0) == y[i]) correct++;
            }
            System.out.printf("Training accuracy: %.1f%%%n", 100.0 * correct / X.length);

            // 5. Index all images into DB
            indexImages(auDir, 0);
            indexImages(tpDir, 1);

        } else if (args[0].equalsIgnoreCase("predict")) {
            if (args.length < 2) { System.out.println("Provide image path."); return; }
            predictImage(new File(args[1]));
        } else {
            System.out.println("Unknown command: " + args[0]);
        }
    }

    static void indexImages(String dir, int label) throws Exception {
        File d = new File(dir);
        if (!d.exists()) return;
        for (File f : d.listFiles()) {
            if (!DatasetGenerator.isImage(f.getName())) continue;
            try {
                long h = ImageHash.hash(f);
                DBHelper.insert(f.getName(), h);
            } catch (Exception e) {
                System.out.println("Hash failed: " + f.getName());
            }
        }
        System.out.println("Indexed images from: " + dir);
    }

    public static PredictionResult predictImage(File file) throws Exception {
        // Load model
        LogisticRegression model = LogisticRegression.load(MODEL_FILE);

        // Extract + normalize features
        double[] raw   = FeatureExtractor.extract(file);
        double[] xNorm = model.normalizeOne(raw);

        // Predict
        double prob   = model.predict(xNorm);
        int    label  = prob >= 0.5 ? 1 : 0;
        String verdict = label == 1 ? "TAMPERED (Fake)" : "AUTHENTIC";
        double conf    = label == 1 ? prob : 1 - prob;

        System.out.println("\n=== Prediction ===");
        System.out.println("File   : " + file.getName());
        System.out.printf ("Result : %s  (confidence: %.1f%%)%n", verdict, conf * 100);

        // Explain
        Explainability.explain(model, xNorm, FeatureExtractor.featureNames());

        // Hash + similarity
        long queryHash = ImageHash.hash(file);
        Map<String, Long> db = DBHelper.getAll();
        SimSearch.Result sim = SimSearch.findClosest(queryHash, db);
        System.out.printf("%nClosest match: %s  (%.1f%% similar)%n", sim.name, sim.similarity);

        List<String> topFeatures = Explainability.explainList(model, xNorm, FeatureExtractor.featureNames());

        return new PredictionResult(verdict, conf * 100, raw, xNorm, topFeatures, sim.name, sim.similarity);
    }

    // Simple struct for UI consumption
    public static class PredictionResult {
        public final String verdict;
        public final double confidence;
        public final double[] rawFeatures;
        public final double[] normFeatures;
        public final List<String> topFeatures;
        public final String closestMatch;
        public final double similarity;

        public PredictionResult(String v, double c, double[] r, double[] n,
                                List<String> tf, String cm, double sim) {
            verdict=v; confidence=c; rawFeatures=r; normFeatures=n;
            topFeatures=tf; closestMatch=cm; similarity=sim;
        }
    }
}