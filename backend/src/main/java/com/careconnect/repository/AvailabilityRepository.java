package com.careconnect.repository;

import com.careconnect.model.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByCaregiverId(Long caregiverId);
    void deleteByCaregiverId(Long caregiverId);
}
