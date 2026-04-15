package focussashka.auction.config;

import focussashka.auction.model.Lot;
import focussashka.auction.model.LotStatus;
import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.repository.LotRepository;
import focussashka.auction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
    public CommandLineRunner seedData(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      LotRepository lotRepository,
                                      @Value("${app.demo-data.admin-password:}") String adminPassword,
                                      @Value("${app.demo-data.seller-password:}") String sellerPassword,
                                      @Value("${app.demo-data.bidder-password:}") String bidderPassword) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            validatePassword(adminPassword, "app.demo-data.admin-password");
            validatePassword(sellerPassword, "app.demo-data.seller-password");
            validatePassword(bidderPassword, "app.demo-data.bidder-password");

            createUser(userRepository, passwordEncoder, "Системный администратор", "admin", adminPassword, Role.ADMIN);
            User seller = createUser(userRepository, passwordEncoder, "Демо-продавец", "seller", sellerPassword, Role.SELLER);
            createUser(userRepository, passwordEncoder, "Демо-участник торгов", "bidder", bidderPassword, Role.BIDDER);

            Lot lot = new Lot();
            lot.setTitle("Ноутбук Lenovo ThinkPad");
            lot.setDescription("Рабочий ноутбук для демонстрации аукционной системы.");
            lot.setStartPrice(new BigDecimal("25000.00"));
            lot.setCurrentPrice(new BigDecimal("25000.00"));
            lot.setMinStep(new BigDecimal("1000.00"));
            lot.setEndTime(LocalDateTime.now().plusDays(2));
            lot.setStatus(LotStatus.OPEN);
            lot.setSeller(seller);
            lot.setWinner(null);
            lotRepository.save(lot);
        };
    }

    private void validatePassword(String password, String propertyName) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Для демо-данных нужно задать свойство " + propertyName + ".");
        }
    }

    private User createUser(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            String fullName,
                            String username,
                            String rawPassword,
                            Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }
}
