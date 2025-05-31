package de.fherfurt.clevercash.api.controller;

import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewSavingDTO;
import de.fherfurt.clevercash.api.models.output.BankAccountDTO;
import de.fherfurt.clevercash.api.models.output.ErrorDTO;
import de.fherfurt.clevercash.api.models.output.SavingDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.api.service.SavingService;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Saving;
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

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Controller class for managing operations related to savings associated with bank accounts.
 * Provides endpoints for creating, retrieving, updating, and deleting savings.
 * Handles HTTP requests and interacts with the {@link SavingService} to perform operations
 * on savings. Ensures that access control is enforced using {@link AuthenticationService}.
 *
 * <p>Endpoints available in this controller:</p>
 * <ul>
 *     <li><b>GET /api/users/{userID}/bankAccounts/{bankAccountID}/savings</b> - Retrieve a list of savings with optional filters.</li>
 *     <li><b>GET /api/users/{userID}/bankAccounts/{bankAccountID}/savings/{savingID}</b> - Retrieve a specific saving by its ID.</li>
 *     <li><b>POST /api/users/{userID}/bankAccounts/{bankAccountID}/savings</b> - Add a new saving to a bank account.</li>
 *     <li><b>DELETE /api/users/{userID}/bankAccounts/{bankAccountID}/savings/{savingID}</b> - Remove a specific saving from a bank account.</li>
 *     <li><b>GET /api/users/{userID}/bankAccounts/{bankAccountID}/savings/activeSaving</b> - Retrieve the active saving for a bank account.</li>
 *     <li><b>DELETE /api/users/{userID}/bankAccounts/{bankAccountID}/savings/activeSaving</b> - Remove the active saving from a bank account.</li>
 *     <li><b>PUT /api/users/{userID}/bankAccounts/{bankAccountID}/savings/credit</b> - Credit a certain amount back to the account and deduct it from the savings.</li>
 * </ul>
 *
 * <p>Handles exceptions and returns appropriate HTTP status codes and error messages for various error conditions:</p>
 * <ul>
 *     <li>400 Bad Request - When request parameters or body are invalid.</li>
 *     <li>403 Forbidden - When the user does not have the required permissions.</li>
 *     <li>404 Not Found - When the requested resource (user, bank account, or saving) is not found.</li>
 *     <li>409 Conflict - When there is a conflict, such as an existing saving with the same name.</li>
 * </ul>
 *
 * @author Richard Prax, Jakob Roch
 */

@AllArgsConstructor
@RestController
@RequestMapping(path = "/api/users/{userID}/bankAccounts/{bankAccountID}/savings")
@Tag(name = "Saving Controller", description = "Endpoints for managing savings related to bank accounts")
public class SavingController {
    private final SavingService savingService;
    private final AuthenticationService authenticationService;

    /**
     * Retrieves savings for a specific user and bank account with optional filters.
     *
     * @param userID        the ID of the user whose savings are to be retrieved
     * @param bankAccountID the ID of the bank account to filter savings for
     * @param startDate     optional start date of the period to filter savings (inclusive), in the format yyyy-MM-dd
     * @param endDate       optional end date of the period to filter savings (inclusive), in the format yyyy-MM-dd
     * @param description   optional substring to filter savings by their description
     * @return a ResponseEntity containing the list of filtered savings, or an appropriate error status if an exception occurs
     */
    @Operation(summary = "Retrieves savings for a specific user and bank account with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved savings", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SavingDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<?>> findSavings(
            @Parameter(description = "ID of the user whose savings are to be retrieved", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account to filter savings for", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "Optional start date of the period to filter savings (inclusive), in the format yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "Optional end date of the period to filter savings (inclusive), in the format yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "Optional substring to filter savings by their description") @RequestParam(required = false) String description,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {

        try {
            authenticationService.verifyUserAccess(userID, request);
            List<Saving> savings = savingService.findAllSavingForUserWithFilters(userID, bankAccountID, startDate, endDate, description);
            List<SavingDTO> savingDTOs = savings
                    .stream()
                    .map(Mapper::savingtoSavingDTO)
                    .collect(Collectors.toList());
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(savingDTOs);
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(List.of(new ErrorDTO(e.getMessage())));
        } catch (DateTimeParseException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(List.of(new ErrorDTO(e.getMessage())));
        } catch (AccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(List.of(new ErrorDTO(e.getMessage())));
        }
    }

    /**
     * Retrieves a specific saving associated with the specified user, bank account, and saving ID.
     *
     * @param userID        the ID of the user
     * @param bankAccountID the ID of the bank account
     * @param savingID      the ID of the saving to retrieve
     * @return a ResponseEntity containing the saving if found, or an appropriate error status if not found
     */
    @Operation(summary = "Retrieves a specific saving associated with the specified user, bank account, and saving ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved saving", content = @Content(schema = @Schema(implementation = SavingDTO.class))),
            @ApiResponse(responseCode = "404", description = "Saving not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @GetMapping("/{savingID}")
    public ResponseEntity<?> findSavingByID(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "ID of the saving to retrieve", required = true) @PathVariable int savingID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Saving saving = savingService.findSavingByID(userID, bankAccountID, savingID);
            SavingDTO savingDTO = Mapper.savingtoSavingDTO(saving);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(savingDTO);
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
     * Adds a saving to a specific bank account associated with the specified user.
     *
     * @param userID        the ID of the user
     * @param bankAccountID the ID of the bank account to add the saving to
     * @param newSavingDTO  the saving to be added
     * @return a ResponseEntity containing the updated bank account with the added saving if successful,
     * or an appropriate error status if the operation fails
     */
    @Operation(summary = "Adds a saving to a specific bank account associated with the specified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Saving added successfully", content = @Content(schema = @Schema(implementation = BankAccountDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @PostMapping
    public ResponseEntity<?> addSavingToBankAccount(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "Saving to be added", required = true) @RequestBody NewSavingDTO newSavingDTO,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Saving saving = savingService.addSavingToBankAccount(userID, bankAccountID, newSavingDTO);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Mapper.savingtoSavingDTO(saving));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (AlreadyExistsException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorDTO(e.getMessage()));
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
     * Removes a saving from a specific bank account associated with the specified user.
     *
     * @param userID        the ID of the user
     * @param bankAccountID the ID of the bank account
     * @param savingID      the ID of the saving to remove
     * @return a ResponseEntity containing the updated bank account with the removed saving if successful,
     * or an appropriate error status if the saving is not found
     */
    @Operation(summary = "Removes a saving from a specific bank account associated with the specified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Saving removed successfully", content = @Content(schema = @Schema(implementation = BankAccountDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @DeleteMapping(path = "/{savingID}")
    public ResponseEntity<?> removeSavingFromBankAccount(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "ID of the saving to remove", required = true) @PathVariable int savingID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            savingService.removeSavingFromBankAccount(userID, bankAccountID, savingID);
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

    /**
     * Retrieves the active saving associated with the specified user and bank account.
     *
     * @param userID        the ID of the user
     * @param bankAccountID the ID of the bank account
     * @return a ResponseEntity containing the active saving if found, or an appropriate error status if not found
     */
    @Operation(summary = "Retrieves the active saving associated with the specified user and bank account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active saving retrieved successfully", content = @Content(schema = @Schema(implementation = SavingDTO.class))),
            @ApiResponse(responseCode = "404", description = "Active saving not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @GetMapping(path = "/activeSaving")
    public ResponseEntity<?> findActiveSaving(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Saving saving = savingService.findActiveSaving(userID, bankAccountID);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Mapper.savingtoSavingDTO(saving));
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
     * Removes the active saving from a specific bank account associated with the specified user.
     *
     * @param userID        the ID of the user
     * @param bankAccountID the ID of the bank account
     * @return a ResponseEntity containing a success message if the active saving is removed successfully,
     * or an appropriate error status if the saving is not found or if there is no active saving to remove
     */
    @Operation(summary = "Removes the active saving from a specific bank account associated with the specified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Active saving removed successfully"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @DeleteMapping(path = "/activeSaving")
    public ResponseEntity<?> removeActiveSaving(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            boolean hasRemovedActiveSaving = savingService.removeActiveSaving(userID, bankAccountID);
            if (!hasRemovedActiveSaving) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ErrorDTO("Bank account does not have an active saving"));
            }
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

    /**
     * Credits a certain amount back to the account and deducts it from the savings amount.
     *
     * @param userID        the ID of the user
     * @param bankAccountID the ID of the bank account
     * @param amountInt     the amount to be credited
     * @return a ResponseEntity containing the updated bank account if successful,
     * or an appropriate error status if the operation fails
     */
    @Operation(summary = "Credits a certain amount back to the account and deducts it from the savings amount")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Amount credited successfully", content = @Content(schema = @Schema(implementation = BankAccountDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @PutMapping(path = "/credit")
    public ResponseEntity<?> transferFromSavingToBalance(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "Amount to be credited", required = true) @RequestParam int amountInt,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            BankAccount bankAccount = savingService.transferFromSavingToBalance(userID, bankAccountID, amountInt);
            BankAccountDTO bankAccountDTO = Mapper.bankAccountToBankAccountDTO(bankAccount);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(bankAccountDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDTO(e.getMessage()));
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
