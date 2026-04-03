package habitathero.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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