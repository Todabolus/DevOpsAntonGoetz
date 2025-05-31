package de.fherfurt.clevercash.api.controller;

import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.output.BankAccountDTO;
import de.fherfurt.clevercash.api.models.input.NewBankAccountDTO;
import de.fherfurt.clevercash.api.models.output.ErrorDTO;
import de.fherfurt.clevercash.api.models.output.UserDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.api.service.BankAccountService;
import de.fherfurt.clevercash.storage.models.BankAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Controller class for managing operations related to bank accounts associated with users.
 * Provides endpoints for retrieving, adding, updating, and deleting bank accounts.
 * Handles HTTP requests and interacts with the {@link BankAccountService} to perform operations
 * on bank accounts. Ensures that access control is enforced using {@link AuthenticationService}.
 *
 * <p>Endpoints available in this controller:</p>
 * <ul>
 *     <li><b>GET /api/users/{userID}/bankAccounts</b> - Retrieve all bank accounts associated with the specified user.</li>
 *     <li><b>GET /api/users/{userID}/bankAccounts/{bankAccountID}</b> - Retrieve a specific bank account by its ID.</li>
 *     <li><b>POST /api/users/{userID}/bankAccounts</b> - Add a new bank account to a specific user.</li>
 *     <li><b>PUT /api/users/{userID}/bankAccounts/{bankAccountID}</b> - Update an existing bank account associated with a user.</li>
 *     <li><b>DELETE /api/users/{userID}/bankAccounts/{bankAccountID}</b> - Remove a bank account from a specific user.</li>
 * </ul>
 *
 * <p>Handles exceptions and returns appropriate HTTP status codes and error messages for various error conditions:</p>
 * <ul>
 *     <li>400 Bad Request - When request parameters or body are invalid.</li>
 *     <li>403 Forbidden - When the user does not have the required permissions.</li>
 *     <li>404 Not Found - When the requested resource (user or bank account) is not found.</li>
 *     <li>409 Conflict - When there is a conflict, such as an existing bank account with the same details.</li>
 * </ul>
 *
 * @author Richard Prax, Jakob Roch
 */

@AllArgsConstructor
@RestController
@RequestMapping(path = "/api/users/{userID}/bankAccounts")
@Tag(name = "BankAccount Controller", description = "Endpoints for managing bank accounts related to users")
public class BankAccountController {
    private final BankAccountService bankAccountService;
    private final AuthenticationService authenticationService;

    /**
     * Retrieves all bank accounts associated with the specified user.
     *
     * @param userID the unique identifier of the user
     * @return a ResponseEntity containing a list of bank accounts associated with the user if found,
     * or ResponseEntity with status code 404 (Not Found) if the user does not exist or has no bank accounts
     */
    @Operation(summary = "Retrieves all bank accounts associated with the specified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bank accounts",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BankAccountDTO.class)))),
            @ApiResponse(responseCode = "403", description = "No access",
                    content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found or no bank accounts",
                    content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<?>> findBankAccounts(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            List<BankAccount> bankAccounts = bankAccountService.findAllBankAccounts(userID);
            List<BankAccountDTO> bankAccountDTOs = bankAccounts.stream()
                    .map(Mapper::bankAccountToBankAccountDTO)
                    .collect(Collectors.toList());
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(bankAccountDTOs);
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonList(new ErrorDTO(e.getMessage())));
        } catch (AccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(List.of(new ErrorDTO(e.getMessage())));
        }
    }

    /**
     * Retrieves a single bank account associated with the specified user by its ID.
     *
     * @param userID        the unique identifier of the user
     * @param bankAccountID the unique identifier of the bank account
     * @return a ResponseEntity containing the bank account if found,
     * or ResponseEntity with status code 404 (Not Found) if the user or bank account does not exist
     */
    @Operation(summary = "Retrieves a single bank account associated with the specified user by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bank account", content = @Content(schema = @Schema(implementation = BankAccountDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or bank account not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @GetMapping("/{bankAccountID}")
    public ResponseEntity<?> findBankAccountByID(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
            BankAccountDTO bankAccountDTO = Mapper.bankAccountToBankAccountDTO(bankAccount);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(bankAccountDTO);
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
     * Adds a bank account to a specific user.
     *
     * @param userID            the unique identifier of the user to whom the bank account will be added
     * @param newBankAccountDTO the bank account to be added
     * @return a ResponseEntity containing the updated user object with the added bank account if successful,
     * or ResponseEntity with status code 400 (Bad Request) if the addition fails,
     * or ResponseEntity with status code 404 (Not Found) if the specified user does not exist
     */
    @Operation(summary = "Adds a bank account to a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bank account added successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @PostMapping
    public ResponseEntity<?> addBankAccountToUser(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "The new bank account that should be added", required = true) @RequestBody NewBankAccountDTO newBankAccountDTO,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            BankAccount newBankAccount = bankAccountService.addBankAccountToUser(userID, newBankAccountDTO);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Mapper.bankAccountToBankAccountDTO(newBankAccount));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (AlreadyExistsException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (AccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDTO(e.getMessage()));
        }
    }

    /**
     * Updates a specific bank account associated with the specified user.
     *
     * @param userID            the unique identifier of the user
     * @param bankAccountID     the unique identifier of the bank account to be updated
     * @param newBankAccountDTO the updated bank account information
     * @return a ResponseEntity containing the updated bank account if successful,
     * or ResponseEntity with status code 400 (Bad Request) if the update fails,
     * or ResponseEntity with status code 404 (Not Found) if the specified user or bank account does not exist
     */
    @Operation(summary = "Updates a specific bank account associated with the specified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bank account updated successfully", content = @Content(schema = @Schema(implementation = BankAccountDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or bank account not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))

    })
    @PutMapping("/{bankAccountID}")
    public ResponseEntity<?> updateBankAccount(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account to be updated", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "The new updated bank account", required = true) @RequestBody NewBankAccountDTO newBankAccountDTO,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            BankAccount updatedBankAccount = bankAccountService.updateBankAccount(userID, bankAccountID, newBankAccountDTO);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Mapper.bankAccountToBankAccountDTO(updatedBankAccount));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (AlreadyExistsException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (AccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDTO(e.getMessage()));
        }
    }

    /**
     * Removes a bank account from a specific user.
     *
     * @param userID        the unique identifier of the user from whom the bank account will be removed
     * @param bankAccountID the unique identifier of the bank account to be removed
     * @return a ResponseEntity with status code 204 (No Content) if successful,
     * or ResponseEntity with status code 404 (Not Found) if the specified user or bank account does not exist
     */
    @Operation(summary = "Removes a bank account from a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bank account removed successfully", content = @Content(schema = @Schema(implementation = UserDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or bank account not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @DeleteMapping("/{bankAccountID}")
    public ResponseEntity<?> deleteUserBankAccount(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account to be removed", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            bankAccountService.deleteUserBankAccount(userID, bankAccountID);
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
