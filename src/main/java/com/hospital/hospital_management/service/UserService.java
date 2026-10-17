package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.Role;
import com.hospital.hospital_management.model.User;
import com.hospital.hospital_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(User user, String rawPassword) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        String cleanUsername = user.getUsername().trim().toLowerCase();
        if (userRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("Username '" + cleanUsername + "' is already taken.");
        }
        if (rawPassword == null || rawPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Please assign a role.");
        }

        user.setUsername(cleanUsername);
        user.setPassword(passwordEncoder.encode(rawPassword.trim()));
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User updateUser(Long id, String fullName, String email, Role role, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        if (email != null) {
            user.setEmail(email.trim());
        }
        if (role != null) {
            user.setRole(role);
        }
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.trim().length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters long.");
            }
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
        }

        return userRepository.save(user);
    }

    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Prevent disabling the last active admin
        if (user.isEnabled() && user.getRole() == Role.ROLE_ADMIN) {
            long activeAdmins = userRepository.findByRoleOrderByCreatedAtDesc(Role.ROLE_ADMIN)
                    .stream().filter(User::isEnabled).count();
            if (activeAdmins <= 1) {
                throw new IllegalStateException("Cannot disable the only active Administrator account.");
            }
        }

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username != null ? username.trim().toLowerCase() : "");
    }
}
