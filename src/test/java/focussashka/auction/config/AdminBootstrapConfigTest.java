package focussashka.auction.config;

import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapConfigTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final AdminBootstrapConfig config = new AdminBootstrapConfig();

    @Test
    void bootstrapSkipsWhenCredentialsAreMissing() throws Exception {
        CommandLineRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, "", "", "Администратор");

        runner.run();

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void bootstrapFailsWhenExistingUserIsNotAdmin() {
        User existingUser = new User();
        existingUser.setRole(Role.BIDDER);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingUser));
        CommandLineRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, "admin", "secret", "Администратор");

        assertThatThrownBy(() -> runner.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Пользователь admin уже существует и не является администратором.");
    }

    @Test
    void bootstrapSkipsWhenAdminAlreadyExists() throws Exception {
        User existingAdmin = new User();
        existingAdmin.setRole(Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existingAdmin));
        CommandLineRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, "admin", "secret", "Администратор");

        runner.run();

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void bootstrapCreatesAdminWhenUserMissing() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        CommandLineRunner runner = config.bootstrapAdmin(userRepository, passwordEncoder, "admin", "secret", "Главный администратор");

        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getPassword()).isEqualTo("encoded-secret");
        assertThat(admin.getFullName()).isEqualTo("Главный администратор");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }
}
