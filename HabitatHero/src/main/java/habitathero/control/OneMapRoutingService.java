package habitathero.control;

import java.time.Duration;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import habitathero.entity.Coordinates;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OneMapRoutingService implements IRoutingService {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final double AVG_TRANSIT_SPEED_KMH = 20.0;
    private static final double MINUTES_PER_HOUR = 60.0;

    private static final String ROUTE_URL_TEMPLATE =
            "https://www.onemap.gov.sg/api/public/routingsvc/route"
                    + "?start=%s,%s"
                    + "&end=%s,%s"
                    + "&routeType=pt"
                    + "&date=2026-04-06"
                    + "&time=08:00:00"
                    + "&mode=TRANSIT";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OneMapRoutingService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public double getTravelTime(Coordinates origin, Coordinates dest) {
        if (origin == null || dest == null) {
            return Double.MAX_VALUE;
        }

        try {
            String requestUrl = String.format(
                    Locale.US,
                    ROUTE_URL_TEMPLATE,
                    origin.getLat(),
                    origin.getLng(),
                    dest.getLat(),
                    dest.getLng());

            String responseBody = restTemplate.getForObject(requestUrl, String.class);
            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("OneMap returned an empty response body.");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            double durationSeconds = root.at("/plan/itineraries/0/duration").asDouble(Double.NaN);

            if (Double.isNaN(durationSeconds) || durationSeconds <= 0.0) {
                throw new IllegalStateException("OneMap response does not contain a valid transit duration.");
            }

            return durationSeconds / MINUTES_PER_HOUR;
        } catch (Exception ex) {
            return estimateTransitMinutesFromHaversine(origin, dest);
        }
    }

    private double estimateTransitMinutesFromHaversine(Coordinates a, Coordinates b) {
        double distanceKm = haversineDistanceKm(a, b);
        if (distanceKm == Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        }

        return (distanceKm / AVG_TRANSIT_SPEED_KMH) * MINUTES_PER_HOUR;
    }

    private double haversineDistanceKm(Coordinates a, Coordinates b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }

        double lat1 = Math.toRadians(a.getLat());
        double lon1 = Math.toRadians(a.getLng());
        double lat2 = Math.toRadians(b.getLat());
        double lon2 = Math.toRadians(b.getLng());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double hav = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double centralAngle = 2.0 * Math.atan2(Math.sqrt(hav), Math.sqrt(1.0 - hav));
        return EARTH_RADIUS_KM * centralAngle;
    }
}
