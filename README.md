# Hospital Management System (HMS)

A production-grade, full-stack Hospital Management System built with **Java 21**, **Spring Boot**, **Spring Security**, **Thymeleaf**, **Spring Data JPA**, and **MySQL**, developed for College Project-Based Learning (PBL).

---

## 🚀 Key Modules & Capabilities

1. **Executive Dashboard**:
   - Live metrics summary cards for Total Patients, Total Doctors, Total Appointments (with today's count), Occupied Beds (with available count), Pending Invoices, and Total Revenue collected.
   - Financial audit cards for total billed, collected revenue, and outstanding balance receivables.
   - Quick-action buttons for common clinical and administrative workflows.

2. **Patient Management**:
   - Comprehensive registration workflow with Bean Validation (name, age, gender, blood group, 10-digit mobile number, address, medical history).
   - Searchable patient directory with instant lookup by name, phone, or patient ID.
   - Clinical profile view with privacy controls (restricting clinical history to authorized staff).
   - Safe edit and delete workflows with modal confirmation and CSRF protection.

3. **Doctor & Schedule Management**:
   - Directory with department, specialization, consultation fees, contact details, and availability days.
   - Automatic schedule conflict prevention to eliminate double bookings.

4. **Appointment Booking & Status Lifecycle**:
   - Time-slot validation and interval overlap conflict detection.
   - Doctor schedule filtering, status updates (`SCHEDULED`, `COMPLETED`, `CANCELLED`), and quick invoice generation.
   - Role-based restrictions ensuring doctors view only their own assigned appointments.

5. **Bed Management & Inpatient Tracking**:
   - Real-time ward capacity tracking and classification tariffs (`GENERAL`, `SEMI_PRIVATE`, `PRIVATE`, `ICU`, `EMERGENCY`).
   - Pessimistic database row locking (`PESSIMISTIC_WRITE`) preventing race conditions during simultaneous admissions.
   - Maintenance status tracking and bed history.

6. **Patient Admission & Discharge**:
   - Admission tracking with attending physician assignment and bed allocation.
   - Discharge workflow with bed release and automatic invoice linking.

7. **Billing & Invoices**:
   - Unique sequential invoice generation (`INV-YYYYMMDD-XXXX`).
   - Multi-item itemized charges with quantity, unit price, discounts, and tax calculation.
   - Real-time balance recalculation and invoice status updates (`UNPAID`, `PARTIALLY_PAID`, `PAID`).
   - Clean printable patient invoice receipts.

8. **Payment Processing**:
   - Payment ledger with multi-channel support (`CASH`, `CARD`, `UPI`, `BANK_TRANSFER`).
   - Transaction reference tracking and balance settlement protection.

9. **Authentication & Role-Based Access Control (RBAC)**:
   - Form-based login/logout with BCrypt password hashing (strength 12) and CSRF protection.
   - Strict role matrix:
     - `ADMIN`: Full system administration, user management, and configuration.
     - `DOCTOR`: View patients, manage assigned appointments, and inspect bed status.
     - `RECEPTIONIST`: Patient intake, appointment scheduling, and admissions/discharges.
     - `BILLING_STAFF`: Invoice creation, payment collection, and financial auditing.
   - Admin staff user management interface (`/users`) for provisioning accounts, role assignments, and account enablement.

10. **Reports & Analytics (`/reports`)**:
    - **Appointment Report**: Date filtering, doctor breakdown, status fulfillment metrics, and distribution charts.
    - **Patient Registration Report**: Date filtering, gender distribution, and blood group breakdown.
    - **Revenue & Payment Report**: Financial audit strictly restricted to `ADMIN` and `BILLING_STAFF`.
    - **Bed Occupancy Report**: Overall occupancy rate %, ward-by-ward capacity progress bars, and tariff analysis.

---

## 🛠️ Technology Stack

- **Language:** Java 21
- **Backend Framework:** Spring Boot 4.1.1
- **Security:** Spring Security 6.x (BCrypt, Session Management, Method Security, CSRF)
- **Persistence:** Spring Data JPA / Hibernate ORM
- **Database:** MySQL 8.0+ (Production/Dev), in-memory H2 (Isolated automated testing)
- **Template Engine:** Thymeleaf with Spring Security dialect
- **Frontend:** HTML5, CSS3, Vanilla JavaScript, Bootstrap 5.3, Bootstrap Icons
- **Testing:** JUnit 5, Mockito, Spring Boot MockMvc (210+ automated tests)

---

## 📋 Prerequisites

1. **Java Development Kit (JDK 21+)**
2. **MySQL Server 8.0+** running on port `3306`
3. Git & Maven Wrapper (included)

---

## ⚙️ Configuration & Setup

### 1. Database Setup
Log in to MySQL and create the database:
```sql
CREATE DATABASE IF NOT EXISTS hospital_db;
```

### 2. Configure Environment Variables
Set your database credentials securely via environment variables:

**Windows PowerShell:**
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:ADMIN_INITIAL_PASSWORD="YourAdminPassword123!"
```

**Linux / macOS:**
```bash
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
export ADMIN_INITIAL_PASSWORD="YourAdminPassword123!"
```

---

## 🏃 Running the Application

Using the included Maven Wrapper:

**Windows PowerShell:**
```powershell
.\mvnw.cmd "-Djava.net.preferIPv4Stack=true" spring-boot:run
```

**Linux / macOS:**
```bash
./mvnw -Djava.net.preferIPv4Stack=true spring-boot:run
```

Once started, navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**

Default initial administrator username: `admin` (password set via `ADMIN_INITIAL_PASSWORD` or auto-generated in logs on first launch).

---

## 🌐 Deploying on Render (with External MySQL)

1. **Provision an External MySQL Database**:
   - Create a MySQL 8.0+ database with your preferred cloud provider (e.g., Aiven, Clever Cloud, PlanetScale, Railway, or AWS RDS).
   - Note down the host, port, database name, username, and password.

2. **Create Web Service on Render**:
   - Go to the [Render Dashboard](https://dashboard.render.com/) and select **New +** $\rightarrow$ **Web Service**.
   - Connect your GitHub repository (`Hospital-management-system`).
   - Select **Docker** as the language/runtime (Render automatically uses the project's root `Dockerfile`).
   - Choose your preferred region and instance tier (Free / Starter).

3. **Configure Environment Variables in Render**:
   Under the **Environment** tab of your Render Web Service, add the following variables:

   | Variable | Required | Description | Example |
   | :--- | :---: | :--- | :--- |
   | `DB_URL` | **Yes** | JDBC connection URL to your external MySQL database | `jdbc:mysql://<host>:<port>/<dbname>?useSSL=true&serverTimezone=UTC` |
   | `DB_USERNAME` | **Yes** | Database username | `avnadmin` / `your_user` |
   | `DB_PASSWORD` | **Yes** | Database password | `your_secure_password` |
   | `ADMIN_INITIAL_PASSWORD` | **Yes** | Initial password for the seeded `admin` account | `AdminSecure2026!` |
   | `DDL_AUTO` | Optional | Hibernate DDL strategy (defaults to `update`) | `update` |
   | `THYMELEAF_CACHE` | Optional | Enable view caching in production (defaults to `false`) | `true` |

   *(Note: Render automatically injects and manages the `PORT` variable).*

---

## 🧪 Running Automated Tests

Execute the complete test suite across all modules (runs in an isolated in-memory H2 environment; no live MySQL server required):
```powershell
.\mvnw.cmd "-Djava.net.preferIPv4Stack=true" clean test
```

---

## 📂 Project Structure

```text
Hospital_management/
├── .mvn/wrapper/
├── src/
│   ├── main/
│   │   ├── java/com/hospital/hospital_management/
│   │   │   ├── config/         # SecurityConfig, initial admin bootstrapper
│   │   │   ├── controller/     # MVC Controllers (Home, Patient, Doctor, Appt, Bed, Admission, Bill, Payment, Report, User, Auth)
│   │   │   ├── dto/report/     # Aggregation DTOs for analytics and reports
│   │   │   ├── model/          # JPA Entities and Enums
│   │   │   ├── repository/     # Spring Data JPA Repositories
│   │   │   ├── service/        # Business logic services & UserDetailsService
│   │   │   └── HospitalManagementApplication.java
│   │   └── resources/
│   │       ├── static/         # CSS styles and JavaScript
│   │       ├── templates/      # Thymeleaf views (admission, appointment, auth, bed, billing, doctor, patient, reports, users)
│   │       └── application.properties
│   └── test/                   # JUnit 5 & Mockito test suites (Controller, Service, Security, Application)
├── pom.xml
└── README.md
```
