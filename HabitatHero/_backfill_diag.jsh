import java.sql.*;
String url = "jdbc:postgresql://localhost:5432/habitathero_db";
String user = "postgres";
String pass = "password123";
try (Connection c = DriverManager.getConnection(url,user,pass)) {
  String[] queries = new String[] {
    "SELECT COUNT(*) AS missing_geo FROM hdb_blocks WHERE postal_code IS NULL OR coordinates IS NULL",
    "SELECT COUNT(*) AS exact_blk_match FROM hdb_blocks b WHERE (b.postal_code IS NULL OR b.coordinates IS NULL) AND EXISTS (SELECT 1 FROM hdb_building_lookup l WHERE TRIM(l.blk_no)=TRIM(b.block_number) AND l.postal_cod IS NOT NULL AND TRIM(l.postal_cod) <> '')",
    "SELECT COUNT(*) AS alnum_blk_match FROM hdb_blocks b WHERE (b.postal_code IS NULL OR b.coordinates IS NULL) AND EXISTS (SELECT 1 FROM hdb_building_lookup l WHERE REGEXP_REPLACE(UPPER(TRIM(l.blk_no)), '[^A-Z0-9]', '', 'g') = REGEXP_REPLACE(UPPER(TRIM(b.block_number)), '[^A-Z0-9]', '', 'g') AND l.postal_cod IS NOT NULL AND TRIM(l.postal_cod) <> '')",
    "SELECT b.block_number, b.street_name FROM hdb_blocks b WHERE (b.postal_code IS NULL OR b.coordinates IS NULL) LIMIT 15",
    "SELECT l.blk_no, l.postal_cod, l.latitude, l.longitude FROM hdb_building_lookup l WHERE l.postal_cod IS NOT NULL AND TRIM(l.postal_cod) <> '' LIMIT 15"
  };
  for (int i=0; i<queries.length; i++) {
    System.out.println("---Q" + (i+1) + "---");
    try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(queries[i])) {
      ResultSetMetaData md = rs.getMetaData();
      int cols = md.getColumnCount();
      int rowCount = 0;
      while (rs.next()) {
        rowCount++;
        StringBuilder sb = new StringBuilder();
        for (int col=1; col<=cols; col++) {
          if (col>1) sb.append(" | ");
          sb.append(md.getColumnLabel(col)).append("=").append(rs.getString(col));
        }
        System.out.println(sb.toString());
      }
      if (rowCount==0) System.out.println("(no rows)");
    }
  }
}
/exit
