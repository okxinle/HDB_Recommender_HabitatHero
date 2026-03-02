package habitathero.entity;
public class Coordinates {

    private double lat;
    private double lng;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Coordinates() {}

    public Coordinates(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public double getLat() { return lat; }
    public double getLng() { return lng; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setLat(double lat) { this.lat = lat; }
    public void setLng(double lng) { this.lng = lng; }

    @Override
    public String toString() {
        return "Coordinates{lat=" + lat + ", lng=" + lng + "}";
    }
}