package com.hospital.hospital_management.config;

import com.hospital.hospital_management.model.*;
import com.hospital.hospital_management.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Automatically seeds comprehensive clinical and administrative demo data
 * whenever the target database is empty (e.g., initial Render cloud deployment or local dev).
 * Disabled in test profile to preserve unit test isolation.
 */
@Component
@Order(2)
@Profile("!test")
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final BedRepository bedRepository;
    private final AdmissionRepository admissionRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DemoDataInitializer(DoctorRepository doctorRepository,
                               PatientRepository patientRepository,
                               BedRepository bedRepository,
                               AdmissionRepository admissionRepository,
                               AppointmentRepository appointmentRepository,
                               BillRepository billRepository,
                               PaymentRepository paymentRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.bedRepository = bedRepository;
        this.admissionRepository = admissionRepository;
        this.appointmentRepository = appointmentRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Ensure default admin user exists
        ensureAdminUser();

        // If patients are already present, do not duplicate demo data
        if (patientRepository.count() > 0) {
            logger.info("DemoDataInitializer: Database already contains {} patient records. Skipping demo seeding.",
                    patientRepository.count());
            return;
        }

        logger.info("DemoDataInitializer: Empty or unseeded database detected. Seeding comprehensive demo hospital records...");

        try {
            // 1. Doctors
            List<Doctor> doctors = seedDoctors();

            // 2. Patients
            List<Patient> patients = seedPatients();

            // 3. Beds
            List<Bed> beds = seedBeds();

            // 4. Inpatient Admissions
            seedAdmissions(patients, beds, doctors);

            // 5. Appointments
            seedAppointments(patients, doctors);

            // 6. Bills & Payments
            seedBillsAndPayments(patients);

            // 7. Demo Staff Users
            seedStaffUsers(doctors);

            logger.info("DemoDataInitializer: Comprehensive demo data successfully initialized!");
        } catch (Exception ex) {
            logger.error("DemoDataInitializer: Error seeding demo data: {}", ex.getMessage(), ex);
        }
    }

    private void ensureAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("AdminPass123!"));
            admin.setFullName("System Administrator");
            admin.setEmail("admin@hospital.local");
            admin.setRole(Role.ROLE_ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            logger.info("DemoDataInitializer: Created fallback 'admin' account with password 'AdminPass123!'.");
        }
    }

    private List<Doctor> seedDoctors() {
        List<Doctor> list = new ArrayList<>();
        list.add(new Doctor("Aditya", "Chopra", "Chief Cardiologist", "Cardiology",
                "9876543210", "dr.aditya@hospital.com", 800.0, "Mon, Wed, Fri (10 AM - 2 PM)"));
        list.add(new Doctor("Aman", "Sheikh", "Chief Neurology", "Neurology",
                "9030837132", "dr.aman@hospital.com", 1000.0, "Mon, Wed, Fri (2 PM - 5 PM)"));
        list.add(new Doctor("Priya", "Sharma", "Senior Pediatrician", "Pediatrics",
                "9876543211", "dr.priya@hospital.com", 600.0, "Mon, Tue, Thu, Sat (9 AM - 1 PM)"));
        list.add(new Doctor("Rajesh", "Verma", "Orthopedic Surgeon", "Orthopedics",
                "9876543212", "dr.rajesh@hospital.com", 900.0, "Tue, Thu, Sat (11 AM - 4 PM)"));
        list.add(new Doctor("Ananya", "Iyer", "Consultant Dermatologist", "Dermatology",
                "9876543213", "dr.ananya@hospital.com", 750.0, "Mon, Wed, Fri (1 PM - 5 PM)"));
        list.add(new Doctor("Vikram", "Patel", "Senior General Physician", "General Medicine",
                "9876543214", "dr.vikram@hospital.com", 500.0, "Mon - Sat (8 AM - 12 PM)"));
        return doctorRepository.saveAll(list);
    }

    private List<Patient> seedPatients() {
        List<Patient> list = new ArrayList<>();
        list.add(new Patient("Rohan", "Gupta", 29, "Male", "O+",
                "9876543210", "rohan.gupta@example.com",
                "102 Sunshine Apartments, MG Road, Bengaluru", "Mild seasonal asthma"));
        list.add(new Patient("Priya", "Nair", 34, "Female", "B+",
                "9811223344", "priya.nair@example.com",
                "Flat 4B, Palm Grove Heights, Indiranagar, Bengaluru", "No known allergies; episodic migraine"));
        list.add(new Patient("Amit", "Patel", 45, "Male", "A+",
                "9822334455", "amit.patel@example.com",
                "78 Green Glen Layout, Bellandur, Bengaluru", "Type 2 Diabetes diagnosed 2021; mild hypertension"));
        list.add(new Patient("Sneha", "Kulkarni", 22, "Female", "AB+",
                "9833445566", "sneha.k@example.com",
                "14 Residency Cross, Richmond Town, Bengaluru", "Seasonal allergic rhinitis; otherwise fit"));
        list.add(new Patient("Vikram", "Malhotra", 58, "Male", "O-",
                "9844556677", "v.malhotra@example.com",
                "501 Prestige Towers, Koramangala, Bengaluru", "Post-angioplasty (2023); daily blood thinners"));
        list.add(new Patient("Meera", "Das", 67, "Female", "B-",
                "9855667788", "meera.das@example.com",
                "22 Orchid Villa, Whitefield, Bengaluru", "Bilateral knee osteoarthritis; chronic asthma"));
        return patientRepository.saveAll(list);
    }

    private List<Bed> seedBeds() {
        List<Bed> list = new ArrayList<>();
        list.add(new Bed("B-101", "General Ward A", BedType.GENERAL, "1st Floor", new BigDecimal("500.00"), "Standard general bed"));
        list.add(new Bed("B-102", "General Ward A", BedType.GENERAL, "1st Floor", new BigDecimal("500.00"), "Window view general bed"));

        Bed b103 = new Bed("B-103", "General Ward A", BedType.GENERAL, "1st Floor", new BigDecimal("500.00"), "Inpatient assigned");
        b103.setStatus(BedStatus.OCCUPIED);
        list.add(b103);

        list.add(new Bed("B-104", "General Ward B", BedType.GENERAL, "1st Floor", new BigDecimal("500.00"), "Standard general bed"));
        list.add(new Bed("ICU-01", "Intensive Care Unit", BedType.ICU, "2nd Floor", new BigDecimal("2500.00"), "Standard ICU critical care bed"));

        Bed icu02 = new Bed("ICU-02", "Intensive Care Unit", BedType.ICU, "2nd Floor", new BigDecimal("3500.00"), "Advanced ventilator equipped ICU bed");
        icu02.setStatus(BedStatus.OCCUPIED);
        list.add(icu02);

        list.add(new Bed("ICU-03", "Intensive Care Unit", BedType.ICU, "2nd Floor", new BigDecimal("3500.00"), "Telemetry cardiac monitor bed"));

        Bed pvt201 = new Bed("PVT-201", "Private Ward", BedType.PRIVATE, "2nd Floor", new BigDecimal("1800.00"), "Private en-suite room");
        pvt201.setStatus(BedStatus.MAINTENANCE);
        list.add(pvt201);

        Bed pvt202 = new Bed("PVT-202", "Private Ward", BedType.PRIVATE, "2nd Floor", new BigDecimal("2000.00"), "Private en-suite deluxe room");
        pvt202.setStatus(BedStatus.OCCUPIED);
        list.add(pvt202);

        list.add(new Bed("PVT-203", "Private Ward", BedType.PRIVATE, "2nd Floor", new BigDecimal("2000.00"), "Private en-suite room with balcony"));
        list.add(new Bed("EMG-01", "Emergency Ward", BedType.EMERGENCY, "Ground Floor", new BigDecimal("1500.00"), "Emergency triage station"));

        Bed emg02 = new Bed("EMG-02", "Emergency Ward", BedType.EMERGENCY, "Ground Floor", new BigDecimal("1500.00"), "Acute emergency observation bed");
        emg02.setStatus(BedStatus.OCCUPIED);
        list.add(emg02);

        list.add(new Bed("SP-301", "Semi-Private Ward", BedType.SEMI_PRIVATE, "3rd Floor", new BigDecimal("1200.00"), "Twin sharing room with curtain divider"));
        return bedRepository.saveAll(list);
    }

    private void seedAdmissions(List<Patient> patients, List<Bed> beds, List<Doctor> doctors) {
        List<Admission> list = new ArrayList<>();

        // Past discharged admission
        Admission a1 = new Admission();
        a1.setPatient(patients.get(0));
        a1.setBed(beds.get(0));
        a1.setAttendingDoctor(doctors.get(0));
        a1.setAdmissionDate(LocalDateTime.now().minusDays(2).withHour(9).withMinute(0));
        a1.setDischargeDate(LocalDateTime.now().minusDays(1).withHour(16).withMinute(0));
        a1.setAdmissionReason("Chest tightness and short-term observation");
        a1.setDischargeNotes("Patient is clinically stable and discharged with medication.");
        a1.setStatus(AdmissionStatus.DISCHARGED);
        list.add(a1);

        // Active admissions
        Admission a2 = new Admission();
        a2.setPatient(patients.get(1));
        a2.setBed(beds.get(2)); // B-103 (OCCUPIED)
        a2.setAttendingDoctor(doctors.get(2)); // Dr. Priya Sharma
        a2.setAdmissionDate(LocalDateTime.now().minusDays(2).withHour(10).withMinute(30));
        a2.setAdmissionReason("Acute gastroenteritis with mild dehydration");
        a2.setStatus(AdmissionStatus.ACTIVE);
        list.add(a2);

        Admission a3 = new Admission();
        a3.setPatient(patients.get(2));
        a3.setBed(beds.get(5)); // ICU-02 (OCCUPIED)
        a3.setAttendingDoctor(doctors.get(0)); // Dr. Aditya Chopra
        a3.setAdmissionDate(LocalDateTime.now().minusDays(1).withHour(14).withMinute(15));
        a3.setAdmissionReason("Unstable angina observation & continuous telemetry monitoring");
        a3.setStatus(AdmissionStatus.ACTIVE);
        list.add(a3);

        Admission a4 = new Admission();
        a4.setPatient(patients.get(4));
        a4.setBed(beds.get(8)); // PVT-202 (OCCUPIED)
        a4.setAttendingDoctor(doctors.get(3)); // Dr. Rajesh Verma
        a4.setAdmissionDate(LocalDateTime.now().minusDays(3).withHour(11).withMinute(0));
        a4.setAdmissionReason("Post-operative orthopedic rehabilitation and physical therapy");
        a4.setStatus(AdmissionStatus.ACTIVE);
        list.add(a4);

        Admission a5 = new Admission();
        a5.setPatient(patients.get(5));
        a5.setBed(beds.get(11)); // EMG-02 (OCCUPIED)
        a5.setAttendingDoctor(doctors.get(5)); // Dr. Vikram Patel
        a5.setAdmissionDate(LocalDateTime.now().withHour(8).withMinute(30));
        a5.setAdmissionReason("Acute severe bronchitis exacerbation & oxygen support");
        a5.setStatus(AdmissionStatus.ACTIVE);
        list.add(a5);

        admissionRepository.saveAll(list);
    }

    private void seedAppointments(List<Patient> patients, List<Doctor> doctors) {
        List<Appointment> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Today's appointments
        Appointment appt1 = new Appointment();
        appt1.setPatient(patients.get(0));
        appt1.setDoctor(doctors.get(0));
        appt1.setAppointmentDate(today);
        appt1.setStartTime(LocalTime.of(10, 0));
        appt1.setEndTime(LocalTime.of(10, 30));
        appt1.setReason("Cardiac evaluation and resting ECG");
        appt1.setStatus(AppointmentStatus.SCHEDULED);
        list.add(appt1);

        Appointment appt2 = new Appointment();
        appt2.setPatient(patients.get(1));
        appt2.setDoctor(doctors.get(0));
        appt2.setAppointmentDate(today);
        appt2.setStartTime(LocalTime.of(11, 0));
        appt2.setEndTime(LocalTime.of(11, 30));
        appt2.setReason("Follow-up ECG and blood pressure review");
        appt2.setStatus(AppointmentStatus.COMPLETED);
        list.add(appt2);

        Appointment appt3 = new Appointment();
        appt3.setPatient(patients.get(3));
        appt3.setDoctor(doctors.get(4));
        appt3.setAppointmentDate(today);
        appt3.setStartTime(LocalTime.of(14, 0));
        appt3.setEndTime(LocalTime.of(14, 30));
        appt3.setReason("Dermatological assessment for persistent eczema flare-up");
        appt3.setStatus(AppointmentStatus.SCHEDULED);
        list.add(appt3);

        Appointment appt4 = new Appointment();
        appt4.setPatient(patients.get(4));
        appt4.setDoctor(doctors.get(3));
        appt4.setAppointmentDate(today);
        appt4.setStartTime(LocalTime.of(15, 0));
        appt4.setEndTime(LocalTime.of(15, 30));
        appt4.setReason("Orthopedic routine knee joint mobility review");
        appt4.setStatus(AppointmentStatus.SCHEDULED);
        list.add(appt4);

        Appointment appt5 = new Appointment();
        appt5.setPatient(patients.get(0));
        appt5.setDoctor(doctors.get(1));
        appt5.setAppointmentDate(today);
        appt5.setStartTime(LocalTime.of(16, 0));
        appt5.setEndTime(LocalTime.of(16, 30));
        appt5.setReason("Neurology consultation for episodic tension headache");
        appt5.setStatus(AppointmentStatus.SCHEDULED);
        list.add(appt5);

        // Tomorrow's appointments
        Appointment appt6 = new Appointment();
        appt6.setPatient(patients.get(2));
        appt6.setDoctor(doctors.get(5));
        appt6.setAppointmentDate(today.plusDays(1));
        appt6.setStartTime(LocalTime.of(9, 30));
        appt6.setEndTime(LocalTime.of(10, 0));
        appt6.setReason("Quarterly diabetic lipid profile and medication review");
        appt6.setStatus(AppointmentStatus.SCHEDULED);
        list.add(appt6);

        Appointment appt7 = new Appointment();
        appt7.setPatient(patients.get(5));
        appt7.setDoctor(doctors.get(2));
        appt7.setAppointmentDate(today.plusDays(1));
        appt7.setStartTime(LocalTime.of(10, 30));
        appt7.setEndTime(LocalTime.of(11, 0));
        appt7.setReason("Pulmonary and pediatric health consultation");
        appt7.setStatus(AppointmentStatus.SCHEDULED);
        list.add(appt7);

        // Cancelled appointment
        Appointment appt8 = new Appointment();
        appt8.setPatient(patients.get(1));
        appt8.setDoctor(doctors.get(1));
        appt8.setAppointmentDate(today.minusDays(1));
        appt8.setStartTime(LocalTime.of(15, 0));
        appt8.setEndTime(LocalTime.of(15, 30));
        appt8.setReason("Migraine diagnostic evaluation");
        appt8.setStatus(AppointmentStatus.CANCELLED);
        list.add(appt8);

        appointmentRepository.saveAll(list);
    }

    private void seedBillsAndPayments(List<Patient> patients) {
        // Bill 1: Paid consultation
        Bill b1 = new Bill("INV-20261016-1002", patients.get(1), LocalDateTime.now().minusDays(1).withHour(12).withMinute(0));
        b1.addItem(new BillItem("Specialist Consultation Fee", 1, new BigDecimal("600.00")));
        b1.addItem(new BillItem("Complete Blood Count (CBC) Panel", 1, new BigDecimal("500.00")));
        b1.addItem(new BillItem("Resting 12-Lead Electrocardiogram (ECG)", 1, new BigDecimal("500.00")));
        b1.setDiscountAmount(new BigDecimal("100.00"));
        b1.setTaxAmount(new BigDecimal("75.00"));
        b1.calculateTotals();
        b1.setPaidAmount(b1.getTotalAmount());
        b1.setBalanceAmount(BigDecimal.ZERO);
        b1.setStatus(BillStatus.PAID);
        b1.setNotes("Settled in full via Google Pay UPI");
        Bill savedB1 = billRepository.save(b1);

        Payment p1 = new Payment();
        p1.setBill(savedB1);
        p1.setAmount(savedB1.getTotalAmount());
        p1.setPaymentMethod(PaymentMethod.UPI);
        p1.setPaymentReference("PAY-20261016-8801");
        p1.setTransactionReference("UPI/62891048291/OKAXIS");
        p1.setPaymentDate(LocalDateTime.now().minusDays(1).withHour(12).withMinute(15));
        p1.setNotes("Settled in full via Google Pay UPI");
        paymentRepository.save(p1);

        // Bill 2: Partially paid ICU bill
        Bill b2 = new Bill("INV-20261017-1003", patients.get(2), LocalDateTime.now().withHour(10).withMinute(0));
        b2.addItem(new BillItem("ICU Critical Care Day Charge", 1, new BigDecimal("3500.00")));
        b2.addItem(new BillItem("High Sensitivity Cardiac Troponin-I Test", 1, new BigDecimal("1000.00")));
        b2.setTaxAmount(new BigDecimal("225.00"));
        b2.calculateTotals();
        b2.setPaidAmount(new BigDecimal("2500.00"));
        b2.setBalanceAmount(b2.getTotalAmount().subtract(b2.getPaidAmount()));
        b2.setStatus(BillStatus.PARTIALLY_PAID);
        b2.setNotes("Partial payment received; balance pending discharge settlement");
        Bill savedB2 = billRepository.save(b2);

        Payment p2 = new Payment();
        p2.setBill(savedB2);
        p2.setAmount(new BigDecimal("2500.00"));
        p2.setPaymentMethod(PaymentMethod.CARD);
        p2.setPaymentReference("PAY-20261017-8802");
        p2.setTransactionReference("POS-HDFC-992813");
        p2.setPaymentDate(LocalDateTime.now().withHour(10).withMinute(15));
        p2.setNotes("Partial settlement via HDFC debit card");
        paymentRepository.save(p2);

        // Bill 3: Unpaid outpatient bill
        Bill b3 = new Bill("INV-20261017-1004", patients.get(4), LocalDateTime.now().withHour(15).withMinute(30));
        b3.addItem(new BillItem("Orthopedic Specialist Consultation", 1, new BigDecimal("900.00")));
        b3.addItem(new BillItem("Digital Knee X-Ray (Bilateral AP & Lateral)", 1, new BigDecimal("1200.00")));
        b3.addItem(new BillItem("Targeted Physical Rehabilitation Therapy", 1, new BigDecimal("1100.00")));
        b3.setDiscountAmount(new BigDecimal("200.00"));
        b3.setTaxAmount(new BigDecimal("150.00"));
        b3.calculateTotals();
        b3.setStatus(BillStatus.UNPAID);
        b3.setNotes("Pending insurance pre-authorization and patient settlement");
        billRepository.save(b3);
    }

    private void seedStaffUsers(List<Doctor> doctors) {
        String defaultHash = passwordEncoder.encode("AdminPass123!");

        if (!userRepository.existsByUsername("receptionist")) {
            User r = new User("receptionist", defaultHash, "Front Desk Receptionist", "reception@hospital.local", Role.ROLE_RECEPTIONIST);
            userRepository.save(r);
        }

        if (!userRepository.existsByUsername("billing")) {
            User b = new User("billing", defaultHash, "Billing & Accounts Desk", "billing@hospital.local", Role.ROLE_BILLING_STAFF);
            userRepository.save(b);
        }

        if (!userRepository.existsByUsername("dr.aditya")) {
            User d = new User("dr.aditya", defaultHash, "Dr. Aditya Chopra", "dr.aditya@hospital.com", Role.ROLE_DOCTOR);
            if (!doctors.isEmpty()) {
                d.setDoctor(doctors.get(0));
            }
            userRepository.save(d);
        }
    }
}
