-- =====================================================================
-- Research Publication Verification & Management System - Database DDL
-- Compatible with MySQL Server 8.0+ and MySQL Workbench
-- =====================================================================

CREATE DATABASE IF NOT EXISTS research_pub_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE research_pub_db;

DROP TABLE IF EXISTS publications;

CREATE TABLE publications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(600) NOT NULL,
    doi VARCHAR(150) NULL,
    authors VARCHAR(600) NOT NULL,
    publication_year INT NOT NULL,
    journal VARCHAR(350) NOT NULL,
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    verification_notes TEXT NULL,
    citations_count INT NOT NULL DEFAULT 0,
    verified_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_doi (doi),
    INDEX idx_status (verification_status),
    INDEX idx_year (publication_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initial Seed Data
INSERT INTO publications (title, doi, authors, publication_year, journal, verification_status, verification_notes, citations_count, verified_at)
VALUES 
(
    'Attention Is All You Need',
    '10.48550/arXiv.1706.03762',
    'Ashish Vaswani, Noam Shazeer, Niki Parmar, Jakob Uszkoreit, Llion Jones, Aidan N. Gomez, Łukasz Kaiser, Illia Polosukhin',
    2017,
    'Advances in Neural Information Processing Systems (NeurIPS)',
    'VERIFIED',
    'Official literature verified. Landmark transformer paper.',
    134200,
    NOW()
),
(
    'Deep Residual Learning for Image Recognition',
    '10.1109/CVPR.2016.90',
    'Kaiming He, Xiangyu Zhang, Shaoqing Ren, Jian Sun',
    2016,
    'IEEE Conference on Computer Vision and Pattern Recognition (CVPR)',
    'VERIFIED',
    'Official DOI verified in IEEE.',
    179426,
    NOW()
),
(
    'BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding',
    '10.18653/v1/N19-1423',
    'Jacob Devlin, Ming-Wei Chang, Kenton Lee, Kristina Toutanova',
    2019,
    'NAACL-HLT',
    'VERIFIED',
    'Official paper verified in NAACL.',
    84500,
    NOW()
);
