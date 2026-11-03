package com.research.pubmanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "publications")
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 600)
    private String title;

    @Column(nullable = false, length = 150)
    private String doi;

    @Column(nullable = false, length = 600)
    private String authors;

    @Column(name = "publication_year", nullable = false)
    private int publicationYear;

    @Column(nullable = false, length = 350)
    private String journal;

    @Column(nullable = false, length = 50)
    private String verificationStatus = "PENDING"; // VERIFIED, PENDING, REJECTED, DUPLICATE

    @Column(columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "citations_count")
    private int citationsCount = 0;

    private LocalDateTime verifiedAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Publication() {}

    public Publication(String title, String doi, String authors, int publicationYear, String journal, String verificationStatus, String verificationNotes, int citationsCount) {
        this.title = title;
        this.doi = doi;
        this.authors = authors;
        this.publicationYear = publicationYear;
        this.journal = journal;
        this.verificationStatus = verificationStatus != null ? verificationStatus : "PENDING";
        this.verificationNotes = verificationNotes;
        this.citationsCount = citationsCount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public int getPublicationYear() { return publicationYear; }
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }

    public String getJournal() { return journal; }
    public void setJournal(String journal) { this.journal = journal; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }

    public int getCitationsCount() { return citationsCount; }
    public void setCitationsCount(int citationsCount) { this.citationsCount = citationsCount; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
