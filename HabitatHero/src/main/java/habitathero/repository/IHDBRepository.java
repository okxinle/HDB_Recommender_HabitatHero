package habitathero.repository;

import habitathero.entity.HDBBlock;
import java.util.List;

public interface IHDBRepository {

    /**
     * Returns all HDB blocks available in the data source.
     *
     * @return a list of all HDBBlock objects
     */
    List<HDBBlock> getAllBlocks();
}