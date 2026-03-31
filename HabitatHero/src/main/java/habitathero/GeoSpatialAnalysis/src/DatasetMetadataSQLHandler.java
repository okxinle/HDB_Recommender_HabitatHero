package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;

import org.json.JSONObject;

public class DatasetMetadataSQLHandler extends SQLDbConnect {

    public DatasetMetadataSQLHandler() {
        super();
    }

    public void createSQLTable() {
        String checkSql = "SELECT to_regclass('public.Dataset_Metadata')";
        String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS Dataset_Metadata (
                        dataset_id TEXT PRIMARY KEY,
                        api_json JSONB,
                        created_at TIMESTAMP WITH TIME ZONE,
                        last_updated_at TIMESTAMP WITH TIME ZONE
                    );
                """;

        try {
            super.connectSQL();
            Statement stmt = conn.createStatement();

            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && rs.getString(1) != null) {
                    System.out.println("Dataset_Metadata table exists: " + rs.getString(1));
                    return;
                }
            }

            stmt.executeUpdate(createTableSQL);
            stmt.close();
            super.closeConnection();
            System.out.println("Dataset_Metadata table created successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Update or insert row entry for metadata sql database
    public void upsertSQLMetadata(JSONObject apiJson) {
        try {
            super.connectSQL();
    
            String datasetId = apiJson.getString("datasetId");  // directly
            String createdAtStr = apiJson.optString("createdAt", null);
            String lastUpdatedStr = apiJson.optString("lastUpdatedAt", null);
    
            OffsetDateTime createdAt = createdAtStr != null ? OffsetDateTime.parse(createdAtStr) : null;
            OffsetDateTime lastUpdatedAt = lastUpdatedStr != null ? OffsetDateTime.parse(lastUpdatedStr) : null;
    
            String sql = """
                INSERT INTO Dataset_Metadata (dataset_id, api_json, created_at, last_updated_at)
                VALUES (?, ?::jsonb, ?, ?)
                ON CONFLICT (dataset_id) DO UPDATE SET
                    api_json = EXCLUDED.api_json,
                    created_at = EXCLUDED.created_at,
                    last_updated_at = EXCLUDED.last_updated_at;
            """;
    
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, datasetId);
            stmt.setString(2, apiJson.toString());
            stmt.setObject(3, createdAt);
            stmt.setObject(4, lastUpdatedAt);
    
            stmt.executeUpdate();
            stmt.close();
            super.closeConnection();
    
            System.out.println("Metadata upserted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject retrieveSQLMetadata(String datasetId) {
        JSONObject result = null;
        try {
            super.connectSQL();
            String sql = "SELECT api_json FROM Dataset_Metadata WHERE dataset_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, datasetId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String jsonString = rs.getString("api_json");
                result = new JSONObject(jsonString);
            }

            rs.close();
            stmt.close();
            super.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}

