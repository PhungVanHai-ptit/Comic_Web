package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.dto.request.UserCreationRequest;
import com.haiphung.comic_web.entity.User;
import com.haiphung.comic_web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.haiphung.comic_web.config.CustomUserDetails;
import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private void refreshSession(User updatedUser) {
        CustomUserDetails userDetails = new CustomUserDetails(updatedUser);
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }


    @GetMapping("/{id}")
    public String showUser(@PathVariable Integer id, Model model) {
        User user = userService.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/users";
        }
        model.addAttribute("user", user);
        return "users/show";
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Integer id, Model model) {
        User user = userService.findById(id).orElse(null);
        if (user == null) return "redirect:/users";
        UserCreationRequest form = new UserCreationRequest();
        form.setEmail(user.getEmail());
        form.setFullName(user.getFullName());
        model.addAttribute("userForm", form);
        model.addAttribute("userId", id);
        return "users/edit";
    }

    @PostMapping("/{id}")
    public String updateUser(@PathVariable Integer id,
                             @ModelAttribute("userForm") @Valid UserCreationRequest request,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userForm", bindingResult);
            redirectAttributes.addFlashAttribute("userForm", request);
            return "redirect:/users/" + id + "/edit";
        }
        try {
            userService.updateUser(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Người dùng cập nhật thành công");
            return "redirect:/users";
        } catch (RuntimeException ex) {
            bindingResult.reject("error.global", ex.getMessage());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userForm", bindingResult);
            redirectAttributes.addFlashAttribute("userForm", request);
            return "redirect:/users/" + id + "/edit";
        }
    }

    // ==================== USER PROFILE: Logged-in User ====================

    /**
     * View current user's profile
     */
    @GetMapping("/profile")
    public String viewProfile(org.springframework.security.core.Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        
        String email = authentication.getName();
        User user = userService.findByEmail(email).orElse(null);
        
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("isEditMode", false);
        return "profile";
    }

    /**
     * Show edit profile form for current user
     */
    @GetMapping("/profile/edit")
    public String editProfileForm(org.springframework.security.core.Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        
        String email = authentication.getName();
        User user = userService.findByEmail(email).orElse(null);
        
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        if (!model.containsAttribute("userForm")) {
            UserCreationRequest form = new UserCreationRequest();
            form.setEmail(user.getEmail());
            form.setFullName(user.getFullName());
            model.addAttribute("userForm", form);
        }
        
        model.addAttribute("user", user);
        model.addAttribute("isEditMode", true);
        return "profile";
    }

    /**
     * Update current user's profile
     */
    @PostMapping("/profile/edit")
    public String updateProfile(org.springframework.security.core.Authentication authentication,
                                @RequestParam(required = false) String oldPassword,
                                @RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String password,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }
        
        String email = authentication.getName();
        User user = userService.findByEmail(email).orElse(null);
        
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        // Validate full name is provided
        if (fullName == null || fullName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập họ và tên");
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/users/profile/edit";
        }
        
        try {
            // If password change is requested
            if (password != null && !password.isEmpty()) {
                // Validate old password
                if (oldPassword == null || oldPassword.isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập mật khẩu cũ để đổi mật khẩu");
                    redirectAttributes.addFlashAttribute("user", user);
                    return "redirect:/users/profile/edit";
                }
                
                // Verify old password
                if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu cũ không đúng");
                    redirectAttributes.addFlashAttribute("user", user);
                    return "redirect:/users/profile/edit";
                }
                
                // Validate password meets all criteria
                boolean hasLength = password.length() >= 8;
                boolean hasUppercase = password.matches(".*[A-Z].*");
                boolean hasNumber = password.matches(".*[0-9].*");
                
                if (!hasLength || !hasUppercase || !hasNumber) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải có ít nhất 8 ký tự, 1 chữ hoa (A-Z) và 1 số (0-9)");
                    redirectAttributes.addFlashAttribute("user", user);
                    return "redirect:/users/profile/edit";
                }
            }
            
            // Create update request with validated data
            UserCreationRequest updateRequest = new UserCreationRequest();
            updateRequest.setEmail(user.getEmail());
            updateRequest.setFullName(fullName.trim());
            
            // Only set password if provided and validated
            if (password != null && !password.isEmpty()) {
                updateRequest.setPassword(password);
            }
            
            // Update user
            User updatedUser = userService.updateUser(user.getUserId(), updateRequest);
            refreshSession(updatedUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công!");
            return "redirect:/users/profile";
            
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/users/profile/edit";
        }
    }

    /**
     * Upload avatar for current user (AJAX endpoint)
     */
    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            org.springframework.security.core.Authentication authentication,
            @RequestParam("avatar") MultipartFile avatarFile) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Chưa đăng nhập"));
        }
        
        try {
            if (avatarFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng chọn ảnh"));
            }

            String email = authentication.getName();
            User user = userService.findByEmail(email).orElse(null);
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Người dùng không tồn tại"));
            }
            
            User updatedUser = userService.updateAvatar(user.getUserId(), avatarFile);
            refreshSession(updatedUser);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật ảnh đại diện thành công",
                    "avatarUrl", updatedUser.getAvatarUrl()
            ));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi tải lên ảnh: " + ex.getMessage()
            ));
        }
    }
}
