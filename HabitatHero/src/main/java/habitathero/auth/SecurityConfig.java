package habitathero.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            // 1. Disable CSRF (Cross-Site Request Forgery) since we are using JWTs
            .csrf(csrf -> csrf.disable())
            
            // 2. KILL THE DEFAULT POPUPS AND PAGES
            .httpBasic(basic -> basic.disable()) // This line kills the black browser popup
            .formLogin(form -> form.disable())   // This line kills the white HTML login page
            
            // 3. Configure which endpoints are public vs. private
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/error").permitAll() 
                .anyRequest().authenticated()
            )
            
            // 4. Tell Spring Security NOT to use traditional login sessions
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 5. Insert your custom JWT Filter before the standard security checks
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}