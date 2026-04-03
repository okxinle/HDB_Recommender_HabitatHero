package habitathero.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import habitathero.entity.HDBBlock;

@Repository
public interface IHDBRepository extends JpaRepository<HDBBlock, Integer> {
    
    // NEW: Helps the pipeline check if a building already exists
    Optional<HDBBlock> findByBlockNumberAndStreetName(String blockNumber, String streetName);

    @Query("""
        SELECT b
        FROM HDBBlock b
        WHERE b.postalCode IS NULL
           OR b.postalCode = ''
           OR b.coordinates IS NULL
        """)
    java.util.List<HDBBlock> findBlocksMissingGeoData();
}