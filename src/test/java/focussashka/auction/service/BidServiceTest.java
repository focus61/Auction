package focussashka.auction.service;

import focussashka.auction.dto.BidForm;
import focussashka.auction.model.Bid;
import focussashka.auction.model.Lot;
import focussashka.auction.model.LotStatus;
import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.repository.BidRepository;
import focussashka.auction.repository.LotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private LotService lotService;

    @Mock
    private LotRepository lotRepository;

    @InjectMocks
    private BidService bidService;

    @Test
    void placeBidSavesBidAndUpdatesLotWhenBidIsValid() {
        User seller = user(1L, "seller", Role.SELLER);
        User bidder = user(2L, "bidder", Role.BIDDER);
        Lot lot = new Lot();
        trySetId(lot, 100L);
        Lot lockedLot = openLot(100L, seller);
        BidForm form = bidForm("1300.00");

        when(lotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lockedLot));
        when(lotService.getMinimumNextBid(lockedLot)).thenReturn(new BigDecimal("1200.00"));
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bid bid = bidService.placeBid(lot, form, bidder);

        assertThat(lockedLot.getCurrentPrice()).isEqualByComparingTo("1300.00");
        assertThat(lockedLot.getWinner()).isSameAs(bidder);
        assertThat(bid.getLot()).isSameAs(lockedLot);
        assertThat(bid.getBidder()).isSameAs(bidder);
        assertThat(bid.getAmount()).isEqualByComparingTo("1300.00");
        assertThat(bid.getCreatedAt()).isNotNull();
        verify(lotRepository).save(lockedLot);
        verify(bidRepository).save(bid);
    }

    @Test
    void placeBidRejectsMissingLot() {
        Lot lot = new Lot();
        trySetId(lot, 100L);

        when(lotRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bidService.placeBid(lot, bidForm("1300.00"), user(2L, "bidder", Role.BIDDER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Лот не найден.");
    }

    @Test
    void placeBidRejectsNonBidderRole() {
        User seller = user(1L, "seller", Role.SELLER);
        Lot lot = new Lot();
        trySetId(lot, 100L);
        Lot lockedLot = openLot(100L, seller);

        when(lotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lockedLot));

        assertThatThrownBy(() -> bidService.placeBid(lot, bidForm("1300.00"), user(2L, "seller2", Role.SELLER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Только участник торгов может делать ставки.");

        verify(bidRepository, never()).save(any(Bid.class));
    }

    @Test
    void placeBidRejectsClosedLot() {
        User seller = user(1L, "seller", Role.SELLER);
        User bidder = user(2L, "bidder", Role.BIDDER);
        Lot lot = new Lot();
        trySetId(lot, 100L);
        Lot lockedLot = openLot(100L, seller);
        lockedLot.setStatus(LotStatus.CLOSED);

        when(lotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lockedLot));

        assertThatThrownBy(() -> bidService.placeBid(lot, bidForm("1300.00"), bidder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Торги по лоту уже завершены.");
    }

    @Test
    void placeBidRejectsOwnLot() {
        User seller = user(1L, "seller", Role.SELLER);
        Lot lot = new Lot();
        trySetId(lot, 100L);
        Lot lockedLot = openLot(100L, seller);

        when(lotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lockedLot));

        assertThatThrownBy(() -> bidService.placeBid(lot, bidForm("1300.00"), user(1L, "bidder", Role.BIDDER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Нельзя делать ставку на собственный лот.");
    }

    @Test
    void placeBidRejectsAmountBelowMinimum() {
        User seller = user(1L, "seller", Role.SELLER);
        User bidder = user(2L, "bidder", Role.BIDDER);
        Lot lot = new Lot();
        trySetId(lot, 100L);
        Lot lockedLot = openLot(100L, seller);

        when(lotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lockedLot));
        when(lotService.getMinimumNextBid(lockedLot)).thenReturn(new BigDecimal("1200.00"));

        assertThatThrownBy(() -> bidService.placeBid(lot, bidForm("1100.00"), bidder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ставка должна быть не меньше 1200.00.");

        verify(bidRepository, never()).save(any(Bid.class));
    }

    private BidForm bidForm(String amount) {
        BidForm form = new BidForm();
        form.setAmount(new BigDecimal(amount));
        return form;
    }

    private Lot openLot(Long id, User seller) {
        Lot lot = new Lot();
        trySetId(lot, id);
        lot.setSeller(seller);
        lot.setStatus(LotStatus.OPEN);
        lot.setCurrentPrice(new BigDecimal("1000.00"));
        lot.setMinStep(new BigDecimal("200.00"));
        lot.setEndTime(LocalDateTime.now().plusHours(1));
        return lot;
    }

    private User user(Long id, String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        trySetId(user, id);
        return user;
    }

    private void trySetId(Object target, Long id) {
        try {
            var field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
