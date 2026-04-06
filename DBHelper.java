//insert into sqlite db

import java.sql.*;
import java.util.*;

public class DBHelper {

    private static final String DB = "images.db";

    static Connection connect() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + DB);
    }

    public static void init() throws Exception {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS images (image_name TEXT PRIMARY KEY, hash INTEGER)");
        }
    }

    public static void insert(String name, long hash) throws Exception {
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(
                "INSERT OR REPLACE INTO images(image_name, hash) VALUES(?,?)")) {
            ps.setString(1, name);
            ps.setLong(2, hash);
            ps.executeUpdate();
        }
    }

    public static Map<String, Long> getAll() throws Exception {
        Map<String, Long> map = new LinkedHashMap<>();
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT image_name, hash FROM images")) {
            while (rs.next()) map.put(rs.getString(1), rs.getLong(2));
        }
        return map;
    }

    public static int count() throws Exception {
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM images")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}