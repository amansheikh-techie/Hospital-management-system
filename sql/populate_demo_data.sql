-- ============================================================
-- Hospital Management System - Comprehensive Demo Data Seed
-- ============================================================

USE hospital_db;

-- 1. Insert Doctors (if not already existing)
INSERT INTO doctors (id, first_name, last_name, specialization, department, contact_number, email, consultation_fee, available_days, registration_date)
VALUES
(3, 'Priya', 'Sharma', 'Senior Pediatrician', 'Pediatrics', '9876543211', 'dr.priya@hospital.com', 600.00, 'Mon, Tue, Thu, Sat (9 AM - 1 PM)', '2026-10-10'),
(4, 'Rajesh', 'Verma', 'Orthopedic Surgeon', 'Orthopedics', '9876543212', 'dr.rajesh@hospital.com', 900.00, 'Tue, Thu, Sat (11 AM - 4 PM)', '2026-10-11'),
(5, 'Ananya', 'Iyer', 'Consultant Dermatologist', 'Dermatology', '9876543213', 'dr.ananya@hospital.com', 750.00, 'Mon, Wed, Fri (1 PM - 5 PM)', '2026-10-12'),
(6, 'Vikram', 'Patel', 'Senior General Physician', 'General Medicine', '9876543214', 'dr.vikram@hospital.com', 500.00, 'Mon - Sat (8 AM - 12 PM)', '2026-10-12')
ON DUPLICATE KEY UPDATE first_name=VALUES(first_name);

-- 2. Insert Patients
INSERT INTO patients (id, first_name, last_name, age, gender, blood_group, contact_number, email, address, medical_history, registration_date)
VALUES
(2, 'Priya', 'Nair', 34, 'Female', 'B+', '9811223344', 'priya.nair@example.com', 'Flat 4B, Palm Grove Heights, Indiranagar, Bengaluru', 'No known chronic allergies; episodic migraine', '2026-10-14'),
(3, 'Amit', 'Patel', 45, 'Male', 'A+', '9822334455', 'amit.patel@example.com', '78 Green Glen Layout, Bellandur, Bengaluru', 'Type 2 Diabetes diagnosed 2021; mild hypertension', '2026-10-15'),
(4, 'Sneha', 'Kulkarni', 22, 'Female', 'AB+', '9833445566', 'sneha.k@example.com', '14 Residency Cross, Richmond Town, Bengaluru', 'Seasonal allergic rhinitis; otherwise fit', '2026-10-15'),
(5, 'Vikram', 'Malhotra', 58, 'Male', 'O-', '9844556677', 'v.malhotra@example.com', '501 Prestige Towers, Koramangala, Bengaluru', 'Post-angioplasty (2023); daily blood thinners', '2026-10-16'),
(6, 'Meera', 'Das', 67, 'Female', 'B-', '9855667788', 'meera.das@example.com', '22 Orchid Villa, Whitefield, Bengaluru', 'Bilateral knee osteoarthritis; chronic asthma', '2026-10-16')
ON DUPLICATE KEY UPDATE first_name=VALUES(first_name);

-- 3. Insert Beds
INSERT INTO beds (id, bed_number, ward, bed_type, floor, status, daily_charge, description, created_at)
VALUES
(4, 'B-102', 'General Ward A', 'GENERAL', '1st Floor', 'AVAILABLE', 500.00, 'Standard hospital bed with IV stand', NOW()),
(5, 'B-103', 'General Ward A', 'GENERAL', '1st Floor', 'OCCUPIED', 500.00, 'Assigned to Inpatient', NOW()),
(6, 'B-104', 'General Ward B', 'GENERAL', '1st Floor', 'AVAILABLE', 500.00, 'Standard hospital bed with window view', NOW()),
(7, 'ICU-02', 'Intensive Care Unit', 'ICU', '2nd Floor', 'OCCUPIED', 3500.00, 'Equipped with multi-parameter cardiac monitor and ventilator', NOW()),
(8, 'ICU-03', 'Intensive Care Unit', 'ICU', '2nd Floor', 'AVAILABLE', 3500.00, 'Advanced ICU critical care bed with telemetry', NOW()),
(9, 'PVT-202', 'Private Ward', 'PRIVATE', '2nd Floor', 'OCCUPIED', 2000.00, 'Private en-suite room with attendant couch and television', NOW()),
(10, 'PVT-203', 'Private Ward', 'PRIVATE', '2nd Floor', 'AVAILABLE', 2000.00, 'Private en-suite room with scenic garden view', NOW()),
(11, 'EMG-01', 'Emergency Ward', 'EMERGENCY', 'Ground Floor', 'AVAILABLE', 1500.00, 'Emergency triage and stabilization station', NOW()),
(12, 'EMG-02', 'Emergency Ward', 'EMERGENCY', 'Ground Floor', 'OCCUPIED', 1500.00, 'Active acute emergency observation bed', NOW()),
(13, 'SP-301', 'Semi-Private Ward', 'SEMI_PRIVATE', '3rd Floor', 'AVAILABLE', 1200.00, 'Shared two-patient room with divider curtain', NOW())
ON DUPLICATE KEY UPDATE ward=VALUES(ward);

-- 4. Insert Admissions (linking occupied beds)
INSERT INTO admissions (id, patient_id, bed_id, attending_doctor_id, admission_date, admission_reason, status, discharge_date, discharge_notes, created_at)
VALUES
(2, 2, 5, 3, '2026-10-15 09:30:00', 'Acute gastroenteritis with mild dehydration', 'ACTIVE', NULL, NULL, '2026-10-15 09:30:00'),
(3, 3, 7, 1, '2026-10-16 14:15:00', 'Unstable angina observation & continuous telemetry monitoring', 'ACTIVE', NULL, NULL, '2026-10-16 14:15:00'),
(4, 5, 9, 4, '2026-10-14 11:00:00', 'Post-operative orthopedic rehabilitation and physical therapy', 'ACTIVE', NULL, NULL, '2026-10-14 11:00:00'),
(5, 6, 12, 6, '2026-10-17 08:30:00', 'Acute severe bronchitis exacerbation & oxygen support', 'ACTIVE', NULL, NULL, '2026-10-17 08:30:00')
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- 5. Insert Appointments across doctors, dates, and statuses
INSERT INTO appointments (id, patient_id, doctor_id, appointment_date, start_time, end_time, reason, status, created_at)
VALUES
(2, 2, 1, '2026-10-17', '11:00:00', '11:30:00', 'Follow-up ECG and blood pressure review', 'COMPLETED', NOW()),
(3, 4, 5, '2026-10-17', '14:00:00', '14:30:00', 'Dermatological assessment for persistent eczema flare-up', 'SCHEDULED', NOW()),
(4, 5, 4, '2026-10-17', '15:00:00', '15:30:00', 'Orthopedic routine knee joint mobility review', 'SCHEDULED', NOW()),
(5, 1, 2, '2026-10-17', '16:00:00', '16:30:00', 'Neurology consultation for episodic tension headache', 'SCHEDULED', NOW()),
(6, 3, 6, '2026-10-18', '09:30:00', '10:00:00', 'Quarterly diabetic lipid profile and medication review', 'SCHEDULED', NOW()),
(7, 6, 3, '2026-10-18', '10:30:00', '11:00:00', 'Pulmonary and pediatric health consultation', 'SCHEDULED', NOW()),
(8, 2, 2, '2026-10-16', '15:00:00', '15:30:00', 'Migraine diagnostic evaluation', 'CANCELLED', NOW())
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- 6. Insert Bills & Invoices
INSERT INTO bills (id, bill_number, patient_id, appointment_id, admission_id, bill_date, status, subtotal, discount_amount, tax_amount, total_amount, paid_amount, balance_amount, is_preliminary, notes, created_at)
VALUES
(2, 'INV-20261016-1002', 2, 2, NULL, '2026-10-16 12:00:00', 'PAID', 1600.00, 100.00, 75.00, 1575.00, 1575.00, 0.00, 0, 'Cardiology diagnostic consultation and laboratory panels', '2026-10-16 12:00:00'),
(3, 'INV-20261017-1003', 3, NULL, 3, '2026-10-17 10:00:00', 'PARTIALLY_PAID', 4500.00, 0.00, 225.00, 4725.00, 2500.00, 2225.00, 0, 'ICU admission and cardiac troponin biomarker testing', '2026-10-17 10:00:00'),
(4, 'INV-20261017-1004', 5, 4, NULL, '2026-10-17 15:30:00', 'UNPAID', 3200.00, 200.00, 150.00, 3150.00, 0.00, 3150.00, 0, 'Orthopedic consultation, digital X-Ray bilateral, physiotherapy session', '2026-10-17 15:30:00')
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- 7. Insert Bill Items
INSERT INTO bill_items (id, bill_id, description, quantity, unit_price, line_total)
VALUES
(1, 2, 'Specialist Consultation Fee', 1, 600.00, 600.00),
(2, 2, 'Complete Blood Count (CBC) Panel', 1, 500.00, 500.00),
(3, 2, 'Resting 12-Lead Electrocardiogram (ECG)', 1, 500.00, 500.00),
(4, 3, 'ICU Critical Care Day Charge', 1, 3500.00, 3500.00),
(5, 3, 'High Sensitivity Cardiac Troponin-I Test', 1, 1000.00, 1000.00),
(6, 4, 'Orthopedic Specialist Consultation', 1, 900.00, 900.00),
(7, 4, 'Digital Knee X-Ray (Bilateral AP & Lateral)', 1, 1200.00, 1200.00),
(8, 4, 'Targeted Physical Rehabilitation Therapy', 1, 1100.00, 1100.00)
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- 8. Insert Payments
INSERT INTO payments (id, bill_id, amount, payment_method, payment_reference, transaction_reference, payment_date, notes, created_at)
VALUES
(2, 2, 1575.00, 'UPI', 'PAY-20261016-8801', 'UPI/62891048291/OKAXIS', '2026-10-16 12:15:00', 'Settled in full via Google Pay UPI', '2026-10-16 12:15:00'),
(3, 3, 2500.00, 'CARD', 'PAY-20261017-8802', 'POS-HDFC-992813', '2026-10-17 10:15:00', 'Partial settlement via HDFC debit card', '2026-10-17 10:15:00')
ON DUPLICATE KEY UPDATE amount=VALUES(amount);

-- 9. Insert Additional System Users for Staff Roles (receptionist, billing, doctor)
-- Note: password hash corresponds to existing active BCrypt hashes
INSERT INTO users (id, username, password, full_name, email, role, enabled, doctor_id, created_at)
VALUES
(3, 'receptionist', '$2a$12$F0KpzZb9P1WaC/vDhtO2iefwBJ9jIBNYGm3N2eHsWnH8wISxdrZqS', 'Front Desk Receptionist', 'reception@hospital.local', 'ROLE_RECEPTIONIST', 1, NULL, NOW()),
(4, 'billing', '$2a$12$F0KpzZb9P1WaC/vDhtO2iefwBJ9jIBNYGm3N2eHsWnH8wISxdrZqS', 'Billing & Accounts Desk', 'billing@hospital.local', 'ROLE_BILLING_STAFF', 1, NULL, NOW()),
(5, 'dr.aditya', '$2a$12$F0KpzZb9P1WaC/vDhtO2iefwBJ9jIBNYGm3N2eHsWnH8wISxdrZqS', 'Dr. Aditya Chopra', 'dr.aditya@hospital.com', 'ROLE_DOCTOR', 1, 1, NOW())
ON DUPLICATE KEY UPDATE full_name=VALUES(full_name);
