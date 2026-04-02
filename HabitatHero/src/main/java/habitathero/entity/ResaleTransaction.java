package habitathero.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "resale_transactions")
public class ResaleTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    // Link to the HDB block record
    @ManyToOne(optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private HDBBlock block;

    // Fields shown in your diagram
    @Column(nullable = false)
    private String town;

    @Column(name = "flat_type", nullable = false)
    private String flatType;

    @Column(name = "floor_area_sqm", nullable = false)
    private double floorAreaSqm;

    @Column(name = "remaining_lease", nullable = false)
    private int remainingLease;

    @Column(name = "resale_price", nullable = false)
    private double resalePrice;

    // Diagram uses String; store like "2026-03" (year-month)
    @Column(nullable = false)
    private String month;

    public ResaleTransaction() {}

    public ResaleTransaction(HDBBlock block, String town, String flatType,
                             double floorAreaSqm, int remainingLease,
                             double resalePrice, String month) {
        this.block = block;
        this.town = town;
        this.flatType = flatType;
        this.floorAreaSqm = floorAreaSqm;
        this.remainingLease = remainingLease;
        this.resalePrice = resalePrice;
        this.month = month;
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public HDBBlock getBlock() { return block; }
    public void setBlock(HDBBlock block) { this.block = block; }

    public String getTown() { return town; }
    public void setTown(String town) { this.town = town; }

    public String getFlatType() { return flatType; }
    public void setFlatType(String flatType) { this.flatType = flatType; }

    public double getFloorAreaSqm() { return floorAreaSqm; }
    public void setFloorAreaSqm(double floorAreaSqm) { this.floorAreaSqm = floorAreaSqm; }

    public int getRemainingLease() { return remainingLease; }
    public void setRemainingLease(int remainingLease) { this.remainingLease = remainingLease; }

    public double getResalePrice() { return resalePrice; }
    public void setResalePrice(double resalePrice) { this.resalePrice = resalePrice; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
}