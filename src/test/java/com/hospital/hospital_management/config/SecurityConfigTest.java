package com.hospital.hospital_management.config;

import com.hospital.hospital_management.model.Role;
import com.hospital.hospital_management.model.User;
import com.hospital.hospital_management.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("testadmin")) {
            User admin = new User();
            admin.setUsername("testadmin");
            admin.setPassword(passwordEncoder.encode("SecretPass123!"));
            admin.setFullName("Test Admin");
            admin.setEmail("testadmin@hospital.local");
            admin.setRole(Role.ROLE_ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        }
    }

    @Test
    void testLoginPageIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void testAccessDeniedPageIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/access-denied"));
    }

    @Test
    void testFormLoginSuccessRedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "testadmin")
                        .param("password", "SecretPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testFormLoginFailureRedirectsToLoginWithError() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "testadmin")
                        .param("password", "WrongPassword!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @WithMockUser(username = "testadmin", roles = {"ADMIN"})
    void testLogoutSuccessRedirectsToLoginWithLogoutParam() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    void testUnauthenticatedAccessToUsersRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testNonAdminAccessToUsersReturnsForbidden() throws Exception {
        mockMvc.perform(get("/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staffuser").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminAccessToUsersPermitted() throws Exception {
        mockMvc.perform(get("/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("adminuser").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"));
    }

    @Test
    void testForgotPasswordPageIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"));
    }

    @Test
    void testForgotPasswordWithWrongSecurityAnswerFails() throws Exception {
        mockMvc.perform(post("/forgot-password").with(csrf())
                        .param("username", "testadmin")
                        .param("securityAnswer", "python")
                        .param("newPassword", "NewPass123!")
                        .param("confirmPassword", "NewPass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void testForgotPasswordWithCorrectSecurityAnswerResetsPassword() throws Exception {
        mockMvc.perform(post("/forgot-password").with(csrf())
                        .param("username", "testadmin")
                        .param("securityAnswer", "java")
                        .param("newPassword", "ResetPass123!")
                        .param("confirmPassword", "ResetPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
