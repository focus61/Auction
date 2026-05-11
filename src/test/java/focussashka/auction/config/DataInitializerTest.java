package focussashka.auction.config;

import focussashka.auction.model.Lot;
import focussashka.auction.model.LotStatus;
import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.repository.LotRepository;
import focussashka.auction.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LotRepository lotRepository;

    private final DataInitializer dataInitializer = new DataInitializer();

    @Test
    void seedDataSkipsWhenUsersAlreadyExist() throws Exception {
        when(userRepository.count()).thenReturn(1L);
        CommandLineRunner runner = dataInitializer.seedData(
                userRepository,
                passwordEncoder,
                lotRepository,
                "admin-password",
                "seller-password",
                "bidder-password"
        );

        runner.run();

        verify(userRepository, never()).save(any(User.class));
        verify(lotRepository, never()).save(any(Lot.class));
    }

    @Test
    void seedDataRequiresAllPasswords() {
        when(userRepository.count()).thenReturn(0L);
        CommandLineRunner runner = dataInitializer.seedData(
                userRepository,
                passwordEncoder,
                lotRepository,
                "",
                "seller-password",
                "bidder-password"
        );

        assertThatThrownBy(() -> runner.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Для демо-данных нужно задать свойство app.demo-data.admin-password.");
    }

    @Test
    void seedDataCreatesDemoUsersAndDemoLot() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any(String.class))).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0, String.class));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CommandLineRunner runner = dataInitializer.seedData(
                userRepository,
                passwordEncoder,
                lotRepository,
                "admin-password",
                "seller-password",
                "bidder-password"
        );

        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(userCaptor.capture());

        List<User> users = userCaptor.getAllValues();
        assertThat(users).extracting(User::getUsername)
                .containsExactly("admin", "seller", "bidder");
        assertThat(users).extracting(User::getRole)
                .containsExactly(Role.ADMIN, Role.SELLER, Role.BIDDER);
        assertThat(users).extracting(User::getPassword)
                .containsExactly("encoded-admin-password", "encoded-seller-password", "encoded-bidder-password");

        ArgumentCaptor<Lot> lotCaptor = ArgumentCaptor.forClass(Lot.class);
        verify(lotRepository).save(lotCaptor.capture());
        Lot lot = lotCaptor.getValue();
        assertThat(lot.getTitle()).isEqualTo("Ноутбук Lenovo ThinkPad");
        assertThat(lot.getStatus()).isEqualTo(LotStatus.OPEN);
        assertThat(lot.getSeller()).isSameAs(users.get(1));
        assertThat(lot.getStartPrice()).isEqualByComparingTo("25000.00");
        assertThat(lot.getCurrentPrice()).isEqualByComparingTo("25000.00");
        assertThat(lot.getMinStep()).isEqualByComparingTo("1000.00");
    }
}
