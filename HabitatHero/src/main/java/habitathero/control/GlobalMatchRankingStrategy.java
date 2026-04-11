package habitathero.control;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import habitathero.entity.HDBBlock;
import habitathero.entity.UserProfile;

/**
 * Default ranking strategy used by the recommendation pipeline.
 *
 * Primary order: higher global match index first.
 * Tie-breakers: lower estimated price first, then higher remaining lease.
 */
@Component
public class GlobalMatchRankingStrategy implements RankingStrategy {

    @Override
    public List<HDBBlock> rank(List<HDBBlock> candidateBlocks, UserProfile profile) {
        if (candidateBlocks == null || candidateBlocks.isEmpty()) {
            return candidateBlocks == null ? List.of() : candidateBlocks;
        }

        List<HDBBlock> ranked = new ArrayList<>(candidateBlocks);
        ranked.sort(
                Comparator.comparingDouble(HDBBlock::getGlobalMatchIndex).reversed()
                        .thenComparingDouble(HDBBlock::getEstimatedPrice)
                        .thenComparing(Comparator.comparingInt(HDBBlock::getRemainingLeaseYears).reversed()));
        return ranked;
    }
}