package habitathero.control;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import habitathero.auth.JwtService;
import habitathero.entity.UserAccount;
import habitathero.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount registerUser(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Registration Invalid. Account already exists");
        }

        UserAccount newUser = new UserAccount();
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setName(name);
        newUser.setCreatedAt(LocalDateTime.now());
        return userRepository.save(newUser);
    }

    public UserAccount authenticateUser(String email, String password) {
        UserAccount user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return user;
    }

    public String generateToken(UserAccount user) {
        return jwtService.generateToken(user);
    }
}
