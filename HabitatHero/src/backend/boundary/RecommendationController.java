package boundary;
import java.util.List;

import control.RecommendationEngine;
import entity.HDBBlock;
import entity.UserProfile;
import exception.ZeroMatchesException;

public class RecommendationController {

	private RecommendationEngine engine;
	private ResultsDashboard dashboard;

	public RecommendationController(RecommendationEngine engine, ResultsDashboard dashboard) {
        this.engine = engine;
        this.dashboard = dashboard;
    }
	/**
	 * 
	 * @param profile
	 */
	public void run(UserProfile profile) {
		try {
			List<HDBBlock> recommendedBlocks = engine.generateRecommendations(profile);
			dashboard.displayResults(recommendedBlocks);
		} catch (ZeroMatchesException e) {
			dashboard.displayError(e.getMessage());
		}
	}

}

