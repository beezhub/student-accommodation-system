package com.beezhub.student_accommodation.repository;

import com.beezhub.student_accommodation.model.entity.WaitingListEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaitingListRepository extends JpaRepository<WaitingListEntry, Long> {
    Optional<WaitingListEntry> findFirstByStatusOrderByCreatedAtAsc(String status);
    boolean existsByStudent_IdAndStatus(Long studentId, String status);
}

