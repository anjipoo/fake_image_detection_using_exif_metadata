import java.sql.*;

public class SimSearch {

    public static void find(String newHash) throws Exception {

        Connection conn = DBHelper.connect();

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT image_name, hash FROM images");

        String bestImage = "";
        int bestDist = Integer.MAX_VALUE;

        while (rs.next()) {

            String dbHash = rs.getString("hash");

            int dist = hamming(newHash, dbHash);

            if (dist < bestDist) {
                bestDist = dist;
                bestImage = rs.getString("image_name");
            }
        }

        System.out.println("Closest match: " + bestImage);
        System.out.println("Distance: " + bestDist);

        conn.close();
    }

    static int hamming(String a, String b) {

        int count = 0;

        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i))
                count++;
        }

        return count;
    }
}