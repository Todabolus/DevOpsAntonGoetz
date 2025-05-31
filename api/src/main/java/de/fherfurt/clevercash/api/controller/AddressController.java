package de.fherfurt.clevercash.api.controller;

import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.MappingException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.output.AddressDTO;
import de.fherfurt.clevercash.api.models.input.NewAddressDTO;
import de.fherfurt.clevercash.api.models.output.ErrorDTO;
import de.fherfurt.clevercash.api.models.output.UserDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.storage.models.Address;
import de.fherfurt.clevercash.api.service.AddressService;
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

import java.util.NoSuchElementException;

/**
 * Controller class for handling operations related to user addresses.
 * Provides endpoints for retrieving, adding, updating, and deleting user addresses.
 * Interacts with {@link AddressService} for address-related tasks and {@link AuthenticationService}
 * to verify user access.
 *
 * <p>Endpoints available in this controller:</p>
 * <ul>
 *     <li><b>GET /api/users/{userID}/address</b> - Retrieve the address of a user by user ID.</li>
 *     <li><b>POST /api/users/{userID}/address</b> - Add a new address for a user by user ID.</li>
 *     <li><b>PUT /api/users/{userID}/address</b> - Update the address of a user by user ID.</li>
 *     <li><b>DELETE /api/users/{userID}/address</b> - Delete the address of a user by user ID.</li>
 * </ul>
 *
 * <p>Handles exceptions and returns appropriate HTTP status codes and error messages for various error conditions:</p>
 * <ul>
 *     <li>400 Bad Request - When there is a mapping error or invalid user input.</li>
 *     <li>403 Forbidden - When the user does not have permission to access or modify the address.</li>
 *     <li>404 Not Found - When the user or address cannot be found.</li>
 *     <li>204 No Content - When the address is successfully deleted.</li>
 * </ul>
 *
 * @author Richard Prax, Jakob Roch
 */

@AllArgsConstructor
@RestController
@RequestMapping(path = "/api/users/{userID}/address")
@Tag(name = "Address Controller", description = "Operations related to user addresses")
public class AddressController {
    private final AddressService addressService;
    private final AuthenticationService authenticationService;

    /**
     * Retrieves the address of a user identified by the provided user ID.
     *
     * @param userID the unique identifier of the user whose address is to be retrieved
     * @return a ResponseEntity containing the address of the user if found, otherwise returns ResponseEntity with status code 404 (Not Found),
     * or a ResponseEntity with status code 403 (Forbidden) when the request is made by a not authenticated user
     */
    @Operation(summary = "Get User Address", description = "Retrieve the address of a user by user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address successfully retrieved", content = @Content(schema = @Schema(implementation = AddressDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @GetMapping
    public ResponseEntity<?> findUserAddress(@Parameter(description = "ID of the user whose address is to be retrieved", required = true) @PathVariable int userID,
                                             @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Address address = addressService.findUserAddress(userID);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Mapper.addressToAddressDTO(address));
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
     * Adds an address for the user identified by the provided user ID.
     *
     * @param userID        the unique identifier of the user to whom the address will be added
     * @param newAddressDTO the address to be added for the user
     * @return a ResponseEntity containing the new created address if successful
     * , otherwise returns ResponseEntity with status code 404 (Not Found) when the user can not be found
     * or a ResponseEntity with status code 400 (Bad Request) if the user input was invalid
     * or a ResponseEntity with status code 403 (Forbidden) when the request is made by a not authenticated user
     */
    @Operation(summary = "Add User Address", description = "Add a new address for a user by user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully added address", content = @Content(schema = @Schema(implementation = AddressDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request due to mapping error", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @PostMapping
    public ResponseEntity<?> addUserAddress(@Parameter(description = "ID of the user to whom the address will be added", required = true) @PathVariable int userID,
                                            @Parameter(description = "The new address that should be added", required = true) @RequestBody NewAddressDTO newAddressDTO,
                                            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Address newAddress = addressService.addUserAddress(userID, newAddressDTO);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Mapper.addressToAddressDTO(newAddress));
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
     * Updates the address of the user identified by the provided user ID.
     *
     * @param userID        the unique identifier of the user whose address is to be updated
     * @param newAddressDTO the updated address for the user
     * @return a ResponseEntity containing the updated user object with the new address if successful,
     * otherwise returns ResponseEntity with status code 404 (Not Found),
     * or a ResponseEntity with status code 400 (Bad Request) when the user input was invalid
     * or a ResponseEntity with status code 403 (Forbidden) when the request is made by a not authenticated user
     */
    @Operation(summary = "Update User Address", description = "Update the address of a user by user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated address", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request due to mapping error", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @PutMapping
    public ResponseEntity<?> updateUserAddress(@Parameter(description = "ID of the user whose address is to be updated", required = true) @PathVariable int userID,
                                               @Parameter(description = "The new address that should be added", required = true) @RequestBody NewAddressDTO newAddressDTO,
                                               @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Address updatedAddress = addressService.updateUserAddress(userID, newAddressDTO);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Mapper.addressToAddressDTO(updatedAddress));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (MappingException me) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDTO(me.getMessage()));
        } catch (AccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDTO(e.getMessage()));
        }
    }

    /**
     * Deletes the address of the user identified by the provided user ID.
     *
     * @param userID the unique identifier of the user whose address is to be deleted
     * @return ResponseEntity with status code 204 (No Content)
     * , otherwise returns ResponseEntity with status code 404 (Not Found)
     * or a ResponseEntity with status code 403 (Forbidden) when the request is made by a not authenticated user
     */
    @Operation(summary = "Delete User Address", description = "Delete the address of a user by user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted address"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @DeleteMapping
    public ResponseEntity<?> deleteUserAddress(@Parameter(description = "ID of the user whose address is to be deleted", required = true) @PathVariable int userID,
                                               @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            addressService.deleteUserAddress(userID);
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .build();
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
}
