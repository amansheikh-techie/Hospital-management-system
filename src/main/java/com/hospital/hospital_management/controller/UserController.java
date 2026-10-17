package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Role;
import com.hospital.hospital_management.model.User;
import com.hospital.hospital_management.service.DoctorService;
import com.hospital.hospital_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final DoctorService doctorService;

    @Autowired
    public UserController(UserService userService, DoctorService doctorService) {
        this.userService = userService;
        this.doctorService = doctorService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("roles", Role.values());
        model.addAttribute("pageTitle", "Staff Account Management");
        return "users/list";
    }

    @GetMapping("/new")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", Role.values());
        model.addAttribute("doctors", doctorService.getAllDoctors());
        model.addAttribute("pageTitle", "Create Staff Account");
        return "users/form";
    }

    @PostMapping
    public String createUser(@ModelAttribute("user") User user,
                             @RequestParam(value = "rawPassword", required = false) String rawPassword,
                             @RequestParam(value = "password", required = false) String password,
                             @RequestParam(value = "doctorId", required = false) Long doctorId,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        String effectivePassword = (rawPassword != null && !rawPassword.isEmpty()) ? rawPassword : password;
        try {
            if (doctorId != null) {
                doctorService.getDoctorById(doctorId).ifPresent(user::setDoctor);
            }
            userService.createUser(user, effectivePassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "User account '" + user.getUsername() + "' created successfully.");
            return "redirect:/users";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("roles", Role.values());
            model.addAttribute("doctors", doctorService.getAllDoctors());
            model.addAttribute("pageTitle", "Create Staff Account");
            return "users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "User not found with ID: " + id);
            return "redirect:/users";
        }

        model.addAttribute("user", userOpt.get());
        model.addAttribute("roles", Role.values());
        model.addAttribute("doctors", doctorService.getAllDoctors());
        model.addAttribute("pageTitle", "Edit User – " + userOpt.get().getUsername());
        return "users/form";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable("id") Long id,
                             @RequestParam("fullName") String fullName,
                             @RequestParam(value = "email", required = false) String email,
                             @RequestParam("role") Role role,
                             @RequestParam(value = "newPassword", required = false) String newPassword,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, fullName, email, role, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "User account updated successfully.");
            return "redirect:/users";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/users/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long id,
                                   java.security.Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            Optional<User> targetUser = userService.getUserById(id);
            if (targetUser.isPresent() && principal != null &&
                    targetUser.get().getUsername().equalsIgnoreCase(principal.getName())) {
                redirectAttributes.addFlashAttribute("errorMessage", "You cannot disable your own administrator account.");
                return "redirect:/users";
            }
            userService.toggleUserStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "User account status updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/users";
    }
}
