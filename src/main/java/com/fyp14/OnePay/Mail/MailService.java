package com.fyp14.OnePay.Mail;

import com.fyp14.OnePay.User.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTestEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Test Email");
        message.setText("This is a test email from OnePay.");
        message.setFrom("onepayemailservice@gmail.com");
        mailSender.send(message);
    }

    public void sendActivationEmail(User user) {
//        String activationLink = "http://localhost:8080/activate?token=" + user.getActivationToken();
        String activationLink = "https://adwwee.com/activate?token=" + user.getActivationToken();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Account Activation");
        message.setText("Please click the following link to activate your account: " + activationLink);
        message.setFrom("onepayemailservice@gmail.com");

        mailSender.send(message);
    }

    public void sendTransactionConfirmationEmail(User user, String token) {
        String confirmationLink = "https://adwwee.com/api/fds/confirmTransaction?token=" + token;
//        String confirmationLink = "http://localhost:8080/api/fds/confirmTransaction?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Confirm Your Suspicious Transaction");
        message.setText("Dear " + user.getUsername() + ",\n\n" +
                "We have detected a suspicious transaction in your account. Please confirm this transaction by clicking the link below within 5 minutes:\n" +
                confirmationLink + "\n\n" +
                "If you do not confirm, the transaction will be marked as FAILED and the amount will be credited back to your account.\n\n" +
                "Best regards,\nOnePay Team");
        message.setFrom("onepayemailservice@gmail.com");

        mailSender.send(message);
    }
}
