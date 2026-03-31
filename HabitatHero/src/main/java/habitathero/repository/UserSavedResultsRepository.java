package habitathero.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import habitathero.entity.UserSavedResults;

@Repository
public interface UserSavedResultsRepository extends JpaRepository<UserSavedResults, Integer> {
}
