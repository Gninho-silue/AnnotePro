package com.example.annotationPlatform.service;

import com.example.annotationPlatform.exception.OperationNonPermitException;
import com.example.annotationPlatform.exception.RessourceNotFoundException;
import com.example.annotationPlatform.model.Role;
import com.example.annotationPlatform.model.User;
import com.example.annotationPlatform.repository.RoleRepository;
import com.example.annotationPlatform.repository.UserRepository;
import com.example.annotationPlatform.util.Constants;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing user-related operations, including creation, updating, and role management.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a secure random password.
     *
     * @return a randomly generated password
     */
    private String generateSecurePassword() {
        StringBuilder password = new StringBuilder(12);
        
        // Ensure at least one character from each category
        password.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        password.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));
        
        // Fill the rest with random characters
        for (int i = 4; i < 12; i++) {
            password.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }
        
        // Shuffle the password characters
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }

    // --- READ Operations ---

    /**
     * Retrieves all users in the system.
     *
     * @return list of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves all users with the annotator role.
     *
     * @return list of annotator users
     */
    public List<User> getAllAnnotators() {
        return userRepository.findByRoles_Name(Constants.ROLE_ANNOTATOR);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the user ID
     * @return the user
     * @throws RessourceNotFoundException if user not found
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RessourceNotFoundException("User not found with ID: " + id));
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username the username
     * @return the user
     * @throws RessourceNotFoundException if user not found
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RessourceNotFoundException("User not found with username: " + username));
    }

    // --- CREATE Operation ---

    /**
     * Creates a new user with the specified role and sends a welcome email.
     *
     * @param user     the user details
     * @param roleName the role to assign
     * @throws OperationNonPermitException if a username or email already exists
     * @throws MessagingException if there's an error sending the welcome email
     */
    @Transactional
    public void createUser(User user, String roleName) throws MessagingException {
        if (userRepository.existsByUsername(user.getUsername()) || userRepository.existsByEmail(user.getEmail())) {
            throw new OperationNonPermitException("User already exists !");
        }

        // Generate a secure random password
        String generatedPassword = generateSecurePassword();
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setCreateDate(LocalDateTime.now());
        user.setEnable(true);
        user.setAccountLocked(false);

        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(roleName);
                    return roleRepository.save(newRole);
                });

        List<Role> roles = new ArrayList<>();
        roles.add(role);
        user.setRoles(roles);

        userRepository.save(user);
        
        // Send welcome email with credentials
        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername(), generatedPassword);
    }
    

    // --- UPDATE Operations ---

    /**
     * Updates an existing user's details.
     *
     * @param id          the user ID
     * @param userDetails the updated user details
     * @throws RessourceNotFoundException if user not found
     * @throws OperationNonPermitException if username or email already exists
     */
    @Transactional
    public void updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        
        // Check if username or email is being changed and if it already exists
        if (!user.getUsername().equals(userDetails.getUsername()) && 
            userRepository.existsByUsername(userDetails.getUsername())) {
            throw new OperationNonPermitException("Username already exists");
        }
        
        if (!user.getEmail().equals(userDetails.getEmail()) && 
            userRepository.existsByEmail(userDetails.getEmail())) {
            throw new OperationNonPermitException("Email already exists");
        }

        // Update basic information
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setLastModifiedDate(LocalDateTime.now());

        userRepository.save(user);
        logger.info("User updated successfully: {}", user.getUsername());
    }

    /**
     * Updates the roles of a user.
     *
     * @param userId    the user ID
     * @param roleNames the list of role names to assign
     * @throws RessourceNotFoundException if user not found
     */
    @Transactional
    public void updateUserRoles(Long userId, List<String> roleNames) {
        User user = getUserById(userId);
        List<Role> roles = new ArrayList<>();

        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName(roleName);
                        return roleRepository.save(newRole);
                    });
            roles.add(role);
        }
        user.setRoles(roles);
        userRepository.save(user);
    }

    /**
     * Toggles the enabled status of a user.
     *
     * @param id the user ID
     * @return the updated user
     * @throws RessourceNotFoundException if user not found
     */
    @Transactional
    public User toggleUserStatus(Long id) {
        User user = getUserById(id);
        user.setEnable(!user.isEnabled());
        return userRepository.save(user);
    }

    /**
     * Toggles the account lock status of a user.
     *
     * @param id the user ID
     * @return the updated user
     * @throws RessourceNotFoundException if user not found
     */
    @Transactional
    public User toggleLockStatus(Long id) {
        User user = getUserById(id);
        user.setAccountLocked(!user.getAccountLocked());
        return userRepository.save(user);
    }

    /**
     * Allows a user to change their own password.
     *
     * @param userId          the ID of the user changing their password
     * @param currentPassword the user's current password
     * @param newPassword     the new password to set
     * @throws RessourceNotFoundException if user not found
     * @throws OperationNonPermitException if current password is incorrect or new password doesn't meet requirements
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new OperationNonPermitException("Current password is incorrect");
        }
        
        // Validate new password complexity
        if (newPassword.length() < 6) {
            throw new OperationNonPermitException("Password must be at least 6 characters long");
        }
        
        // Check if password contains at least one of each: uppercase, lowercase, digit, special char
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : newPassword.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (SPECIAL.indexOf(c) >= 0) hasSpecial = true;
        }
        
        if (!(hasUpper && hasLower && hasDigit && hasSpecial)) {
            throw new OperationNonPermitException(
                "Password must contain at least one uppercase letter, one lowercase letter, " +
                "one digit, and one special character");
        }
        
        // Check if new password is different from current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new OperationNonPermitException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastModifiedDate(LocalDateTime.now());
        userRepository.save(user);
        
        // Send notification email
        try {
            emailService.sendPasswordChangeNotification(user.getEmail(), user.getUsername());
            logger.info("Password changed successfully for user: {}", user.getUsername());
        } catch (MessagingException e) {
            logger.error("Failed to send password change notification to user: {}", user.getUsername(), e);
        }
    }

    // --- DELETE Operation ---

    /**
     * Deletes a user by their ID.
     *
     * @param id the user ID
     * @throws RessourceNotFoundException if user not found
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
}