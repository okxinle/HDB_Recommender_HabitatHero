package habitathero.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import habitathero.control.BlockCandidateView;
import habitathero.entity.ResaleTransaction;

@Repository
public interface ResaleTransactionRepository extends JpaRepository<ResaleTransaction, Long> {

interface BlockFloorAreaView {
		Integer getBlockId();
		Double getAvgFloorAreaSqm();
	}

	interface TownPsfView {
		String getTown();
		Double getAvgPsf();
		Long getTransactionCount();
	}

    // COMBINED: Uses teammate's advanced SELECT and UPPER(TRIM) logic, 
    // but removes the HAVING clause as per your local fix.
	@Query("""
		SELECT new habitathero.control.BlockCandidateView(
			b,
			AVG(t.resalePrice),
			AVG(t.floorAreaSqm),
			AVG(t.remainingLease),
			COUNT(t)
		)
		FROM ResaleTransaction t
		JOIN t.block b
		WHERE b.coordinates IS NOT NULL
		  AND (:townFilterDisabled = true OR UPPER(TRIM(b.town)) IN :preferredTowns)
		  AND (:preferredFlatType IS NULL OR :preferredFlatType = '' OR UPPER(TRIM(t.flatType)) = UPPER(TRIM(:preferredFlatType)))
		GROUP BY b
		""")
	List<BlockCandidateView> findCandidateBlocks(
			@Param("preferredFlatType") String preferredFlatType,
			@Param("preferredTowns") List<String> preferredTowns,
			@Param("townFilterDisabled") boolean townFilterDisabled);

	@Query("""
		SELECT
			t.block.blockId AS blockId,
			AVG(t.floorAreaSqm) AS avgFloorAreaSqm
		FROM ResaleTransaction t
		WHERE t.block.blockId IN :blockIds
		GROUP BY t.block.blockId
		""")
	List<BlockFloorAreaView> findAverageFloorAreaByBlockIds(@Param("blockIds") List<Integer> blockIds);

	@Query("""
		SELECT
			UPPER(TRIM(t.town)) AS town,
			AVG(t.resalePrice / (t.floorAreaSqm * 10.7639)) AS avgPsf,
			COUNT(t) AS transactionCount
		FROM ResaleTransaction t
		WHERE UPPER(TRIM(t.town)) IN :towns
		  AND t.floorAreaSqm > 0
		GROUP BY UPPER(TRIM(t.town))
		""")
	List<TownPsfView> findAveragePsfByTowns(@Param("towns") List<String> towns);
}