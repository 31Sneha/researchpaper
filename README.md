# Research Publication Verification & Management System

> **Automated Global Duplicate Detection, Author Verification, and Real-Time Citation Tracking**

[![Live Demo on Vercel](https://img.shields.io/badge/Live%20Demo-Vercel-success?style=for-the-badge&logo=vercel)](https://publication-verification-system.vercel.app/)
[![GitHub Repo](https://img.shields.io/badge/GitHub-Repository-181717?style=for-the-badge&logo=github)](https://github.com/ManishTanwar7/publication-verification-system)
[![Project Report](https://img.shields.io/badge/Project%20Report-Documentation-blue?style=for-the-badge&logo=googledocs)](docs/PROJECT_REPORT.md)

---

## 🌐 Live Web Application
- **URL**: [https://publication-verification-system.vercel.app/](https://publication-verification-system.vercel.app/)
- **Comprehensive Academic Report**: [Read the Full Project Report with Images & Appendix](docs/PROJECT_REPORT.md)

---

## 🌟 Key Features

1. **🚨 Auto-Duplicate Rejection (150M+ Scholarly Records)**:
   - Queries Crossref / Google Scholar registries in real time.
   - Computes mathematical string similarity using the **Levenshtein Distance Algorithm**.
   - If similarity is $\ge 70\%$, the submission is **automatically rejected** as a duplicate paper.
2. **🔎 Standalone Title Existence Checker**:
   - Check any paper title before submitting to view top 15 matching papers, authors, publication years, similarity percentages, and real-time citations.
3. **👥 Author Identity Verification**:
   - Detects author mismatches and predatory re-submissions by displaying registered authors from global literature.
4. **📈 Real-Time Citation Tracking**:
   - Dynamically tracks citation counts (`is-referenced-by-count`) from the global scholarly index.
5. **📑 Printable Researcher Performance Reports**:
   - Generates summary statistics (Total Papers, Verified, Auto-Rejected, Total Citations, Author breakdown) with one-click **Print / Export PDF**.
6. **💾 Dual-Persistence Storage**:
   - Automatic browser local storage persistence + export to MySQL SQL DDL (`database/schema.sql`).
7. **🖥️ Multi-Platform Deployment**:
   - **Cloud Web Dashboard**: Vercel serverless deployment.
   - **Enterprise Backend**: Java Spring Boot 3 + MySQL REST API.
   - **Desktop GUI**: Native JavaFX 21 desktop application.

---

## 📁 Project Structure

```
publication-verification-system/
├── index.html                       # Cloud Web Dashboard (Vercel Root)
├── database/
│   └── schema.sql                   # MySQL Database Schema & Seed Data
├── docs/
│   ├── PROJECT_REPORT.md            # Comprehensive Academic Report
│   └── images/                      # System Screenshots & Figures
├── run-app.bat                      # Spring Boot Local Launcher (Port 8081)
├── run-javafx.bat                   # Native JavaFX Desktop GUI Launcher
├── pom.xml                          # Maven Build Configuration
└── src/                             # Spring Boot & JavaFX Source Code
```

---

## 💻 Local Setup & Execution

### 1. MySQL Database Setup
Open **MySQL Workbench** or command line and run:
```sql
SOURCE database/schema.sql;
```

### 2. Spring Boot Web Server (Port 8081)
Double-click `run-app.bat` or run:
```bash
mvn spring-boot:run
```
Then visit: `http://localhost:8081`

### 3. Native JavaFX Desktop Application
Double-click `run-javafx.bat` or run:
```bash
mvn javafx:run
```
