package com.research.pubmanager;

import com.research.pubmanager.entity.Publication;
import com.research.pubmanager.repository.PublicationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
public class PublicationWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicationWebApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(PublicationRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                Publication p1 = new Publication(
                        "Attention Is All You Need",
                        "10.48550/arXiv.1706.03762",
                        "Ashish Vaswani, Noam Shazeer, Niki Parmar, Jakob Uszkoreit, Llion Jones, Aidan N. Gomez, Łukasz Kaiser, Illia Polosukhin",
                        2017,
                        "Advances in Neural Information Processing Systems (NeurIPS)",
                        "VERIFIED",
                        "Official literature verified. Landmark transformer paper.",
                        134200
                );
                p1.setVerifiedAt(LocalDateTime.now());

                Publication p2 = new Publication(
                        "Deep Residual Learning for Image Recognition",
                        "10.1109/CVPR.2016.90",
                        "Kaiming He, Xiangyu Zhang, Shaoqing Ren, Jian Sun",
                        2016,
                        "IEEE CVPR",
                        "VERIFIED",
                        "Official paper verified in IEEE.",
                        179426
                );
                p2.setVerifiedAt(LocalDateTime.now());

                Publication p3 = new Publication(
                        "BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding",
                        "10.18653/v1/N19-1423",
                        "Jacob Devlin, Ming-Wei Chang, Kenton Lee, Kristina Toutanova",
                        2019,
                        "NAACL-HLT",
                        "VERIFIED",
                        "Official paper verified in NAACL.",
                        84500
                );
                p3.setVerifiedAt(LocalDateTime.now());

                repository.save(p1);
                repository.save(p2);
                repository.save(p3);
            }
        };
    }
}
