import java.io.*;
import java.util.*;

public class CSVLoader {

    public static class DataSet {
        double[][] X;
        int[] y;
    }

    public static DataSet load(String path) throws Exception {

        BufferedReader br=new BufferedReader(new FileReader(path));

        List<double[]> features=new ArrayList<>();
        List<Integer> labels=new ArrayList<>();

        String line;

        br.readLine(); // skip header

        while ((line = br.readLine()) != null) {

            String[] parts = line.split("[,\t]");

            int featureCount = parts.length - 2;

            double[] row = new double[featureCount];

            for (int i = 1; i <= featureCount; i++)
                row[i - 1] = Double.parseDouble(parts[i]);

            int label = Integer.parseInt(parts[parts.length - 1]);

            features.add(row);
            labels.add(label);
        }
        
        br.close();

        DataSet ds=new DataSet();

        ds.X=features.toArray(new double[0][]);
        ds.y=labels.stream().mapToInt(i->i).toArray();

        return ds;
    }
}