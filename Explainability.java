//give feature importance

import java.util.*;

public class Explainability {

    // contribution = weight * normalizedFeatureValue
    public static void explain(LogisticRegression model, double[] xNorm, String[] names) {
        int n = model.numFeatures;
        double[] contrib = new double[n];
        for (int j = 0; j < n; j++) contrib[j] = model.weights[j] * xNorm[j];

        // sort by absolute contribution descending
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(Math.abs(contrib[b]), Math.abs(contrib[a])));

        System.out.println("\n--- Top 5 Feature Contributions ---");
        for (int k = 0; k < Math.min(5, n); k++) {
            int i = idx[k];
            System.out.printf("  %-20s  weight=%.4f  norm=%.4f  contrib=%.4f%n",
                names[i], model.weights[i], xNorm[i], contrib[i]);
        }
    }

    // Returns top 5 as formatted strings for UI
    public static List<String> explainList(LogisticRegression model, double[] xNorm, String[] names) {
        int n = model.numFeatures;
        double[] contrib = new double[n];
        for (int j = 0; j < n; j++) contrib[j] = model.weights[j] * xNorm[j];
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(Math.abs(contrib[b]), Math.abs(contrib[a])));
        List<String> result = new ArrayList<>();
        for (int k = 0; k < Math.min(5, n); k++) {
            int i = idx[k];
            result.add(String.format("%-20s contrib=%.4f", names[i], contrib[i]));
        }
        return result;
    }
}