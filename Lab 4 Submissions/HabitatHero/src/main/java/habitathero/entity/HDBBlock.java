package habitathero.entity;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "hdb_blocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"block_number", "street_name"})
})
public class HDBBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int blockId;
    
    @Column(name = "block_number")
    private String blockNumber;
    
    @Column(name = "street_name")
    private String streetName;

    private String town;
    private String postalCode;
    private double estimatedPrice; // Kept for your recommendation engine later
    private int remainingLeaseYears;
    private boolean westSunStatus;
    private String noiseRiskLevel;
    private boolean futureRiskFlag;
    private double globalMatchIndex;

    @Transient
    private double convenienceScore;

    @Transient
    private double floorAreaSqm;

    @Transient
    private double townAveragePsf;

    @Transient
    private long townTransactionCount;

    @Transient
    private Map<String, Boolean> convenienceFactors;

    @Transient
    private Map<String, List<String>> matchedAmenities;

    @Transient
    private Map<String, AmenityLocation> nearestAmenities;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Coordinates coordinates;

    public HDBBlock() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public int getBlockId() { return blockId; }
    public String getBlockNumber() { return blockNumber; }
    public String getStreetName() { return streetName; }
    public String getTown() { return town; }
    public String getPostalCode() { return postalCode; }
    public double getEstimatedPrice() { return estimatedPrice; }
    public int getRemainingLeaseYears() { return remainingLeaseYears; }
    public boolean isWestSunStatus() { return westSunStatus; }
    public String getNoiseRiskLevel() { return noiseRiskLevel; }
    public boolean isFutureRiskFlag() { return futureRiskFlag; }
    public double getGlobalMatchIndex() { return globalMatchIndex; }
    public double getConvenienceScore() { return convenienceScore; }
    public double getFloorAreaSqm() { return floorAreaSqm; }
    public double getTownAveragePsf() { return townAveragePsf; }
    public long getTownTransactionCount() { return townTransactionCount; }
    public Map<String, Boolean> getConvenienceFactors() { return convenienceFactors; }
    public Map<String, List<String>> getMatchedAmenities() { return matchedAmenities; }
    public Map<String, AmenityLocation> getNearestAmenities() { return nearestAmenities; }
    public Coordinates getCoordinates() { return coordinates; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setBlockId(int blockId) { this.blockId = blockId; }
    public void setBlockNumber(String blockNumber) { this.blockNumber = blockNumber; }
    public void setStreetName(String streetName) { this.streetName = streetName; }
    public void setTown(String town) { this.town = town; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setEstimatedPrice(double estimatedPrice) { this.estimatedPrice = estimatedPrice; }
    public void setRemainingLeaseYears(int remainingLeaseYears) { this.remainingLeaseYears = remainingLeaseYears; }
    public void setWestSunStatus(boolean westSunStatus) { this.westSunStatus = westSunStatus; }
    public void setNoiseRiskLevel(String noiseRiskLevel) { this.noiseRiskLevel = noiseRiskLevel; }
    public void setFutureRiskFlag(boolean futureRiskFlag) { this.futureRiskFlag = futureRiskFlag; }
    public void setGlobalMatchIndex(double globalMatchIndex) { this.globalMatchIndex = globalMatchIndex; }
    public void setConvenienceScore(double convenienceScore) { this.convenienceScore = convenienceScore; }
    public void setFloorAreaSqm(double floorAreaSqm) { this.floorAreaSqm = floorAreaSqm; }
    public void setTownAveragePsf(double townAveragePsf) { this.townAveragePsf = townAveragePsf; }
    public void setTownTransactionCount(long townTransactionCount) { this.townTransactionCount = townTransactionCount; }
    public void setConvenienceFactors(Map<String, Boolean> convenienceFactors) { this.convenienceFactors = convenienceFactors; }
    public void setMatchedAmenities(Map<String, List<String>> matchedAmenities) { this.matchedAmenities = matchedAmenities; }
    public void setNearestAmenities(Map<String, AmenityLocation> nearestAmenities) { this.nearestAmenities = nearestAmenities; }
    public void setCoordinates(Coordinates coordinates) { this.coordinates = coordinates; }

    @Override
    public String toString() {
        return "HDBBlock{blockId=" + blockId
                + ", blockNumber='" + blockNumber + "'"
                + ", streetName='" + streetName + "'"
                + ", postalCode='" + postalCode + "'"
                + ", town='" + town + "'"
                + ", estimatedPrice=" + estimatedPrice
                + ", remainingLeaseYears=" + remainingLeaseYears
                + ", westSunStatus=" + westSunStatus
                + ", noiseRiskLevel='" + noiseRiskLevel + "'"
                + ", futureRiskFlag=" + futureRiskFlag
                + ", globalMatchIndex=" + globalMatchIndex + "}";
    }
}