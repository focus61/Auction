package focussashka.auction.service;

import focussashka.auction.dto.LotForm;
import focussashka.auction.model.Bid;
import focussashka.auction.model.Lot;
import focussashka.auction.model.LotStatus;
import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.repository.BidRepository;
import focussashka.auction.repository.LotRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LotService {

    private final LotRepository lotRepository;
    private final BidRepository bidRepository;

    public LotService(LotRepository lotRepository, BidRepository bidRepository) {
        this.lotRepository = lotRepository;
        this.bidRepository = bidRepository;
    }

    public List<Lot> findAll() {
        closeExpiredLots();
        return lotRepository.findAllByOrderByEndTimeAsc();
    }

    public List<Lot> findSellerLots(User seller) {
        closeExpiredLots();
        return lotRepository.findBySellerOrderByEndTimeDesc(seller);
    }

    public Lot getById(Long id) {
        closeExpiredLots();
        return lotRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Лот не найден."));
    }

    @Transactional
    public Lot create(LotForm form, User seller) {
        if (seller.getRole() != Role.SELLER) {
            throw new IllegalArgumentException("Только продавец может создавать лоты.");
        }

        Lot lot = new Lot();
        lot.setTitle(form.getTitle());
        lot.setDescription(form.getDescription());
        lot.setStartPrice(form.getStartPrice());
        lot.setCurrentPrice(form.getStartPrice());
        lot.setMinStep(form.getMinStep());
        lot.setEndTime(form.getEndTime());
        lot.setSeller(seller);
        lot.setStatus(LotStatus.OPEN);
        return lotRepository.save(lot);
    }

    public BigDecimal getMinimumNextBid(Lot lot) {
        return lot.getCurrentPrice().add(lot.getMinStep());
    }

    @Transactional
    public void closeExpiredLots() {
        List<Lot> expiredLots = lotRepository.findByStatusAndEndTimeBefore(LotStatus.OPEN, LocalDateTime.now());
        for (Lot lot : expiredLots) {
            closeLot(lot);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduledCloseExpiredLots() {
        closeExpiredLots();
    }

    private void closeLot(Lot lot) {
        Bid highestBid = bidRepository.findFirstByLotOrderByAmountDescCreatedAtAsc(lot).orElse(null);
        lot.setStatus(LotStatus.CLOSED);
        lot.setWinner(highestBid != null ? highestBid.getBidder() : null);
        lotRepository.save(lot);
    }
}
