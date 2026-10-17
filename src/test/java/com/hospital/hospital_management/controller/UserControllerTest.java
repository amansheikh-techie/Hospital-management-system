package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Role;
import com.hospital.hospital_management.model.User;
import com.hospital.hospital_management.repository.UserRepository;
import com.hospital.hospital_management.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private Long staffUserId;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis() % 100000;
        String username = "staff_" + ts;
        User u = new User();
        u.setUsername(username);
        u.setFullName("Staff Member");
        u.setEmail(username + "@hospital.local");
        u.setRole(Role.ROLE_RECEPTIONIST);
        User user = userService.createUser(u, "Password123!");
        staffUserId = user.getId();
    }

    @Test
    void testListUsersPageLoadsForAdmin() throws Exception {
        mockMvc.perform(get("/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("roles"));
    }

    @Test
    void testShowCreateUserFormForAdmin() throws Exception {
        mockMvc.perform(get("/users/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attributeExists("doctors"));
    }

    @Test
    void testRegisterUserSuccess() throws Exception {
        long ts = (System.currentTimeMillis() + 1) % 100000;
        String newUsername = "receptionist_" + ts;

        mockMvc.perform(post("/users").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("username", newUsername)
                        .param("password", "SecurePass123!")
                        .param("fullName", "Jane Doe")
                        .param("email", newUsername + "@hospital.local")
                        .param("role", "ROLE_RECEPTIONIST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attributeExists("successMessage"));

        assertTrue(userRepository.existsByUsername(newUsername));
    }

    @Test
    void testRegisterUserValidationFailureReturnsForm() throws Exception {
        mockMvc.perform(post("/users").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("username", "")
                        .param("password", "123")
                        .param("fullName", "")
                        .param("role", "ROLE_RECEPTIONIST"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void testToggleUserStatusTogglesEnabled() throws Exception {
        User user = userRepository.findById(staffUserId).orElseThrow();
        boolean initialStatus = user.isEnabled();

        mockMvc.perform(post("/users/" + staffUserId + "/toggle-status").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attributeExists("successMessage"));

        User updatedUser = userRepository.findById(staffUserId).orElseThrow();
        assertFalse(initialStatus == updatedUser.isEnabled());
    }

    @Test
    void testAdminCannotDisableSelf() throws Exception {
        // Register an admin user with username 'admin' if not present
        User adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            User a = new User();
            a.setUsername("admin");
            a.setFullName("Admin User");
            a.setEmail("admin@hospital.local");
            a.setRole(Role.ROLE_ADMIN);
            return userService.createUser(a, "AdminPass123!");
        });

        mockMvc.perform(post("/users/" + adminUser.getId() + "/toggle-status").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // --- Negative Authorization Tests ---

    @Test
    void testDoctorCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/users").with(user("doctor_user").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReceptionistCannotCreateUser() throws Exception {
        mockMvc.perform(post("/users").with(user("recep_user").roles("RECEPTIONIST")).with(csrf())
                        .param("username", "hacker")
                        .param("password", "Secret123!")
                        .param("fullName", "Attacker")
                        .param("role", "ROLE_ADMIN"))
                .andExpect(status().isForbidden());
    }
}
