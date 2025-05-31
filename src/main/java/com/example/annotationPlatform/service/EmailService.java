package com.example.annotationPlatform.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Sends a welcome email to a new user with their credentials.
     *
     * @param to      the recipient's email address
     * @param username the username
     * @param password the generated password
     * @throws MessagingException if there's an error sending the email
     */
    public void sendWelcomeEmail(String to, String username, String password) throws MessagingException {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("password", password);
        
        String emailContent = templateEngine.process("email/welcome-email", context);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(to);
        helper.setSubject("Bienvenue sur la Plateforme d'Annotation");
        helper.setText(emailContent, true);
        
        mailSender.send(message);
    }

    /**
     * Sends a password change notification email to a user.
     *
     * @param email    the recipient's email address
     * @param username the username of the recipient
     * @throws MessagingException if there is an error sending the email
     */
    public void sendPasswordChangeNotification(String email, String username) throws MessagingException {
        Context context = new Context();
        context.setVariable("username", username);
        
        String emailContent = templateEngine.process("email/password-change-notification", context);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(email);
        helper.setSubject("Votre mot de passe a été modifié");
        helper.setText(emailContent, true);
        
        mailSender.send(message);
    }
} 