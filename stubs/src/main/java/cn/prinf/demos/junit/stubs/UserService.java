package cn.prinf.demos.junit.stubs;

public class UserService {
    private UserRepository userRepository;
    private EmailService emailService;
    private EncryptionService encryption;

    public UserService(UserRepository userRepository, EmailService emailService, EncryptionService encryption) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.encryption = encryption;
    }

    public void register(User user) {
        user.setPassword(encryption.sha256(user.getPassword()));

        userRepository.savedUser(user);

        String emailSubject = "Register Notification";
        String emailContent = "Register Account successful! your username is " + user.getUsername();
        emailService.sendEmail(user.getEmail(), emailSubject, emailContent);
    }
}
