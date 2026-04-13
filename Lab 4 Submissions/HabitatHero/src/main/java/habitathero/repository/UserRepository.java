package habitathero.repository; 

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import habitathero.entity.UserAccount;

@Repository
public interface UserRepository extends JpaRepository<UserAccount, Integer> {
    
    // This custom method allows the JwtAuthenticationFilter to find a user by their email
    Optional<UserAccount> findByEmail(String email);
    
}