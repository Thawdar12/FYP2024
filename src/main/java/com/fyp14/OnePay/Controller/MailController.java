package com.fyp14.OnePay.Controller;

import com.fyp14.OnePay.Mail.MailService;
import com.fyp14.OnePay.User.User;
import com.fyp14.OnePay.User.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MailController {

    private final MailService mailService;
    private final UserRepository userRepository;

    @Autowired
    public MailController(MailService mailService, UserRepository userRepository) {
        this.mailService = mailService;
        this.userRepository = userRepository;
    }

    @GetMapping("/sendTestEmail")
    public String sendTestEmail() {
        String recipientEmail = "adwwee@icloud.com"; // Replace with the recipient's email
        mailService.sendTestEmail(recipientEmail);
        return "Test email sent successfully.";
    }

    @GetMapping("/activate")
    public ResponseEntity<String> activateAccount(@RequestParam("token") String token) {
        User user = userRepository.findByActivationToken(token);
        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid activation token.");
        }

        user.setEnabled(true);
        user.setActivationToken(null);
        userRepository.save(user);

        String htmlResponse = "<html>" +
                "<head>" +
                "<meta http-equiv=\"refresh\" content=\"3;url=/OnePay/home\" />" +
                "</head>" +
                "<body>" +
                "<p>Account activated successfully. Redirecting to home page...</p>" +
                "</body>" +
                "</html>";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlResponse);
    }

}
