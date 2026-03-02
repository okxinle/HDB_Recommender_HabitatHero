package habitathero.control;
import habitathero.entity.Coordinates;

public interface IRoutingService {

    /**
     * Returns the estimated travel time in minutes between two coordinates.
     *
     * @param origin the starting location
     * @param dest   the destination location
     * @return travel time in minutes
     */
    int getTravelTime(Coordinates origin, Coordinates dest);
}