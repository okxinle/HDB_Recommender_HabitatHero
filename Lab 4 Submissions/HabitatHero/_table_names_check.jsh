import java.sql.*;
String url = "jdbc:postgresql://localhost:5432/habitathero_db";
String user = "postgres";
String pass = "password123";
try (Connection c = DriverManager.getConnection(url,user,pass)) {
  String q = "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name";
  try (PreparedStatement ps = c.prepareStatement(q); ResultSet rs = ps.executeQuery()) {
    while (rs.next()) {
      String t = rs.getString(1);
      if (t.contains("transport") || t.contains("line") || t.contains("rail")) {
        System.out.println(t);
      }
    }
  }
}
/exit
