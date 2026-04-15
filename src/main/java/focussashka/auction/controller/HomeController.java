package focussashka.auction.controller;

import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.service.BidService;
import focussashka.auction.service.LotService;
import focussashka.auction.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserService userService;
    private final LotService lotService;
    private final BidService bidService;

    public HomeController(UserService userService, LotService lotService, BidService bidService) {
        this.userService = userService;
        this.lotService = lotService;
        this.bidService = bidService;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        User currentUser = userService.getByUsername(authentication.getName());
        boolean isSeller = currentUser.getRole() == Role.SELLER;
        boolean isBidder = currentUser.getRole() == Role.BIDDER;

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("role", currentUser.getRole());
        model.addAttribute("lots", isSeller ? lotService.findSellerLots(currentUser) : lotService.findAll());
        model.addAttribute("myBids", isBidder ? bidService.findForBidder(currentUser) : null);
        return "index";
    }
}
