package focussashka.auction.controller;

import focussashka.auction.dto.RegistrationForm;
import focussashka.auction.model.Role;
import focussashka.auction.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private static final Role[] REGISTRATION_ROLES = {Role.SELLER, Role.BIDDER};

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        addRegistrationRoles(model);
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationForm registrationForm,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            addRegistrationRoles(model);
            return "register";
        }

        try {
            userService.register(registrationForm);
        } catch (IllegalArgumentException ex) {
            addRegistrationRoles(model);
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }

        return "redirect:/login?registered";
    }

    private void addRegistrationRoles(Model model) {
        model.addAttribute("roles", REGISTRATION_ROLES);
    }
}
