package com.inventory.repository;

import com.inventory.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByNameAndClient_Id(String name, Long clientId);
    Optional<Brand> findByNameAndIdNotInAndClient_Id(String name, List<Long> ids, Long clientId);
}


