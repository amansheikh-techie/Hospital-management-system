package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password. Please check your credentials.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        model.addAttribute("pageTitle", "Staff Sign In");
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("pageTitle", "Reset Password");
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam(value = "username", required = false) String username,
                                       @RequestParam(value = "securityAnswer", required = false) String securityAnswer,
                                       @RequestParam(value = "newPassword", required = false) String newPassword,
                                       @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        // Validate required fields
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Username is required.");
            model.addAttribute("pageTitle", "Reset Password");
            return "auth/forgot-password";
        }

        // Validate security question: "What is your language?" -> answer must be "java"
        if (securityAnswer == null || !securityAnswer.trim().equalsIgnoreCase("java")) {
            model.addAttribute("errorMessage", "Incorrect security answer! Question: 'What is your language?' (Expected answer: java)");
            model.addAttribute("username", username);
            model.addAttribute("pageTitle", "Reset Password");
            return "auth/forgot-password";
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            model.addAttribute("errorMessage", "New password must be at least 6 characters long.");
            model.addAttribute("username", username);
            model.addAttribute("pageTitle", "Reset Password");
            return "auth/forgot-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match. Please re-enter.");
            model.addAttribute("username", username);
            model.addAttribute("pageTitle", "Reset Password");
            return "auth/forgot-password";
        }

        try {
            userService.resetPassword(username, newPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password for '" + username.trim() + "' reset successfully! Please sign in with your new password.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("pageTitle", "Reset Password");
            return "auth/forgot-password";
        }
    }

    @GetMapping("/access-denied")
    public String showAccessDeniedPage(Model model) {
        model.addAttribute("pageTitle", "Access Restricted");
        return "auth/access-denied";
    }
}
