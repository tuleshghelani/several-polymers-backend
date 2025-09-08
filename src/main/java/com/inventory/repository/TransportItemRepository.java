package com.inventory.repository;

import com.inventory.entity.TransportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransportItemRepository extends JpaRepository<TransportItem, Long> {
    List<TransportItem> findByTransportBagId(Long transportBagId);
    List<TransportItem> findByTransportId(Long transportId);
    void deleteByTransportBagId(Long transportBagId);
    void deleteByTransportId(Long transportId);
} 