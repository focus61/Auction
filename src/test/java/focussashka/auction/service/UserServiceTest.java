package focussashka.auction.service;

import focussashka.auction.dto.RegistrationForm;
import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerCreatesUserWithEncodedPassword() {
        RegistrationForm form = registrationForm("alex", "raw-password", Role.BIDDER);
        when(userRepository.existsByUsername("alex")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.register(form);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User persistedUser = userCaptor.getValue();
        assertThat(savedUser).isSameAs(persistedUser);
        assertThat(persistedUser.getFullName()).isEqualTo("Alex Example");
        assertThat(persistedUser.getUsername()).isEqualTo("alex");
        assertThat(persistedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(persistedUser.getRole()).isEqualTo(Role.BIDDER);
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegistrationForm form = registrationForm("alex", "raw-password", Role.BIDDER);
        when(userRepository.existsByUsername("alex")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь с таким логином уже существует.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsAdminRole() {
        RegistrationForm form = registrationForm("alex", "raw-password", Role.ADMIN);
        when(userRepository.existsByUsername("alex")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Роль администратора назначается только системой.");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getByUsernameThrowsWhenUserMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Пользователь не найден.");
    }

    @Test
    void findAllUsersSortsByFullNameIgnoringCase() {
        User zed = user("zed", "Zed");
        User anna = user("anna", "anna");
        User boris = user("boris", "Boris");
        when(userRepository.findAll()).thenReturn(List.of(zed, anna, boris));

        List<User> users = userService.findAllUsers();

        assertThat(users).extracting(User::getUsername)
                .containsExactly("anna", "boris", "zed");
    }

    @Test
    void loadUserByUsernameBuildsSpringSecurityUser() {
        User user = user("seller", "Seller User");
        user.setPassword("encoded-password");
        user.setRole(Role.SELLER);
        when(userRepository.findByUsername("seller")).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername("seller");

        assertThat(userDetails.getUsername()).isEqualTo("seller");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_SELLER");
    }

    private RegistrationForm registrationForm(String username, String password, Role role) {
        RegistrationForm form = new RegistrationForm();
        form.setFullName("Alex Example");
        form.setUsername(username);
        form.setPassword(password);
        form.setRole(role);
        return form;
    }

    private User user(String username, String fullName) {
        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        return user;
    }
}
