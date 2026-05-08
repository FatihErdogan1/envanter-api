package org.example.inventoryapi.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendPasswordReset(String to, String tempPassword) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Geçici Şifreniz");
            helper.setText(buildPasswordResetHtml(tempPassword), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Mail gönderilemedi: " + e.getMessage(), e);
        }
    }

    private String buildPasswordResetHtml(String tempPassword) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;border:1px solid #e0e0e0;border-radius:8px;">
                  <h2 style="color:#333;">Şifre Sıfırlama</h2>
                  <p>Geçici şifreniz aşağıdadır. Giriş yaptıktan sonra şifrenizi değiştirmeniz zorunludur.</p>
                  <div style="background:#f5f5f5;padding:12px 20px;border-radius:6px;font-size:20px;letter-spacing:2px;font-weight:bold;">
                    %s
                  </div>
                  <p style="color:#888;font-size:12px;margin-top:16px;">Bu maili siz talep etmediyseniz lütfen yöneticinizle iletişime geçin.</p>
                </div>
                """.formatted(tempPassword);
    }
}
