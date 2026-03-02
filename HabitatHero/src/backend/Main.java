import boundary.*;
import control.*;
import entity.*;
import repository.*;

import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        // ── 1. Wire up dependencies 
        DummyHDBRepository  dummyRepo    = new DummyHDBRepository();
        DummyRoutingService dummyRouting = new DummyRoutingService();

        RecommendationEngine engine    = new RecommendationEngine(dummyRepo, dummyRouting);
        ResultsDashboard     dashboard = new ResultsDashboard() {
            @Override
            public void displayResults(List<HDBBlock> rankedBlocks) {
                System.out.println("=== Recommendation Results (" + rankedBlocks.size() + " blocks) ===");
                for (HDBBlock block : rankedBlocks) {
                    System.out.printf("  [%s] %s | $%.0f | %d yrs lease | Score: %.1f%%%n",
                            block.getPostalCode(),
                            block.getTown(),
                            block.getEstimatedPrice(),
                            block.getRemainingLeaseYears(),
                            block.getGlobalMatchIndex());
                }
            }

            public void displayError(String message) {
                System.out.println(message);
            }
        };

        RecommendationController controller = new RecommendationController(engine, dashboard);

        // ── 2. Fake UserProfile 
        StructuralConstraints constraints = new StructuralConstraints(
                300_000.0,
                Arrays.asList("Tampines", "Bishan", "Queenstown"),
                "4-room",
                55
        );

        // Commute disabled — no destination coordinates needed for this test
        CommuterProfile commute = new CommuterProfile(false, null, null);

        // Soft preferences
        List<WeightedPreference> softConstraints = Arrays.asList(
                new WeightedPreference("Solar Orientation", 0.5, false),
                new WeightedPreference("Acoustic Comfort",  0.5, false)
        );

        UserProfile profile = new UserProfile(1, constraints, commute, softConstraints);

        // ── 3. Run 
        controller.run(profile);
    }
}

