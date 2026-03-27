package com.haiphung.comic_web.service;

import com.haiphung.comic_web.dto.request.UserCreationRequest;
import com.haiphung.comic_web.entity.User;
import com.haiphung.comic_web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private MinioService minioService;


    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> searchUsers(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(keyword.trim(), keyword.trim(), pageable);
        }
        return userRepository.findAll(pageable);
    }

    public Optional<User> getUserById(Integer id) {
        return userRepository.findById(id);
    }


    @Transactional
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }


    /**
     * Find all users (for admin management)
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }


    public User updateUser(Integer id, UserCreationRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        if (!user.getEmail().equals(request.getEmail())){
            if (userRepository.findByEmail(request.getEmail()).isPresent()){
                throw new RuntimeException("Email đã tồn tại");
            }
            user.setEmail(request.getEmail());
        }

        user.setFullName(request.getFullName());
        if (request.getPassword() != null && !request.getPassword().isBlank()){
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Delete user by ID
     */
    public void deleteById(Integer id) {
        userRepository.deleteById(id);
    }

    /**
     * Update user avatar - upload to Minio and save URL
     */
    public User updateAvatar(Integer userId, MultipartFile avatarFile) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Delete old avatar if exists
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            minioService.deleteAvatar(user.getAvatarUrl());
        }

        // Upload new avatar
        String avatarUrl = minioService.uploadAvatar(userId, avatarFile);
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}
