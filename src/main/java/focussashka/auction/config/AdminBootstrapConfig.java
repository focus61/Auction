package focussashka.auction.config;

import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class AdminBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfig.class);

    @Bean
    public CommandLineRunner bootstrapAdmin(UserRepository userRepository,
                                            PasswordEncoder passwordEncoder,
                                            @Value("${app.admin.username:}") String username,
                                            @Value("${app.admin.password:}") String password,
                                            @Value("${app.admin.full-name:Администратор}") String fullName) {
        return args -> {
            if (username.isBlank() || password.isBlank()) {
                log.info("Admin bootstrap skipped: APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD not provided.");
                return;
            }

            User existingUser = userRepository.findByUsername(username).orElse(null);
            if (existingUser != null) {
                if (existingUser.getRole() != Role.ADMIN) {
                    throw new IllegalStateException("Пользователь " + username + " уже существует и не является администратором.");
                }
                log.info("Admin bootstrap: user '{}' already exists.", username);
                return;
            }

            userRepository.saveAndFlush(createAdmin(username, password, fullName, passwordEncoder));
            log.info("Admin bootstrap: user '{}' created.", username);
        };
    }

    private User createAdmin(String username, String password, String fullName, PasswordEncoder passwordEncoder) {
        User admin = new User();
        admin.setFullName(fullName);
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        return admin;
    }
}
