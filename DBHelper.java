import java.sql.*;

public class DBHelper {

    // Connect to SQLite
    public static Connection connect() throws Exception {
        // Explicitly load SQLite JDBC driver
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:mpj.db");
    }

    // Create table if it doesn't exist
    public static void createTable() throws Exception {

        Connection conn = connect();

        String sql = "CREATE TABLE IF NOT EXISTS images (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "image_name TEXT," +
                "hash TEXT," +
                "hasMake REAL," +
                "hasModel REAL," +
                "hasDateTime REAL," +
                "hasGPS REAL," +
                "hasSoftware REAL," +
                "softwareIsEditor REAL," +
                "metadataMissing REAL," +
                "entropy REAL," +
                "lap REAL," +
                "edge REAL," +
                "meanR REAL," +
                "meanG REAL," +
                "meanB REAL," +
                "varR REAL," +
                "varG REAL," +
                "varB REAL," +
                "label INTEGER" +
                ");";

        Statement stmt = conn.createStatement();
        stmt.execute(sql);

        stmt.close();
        conn.close();
    }
}
