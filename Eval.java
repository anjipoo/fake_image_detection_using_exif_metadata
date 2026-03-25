public class Eval {
    public static void evaluate(int[] trueY, int[] predY) {

        int TP=0, TN=0, FP=0, FN=0;

        for (int i=0; i<trueY.length; i++) {

            if (trueY[i]==1 && predY[i]==1) TP++;
            if (trueY[i]==0 && predY[i]==0) TN++;
            if (trueY[i]==0 && predY[i]==1) FP++;
            if (trueY[i]==1 && predY[i]==0) FN++;
        }

        double accuracy=(double)(TP + TN)/trueY.length;
        double precision=TP/(double)(TP + FP);
        double recall=TP/(double)(TP + FN);
        double f1=2*precision*recall/(precision+recall);

        System.out.println("Accuracy: "+accuracy);
        System.out.println("Precision: "+precision);
        System.out.println("Recall: "+recall);
        System.out.println("F1 Score: "+f1);

        System.out.println("\nConfusion Matrix");

        System.out.println("        Pred0  Pred1");
        System.out.println("True0   "+TN +"      "+FP);
        System.out.println("True1   "+FN+"      "+TP);
    }
}
