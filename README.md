# Hospital Management System (HMS)

A full-stack Hospital Management System built with **Java 21**, **Spring Boot**, **Thymeleaf**, **Spring Data JPA**, and **MySQL**, designed for Project-Based Learning (PBL).

---

## 🚀 Features

- **Executive Dashboard**: Real-time hospital metrics including total patients, doctors, appointments, available/occupied beds, and billing statuses.
- **Patient Management**:
  - Full registration workflow with Bean Validation (name, age, gender, blood group, 10-digit mobile number, address, medical history).
  - Searchable patient directory with instant lookup by Patient ID, Name, or Phone.
  - Patient detailed clinical profile view.
  - Safe patient update and delete with modal confirmation.
  - Flash notification messages for user actions.
- **Responsive Web UI**: Built with Bootstrap 5, Bootstrap Icons, and custom CSS design tokens for both desktop and mobile devices.
- **Clean Layered Architecture**: Strictly follows Model-View-Controller (MVC) separation of concerns (Entities, Repositories, Services, Controllers, Templates).

---

## 🛠️ Technology Stack

- **Language:** Java 21
- **Backend Framework:** Spring Boot 4.1.1
- **Build Tool:** Apache Maven (Maven Wrapper included)
- **Persistence:** Spring Data JPA / Hibernate ORM
- **Database:** MySQL 8.x (with in-memory H2 database for isolated testing)
- **Template Engine:** Thymeleaf
- **Frontend:** HTML5, CSS3, Vanilla JavaScript, Bootstrap 5.3, Bootstrap Icons
- **Testing:** JUnit 5, Mockito, Spring Boot Test (MockMvc)

---

## 📋 Prerequisites

1. **Java Development Kit (JDK 21+)**
2. **MySQL Server 8.0+** running on port `3306`
3. Git

---

## ⚙️ Configuration & Setup

### 1. Database Setup
Log in to your local MySQL instance and create the database:
```sql
CREATE DATABASE IF NOT EXISTS hospital_db;
```

### 2. Configure Environment Variables
To keep credentials secure and prevent committing passwords to source control, configure your MySQL credentials as environment variables:

**Windows PowerShell:**
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
```

**Linux / macOS:**
```bash
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
```

---

## 🏃 Running the Application

Using the included Maven Wrapper:

**Windows:**
```powershell
.\mvnw.cmd "-Djava.net.preferIPv4Stack=true" spring-boot:run
```

**Linux / macOS:**
```bash
./mvnw -Djava.net.preferIPv4Stack=true spring-boot:run
```

Once started, navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 🧪 Running Automated Tests

Run the full test suite (uses test-scoped H2 in-memory database; no live MySQL server required):
```powershell
.\mvnw.cmd "-Djava.net.preferIPv4Stack=true" clean test
```

---

## 📂 Project Structure

```text
Hospital_management/
├── .mvn/wrapper/               # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/hospital/hospital_management/
│   │   │   ├── controller/     # HomeController, PatientController
│   │   │   ├── model/          # Patient entity
│   │   │   ├── repository/     # PatientRepository
│   │   │   ├── service/        # PatientService
│   │   │   └── HospitalManagementApplication.java
│   │   └── resources/
│   │       ├── static/         # CSS and JavaScript assets
│   │       ├── templates/      # Thymeleaf views (fragments, patient, index)
│   │       └── application.properties
│   └── test/                   # JUnit 5 & Mockito test suites
├── pom.xml                     # Maven project configuration
├── README.md                   # Project documentation
└── .gitignore                  # Git ignore rules
```
