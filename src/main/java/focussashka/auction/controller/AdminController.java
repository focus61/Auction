package focussashka.auction.controller;

import focussashka.auction.model.Bid;
import focussashka.auction.model.Lot;
import focussashka.auction.model.LotStatus;
import focussashka.auction.model.User;
import focussashka.auction.service.BidService;
import focussashka.auction.service.LotService;
import focussashka.auction.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminController {

    private final UserService userService;
    private final LotService lotService;
    private final BidService bidService;

    public AdminController(UserService userService, LotService lotService, BidService bidService) {
        this.userService = userService;
        this.lotService = lotService;
        this.bidService = bidService;
    }

    @GetMapping("/admin")
    public String adminDashboard(Authentication authentication, Model model) {
        User currentUser = userService.getByUsername(authentication.getName());
        List<User> users = userService.findAllUsers();
        List<Lot> lots = lotService.findAll();
        List<Lot> closedLots = lots.stream()
                .filter(lot -> lot.getStatus() == LotStatus.CLOSED)
                .toList();
        List<LotBidHistoryView> bidHistories = lots.stream()
                .map(lot -> new LotBidHistoryView(lot, bidService.findForLot(lot)))
                .toList();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("users", users);
        model.addAttribute("lots", lots);
        model.addAttribute("closedLots", closedLots);
        model.addAttribute("bidHistories", bidHistories);
        return "admin";
    }

    public static class LotBidHistoryView {

        private final Lot lot;
        private final List<Bid> bids;

        public LotBidHistoryView(Lot lot, List<Bid> bids) {
            this.lot = lot;
            this.bids = bids;
        }

        public Lot getLot() {
            return lot;
        }

        public List<Bid> getBids() {
            return bids;
        }
    }
}
