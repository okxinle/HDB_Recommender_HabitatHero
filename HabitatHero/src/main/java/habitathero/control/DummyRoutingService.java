package habitathero.control;
import java.util.Random;

import habitathero.entity.Coordinates;

public class DummyRoutingService implements IRoutingService {

    private static final double MIN_MINUTES = 20.0;
    private static final double MAX_MINUTES = 60.0;

    private final Random random = new Random();

    @Override
    public double getTravelTime(Coordinates origin, Coordinates dest) {
        // Returns a random travel time between 20 and 60 minutes
        return MIN_MINUTES + ((MAX_MINUTES - MIN_MINUTES) * random.nextDouble());
    }
}