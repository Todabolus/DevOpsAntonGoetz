package de.fherfurt.clevercash.api.controller;

import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.MappingException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewUserDTO;
import de.fherfurt.clevercash.api.models.output.ErrorDTO;
import de.fherfurt.clevercash.api.models.output.UserDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.api.service.UserService;
import de.fherfurt.clevercash.storage.models.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Controller class for managing operations related to users.
 * Provides endpoints for retrieving, updating, and deleting user information.
 * Handles HTTP requests and interacts with the {@link UserService} to perform operations
 * on users. Ensures that access control is enforced using {@link AuthenticationService}.
 *
 * <p>Endpoints available in this controller:</p>
 * <ul>
 *     <li><b>GET /api/users</b> - Retrieve a list of all users.</li>
 *     <li><b>GET /api/users/{userID}</b> - Retrieve a specific user by their ID.</li>
 *     <li><b>GET /api/users/email</b> - Retrieve a specific user by their email address.</li>
 *     <li><b>PUT /api/users/{userID}</b> - Update the details of a specific user.</li>
 *     <li><b>DELETE /api/users/{userID}</b> - Delete a specific user by their ID.</li>
 * </ul>
 *
 * <p>Handles exceptions and returns appropriate HTTP status codes and error messages for various error conditions:</p>
 * <ul>
 *     <li>400 Bad Request - When request parameters or body are invalid.</li>
 *     <li>403 Forbidden - When the user does not have the required permissions.</li>
 *     <li>404 Not Found - When the requested resource (user) is not found.</li>
 *     <li>204 No Content - When a user is successfully deleted.</li>
 * </ul>
 *
 * @author Richard Prax, Jakob Roch
 */

@AllArgsConstructor
@RestController
@RequestMapping(path = "/api/users")
@Tag(name = "User-Controller", description = "Controller to retrieve all user information")
public class UserController {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    /**
     * Retrieves a list of all users.
     *
     * @return a ResponseEntity containing a list of all users or an appropriate HTTP status code
     */
    @GetMapping
    @Operation(summary = "Retrieve all users", description = "Mapping to retrieve all users from database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successful operation")
    })
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userService
                        .getAllUsers()
                        .stream()
                        .map(Mapper::userToUserDTO)
                        .collect(Collectors.toList()));
    }

    /**
     * Retrieves a specific user by their ID.
     *
     * @param userID the ID of the user to retrieve
     * @return a ResponseEntity containing the requested user or an appropriate HTTP status code
     */
    @GetMapping(path = "/{userID}")
    @Operation(summary = "Retrieve user by ID", description = "Retrieve a specific user by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<?> getUserById(@Parameter(description = "ID of the user to be retrieved", required = true) @PathVariable int userID,
                                         @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            User user = userService.findUserByID(userID);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Mapper.userToUserDTO(user));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (AccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDTO(e.getMessage()));
        }
    }

    /**
     * Retrieves a specific user by their email address.
     *
     * @param email the email address of the user to retrieve
     * @return a ResponseEntity containing the requested user or an appropriate HTTP status code
     */
    @GetMapping(path = "/email")
    @Operation(summary = "Retrieve user by email", description = "Retrieve a specific user by their email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<?> getUserByEmail(@Parameter(description = "Email address of the user to be retrieved", required = true) @RequestParam String email,
                                            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            User user = userService.getUserByMail(email);
            authenticationService.verifyUserAccess(user.getId(), request);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Mapper.userToUserDTO(user));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (AccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDTO(e.getMessage()));
        }
    }

    /**
     * Updates the details of a specific user.
     *
     * @param userID     the ID of the user to update
     * @param newUserDTO the user object containing the updated details
     * @return a ResponseEntity containing the updated user or an appropriate HTTP status code
     */
    @PutMapping(path = "/{userID}")
    @Operation(summary = "Update user", description = "Update the details of a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<?> updateUser(@Parameter(description = "ID of the user to be updated", required = true) @PathVariable int userID,
                                        @Parameter(description = "Updated user object", required = true) @RequestBody NewUserDTO newUserDTO,
                                        @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            User updatedUser = userService.updateUser(userID, newUserDTO);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Mapper.userToUserDTO(updatedUser));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (MappingException me) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDTO(me.getMessage()));
        } catch (AccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDTO(e.getMessage()));
        }
    }

    /**
     * Deletes a specific user by their ID.
     *
     * @param userID the ID of the user to delete
     * @return a ResponseEntity with an appropriate HTTP status code
     */
    @DeleteMapping(path = "/{userID}")
    @Operation(summary = "Delete user", description = "Delete a specific user by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<?> deleteUser(@Parameter(description = "ID of the user to be deleted", required = true) @PathVariable int userID,
                                        @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            userService.deleteUser(userID);
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .build();
        } catch (AccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDTO(e.getMessage()));
        }
    }
}
