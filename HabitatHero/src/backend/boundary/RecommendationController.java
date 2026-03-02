package boundary;
import java.util.List;

import control.RecommendationEngine;
import entity.HDBBlock;
import entity.UserProfile;

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
		List<HDBBlock> recommendedBlocks = engine.generateRecommendations(profile);
		//dashboard.displayResults(recommendedBlocks);
	}

}

