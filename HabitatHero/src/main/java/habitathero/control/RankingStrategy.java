package habitathero.control;

import java.util.List;

import habitathero.entity.HDBBlock;
import habitathero.entity.UserProfile;

/**
 * Strategy contract for ranking recommended HDB blocks.
 */
public interface RankingStrategy {
    List<HDBBlock> rank(List<HDBBlock> candidateBlocks, UserProfile profile);
}
