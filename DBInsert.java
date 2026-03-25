import java.sql.*;

public class DBInsert {

    public static void insert(String name, String hash,
                              DatasetGenerator.Meta m,
                              DatasetGenerator.Pixel p,
                              int label) throws Exception {

        Connection conn = DBHelper.connect();

        // 20 columns: NULL for id + 19 placeholders
        String sql = "INSERT INTO images VALUES (NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setString(1, name);
        ps.setString(2, hash);

        ps.setDouble(3, m.hasMake);
        ps.setDouble(4, m.hasModel);
        ps.setDouble(5, m.hasDateTime);
        ps.setDouble(6, m.hasGPS);
        ps.setDouble(7, m.hasSoftware);
        ps.setDouble(8, m.softwareIsEditor);
        ps.setDouble(9, m.metadataMissing);

        ps.setDouble(10, p.entropy);
        ps.setDouble(11, p.lap);
        ps.setDouble(12, p.edge);

        ps.setDouble(13, p.meanR);
        ps.setDouble(14, p.meanG);
        ps.setDouble(15, p.meanB);

        ps.setDouble(16, p.varR);
        ps.setDouble(17, p.varG);
        ps.setDouble(18, p.varB);

        ps.setInt(19, label);

        ps.executeUpdate();
        ps.close();
        conn.close();
    }
}
