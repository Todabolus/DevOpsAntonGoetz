package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.config.JwtService;
import de.fherfurt.clevercash.storage.models.Token;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.storage.repositories.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Service class for handling user logout.
 * This service invalidates the JWT token and updates the user's last online timestamp.
 *
 * @author Richard Prax
 */
@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final UserService userService;

    /**
     * Logs out the user by invalidating the JWT token and updating the user's last online timestamp.
     *
     * @param request        the HTTP request
     * @param response       the HTTP response
     * @param authentication the authentication information
     * @throws NoSuchElementException if the user associated with the JWT token is not found
     */
    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws NoSuchElementException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        jwt = authHeader.substring(7);
        Token storedToken = tokenRepository.findByToken(jwt)
                .orElse(null);

        String username = jwtService.extractUsername(jwt);
        User user = userService.getUserByMail(username);
        user.setLastOnline(LocalDateTime.now());
        userService.createOrUpdateUser(user);

        if (storedToken != null) {
            storedToken.setExpired(true);
            storedToken.setRevoked(true);
            tokenRepository.save(storedToken);
            SecurityContextHolder.clearContext();
        }
    }
}
