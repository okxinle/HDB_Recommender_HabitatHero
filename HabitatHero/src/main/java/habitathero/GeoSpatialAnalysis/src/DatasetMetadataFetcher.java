import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import org.json.JSONObject;

public class DatasetMetadataFetcher {

    public JSONObject getMetadata(String datasetId) {
        try {
            String url = "https://api-production.data.gov.sg/v2/public/api/datasets/"
                    + datasetId + "/metadata";

            HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
            conn.setRequestMethod("GET");

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject root = new JSONObject(response.toString());
            JSONObject data = root.optJSONObject("data");

            if (data == null) {
                System.out.println("No data found in API response\n");
                return null;
            }

            System.out.println("Metadata of database fetched from DataGovAPI\n");
            return data;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}