package habitathero.GeoSpatialAnalysis.src;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.OffsetDateTime;

import org.json.JSONObject;

// API to fetch and download dataset from data gov api
public class DataGovAPIHandler {
    public static DataGovAPIHandler instance;

    public static void main() {
        try {
            // DataGovAPI.getInstance().pollDownload("d_16b157c52ed637edd6ba1232e026258d");
            System.out.println(System.getProperty("user.dir"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DataGovAPIHandler getInstance() {
        if (instance == null) {
            instance = new DataGovAPIHandler();
        }
        return instance;
    }

    public Boolean pollForcedDownloadAndSave(String dataset_id, String localFilePath) {
        String pollUrl = "https://api-open.data.gov.sg/v1/public/api/datasets/" + dataset_id + "/poll-download";
    
        try {
            int maxRetries = 5;
            int attempt = 0;
            boolean success = false;
    
            while (attempt < maxRetries) {
                try {
                    //Polling for download link
                    HttpURLConnection conn = (HttpURLConnection) new URI(pollUrl).toURL().openConnection();
                    conn.setRequestMethod("GET");
    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
    
                    String jsonStr = response.toString();
                    System.out.println("Poll response: " + jsonStr);
    
                    JSONObject json = new JSONObject(jsonStr);
    
                    // Check success using "code"
                    if (json.getInt("code") == 0) {
                        JSONObject data = json.getJSONObject("data");
    
                        if (data.has("url")) {
                            String downloadUrl = data.getString("url");
                            System.out.println("\nDownload URL found: " + downloadUrl);
    
                            // Download file from temp link (streaming, safe for large files)
                            try (InputStream in = new URI(downloadUrl).toURL().openStream();
                                 FileOutputStream out = new FileOutputStream(localFilePath)) {
    
                                byte[] buffer = new byte[32768]; // 32KB buffer
                                int bytesRead;
                                long totalBytes = 0;
    
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                    totalBytes += bytesRead;
                                }
    
                                System.out.println("\nDownloaded bytes: " + totalBytes + "\n");
                            }
    
                            // Update metadata after successful download
                            JSONObject metadata = DataGovMetadataMgr.getInstance().fetchAPIMetadata(dataset_id);
                            DataGovMetadataMgr.getInstance().upsertSQLMetadata(metadata);
    
                            System.out.println("\nGeoJSON saved to: " + localFilePath);
                            success = true;
                            break;
                        }
                    }
    
                    // Still processing
                    Thread.sleep(2000);
    
                } catch (Exception inner) {
                    attempt++;
                    System.out.println("Error occurred (attempt " + attempt + "): " + inner.getMessage());
    
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
    
            if (!success) {
                System.out.println("Failed to download after " + maxRetries + " attempts");
            }
    
            return success;
    
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            return false;
        }
    }

    public Boolean pollDownloadAndSave(String dataset_id, String localFilePath) {

        // Check if API data is newer
        if (!checkAPIDataCurrency(dataset_id)) {
            System.out.println("\nDownload terminated: local data is current\n");
            return false;
        }

        return pollForcedDownloadAndSave(dataset_id, localFilePath);
    }

    public Boolean checkAPIDataCurrency(String dataset_id) {
        Boolean currencyStatus = false;

        try {
            // Fetch metadata from API and local database
            JSONObject metadataApi = DataGovMetadataMgr.getInstance().fetchAPIMetadata(dataset_id);
            JSONObject metadataSql = DataGovMetadataMgr.getInstance().retrieveSQLMetadata(dataset_id);

            if (metadataApi == null || metadataSql == null) {
                System.out.println("Metadata cannot be found or fetched for dataset: " + dataset_id);
                return false;
            }

            // Extract lastUpdatedAt from API and SQL metadata
            String apiLastUpdatedStr = metadataApi.has("lastUpdatedAt") && !metadataApi.isNull("lastUpdatedAt")
                    ? metadataApi.getString("lastUpdatedAt")
                    : null;

            String sqlLastUpdatedStr = metadataSql.has("lastUpdatedAt") && !metadataSql.isNull("lastUpdatedAt")
                    ? metadataSql.getString("lastUpdatedAt")
                    : null;

            if (apiLastUpdatedStr == null) {
                System.out.println("Missing lastUpdatedAt in API metadata.\n");
                return false;
            } else if (sqlLastUpdatedStr == null) {
                System.out.println("Missing lastUpdatedAt in SQL metadata.\n");
                return false;
            }

            OffsetDateTime apiDatasetDateTime = OffsetDateTime.parse(apiLastUpdatedStr);
            OffsetDateTime sqlDatasetDateTime = OffsetDateTime.parse(sqlLastUpdatedStr);

            // Compare timestamps
            if (sqlDatasetDateTime.isBefore(apiDatasetDateTime)) {
                currencyStatus = true;
                System.out.println("For data: " + metadataSql.getString("name"));
                System.out.println("Local database is older than DataGov database.");
                System.out.println("Local Last Updated: " + sqlLastUpdatedStr);
                System.out.println("DataGov Last Updated: " + apiLastUpdatedStr);

            } else {
                currencyStatus = false;
                System.out.println("For the data:" + metadataSql.getString("name"));
                System.out.println("Local database is up-to-date or newer than DataGov database.");
                System.out.println("Local Last Updated: " + sqlLastUpdatedStr);
                System.out.println("DataGov Last Updated: " + apiLastUpdatedStr);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return currencyStatus;
    }

}

