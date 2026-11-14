package com.careconnect.repository;

import com.careconnect.model.CareRequest;
import com.careconnect.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRequest(CareRequest request);
    List<Review> findByCaregiverId(Long caregiverId);
    List<Review> findByPatientId(Long patientId);
}
