package com.inventory.repository;

import com.inventory.entity.Sale;
import com.inventory.entity.Transport;
import com.inventory.entity.UserMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMasterRepository extends JpaRepository<UserMaster, Long> {
    Optional<UserMaster> findByRefreshToken(String refreshToken);
} 