package com.research.pubmanager.config;

import com.research.pubmanager.entity.Publication;
import com.research.pubmanager.repository.PublicationRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final PublicationRepository repository;

    public DataInitializer(PublicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        long currentCount = repository.count();
        if (currentCount < 10) {
            log.info("Current publication count is {}. Seeding comprehensive 500 research papers dataset...", currentCount);
            try {
                ClassPathResource resource = new ClassPathResource("data-500.json");
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        String jsonText = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JSONArray array = new JSONArray(jsonText);
                        List<Publication> toSave = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            Publication p = new Publication();
                            p.setTitle(obj.optString("title"));
                            p.setDoi(obj.optString("doi"));
                            p.setAuthors(obj.optString("authors"));
                            p.setPublicationYear(obj.optInt("publicationYear", 2024));
                            p.setJournal(obj.optString("journal"));
                            p.setVerificationStatus(obj.optString("verificationStatus", "VERIFIED"));
                            p.setVerificationNotes(obj.optString("verificationNotes", "Verified original research."));
                            p.setCitationsCount(obj.optInt("citationsCount", 0));
                            p.setVerifiedAt(LocalDateTime.now());
                            toSave.add(p);
                        }
                        repository.saveAll(toSave);
                        log.info("Successfully seeded {} research papers into database!", toSave.size());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to seed initial research papers dataset: {}", e.getMessage(), e);
            }
        } else {
            log.info("Database already populated with {} publications.", currentCount);
        }
    }
}
