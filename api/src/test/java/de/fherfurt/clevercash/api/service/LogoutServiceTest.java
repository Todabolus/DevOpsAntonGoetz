package de.fherfurt.clevercash.api.service;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

import de.fherfurt.clevercash.api.config.JwtService;
import de.fherfurt.clevercash.storage.models.Token;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.storage.repositories.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


import java.time.LocalDateTime;
import java.util.Optional;

public class LogoutServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LogoutService logoutService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLogoutNoAuthHeader() {
        // arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // act
        logoutService.logout(request, response, authentication);

        // assert
        verify(tokenRepository, never()).findByToken(anyString());
    }

    @Test
    public void testLogoutInvalidAuthHeader() {
        // arrange
        when(request.getHeader("Authorization")).thenReturn("InvalidToken");

        // act
        logoutService.logout(request, response, authentication);

        // assert
        verify(tokenRepository, never()).findByToken(anyString());
    }

    @Test
    public void testLogoutValidToken() {
        // arrange
        String jwt = "valid.jwt.token";
        String username = "user@example.com";
        Token token = new Token();
        User user = new User();
        user.setEmail(username);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(tokenRepository.findByToken(jwt)).thenReturn(Optional.of(token));
        when(jwtService.extractUsername(jwt)).thenReturn(username);
        when(userService.getUserByMail(username)).thenReturn(user);

        // act
        logoutService.logout(request, response, authentication);

        // assert
        verify(tokenRepository).findByToken(jwt);
        verify(jwtService).extractUsername(jwt);
        verify(userService).getUserByMail(username);
        verify(userService).createOrUpdateUser(user);
        assertEquals(LocalDateTime.now().getDayOfYear(), user.getLastOnline().getDayOfYear());
        assertTrue(token.isExpired());
        assertTrue(token.isRevoked());
        verify(tokenRepository).save(token);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
