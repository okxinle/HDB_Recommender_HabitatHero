import java.sql.ResultSet;

public class MainSpatialMgr {

    public void main(String[] args) {
        Coordinate coords = postalCodeToCoordinate("670180");
        System.out.printf("Coordinate: %f, %f\n", coords.getLatitude(), coords.getLongitude());
    }

    public double calNoiseLevel(String postal_code) {
        // convert postal code to coordinate
        Coordinate coords = this.postalCodeToCoordinate(postal_code);
        return this.calNoiseLevel(coords);
    }

    public double calNoiseLevel(Coordinate coords){
        ResultSet rs = null;
        int mrtSpl = 85; // Average sound pressure level in dbA of mrt train
        int lrtSpl = 67; // Average soud pressure level in dbA of lrt train
        double splNew = 0;
        String rail_type = "";
        double distance = 0;

        //calculate nearest distance to rail line from coordinate
        rs = TransportLineMgr.getInstance().calMinDistToLine(coords);
        try {
            rail_type = rs.getString("RAIL_TYPE");
            distance = rs.getDouble("distance_meters");
            System.out.println(rail_type);

        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
         * Calculate sound pressure level at new distance
         * Formula: Lp2 = Lp1 - 20 * log10(r2 / r1)
         */
        if (rail_type.equals("MRT")) {
            splNew = mrtSpl - (20 * Math.log10(distance / 5));
        } else if (rail_type.equals("LRT")) {
            splNew = lrtSpl - (20 * Math.log10(distance / 5));
        }
        return splNew;
    }

    public double calWestSunLevel() {
        return 0;
    }

    public ResultSet calFutureDevelopmentRisk(Coordinate coords, double distance) {
        return LandUseMgr.getInstance().checkProxNewDev(coords, distance);
    }

    public ResultSet calFutureDevelopmentRisk(String postal_code, double distance){
        // convert postal code to coordinate
        Coordinate coords = this.postalCodeToCoordinate(postal_code);
        return this.calFutureDevelopmentRisk(coords, distance);
    }

    //only used internally within class
    private Coordinate postalCodeToCoordinate(String postal_code) {
        return HDBBuildingMgr.getInstance().postalCodeToCoordinate(postal_code);
    }

}
