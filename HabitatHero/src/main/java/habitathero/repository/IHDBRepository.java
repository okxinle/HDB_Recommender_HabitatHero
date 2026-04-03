package habitathero.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import habitathero.entity.HDBBlock;

@Repository
public interface IHDBRepository extends JpaRepository<HDBBlock, Integer> {
    
    // NEW: Helps the pipeline check if a building already exists
    Optional<HDBBlock> findByBlockNumberAndStreetName(String blockNumber, String streetName);
}