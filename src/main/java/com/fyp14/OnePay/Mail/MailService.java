package com.fyp14.OnePay.Mail;

import com.fyp14.OnePay.User.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Autowired
    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTestEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Test Email");
        message.setText("This is a test email from OnePay.");
        message.setFrom("your-email@gmail.com");

        mailSender.send(message);
    }

    public void sendActivationEmail(User user) {
        String activationLink = "http://localhost:8080/activate?token=" + user.getActivationToken();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Account Activation");
        message.setText("Please click the following link to activate your account: " + activationLink);
        message.setFrom("your-email@gmail.com");

        mailSender.send(message);
    }
}
