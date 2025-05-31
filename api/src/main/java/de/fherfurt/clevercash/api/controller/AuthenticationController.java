package de.fherfurt.clevercash.api.controller;

import de.fherfurt.clevercash.api.models.input.AuthenticationRequest;
import de.fherfurt.clevercash.api.models.input.NewUserDTO;
import de.fherfurt.clevercash.api.models.output.AuthenticationResponse;
import de.fherfurt.clevercash.api.models.output.ErrorDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

/**
 * Controller class for handling user authentication operations.
 * Provides endpoints for user registration and login.
 * Interacts with {@link AuthenticationService} for authentication-related tasks
 * and {@link UserService} for user management.
 *
 * <p>Endpoints available in this controller:</p>
 * <ul>
 *     <li><b>POST /api/auth/register</b> - Register a new user with the provided details.</li>
 *     <li><b>POST /api/auth/authenticate</b> - Authenticate a user with email and password.</li>
 * </ul>
 *
 * <p>Handles exceptions and returns appropriate HTTP status codes and error messages for various error conditions:</p>
 * <ul>
 *     <li>400 Bad Request - When user input is invalid or cannot be parsed.</li>
 *     <li>403 Forbidden - When authentication fails due to invalid credentials.</li>
 *     <li>409 Conflict - When attempting to register a user with an email that already exists.</li>
 * </ul>
 *
 * @author Richard Prax, Jakob Roch
 */


@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Operations related to user authentication")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final UserService userService;

    /**
     * Registers a new user.
     *
     * @param newUserDTO the user object containing registration details
     * @return a ResponseEntity containing the newly registered user if registration is successful,
     * ResponseEntity with status code 409 (Conflict) if a user with the provided email already exists,
     * or ResponseEntity with status code 400 (Bad Request) if the user input is not valid
     */
    @Operation(summary = "Register User", description = "Register a new user with provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully registered user", content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid user input", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "User with provided email already exists", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @PostMapping(path = "/register")
    public ResponseEntity<?> register(@Parameter(description = "The new user that should be added", required = true) @RequestBody NewUserDTO newUserDTO) {
        try {
            userService.getUserByMail(newUserDTO.getEmail());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorDTO("User with provided email already exists"));
        } catch (NoSuchElementException e) { // user not found, proceed with registration
            try {
                if (!authenticationService.isValidUserInput(newUserDTO)) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(new ErrorDTO("Invalid user input"));
                }
                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(authenticationService.register(newUserDTO));
            } catch (DateTimeParseException | IllegalArgumentException e1) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorDTO(e1.getMessage()));
            }
        }
    }

    /**
     * Authenticates a user login attempt.
     *
     * @param request the object containing login credentials (email and password)
     * @return a ResponseEntity containing the user object if login is successful,
     * ResponseEntity with status code 403 (Forbidden) if login credentials are invalid
     */
    @Operation(summary = "User Login", description = "Authenticate a user with email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated user", content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "403", description = "When the authentication fails"),
    })
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@Parameter(description = "The request containing Email and password", required = true) @RequestBody AuthenticationRequest request) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authenticationService.authenticate(request));
    }
}
