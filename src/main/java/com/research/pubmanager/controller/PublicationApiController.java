package com.research.pubmanager.controller;

import com.research.pubmanager.entity.Publication;
import com.research.pubmanager.repository.PublicationRepository;
import com.research.pubmanager.service.CrossrefVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/publications")
@CrossOrigin(origins = "*")
public class PublicationApiController {

    private final PublicationRepository repository;
    private final CrossrefVerificationService verificationService;

    public PublicationApiController(PublicationRepository repository, CrossrefVerificationService verificationService) {
        this.repository = repository;
        this.verificationService = verificationService;
    }

    @GetMapping
    public List<Publication> getPublications(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        return repository.searchPublications(query, status);
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        List<Publication> all = repository.findAll();

        long total = all.size();
        long verified = all.stream().filter(p -> "VERIFIED".equalsIgnoreCase(p.getVerificationStatus()) || "ORIGINAL".equalsIgnoreCase(p.getVerificationStatus())).count();
        long pending = all.stream().filter(p -> "PENDING".equalsIgnoreCase(p.getVerificationStatus())).count();
        long rejected = all.stream().filter(p -> "REJECTED".equalsIgnoreCase(p.getVerificationStatus()) || "DUPLICATE".equalsIgnoreCase(p.getVerificationStatus()) || "DISCREPANCY".equalsIgnoreCase(p.getVerificationStatus())).count();
        long totalCitations = all.stream().mapToLong(Publication::getCitationsCount).sum();

        metrics.put("total", total);
        metrics.put("verified", verified);
        metrics.put("pending", pending);
        metrics.put("rejected", rejected);
        metrics.put("totalCitations", totalCitations);
        return metrics;
    }

    @GetMapping("/report")
    public Map<String, Object> generateResearcherReport() {
        Map<String, Object> report = new HashMap<>();
        List<Publication> all = repository.findAll();

        long total = all.size();
        long verified = all.stream().filter(p -> "VERIFIED".equalsIgnoreCase(p.getVerificationStatus()) || "ORIGINAL".equalsIgnoreCase(p.getVerificationStatus())).count();
        long rejected = all.stream().filter(p -> "REJECTED".equalsIgnoreCase(p.getVerificationStatus()) || "DUPLICATE".equalsIgnoreCase(p.getVerificationStatus())).count();
        long totalCitations = all.stream().mapToLong(Publication::getCitationsCount).sum();

        // Group by Author
        Map<String, Long> authorCounts = all.stream()
                .collect(Collectors.groupingBy(Publication::getAuthors, Collectors.counting()));

        report.put("totalPublications", total);
        report.put("verifiedPublications", verified);
        report.put("rejectedDuplicates", rejected);
        report.put("totalCitations", totalCitations);
        report.put("acceptanceRate", total > 0 ? Math.round(((double) verified / total) * 100) : 0);
        report.put("authorSummary", authorCounts);
        report.put("publications", all);
        report.put("generatedAt", LocalDateTime.now().toString());

        return report;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Publication> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Adds a new publication with AUTO-DETECTION:
     * Automatically verifies if the publication already exists in the global academic database,
     * checks author information, tracks citations, and AUTO-REJECTS duplicates!
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Publication publication,
                                    @RequestParam(required = false, defaultValue = "false") boolean force) {
        if (publication.getTitle() == null || publication.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Paper Title is required."));
        }

        String title = publication.getTitle().trim();
        String authors = publication.getAuthors() != null ? publication.getAuthors().trim() : "";
        int year = publication.getPublicationYear();
        String doi = publication.getDoi() != null ? verificationService.normalizeDoi(publication.getDoi()) : "";
        publication.setDoi(doi);

        // Check local database for exact DOI duplicate
        if (!doi.isBlank() && repository.findByDoiIgnoreCase(doi).isPresent() && !force) {
            return ResponseEntity.badRequest().body(Map.of("error", "AUTO-REJECTED: A publication with this exact DOI already exists in your database!"));
        }

        // AUTO-VERIFICATION against global literature
        CrossrefVerificationService.PriorArtCheckResult check = verificationService.checkExistingPublications(title, authors, year);

        if (check.isAlreadyExists()) {
            // Paper already exists in published literature!
            publication.setVerificationStatus("REJECTED");
            publication.setVerificationNotes(check.getVerdict());
            if (check.getTopMatch() != null) {
                publication.setCitationsCount(check.getTopMatch().getCitations());
            }
        } else {
            publication.setVerificationStatus("VERIFIED");
            publication.setVerificationNotes(check.getVerdict());
        }

        publication.setVerifiedAt(LocalDateTime.now());
        Publication saved = repository.save(publication);

        Map<String, Object> res = new HashMap<>();
        res.put("publication", saved);
        res.put("checkResult", check);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Publication updated) {
        return repository.findById(id).map(existing -> {
            existing.setTitle(updated.getTitle());
            existing.setDoi(verificationService.normalizeDoi(updated.getDoi()));
            existing.setAuthors(updated.getAuthors());
            existing.setPublicationYear(updated.getPublicationYear());
            existing.setJournal(updated.getJournal());
            existing.setVerificationStatus(updated.getVerificationStatus());
            existing.setVerificationNotes(updated.getVerificationNotes());
            if (updated.getCitationsCount() > 0) {
                existing.setCitationsCount(updated.getCitationsCount());
            }
            return ResponseEntity.ok(repository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/check-existence")
    public ResponseEntity<?> checkTitleExistence(@RequestParam String title,
                                                 @RequestParam(required = false, defaultValue = "") String authors,
                                                 @RequestParam(required = false, defaultValue = "0") int year) {
        CrossrefVerificationService.PriorArtCheckResult result = verificationService.checkExistingPublications(title, authors, year);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/auto-verify")
    public ResponseEntity<?> autoVerifyRow(@PathVariable Long id) {
        return repository.findById(id).map(pub -> {
            CrossrefVerificationService.PriorArtCheckResult result = verificationService.checkExistingPublications(
                    pub.getTitle(), pub.getAuthors(), pub.getPublicationYear());

            if (result.isAlreadyExists()) {
                pub.setVerificationStatus("REJECTED");
                pub.setVerificationNotes(result.getVerdict());
                if (result.getTopMatch() != null) {
                    pub.setCitationsCount(result.getTopMatch().getCitations());
                }
            } else {
                pub.setVerificationStatus("VERIFIED");
                pub.setVerificationNotes(result.getVerdict());
            }

            pub.setVerifiedAt(LocalDateTime.now());
            repository.save(pub);

            Map<String, Object> resp = new HashMap<>();
            resp.put("publication", pub);
            resp.put("result", result);
            return ResponseEntity.ok(resp);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/quick-import")
    public ResponseEntity<?> quickImport(@RequestParam String doi) {
        try {
            String cleanDoi = verificationService.normalizeDoi(doi);
            if (repository.findByDoiIgnoreCase(cleanDoi).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "This DOI is already registered in your database."));
            }

            Publication fetched = verificationService.fetchFromCrossref(cleanDoi);
            fetched.setVerifiedAt(LocalDateTime.now());
            Publication saved = repository.save(fetched);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
