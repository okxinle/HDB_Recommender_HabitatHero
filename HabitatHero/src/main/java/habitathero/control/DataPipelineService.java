package habitathero.control;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import habitathero.entity.AuditLog;
import habitathero.entity.HDBBlock;
import habitathero.repository.AuditLogRepository;
import habitathero.repository.IHDBRepository;

@Service
public class DataPipelineService {

    @Autowired
    private IHDBRepository hdbRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    // The Data.gov.sg resource ID for HDB Resale Flat Prices
    private final String API_URL = "https://data.gov.sg/api/action/datastore_search?resource_id=f1765b54-a209-4718-8d38-a39237f502b3&limit=1000";

    // Runs automatically every day at 2:00 AM server time
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional // Ensures the database update is atomic
    public void syncHdbData() {
        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setActionType("HDB_DATA_SYNC");

        try {
            RestTemplate restTemplate = new RestTemplate();
            
            // 1. Setup headers with the API Key
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", "YOUR_DATAGOVSG_API_KEY_HERE"); // NOTE: Put your actual key here
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 2. Fetch data from the API using exchange (this includes your headers)
            ResponseEntity<Map> response = restTemplate.exchange(
                API_URL, 
                HttpMethod.GET, 
                entity, 
                Map.class
            );

            // 3. Parse the CKAN JSON structure safely
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("result")) {
                throw new RuntimeException("API response body is null or missing 'result' object.");
            }
            
            Map<String, Object> result = (Map<String, Object>) body.get("result");
            List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");

            List<HDBBlock> blocksToSave = new ArrayList<>();

            // 4. Map JSON to your HDBBlock entity
            for (Map<String, Object> record : records) {
                HDBBlock block = new HDBBlock();
                block.setBlockNumber((String) record.get("block"));
                block.setStreetName((String) record.get("street_name"));
                block.setTown((String) record.get("town"));
                block.setFlatType((String) record.get("flat_type"));
                
                // Parse numeric values carefully
                String priceStr = (String) record.get("resale_price");
                if (priceStr != null) {
                    block.setResalePrice(Double.parseDouble(priceStr));
                }
                
                blocksToSave.add(block);
            }

            // 5. Perform the UPSERT (Save All)
            hdbRepository.saveAll(blocksToSave);

            // 6. Log Success
            log.setStatus("SUCCESS");
            log.setDetails("Successfully synced " + blocksToSave.size() + " records.");

        } catch (Exception e) {
            // 7. Log Failure
            log.setStatus("FAILED");
            log.setDetails("Error during sync: " + e.getMessage());
            e.printStackTrace(); // Keep for server console debugging
        } finally {
            auditLogRepository.save(log);
        }
    }
}