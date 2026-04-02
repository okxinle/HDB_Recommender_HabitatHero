package habitathero.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "hdb_blocks")
public class HDBBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int         blockId;
    
    // --- NEW FIELDS REQUIRED FOR DATA.GOV.SG API SYNC ---
    private String      blockNumber;
    private String      streetName;
    private String      flatType;
    private double      resalePrice;

    // --- YOUR EXISTING FIELDS ---
    private String      postalCode;
    private String      town;
    private double      estimatedPrice;
    private int         remainingLeaseYears;
    private boolean     westSunStatus;
    private String      noiseRiskLevel;
    private boolean     futureRiskFlag;
    private double      globalMatchIndex;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Coordinates coordinates;

    // ── Constructors ─────────────────────────────────────────────────────────

    public HDBBlock() {}

    public HDBBlock(int blockId,
                    String postalCode,
                    String town,
                    double estimatedPrice,
                    int remainingLeaseYears,
                    boolean westSunStatus,
                    String noiseRiskLevel,
                    boolean futureRiskFlag) {
        this.blockId             = blockId;
        this.postalCode          = postalCode;
        this.town                = town;
        this.estimatedPrice      = estimatedPrice;
        this.remainingLeaseYears = remainingLeaseYears;
        this.westSunStatus       = westSunStatus;
        this.noiseRiskLevel      = noiseRiskLevel;
        this.futureRiskFlag      = futureRiskFlag;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int     getBlockId()             { return blockId; }
    public String  getPostalCode()          { return postalCode; }
    public String  getTown()                { return town; }
    public double  getEstimatedPrice()      { return estimatedPrice; }
    public int     getRemainingLeaseYears() { return remainingLeaseYears; }
    public boolean isWestSunStatus()        { return westSunStatus; }
    public String  getNoiseRiskLevel()      { return noiseRiskLevel; }
    public boolean isFutureRiskFlag()       { return futureRiskFlag; }
    public double  getGlobalMatchIndex()    { return globalMatchIndex; }
    public Coordinates getCoordinates()     { return coordinates; }
    
    // New Getters
    public String  getBlockNumber()         { return blockNumber; }
    public String  getStreetName()          { return streetName; }
    public String  getFlatType()            { return flatType; }
    public double  getResalePrice()         { return resalePrice; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setBlockId(int blockId)                         { this.blockId             = blockId; }
    public void setPostalCode(String postalCode)                { this.postalCode          = postalCode; }
    public void setTown(String town)                            { this.town                = town; }
    public void setEstimatedPrice(double estimatedPrice)        { this.estimatedPrice      = estimatedPrice; }
    public void setRemainingLeaseYears(int remainingLeaseYears) { this.remainingLeaseYears = remainingLeaseYears; }
    public void setWestSunStatus(boolean westSunStatus)         { this.westSunStatus       = westSunStatus; }
    public void setNoiseRiskLevel(String noiseRiskLevel)        { this.noiseRiskLevel      = noiseRiskLevel; }
    public void setFutureRiskFlag(boolean futureRiskFlag)       { this.futureRiskFlag      = futureRiskFlag; }
    public void setGlobalMatchIndex(double globalMatchIndex)    { this.globalMatchIndex    = globalMatchIndex; }
    public void setCoordinates(Coordinates coordinates)         { this.coordinates         = coordinates; }

    // New Setters
    public void setBlockNumber(String blockNumber)              { this.blockNumber         = blockNumber; }
    public void setStreetName(String streetName)                { this.streetName          = streetName; }
    public void setFlatType(String flatType)                    { this.flatType            = flatType; }
    public void setResalePrice(double resalePrice)              { this.resalePrice         = resalePrice; }

    @Override
    public String toString() {
        return "HDBBlock{blockId=" + blockId
                + ", blockNumber='" + blockNumber + "'"
                + ", streetName='" + streetName + "'"
                + ", flatType='" + flatType + "'"
                + ", resalePrice=" + resalePrice
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