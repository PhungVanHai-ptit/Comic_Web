package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.dto.request.UserCreationRequest;
import com.haiphung.comic_web.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // ==================== REGISTER ====================

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("userForm", new UserCreationRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("userForm") @Valid UserCreationRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userForm", bindingResult);
            redirectAttributes.addFlashAttribute("userForm", request);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi trong dữ liệu nhập. Vui lòng kiểm tra lại.");
            return "redirect:/auth/register";
        }
        try {
            authService.registerUser(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("userForm", request);
            return "redirect:/auth/register";
        } catch (Exception ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + ex.getMessage());
            redirectAttributes.addFlashAttribute("userForm", request);
            return "redirect:/auth/register";
        }
    }

    // ==================== LOGIN ====================

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Spring Security handles POST /auth/login automatically via SecurityConfig

    // ==================== FORGOT PASSWORD ====================

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/send-otp")
    public String sendOtp(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            String token = authService.generateAndSendOtp(email);
            redirectAttributes.addFlashAttribute("successMessage", "OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.");
            return "redirect:/auth/verify-otp?token=" + token;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/forgot-password";
        }
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam(required = false) String token, Model model) {
        if (token == null || !authService.isTokenValid(token)) {
            model.addAttribute("errorMessage", "Token không hợp lệ hoặc đã hết hạn");
            return "verify-otp";
        }
        model.addAttribute("token", token);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String token,
                            @RequestParam String otp,
                            RedirectAttributes redirectAttributes) {
        try {
            authService.verifyOtp(token, otp);
            redirectAttributes.addFlashAttribute("successMessage", "OTP xác thực thành công. Vui lòng nhập mật khẩu mới.");
            return "redirect:/auth/reset-password?token=" + token;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("token", token);
            return "redirect:/auth/verify-otp?token=" + token;
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
        if (token == null || !authService.isTokenValid(token)) {
            model.addAttribute("errorMessage", "Token không hợp lệ hoặc đã hết hạn");
            return "reset-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new RuntimeException("Mật khẩu không khớp");
            }

            authService.resetPassword(token, newPassword);

            redirectAttributes.addFlashAttribute("successMessage", "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập.");
            return "redirect:/auth/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("token", token);
            return "redirect:/auth/reset-password?token=" + token;
        }
    }
}