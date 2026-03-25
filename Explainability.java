public class Explainability {

    public static void explain(double[] features, double[] weights) {

        System.out.println("\nFeature Contributions:");

        int n = Math.min(features.length, weights.length);

        for (int i = 0; i < n; i++) {
            double contribution = weights[i] * features[i];
            System.out.println("Feature " + i + " contribution: " + contribution);
        }

        // Optional: print bias contribution if weights has extra element
        if (weights.length > features.length) {
            double bias = weights[weights.length - 1];
            System.out.println("Bias contribution: " + bias);
        }
    }
}
