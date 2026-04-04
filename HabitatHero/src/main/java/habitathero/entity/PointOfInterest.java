package habitathero.entity;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "point_of_interest")
public class PointOfInterest {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "SCHOOL",
            "HAWKER_CENTRE",
            "SUPERMARKET",
            "PARK",
            "HOSPITAL",
            "PLAYGROUND");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    public PointOfInterest() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @PrePersist
    @PreUpdate
    public void normalizeAndValidateCategory() {
        if (category == null) {
            throw new IllegalArgumentException("POI category is required.");
        }

        String normalized = category.trim().toUpperCase();
        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid POI category: " + category);
        }

        category = normalized;
    }
}
