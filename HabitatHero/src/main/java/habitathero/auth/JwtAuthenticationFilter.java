package habitathero.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import habitathero.entity.UserAccount;
import habitathero.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository; // To fetch the user if the token is valid

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Check if the header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the token string (skip the "Bearer " part)
        jwt = authHeader.substring(7);
        
        try {
            userEmail = jwtService.extractEmail(jwt); // Math happens here to read the token

            // 3. If we found an email and the user isn't already authenticated in this session
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Fetch the user from the database
                UserAccount user = userRepository.findByEmail(userEmail).orElse(null);

                // 4. Validate the token
                if (user != null && jwtService.isTokenValid(jwt, user)) {
                    
                    // 5. Token is good! Tell Spring Security this user is allowed in.
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))); // You can add roles here later (like ADMIN vs USER)
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token was expired or tampered with. It will drop down and return an unauthorized error.
            System.out.println("Invalid JWT Token: " + e.getMessage());
        }

        // Pass the request along to the next step (like your ProfileController)
        filterChain.doFilter(request, response);
    }
}