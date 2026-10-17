package com.hospital.hospital_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6 configuration establishing form-based authentication,
 * CSRF protection, session management, and role-based access control (RBAC).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Static assets & public endpoints
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/login", "/forgot-password", "/access-denied").permitAll()

                // User Management (Admin only)
                .requestMatchers("/users/**").hasRole("ADMIN")

                // Doctor Management: Only Admin can create, edit, or delete doctors
                .requestMatchers("/doctors/new", "/doctors/*/edit", "/doctors/*/delete").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/doctors/**").hasRole("ADMIN")
                .requestMatchers("/doctors", "/doctors/*").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST", "BILLING_STAFF")

                // Patient Management: Admin & Receptionist can register/edit; Doctor & Billing can view
                .requestMatchers("/patients/new", "/patients/*/edit").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.POST, "/patients/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers("/patients", "/patients/*").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR", "BILLING_STAFF")

                // Appointments: Admin & Receptionist can schedule/reschedule; Doctor can manage status
                .requestMatchers("/appointments/new", "/appointments/*/edit").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers("/appointments/*/cancel").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")
                .requestMatchers(HttpMethod.POST, "/appointments/**").hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")
                .requestMatchers("/appointments", "/appointments/*").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST", "BILLING_STAFF")

                // Beds: Only Admin can add beds, change maintenance status; others can view
                .requestMatchers("/beds/new", "/beds/*/edit", "/beds/*/status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/beds/**").hasRole("ADMIN")
                .requestMatchers("/beds", "/beds/*").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST", "BILLING_STAFF")

                // Admissions: Admin & Receptionist can admit and discharge; others can view
                .requestMatchers("/admissions/new", "/admissions/*/discharge").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.POST, "/admissions/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers("/admissions", "/admissions/*").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST", "BILLING_STAFF")

                // Billing: Admin & Billing Staff can create invoices; others have view/print access
                .requestMatchers("/bills/new/**", "/bills/from-*").hasAnyRole("ADMIN", "BILLING_STAFF")
                .requestMatchers(HttpMethod.POST, "/bills/**").hasAnyRole("ADMIN", "BILLING_STAFF")
                .requestMatchers("/bills", "/bills/*", "/bills/*/print").hasAnyRole("ADMIN", "BILLING_STAFF", "RECEPTIONIST", "DOCTOR")

                // Payments: Admin & Billing Staff can collect payments and view payment ledgers
                .requestMatchers("/bills/*/pay", "/payments", "/payments/**").hasAnyRole("ADMIN", "BILLING_STAFF")

                // Reports: Financial reports restricted to ADMIN and BILLING_STAFF; other reports for authenticated staff
                .requestMatchers("/reports/financial", "/reports/financial/**").hasAnyRole("ADMIN", "BILLING_STAFF")
                .requestMatchers("/reports", "/reports/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST", "BILLING_STAFF")

                // Dashboard & Any other request
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }
}
