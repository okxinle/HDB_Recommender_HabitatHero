import org.json.JSONObject;

public class TransportLineCalNoiseLevel {
    private static final int mrtAvgSpl = 85;// Average sound pressure level in dbA of mrt train
    private static final int lrtAvgSpl = 67;// Average soud pressure level in dbA of lrt train


    public JSONObject calNoiseLevel(JSONObject transportLineMinDistResult) {
        if (transportLineMinDistResult.has("error")) {
            System.out.println("Error calculating transport distance: " + transportLineMinDistResult.getString("error"));
            JSONObject errorResult = new JSONObject();
            errorResult.put("error", transportLineMinDistResult.getString("error"));
            return errorResult;
        }

        double splNew = 0;
        String rail_type = transportLineMinDistResult.optString("rail_type", "");
        double distance = transportLineMinDistResult.optDouble("distance_meters", 0);

        /*
         * Calculate sound pressure level at new distance
         * Formula: Lp2 = Lp1 - 20 * log10(r2 / r1)
         */
        if (rail_type.equals("MRT")) {
            splNew = mrtAvgSpl - (20 * Math.log10(distance / 5));
        } else if (rail_type.equals("LRT")) {
            splNew = lrtAvgSpl - (20 * Math.log10(distance / 5));
        }

        JSONObject result = new JSONObject();
        //Append LineMinDistResult to NoiseLevelResult
        result.put("noise_level_db", splNew);
        for (String key : transportLineMinDistResult.keySet()) {
            result.put(key, transportLineMinDistResult.get(key));
        }

        return result;
    }
}
