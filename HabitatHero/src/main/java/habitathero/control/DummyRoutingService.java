package habitathero.control;
import java.util.Random;

import org.springframework.stereotype.Service;

import habitathero.entity.Coordinates;

@Service
public class DummyRoutingService implements IRoutingService {

    private static final int MIN_MINUTES = 20;
    private static final int MAX_MINUTES = 60;

    private final Random random = new Random();

    @Override
    public int getTravelTime(Coordinates origin, Coordinates dest) {
        // Returns a random travel time between 20 and 60 minutes
        return MIN_MINUTES + random.nextInt(MAX_MINUTES - MIN_MINUTES + 1);
    }
}