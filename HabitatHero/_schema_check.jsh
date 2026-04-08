import java.sql.*;
String url = "jdbc:postgresql://localhost:5432/habitathero_db";
String user = "postgres";
String pass = "password123";
try (Connection c = DriverManager.getConnection(url,user,pass)) {
  String q = "SELECT table_name, column_name, data_type, udt_name FROM information_schema.columns WHERE table_schema='public' AND table_name IN ('transport_line_cal_result','sun_facing_analysis_result') ORDER BY table_name, ordinal_position";
  try (PreparedStatement ps = c.prepareStatement(q); ResultSet rs = ps.executeQuery()) {
    while (rs.next()) {
      System.out.println(rs.getString(1)+"|"+rs.getString(2)+"|"+rs.getString(3)+"|"+rs.getString(4));
    }
  }
}
/exit
