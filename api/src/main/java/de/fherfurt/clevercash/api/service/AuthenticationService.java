package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.config.JwtService;
import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.models.input.AuthenticationRequest;
import de.fherfurt.clevercash.api.models.input.NewUserDTO;
import de.fherfurt.clevercash.api.models.output.AuthenticationResponse;
import de.fherfurt.clevercash.api.util.HelperFunctions;
import de.fherfurt.clevercash.storage.models.Token;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.storage.repositories.TokenRepository;
import de.fherfurt.clevercash.storage.util.Role;
import de.fherfurt.clevercash.storage.util.TokenType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * Service for handling user authentication and registration.
 *
 * @author Richard Prax
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    /**
     * Registers a new user and returns an authentication response with a JWT token.
     *
     * @param newUserDTO Data transfer object containing new user information.
     * @return AuthenticationResponse containing the user's ID and access token.
     */
    public AuthenticationResponse register(NewUserDTO newUserDTO) {
        User user = User.builder()
                .firstName(newUserDTO.getFirstName())
                .lastName(newUserDTO.getLastName())
                .email(newUserDTO.getEmail())
                .password(passwordEncoder.encode(newUserDTO.getPassword()))
                .role(Role.USER)
                .birthDate(LocalDate.parse(newUserDTO.getBirthDate()))
                .build();

        User savedUser = userService.createOrUpdateUser(user);
        String jwtToken = jwtService.generateToken(user);
        saveUserToken(savedUser, jwtToken);

        return AuthenticationResponse.builder()
                .userID(savedUser.getId())
                .accessToken(jwtToken)
                .build();
    }

    /**
     * Authenticates a user and returns an authentication response with a JWT token.
     *
     * @param request Authentication request containing the user's email and password.
     * @return AuthenticationResponse containing the user's ID and access token.
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userService.getUserByMail(request.getEmail());
        String jwtToken = jwtService.generateToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return AuthenticationResponse.builder()
                .userID(user.getId())
                .accessToken(jwtToken)
                .build();
    }

    /**
     * Saves the JWT token for the specified user.
     *
     * @param user     The user to whom the token belongs.
     * @param jwtToken The JWT token to be saved.
     */
    private void saveUserToken(User user, String jwtToken) {
        Token token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    /**
     * Revokes all valid tokens for the specified user.
     *
     * @param user The user whose tokens are to be revoked.
     */
    private void revokeAllUserTokens(User user) {
        List<Token> validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    /**
     * Verifies if the logged-in user has access to the specified user's data.
     *
     * @param userID  The ID of the user whose data is being accessed.
     * @param request The HTTP request containing the authorization header.
     * @throws NoSuchElementException If the user is not found.
     * @throws AccessException        If the logged-in user does not have access to the data.
     */
    public void verifyUserAccess(int userID, HttpServletRequest request) throws NoSuchElementException, AccessException {
        int userFromRequest = getUserIDByHttpRequest(request);
        if (userFromRequest != userID) {
            throw new AccessException("Logged in user can not access data from user with id: " + userID);
        }
    }

    /**
     * Extracts the user ID from the HTTP request.
     *
     * @param request The HTTP request containing the authorization header.
     * @return The user ID extracted from the JWT token.
     * @throws IllegalArgumentException If the authorization header is invalid.
     * @throws NoSuchElementException   If the user is not found.
     */
    private int getUserIDByHttpRequest(HttpServletRequest request) throws IllegalArgumentException, NoSuchElementException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header is invalid");
        }
        jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);
        User user = userService.getUserByMail(username);
        return user.getId();
    }

    /**
     * Validates the user input for registration.
     *
     * @param newUserDTO Data transfer object containing new user information.
     * @return True if the user input is valid, false otherwise.
     */
    public boolean isValidUserInput(NewUserDTO newUserDTO) {
        HelperFunctions.validateEmail(newUserDTO.getEmail());
        HelperFunctions.validatePassword(newUserDTO.getPassword());
        return Stream.of(newUserDTO.getEmail(), newUserDTO.getFirstName(), newUserDTO.getLastName(), newUserDTO.getPassword())
                .noneMatch(field -> field == null || field.trim().isEmpty() || field.trim().isBlank());
    }
}
