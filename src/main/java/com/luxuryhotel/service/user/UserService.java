package com.luxuryhotel.service.user;

import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.common.PageResponse;
import com.luxuryhotel.dto.user.UpdateUserRequest;
import com.luxuryhotel.dto.user.UserResponse;
import com.luxuryhotel.mapper.UserMapper;
import com.luxuryhotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        return userMapper.toResponse(getCurrentUserEntity());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsersPage(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return userMapper.toResponse(findUserById(userId));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = findUserById(userId);
        boolean isTargetingCurrentUser = user.getId().equals(getCurrentUserEntity().getId());
        boolean wasAdmin = user.getRoles().contains(Role.ADMIN);
        boolean willRemainAdmin = request.roles().contains(Role.ADMIN);

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null && !request.phone().isBlank() && !request.phone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.phone().trim())) {
                throw new IllegalArgumentException("Phone is already in use");
            }
            user.setPhone(request.phone().trim());
        }

        if (wasAdmin && !willRemainAdmin && userRepository.countByRolesContaining(Role.ADMIN) <= 1) {
            throw new IllegalStateException("Cannot remove the last admin role");
        }
        if (wasAdmin && Boolean.FALSE.equals(request.enabled()) && userRepository.countByRolesContaining(Role.ADMIN) <= 1) {
            throw new IllegalStateException("Cannot disable the last admin account");
        }
        if (isTargetingCurrentUser && !willRemainAdmin) {
            throw new IllegalStateException("You cannot remove your own admin role");
        }

        user.setRoles(request.roles());
        user.setEnabled(request.enabled());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User currentUser = getCurrentUserEntity();
        if (currentUser.getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        User user = findUserById(userId);
        if (user.getRoles().contains(Role.ADMIN) && userRepository.countByRolesContaining(Role.ADMIN) <= 1) {
            throw new IllegalStateException("Cannot delete the last admin account");
        }
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
