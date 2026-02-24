public class RecommendationEngine {

	/**
	 * 
	 * @param profile
	 */
	public List<HDBBlock> generateRecommendations(UserProfile profile) {
		// TODO - implement RecommendationEngine.generateRecommendations
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param allBlocks
	 */
	private void applyHardFilters(List<HDBBlock> allBlocks) {
		// TODO - implement RecommendationEngine.applyHardFilters
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param block
	 * @param preferences
	 */
	private void calculateLivabilityScore(HDBBlock block, List<WeightedPreferences> preferences) {
		// TODO - implement RecommendationEngine.calculateLivabilityScore
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param block
	 * @param commuteProfile
	 */
	private float calculateCommuteScore(HDBBlock block, CommuterProfile commuteProfile) {
		// TODO - implement RecommendationEngine.calculateCommuteScore
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param timeA
	 * @param timeB
	 */
	private float calculateFairnessIndex(int timeA, int timeB) {
		// TODO - implement RecommendationEngine.calculateFairnessIndex
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param blocks
	 */
	private void sortBlocksByMatchIndex(List<HDBBlock> blocks) {
		// TODO - implement RecommendationEngine.sortBlocksByMatchIndex
		throw new UnsupportedOperationException();
	}

}