package habitathero.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import habitathero.entity.ResaleTransaction;

@Repository
public interface ResaleTransactionRepository extends JpaRepository<ResaleTransaction, Long> {
}