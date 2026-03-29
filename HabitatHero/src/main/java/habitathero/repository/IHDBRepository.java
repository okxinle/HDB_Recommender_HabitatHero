package habitathero.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import habitathero.entity.HDBBlock;

@Repository
public interface IHDBRepository extends JpaRepository<HDBBlock, Integer> {
    // JpaRepository provides saveAll(), findAll(), etc. automatically!
}