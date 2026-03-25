import java.util.Random;

public class Main {

    // Shuffle dataset
    static void shuffle(double[][] X, int[] y) {

        Random rand = new Random(42);

        for (int i = X.length - 1; i > 0; i--) {

            int j = rand.nextInt(i + 1);

            double[] tempX = X[i];
            X[i] = X[j];
            X[j] = tempX;

            int tempY = y[i];
            y[i] = y[j];
            y[j] = tempY;
        }
    }


    // Normalize features (min-max scaling)
    static double[] minVals;
    static double[] maxVals;

    static void normalize(double[][] X) {

        int n = X.length;
        int m = X[0].length;

        minVals = new double[m];
        maxVals = new double[m];

        for (int j = 0; j < m; j++) {

            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                min = Math.min(min, X[i][j]);
                max = Math.max(max, X[i][j]);
            }

            minVals[j] = min;
            maxVals[j] = max;

            for (int i = 0; i < n; i++) {
                if (max - min != 0)
                    X[i][j] = (X[i][j] - min) / (max - min);
            }
        }
    }



    public static void main(String[] args) throws Exception {

        CSVLoader.DataSet data = CSVLoader.load("mpj_data.csv");

        shuffle(data.X, data.y);

        normalize(data.X);

        LogisticRegression model = new LogisticRegression();

        model.train(data.X, data.y);

        System.out.println("\nModel trained successfully.");

        // Example unseen image feature vector
        double[] unseen = {

                1,1,1,1,1,0,0,

                4.334531928459312,
                1.2207578808191872,
                4.327719103652263E-4,

                16.262763003753538,
                9.916737874348959,
                6.215604428891782,

                632.1675280514794,
                236.1119475687474,
                146.6718079767289
        };

        // Simple scaling for unseen row (to avoid extreme values)
        for (int j = 0; j < unseen.length; j++) {
            if (maxVals[j] - minVals[j] != 0)
                unseen[j] = (unseen[j] - minVals[j]) / (maxVals[j] - minVals[j]);
        }


        double probability = model.predictProb(unseen);

        int prediction = model.predict(unseen);

        System.out.println("\n--- Unseen Image Prediction ---");

        System.out.println("Probability FAKE: " + probability);

        System.out.println("Prediction: " + (prediction == 1 ? "FAKE" : "GENUINE"));

        System.out.println("\nFeature Contributions:");

        Explainability.explain(unseen, model.weights);
    }
}