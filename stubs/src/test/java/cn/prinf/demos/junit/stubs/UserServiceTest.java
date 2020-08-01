package cn.prinf.demos.junit.stubs;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UserServiceTest {

    @Test
    public void should_register() {
        UserRepository mockedUserRepository = mock(UserRepository.class);
        EmailService mockedEmailService = mock(EmailService.class);
        EncryptionService mockedEncryptionService = mock(EncryptionService.class);
        UserService userService = new UserService(mockedUserRepository, mockedEmailService, mockedEncryptionService);

        // given
        User user = new User("admin@test.com", "admin", "xxx");

        // when
        userService.register(user);

        // then
        verify(mockedEmailService).sendEmail(
                eq("admin@test.com"),
                eq("Register Notification"),
                eq("Register Account successful! your username is admin"));
    }
}
