package com.research.pubmanager.repository;

import com.research.pubmanager.entity.Publication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {

    Optional<Publication> findByDoiIgnoreCase(String doi);

    @Query("SELECT p FROM Publication p WHERE " +
           "(:status = 'ALL' OR p.verificationStatus = :status) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.authors) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.doi) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.journal) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.id DESC")
    List<Publication> searchPublications(@Param("query") String query, @Param("status") String status);

    long countByVerificationStatus(String status);
}
