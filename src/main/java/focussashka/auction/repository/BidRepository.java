package focussashka.auction.repository;

import focussashka.auction.model.Bid;
import focussashka.auction.model.Lot;
import focussashka.auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByLotOrderByCreatedAtDesc(Lot lot);

    Optional<Bid> findFirstByLotOrderByAmountDescCreatedAtAsc(Lot lot);

    List<Bid> findByBidderOrderByCreatedAtDesc(User bidder);
}
