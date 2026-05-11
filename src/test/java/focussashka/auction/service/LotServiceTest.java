package focussashka.auction.service;

import focussashka.auction.dto.LotForm;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {

    @Mock
    private LotRepository lotRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private LotService lotService;

    @Test
    void createRejectsNonSeller() {
        User bidder = user(10L, "bidder", Role.BIDDER);

        assertThatThrownBy(() -> lotService.create(lotForm(), bidder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Только продавец может создавать лоты.");

        verify(lotRepository, never()).save(any(Lot.class));
    }

    @Test
    void createInitializesOpenLotWithCurrentPriceEqualToStartPrice() {
        User seller = user(1L, "seller", Role.SELLER);
        when(lotRepository.save(any(Lot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lot createdLot = lotService.create(lotForm(), seller);

        assertThat(createdLot.getTitle()).isEqualTo("Ноутбук");
        assertThat(createdLot.getDescription()).isEqualTo("Описание");
        assertThat(createdLot.getStartPrice()).isEqualByComparingTo("15000.00");
        assertThat(createdLot.getCurrentPrice()).isEqualByComparingTo("15000.00");
        assertThat(createdLot.getMinStep()).isEqualByComparingTo("500.00");
        assertThat(createdLot.getEndTime()).isEqualTo(LocalDateTime.of(2030, 1, 1, 12, 0));
        assertThat(createdLot.getStatus()).isEqualTo(LotStatus.OPEN);
        assertThat(createdLot.getSeller()).isSameAs(seller);
    }

    @Test
    void getByIdThrowsWhenLotMissing() {
        when(lotRepository.findByStatusAndEndTimeBefore(eq(LotStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(lotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lotService.getById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Лот не найден.");
    }

    @Test
    void closeExpiredLotsClosesLotAndAssignsHighestBidWinner() {
        Lot expiredLot = openLot(user(1L, "seller", Role.SELLER));
        User winner = user(2L, "winner", Role.BIDDER);
        Bid highestBid = new Bid();
        highestBid.setBidder(winner);
        when(lotRepository.findByStatusAndEndTimeBefore(eq(LotStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredLot));
        when(bidRepository.findFirstByLotOrderByAmountDescCreatedAtAsc(expiredLot)).thenReturn(Optional.of(highestBid));

        lotService.closeExpiredLots();

        assertThat(expiredLot.getStatus()).isEqualTo(LotStatus.CLOSED);
        assertThat(expiredLot.getWinner()).isSameAs(winner);
        verify(lotRepository).save(expiredLot);
    }

    @Test
    void closeExpiredLotsClosesLotWithoutWinnerWhenNoBids() {
        Lot expiredLot = openLot(user(1L, "seller", Role.SELLER));
        when(lotRepository.findByStatusAndEndTimeBefore(eq(LotStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredLot));
        when(bidRepository.findFirstByLotOrderByAmountDescCreatedAtAsc(expiredLot)).thenReturn(Optional.empty());

        lotService.closeExpiredLots();

        assertThat(expiredLot.getStatus()).isEqualTo(LotStatus.CLOSED);
        assertThat(expiredLot.getWinner()).isNull();
        verify(lotRepository).save(expiredLot);
    }

    @Test
    void findAllClosesExpiredLotsBeforeLoadingList() {
        List<Lot> lots = List.of(openLot(user(1L, "seller", Role.SELLER)));
        when(lotRepository.findByStatusAndEndTimeBefore(eq(LotStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(lotRepository.findAllByOrderByEndTimeAsc()).thenReturn(lots);

        List<Lot> result = lotService.findAll();

        assertThat(result).isEqualTo(lots);
        verify(lotRepository).findAllByOrderByEndTimeAsc();
    }

    private LotForm lotForm() {
        LotForm form = new LotForm();
        form.setTitle("Ноутбук");
        form.setDescription("Описание");
        form.setStartPrice(new BigDecimal("15000.00"));
        form.setMinStep(new BigDecimal("500.00"));
        form.setEndTime(LocalDateTime.of(2030, 1, 1, 12, 0));
        return form;
    }

    private User user(Long id, String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        trySetId(user, id);
        return user;
    }

    private Lot openLot(User seller) {
        Lot lot = new Lot();
        lot.setSeller(seller);
        lot.setStatus(LotStatus.OPEN);
        lot.setCurrentPrice(new BigDecimal("1000.00"));
        lot.setMinStep(new BigDecimal("100.00"));
        lot.setEndTime(LocalDateTime.now().minusHours(1));
        return lot;
    }

    private void trySetId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
