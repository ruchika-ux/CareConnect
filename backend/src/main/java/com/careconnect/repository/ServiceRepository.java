package com.careconnect.repository;

import com.careconnect.model.ServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceCatalog, Long> {
    java.util.Optional<ServiceCatalog> findByName(String name);
}
