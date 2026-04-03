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
import habitathero.entity.ResaleTransaction;
import habitathero.repository.AuditLogRepository;
import habitathero.repository.IHDBRepository;
import habitathero.repository.ResaleTransactionRepository;

@Service
public class DataPipelineService {

    @Autowired
    private IHDBRepository hdbRepository;

    @Autowired
    private ResaleTransactionRepository transactionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final String BASE_API_URL = "https://data.gov.sg/api/action/datastore_search?resource_id=f1765b54-a209-4718-8d38-a39237f502b3";

    @Scheduled(cron = "0 0 2 * * ?")
    public void syncHdbData() {
        AuditLog log = new AuditLog();
        log.setTimestamp(LocalDateTime.now());
        log.setActionType("HDB_DATA_SYNC_ALL");

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", "v2:5a244eb57abb2f7764779ca43c86ae26b2222a9e5458f522a5576a434a547599:D9iWv4WJNLtSbAI3MeDCL5RpntDtiRES"); 
            HttpEntity<String> entity = new HttpEntity<>(headers);

            int offset = 0;
            int limit = 1000; 
            boolean hasMoreRecords = true;
            int totalRecordsSynced = 0;

            while (hasMoreRecords) {
                String paginatedUrl = BASE_API_URL + "&limit=" + limit + "&offset=" + offset;
                System.out.println("Fetching 1000 records starting from offset: " + offset + "...");

                ResponseEntity<Map> response = restTemplate.exchange(
                    paginatedUrl, HttpMethod.GET, entity, Map.class
                );

                Map<String, Object> body = response.getBody();
                if (body == null || !body.containsKey("result")) break;
                
                Map<String, Object> result = (Map<String, Object>) body.get("result");
                List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");

                if (records == null || records.isEmpty()) {
                    hasMoreRecords = false;
                    break;
                }

                List<ResaleTransaction> transactionsToSave = new ArrayList<>();

                for (Map<String, Object> record : records) {
                    String blockNo = (String) record.get("block");
                    String street = (String) record.get("street_name");
                    String town = (String) record.get("town");

                    // 1. Find or create the HDB Block
                    HDBBlock block = hdbRepository.findByBlockNumberAndStreetName(blockNo, street)
                                                  .orElse(new HDBBlock());

                    if (block.getBlockId() == 0) {
                        block.setBlockNumber(blockNo);
                        block.setStreetName(street);
                        block.setTown(town);
                        block = hdbRepository.save(block);
                    }

                    // 2. Create the Resale Transaction using your exact entity mapping
                    ResaleTransaction transaction = new ResaleTransaction();
                    transaction.setBlock(block);
                    transaction.setTown(town);
                    transaction.setMonth((String) record.get("month"));
                    transaction.setFlatType((String) record.get("flat_type"));
                    
                    // Safely parse double values
                    String floorAreaStr = (String) record.get("floor_area_sqm");
                    if (floorAreaStr != null) {
                        transaction.setFloorAreaSqm(Double.parseDouble(floorAreaStr));
                    }

                    String priceStr = (String) record.get("resale_price");
                    if (priceStr != null) {
                        transaction.setResalePrice(Double.parseDouble(priceStr));
                    }

                    // Safely parse the messy remaining lease string into an int
                    String leaseStr = (String) record.get("remaining_lease");
                    transaction.setRemainingLease(parseLeaseYears(leaseStr));
                    
                    transactionsToSave.add(transaction);
                }

                // 3. Save chunk to database
                transactionRepository.saveAll(transactionsToSave);
                totalRecordsSynced += transactionsToSave.size();

                offset += limit;
                Thread.sleep(500); // 500ms pause to prevent rate limiting
            }

            log.setStatus("SUCCESS");
            log.setDetails("Successfully synced a total of " + totalRecordsSynced + " transactions.");

        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setDetails("Error during sync: " + e.getMessage());
            e.printStackTrace(); 
        } finally {
            auditLogRepository.save(log);
        }
    }

    /**
     * Helper method to extract the integer year from strings like "94 years 10 months" or "94"
     */
    private int parseLeaseYears(String leaseStr) {
        if (leaseStr == null || leaseStr.isEmpty()) return 0;
        try {
            // Split by space and take the first part (the number of years)
            String yearPart = leaseStr.split(" ")[0];
            return Integer.parseInt(yearPart);
        } catch (NumberFormatException e) {
            return 0; // Default fallback if parsing fails
        }
    }
}