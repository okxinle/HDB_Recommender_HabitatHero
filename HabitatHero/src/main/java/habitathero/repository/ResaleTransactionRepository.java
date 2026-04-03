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

	@Query("""
		SELECT new habitathero.control.BlockCandidateView(
			b,
			AVG(t.resalePrice),
			AVG(t.remainingLease),
			COUNT(t)
		)
		FROM ResaleTransaction t
		JOIN t.block b
		WHERE b.coordinates IS NOT NULL
		  AND (:townFilterDisabled = true OR UPPER(TRIM(b.town)) IN :preferredTowns)
		  AND (:preferredFlatType IS NULL OR :preferredFlatType = '' OR UPPER(TRIM(t.flatType)) = UPPER(TRIM(:preferredFlatType)))
		GROUP BY b
		HAVING AVG(t.resalePrice) <= :maxBudget
		   AND AVG(t.remainingLease) >= :minLeaseYears
		""")
	List<BlockCandidateView> findCandidateBlocks(
			@Param("maxBudget") double maxBudget,
			@Param("minLeaseYears") int minLeaseYears,
			@Param("preferredFlatType") String preferredFlatType,
			@Param("preferredTowns") List<String> preferredTowns,
			@Param("townFilterDisabled") boolean townFilterDisabled);
}