//logreg model for prediction

import java.io.*;
import java.util.*;

public class LogisticRegression {

    double[] weights;
    double   bias;
    double[] minVal, maxVal;
    int      numFeatures;

    public LogisticRegression(int n) {
        numFeatures = n;
        weights = new double[n];
        bias    = 0;
        minVal  = new double[n];
        maxVal  = new double[n];
    }

    // Min-max normalization — fit on training data
    public double[][] fitNormalize(double[][] X) {
        Arrays.fill(minVal, Double.MAX_VALUE);
        Arrays.fill(maxVal, -Double.MAX_VALUE);
        for (double[] row : X)
            for (int j = 0; j < numFeatures; j++) {
                if (row[j] < minVal[j]) minVal[j] = row[j];
                if (row[j] > maxVal[j]) maxVal[j] = row[j];
            }
        return applyNormalize(X);
    }

    public double[] normalizeOne(double[] x) {
        double[] out = new double[numFeatures];
        for (int j = 0; j < numFeatures; j++) {
            double range = maxVal[j] - minVal[j];
            out[j] = range == 0 ? 0 : (x[j] - minVal[j]) / range;
        }
        return out;
    }

    double[][] applyNormalize(double[][] X) {
        double[][] N = new double[X.length][numFeatures];
        for (int i = 0; i < X.length; i++) N[i] = normalizeOne(X[i]);
        return N;
    }

    static double sigmoid(double z) { return 1.0 / (1.0 + Math.exp(-z)); }

    public void train(double[][] X, int[] y, double lr, int epochs) {
        int m = X.length;
        for (int ep = 0; ep < epochs; ep++) {
            double[] dw = new double[numFeatures];
            double db = 0;
            for (int i = 0; i < m; i++) {
                double z = bias; for (int j=0;j<numFeatures;j++) z += weights[j]*X[i][j];
                double err = sigmoid(z) - y[i];
                for (int j=0;j<numFeatures;j++) dw[j] += err * X[i][j];
                db += err;
            }
            for (int j=0;j<numFeatures;j++) weights[j] -= lr * dw[j] / m;
            bias -= lr * db / m;
        }
    }

    public double predict(double[] xNorm) {
        double z = bias;
        for (int j=0;j<numFeatures;j++) z += weights[j]*xNorm[j];
        return sigmoid(z);
    }

    // Save: first line = min, second = max, third = weights + bias
    public void save(String path) throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(path))) {
            pw.println(join(minVal));
            pw.println(join(maxVal));
            double[] wb = Arrays.copyOf(weights, numFeatures+1);
            wb[numFeatures] = bias;
            pw.println(join(wb));
        }
    }

    public static LogisticRegression load(String path) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            double[] min = parseLine(br.readLine());
            double[] max = parseLine(br.readLine());
            double[] wb  = parseLine(br.readLine());
            int n = min.length;
            LogisticRegression lr = new LogisticRegression(n);
            lr.minVal  = min; lr.maxVal = max;
            lr.weights = Arrays.copyOf(wb, n);
            lr.bias    = wb[n];
            return lr;
        }
    }

    static String join(double[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<a.length;i++) { if(i>0) sb.append(","); sb.append(a[i]); }
        return sb.toString();
    }
    static double[] parseLine(String s) {
        String[] t = s.split(","); double[] a = new double[t.length];
        for (int i=0;i<t.length;i++) a[i] = Double.parseDouble(t[i]);
        return a;
    }
}