package com.careconnect.repository;

import com.careconnect.model.CareRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CareRequestRepository extends JpaRepository<CareRequest, Long> {
    List<CareRequest> findByPatientId(Long patientId);
    List<CareRequest> findByCaregiverId(Long caregiverId);
    long countByStatus(CareRequest.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CareRequest r where r.id = :id")
    Optional<CareRequest> lockById(@Param("id") Long id);

    @Query("select r from CareRequest r where r.caregiver.id = :cid and r.requestedDate = :date "
         + "and r.status in :statuses and r.id <> :excludeId")
    List<CareRequest> findLive(@Param("cid") Long caregiverId, @Param("date") LocalDate date,
                               @Param("statuses") Collection<CareRequest.Status> statuses,
                               @Param("excludeId") Long excludeId);
}
