package focussashka.auction.repository;

import focussashka.auction.model.Lot;
import focussashka.auction.model.LotStatus;
import focussashka.auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LotRepository extends JpaRepository<Lot, Long> {

    List<Lot> findAllByOrderByEndTimeAsc();

    List<Lot> findBySellerOrderByEndTimeDesc(User seller);

    List<Lot> findByStatusAndEndTimeBefore(LotStatus status, LocalDateTime threshold);
}
