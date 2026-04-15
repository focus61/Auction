package focussashka.auction.controller;

import focussashka.auction.dto.BidForm;
import focussashka.auction.dto.LotForm;
import focussashka.auction.model.Lot;
import focussashka.auction.model.LotStatus;
import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.service.BidService;
import focussashka.auction.service.LotService;
import focussashka.auction.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Controller
public class LotController {

    private final LotService lotService;
    private final BidService bidService;
    private final UserService userService;

    public LotController(LotService lotService, BidService bidService, UserService userService) {
        this.lotService = lotService;
        this.bidService = bidService;
        this.userService = userService;
    }

    @GetMapping("/seller/lots/new")
    public String newLotForm(Model model) {
        LotForm lotForm = new LotForm();
        lotForm.setEndTime(LocalDateTime.now().plusDays(1).withSecond(0).withNano(0));
        model.addAttribute("lotForm", lotForm);
        return "lot-form";
    }

    @PostMapping("/seller/lots")
    public String createLot(@Valid @ModelAttribute LotForm lotForm,
                            BindingResult bindingResult,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "lot-form";
        }

        User seller = userService.getByUsername(authentication.getName());
        try {
            lotService.create(lotForm, seller);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "lot-form";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Лот опубликован.");
        return "redirect:/";
    }

    @GetMapping("/lots/{id}")
    public String lotDetails(@PathVariable Long id, Authentication authentication, Model model) {
        Lot lot = lotService.getById(id);
        User currentUser = userService.getByUsername(authentication.getName());
        ensureLotAccess(lot, currentUser);
        BidForm bidForm = new BidForm();
        populateLotDetailsModel(model, lot, currentUser, bidForm);
        return "lot-details";
    }

    @PostMapping("/bidder/lots/{id}/bids")
    public String placeBid(@PathVariable Long id,
                           @Valid @ModelAttribute BidForm bidForm,
                           BindingResult bindingResult,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        Lot lot = lotService.getById(id);
        User bidder = userService.getByUsername(authentication.getName());
        ensureLotAccess(lot, bidder);

        if (bindingResult.hasErrors()) {
            populateLotDetailsModel(model, lot, bidder, bidForm);
            return "lot-details";
        }

        try {
            bidService.placeBid(lot, bidForm, bidder);
            redirectAttributes.addFlashAttribute("successMessage", "Ставка принята.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/lots/" + id;
    }

    private void populateLotDetailsModel(Model model, Lot lot, User currentUser, BidForm bidForm) {
        if (lot.getStatus() == LotStatus.OPEN && bidForm.getAmount() == null) {
            bidForm.setAmount(lotService.getMinimumNextBid(lot));
        }

        model.addAttribute("lot", lot);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("bidHistory", bidService.findForLot(lot));
        model.addAttribute("bidForm", bidForm);
        model.addAttribute("minimumNextBid", lot.getStatus() == LotStatus.OPEN ? lotService.getMinimumNextBid(lot) : null);
    }

    private void ensureLotAccess(Lot lot, User currentUser) {
        if (currentUser.getRole() == Role.SELLER
                && !lot.getSeller().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(FORBIDDEN, "Продавец может просматривать только свои лоты.");
        }
    }
}
