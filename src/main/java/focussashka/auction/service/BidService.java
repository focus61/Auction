package focussashka.auction.service;

import focussashka.auction.dto.BidForm;
import focussashka.auction.model.Bid;
import focussashka.auction.model.Lot;
import focussashka.auction.model.LotStatus;
import focussashka.auction.model.Role;
import focussashka.auction.model.User;
import focussashka.auction.repository.BidRepository;
import focussashka.auction.repository.LotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BidService {

    private final BidRepository bidRepository;
    private final LotService lotService;
    private final LotRepository lotRepository;

    public BidService(BidRepository bidRepository, LotService lotService, LotRepository lotRepository) {
        this.bidRepository = bidRepository;
        this.lotService = lotService;
        this.lotRepository = lotRepository;
    }

    public List<Bid> findForLot(Lot lot) {
        return bidRepository.findByLotOrderByCreatedAtDesc(lot);
    }

    public List<Bid> findForBidder(User bidder) {
        return bidRepository.findByBidderOrderByCreatedAtDesc(bidder);
    }

    @Transactional
    public Bid placeBid(Lot lot, BidForm form, User bidder) {
        lotService.closeExpiredLots();
        LocalDateTime now = LocalDateTime.now();
        if (bidder.getRole() != Role.BIDDER) {
            throw new IllegalArgumentException("Только участник торгов может делать ставки.");
        }
        if (lot.getStatus() != LotStatus.OPEN || lot.getEndTime().isBefore(now)) {
            throw new IllegalArgumentException("Торги по лоту уже завершены.");
        }
        if (lot.getSeller().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("Нельзя делать ставку на собственный лот.");
        }

        BigDecimal minimumAmount = lotService.getMinimumNextBid(lot);
        if (form.getAmount().compareTo(minimumAmount) < 0) {
            throw new IllegalArgumentException("Ставка должна быть не меньше " + minimumAmount + ".");
        }

        Bid bid = new Bid();
        bid.setLot(lot);
        bid.setBidder(bidder);
        bid.setAmount(form.getAmount());
        bid.setCreatedAt(now);

        lot.setCurrentPrice(form.getAmount());
        lot.setWinner(bidder);
        lotRepository.save(lot);
        return bidRepository.save(bid);
    }
}
