package com.haiphung.comic_web.service;

import com.haiphung.comic_web.dto.request.UserCreationRequest;
import com.haiphung.comic_web.entity.PasswordResetToken;
import com.haiphung.comic_web.entity.Role;
import com.haiphung.comic_web.entity.User;
import com.haiphung.comic_web.repository.PasswordResetTokenRepository;
import com.haiphung.comic_web.repository.RoleRepository;
import com.haiphung.comic_web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    // Temporary OTP storage (token -> otp)
    private Map<String, String> otpStore = new HashMap<>();

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final String SENDER_EMAIL = "your-email@gmail.com";

    // ==================== USER REGISTRATION ====================

    /**
     * Register a new user
     */
    public User registerUser(UserCreationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPasswordHash(encodedPassword);

        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new RuntimeException("Role USER không tồn tại"));

        user.setRole(userRole);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    // ==================== PASSWORD RESET ====================

    /**
     * Generate OTP and send to user's email
     */
    public String generateAndSendOtp(String email) throws Exception {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }

        User user = userOpt.get();

        // Generate OTP (6 digits)
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Generate token
        String token = UUID.randomUUID().toString();

        // Save OTP to memory
        otpStore.put(token, otp);

        // Create and save reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenString(token);
        resetToken.setUser(user);
        resetToken.setCreatedAt(LocalDateTime.now());
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        passwordResetTokenRepository.save(resetToken);

        // Send OTP via email
        sendOtpEmail(user.getEmail(), user.getFullName(), otp, token);

        return token;
    }

    /**
     * Send OTP to user's email
     */
    private void sendOtpEmail(String email, String fullName, String otp, String token) throws Exception {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(SENDER_EMAIL);
            message.setTo(email);
            message.setSubject("MangaRead - Mã OTP Đặt Lại Mật Khẩu");
            message.setText("Xin chào " + fullName + ",\n\n" +
                    "Mã OTP của bạn là: " + otp + "\n\n" +
                    "Mã này sẽ hết hạn trong 10 phút.\n\n" +
                    "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ MangaRead");

            mailSender.send(message);
        } catch (Exception e) {
            throw new Exception("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }

    /**
     * Verify OTP
     */
    public String verifyOtp(String token, String otp) throws Exception {
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByTokenString(token);
        if (!resetTokenOpt.isPresent()) {
            throw new RuntimeException("Token không hợp lệ");
        }

        PasswordResetToken resetToken = resetTokenOpt.get();

        // Check expiry
        if (LocalDateTime.now().isAfter(resetToken.getExpiryDate())) {
            otpStore.remove(token);
            throw new RuntimeException("Token đã hết hạn");
        }

        // Check if OTP exists
        if (!otpStore.containsKey(token)) {
            throw new RuntimeException("OTP không tìm thấy hoặc đã hết hạn");
        }

        // Verify OTP
        String storedOtp = otpStore.get(token);
        if (!storedOtp.equals(otp)) {
            throw new RuntimeException("Mã OTP không đúng");
        }

        // Remove OTP from store (one-time use)
        otpStore.remove(token);

        return token;
    }

    /**
     * Reset password with verified token
     */
    public void resetPassword(String token, String newPassword) throws Exception {
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByTokenString(token);
        String newencodedPassword = passwordEncoder.encode(newPassword);

        if (!resetTokenOpt.isPresent()) {
            throw new RuntimeException("Token không hợp lệ");
        }

        PasswordResetToken resetToken = resetTokenOpt.get();

        // Check expiry
        if (LocalDateTime.now().isAfter(resetToken.getExpiryDate())) {
            throw new RuntimeException("Token đã hết hạn");
        }

        // Update password
        User user = resetToken.getUser();
        user.setPasswordHash(newencodedPassword);
        userRepository.save(user);

        // Delete used token
        passwordResetTokenRepository.delete(resetToken);
    }

    /**
     * Check if token is valid
     */
    public boolean isTokenValid(String token) {
        Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByTokenString(token);

        if (!resetTokenOpt.isPresent()) {
            return false;
        }

        PasswordResetToken resetToken = resetTokenOpt.get();
        return LocalDateTime.now().isBefore(resetToken.getExpiryDate());
    }
}
