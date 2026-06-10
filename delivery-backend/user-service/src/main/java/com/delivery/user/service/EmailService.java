package com.delivery.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Delivery App Verification Code");

            // A clean, simple HTML email template
            String htmlContent = String.format(
                    "<h3>Welcome to the Delivery App!</h3>" +
                            "<p>Your 6-digit verification code is: <b><span style='font-size: 24px; color: #28a745;'>%s</span></b></p>" +
                            "<p>This code will expire in 5 minutes.</p>",
                    otpCode
            );

            helper.setText(htmlContent, true);

            log.info("📧 Sending OTP email to {}", toEmail);
            mailSender.send(message);
            log.info("✅ OTP email sent successfully!");

        } catch (MessagingException e) {
            log.error("❌ Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email.");
        }
    }
}