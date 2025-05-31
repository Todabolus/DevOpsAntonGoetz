package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.config.JwtService;
import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.models.input.AuthenticationRequest;
import de.fherfurt.clevercash.api.models.input.NewUserDTO;
import de.fherfurt.clevercash.api.models.output.AuthenticationResponse;
import de.fherfurt.clevercash.storage.models.Token;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.storage.repositories.TokenRepository;
import de.fherfurt.clevercash.storage.util.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

public class AuthenticationServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private NewUserDTO newUserDTO;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        newUserDTO = TestUtils.getNewTestUser1();

        user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void register_ShouldReturnAuthenticationResponse() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userService.createOrUpdateUser(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwtToken");
        newUserDTO.setBirthDate("2000-01-01");

        AuthenticationResponse response = authenticationService.register(newUserDTO);

        assertNotNull(response);
        assertEquals("jwtToken", response.getAccessToken());
        assertEquals(user.getId(), response.getUserID());

        verify(userService, times(1)).createOrUpdateUser(any(User.class));
        verify(tokenRepository, times(1)).save(any(Token.class));
    }

    @Test
    void authenticate_ShouldReturnAuthenticationResponse() {
        AuthenticationRequest request = new AuthenticationRequest("john.doe@example.com", "password");
        when(userService.getUserByMail(anyString())).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwtToken");

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertNotNull(response);
        assertEquals("jwtToken", response.getAccessToken());
        assertEquals(user.getId(), response.getUserID());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenRepository, times(1)).save(any(Token.class));
    }

    @Test
    void verifyUserAccess_ShouldThrowAccessException_WhenUserIdMismatch() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer jwtToken");
        when(jwtService.extractUsername(anyString())).thenReturn("john.doe@example.com");
        when(userService.getUserByMail(anyString())).thenReturn(user);

        assertThrows(AccessException.class, () -> authenticationService.verifyUserAccess(2, request));
    }

    @Test
    void isValidUserInput_ShouldReturnTrue_WhenInputIsValid() {
        boolean isValid = authenticationService.isValidUserInput(newUserDTO);
        assertTrue(isValid);
    }

    @Test
    void isValidUserInput_ShouldReturnFalse_WhenInputIsInvalid() {
        newUserDTO.setEmail(" ");
        assertThrows(IllegalArgumentException.class, () -> authenticationService.isValidUserInput(newUserDTO));
    }
}
