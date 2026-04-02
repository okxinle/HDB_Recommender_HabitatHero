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

    // 1. BASE URL (Notice we removed the limit=1000 from the end of the string)
    private final String BASE_API_URL = "https://data.gov.sg/api/action/datastore_search?resource_id=f1765b54-a209-4718-8d38-a39237f502b3";

    // Runs automatically every day at 2:00 AM server time
    @Scheduled(cron = "0 0 2 * * ?")
    // NOTE: We removed @Transactional here so the database can commit in batches.
    public void syncHdbData() {
        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setActionType("HDB_DATA_SYNC_ALL");

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", "v2:5a244eb57abb2f7764779ca43c86ae26b2222a9e5458f522a5576a434a547599:D9iWv4WJNLtSbAI3MeDCL5RpntDtiRES"); 
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 2. PAGINATION VARIABLES
            int offset = 0;
            int limit = 1000; // Fetch 1000 records at a time
            boolean hasMoreRecords = true;
            int totalRecordsSynced = 0;

            // 3. THE PAGINATION LOOP
            while (hasMoreRecords) {
                // Build the URL for the current chunk
                String paginatedUrl = BASE_API_URL + "&limit=" + limit + "&offset=" + offset;
                
                System.out.println("Fetching 1000 records starting from offset: " + offset + "...");

                ResponseEntity<Map> response = restTemplate.exchange(
                    paginatedUrl, 
                    HttpMethod.GET, 
                    entity, 
                    Map.class
                );

                Map<String, Object> body = response.getBody();
                if (body == null || !body.containsKey("result")) {
                    throw new RuntimeException("API response body is null.");
                }
                
                Map<String, Object> result = (Map<String, Object>) body.get("result");
                List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");

                // 4. STOP CONDITION: If the API returns 0 records, we have downloaded everything!
                if (records == null || records.isEmpty()) {
                    hasMoreRecords = false;
                    break;
                }

                List<HDBBlock> blocksToSave = new ArrayList<>();

                // Map JSON to HDBBlock entity
                for (Map<String, Object> record : records) {
                    HDBBlock block = new HDBBlock();
                    block.setBlockNumber((String) record.get("block"));
                    block.setStreetName((String) record.get("street_name"));
                    block.setTown((String) record.get("town"));
                    block.setFlatType((String) record.get("flat_type"));
                    
                    String priceStr = (String) record.get("resale_price");
                    if (priceStr != null) {
                        block.setResalePrice(Double.parseDouble(priceStr));
                    }
                    blocksToSave.add(block);
                }

                // Save this chunk of 1000 to the database
                hdbRepository.saveAll(blocksToSave);
                totalRecordsSynced += blocksToSave.size();

                // 5. MOVE THE OFFSET FORWARD (Next loop starts at 1000, then 2000, etc.)
                offset += limit;

                // 6. RATE LIMIT PREVENTER: Pause for half a second before asking for the next chunk
                Thread.sleep(500); 
            }

            // 7. Log Success
            log.setStatus("SUCCESS");
            log.setDetails("Successfully paginated and synced a total of " + totalRecordsSynced + " records.");
            System.out.println("FULL SYNC COMPLETE: " + totalRecordsSynced + " records saved.");

        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setDetails("Error during sync: " + e.getMessage());
            e.printStackTrace(); 
        } finally {
            auditLogRepository.save(log);
        }
    }
}