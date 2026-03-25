import java.util.Random;

public class LogisticRegression {

    double[] weights;
    double lr=0.01;
    int epochs=1000;

    public void train(double[][] X, int[] y) {

        int n=X.length;
        int m=X[0].length;

        weights=new double[m+1
            
        ];

        Random r=new Random();

        for (int i=0; i<m; i++)
            weights[i]=r.nextDouble() * 0.01;

        for (int e=0; e<epochs; e++) {

            for (int i=0; i<n; i++) {

                double pred=predictProb(X[i]);

                double error=pred-y[i];

                for (int j=0; j<m; j++)
                    weights[j]-=lr*error*X[i][j];
                weights[m] -= lr * error;
            }
        }
    }

    public double predictProb(double[] x) {

        double z = weights[weights.length - 1]; // bias

        for (int i = 0; i < x.length; i++)
            z += weights[i] * x[i];

        return 1.0 / (1.0 + Math.exp(-z));
    }


    public int predict(double[] x) {

        return predictProb(x)>=0.5 ? 1:0;
    }
}