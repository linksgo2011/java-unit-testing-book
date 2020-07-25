package cn.prinf.demos.junit.stubs;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class UserServiceTest {

    @Test
    public void should_register() {
        UserRepository mockedUserRepository = mock(UserRepository.class);
        EmailService mockedEmailService = mock(EmailService.class);
        EncryptionService mockedEncryptionService = mock(EncryptionService.class);
        new UserService(mockedUserRepository,mockedEmailService,mockedEncryptionService);
    }
}
