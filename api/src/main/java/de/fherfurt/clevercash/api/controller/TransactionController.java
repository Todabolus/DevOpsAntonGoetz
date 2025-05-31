package de.fherfurt.clevercash.api.controller;

import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewTransactionDTO;
import de.fherfurt.clevercash.api.models.output.ErrorDTO;
import de.fherfurt.clevercash.api.models.output.TransactionDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.api.service.TransactionService;
import de.fherfurt.clevercash.storage.models.Transaction;
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

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Controller class for managing operations related to transactions associated with bank accounts.
 * Provides endpoints for retrieving and adding transactions.
 * Handles HTTP requests and interacts with the {@link TransactionService} to perform operations
 * on transactions. Ensures that access control is enforced using {@link AuthenticationService}.
 *
 * <p>Endpoints available in this controller:</p>
 * <ul>
 *     <li><b>GET /api/users/{userID}/bankAccounts/{bankAccountID}/transactions</b> - Retrieve a list of transactions with optional filters such as date range, transaction type, and description.</li>
 *     <li><b>GET /api/users/{userID}/bankAccounts/{bankAccountID}/transactions/{transactionID}</b> - Retrieve a specific transaction by its ID.</li>
 *     <li><b>POST /api/users/{userID}/bankAccounts/{bankAccountID}/transactions</b> - Add a new transaction to a bank account.</li>
 * </ul>
 *
 * <p>Handles exceptions and returns appropriate HTTP status codes and error messages for various error conditions:</p>
 * <ul>
 *     <li>400 Bad Request - When request parameters or body are invalid.</li>
 *     <li>403 Forbidden - When the user does not have the required permissions.</li>
 *     <li>404 Not Found - When the requested resource (user, bank account, or transaction) is not found.</li>
 * </ul>
 *
 * @author Richard Prax, Jakob Roch
 */

@AllArgsConstructor
@RestController
@RequestMapping(path = "/api/users/{userID}/bankAccounts/{bankAccountID}/transactions")
@Tag(name = "Transaction-Controller", description = "Controller to manage transactions for a specific bank account")
public class TransactionController {
    private final TransactionService transactionService;
    private final AuthenticationService authenticationService;

    /**
     * Retrieves transactions for a specific user and bank account with optional filters.
     *
     * @param userID          the ID of the user whose transactions are to be retrieved
     * @param bankAccountID   the ID of the bank account to filter transactions for
     * @param startDate       optional start date of the period to filter transactions (inclusive), in the format yyyy-MM-dd
     * @param endDate         optional end date of the period to filter transactions (inclusive), in the format yyyy-MM-dd
     * @param transactionType optional type of transaction to filter by
     * @param description     optional substring to filter transactions by their description
     * @return a ResponseEntity containing the list of filtered transactions, or an appropriate error status if an exception occurs
     */
    @GetMapping
    @Operation(summary = "Retrieve transactions", description = "Retrieve transactions for a specific user and bank account with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Transactions not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<List<?>> findTransactions(
            @Parameter(description = "ID of the user whose transactions are to be retrieved", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account to filter transactions for", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "Optional start date of the period to filter transactions (inclusive), in the format yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "Optional end date of the period to filter transactions (inclusive), in the format yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "Optional type of transaction to filter by") @RequestParam(required = false) String transactionType,
            @Parameter(description = "Optional substring to filter transactions by their description") @RequestParam(required = false) String description,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {

        try {
            authenticationService.verifyUserAccess(userID, request);
            List<Transaction> transactions = transactionService.findAllTransactionsForUserWithFilters(userID, bankAccountID, startDate, endDate, transactionType, description);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(transactions
                            .stream()
                            .map(Mapper::transactionToTransactionDTO)
                            .collect(Collectors.toList()));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(List.of(new ErrorDTO(e.getMessage())));
        } catch (DateTimeParseException | IllegalArgumentException e) {
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
     * Retrieves a specific transaction for a given bank account.
     *
     * @param userID        the ID of the user who owns the bank account
     * @param bankAccountID the ID of the bank account
     * @param transactionID the ID of the transaction to retrieve
     * @return a ResponseEntity containing the requested transaction or an appropriate HTTP status code
     */
    @GetMapping(path = "/{transactionID}")
    @Operation(summary = "Retrieve transaction by ID", description = "Retrieve a specific transaction for a given bank account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<?> findTransactionByID(
            @Parameter(description = "ID of the user who owns the bank account", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "ID of the transaction to retrieve", required = true) @PathVariable int transactionID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Transaction transaction = transactionService.findTransactionByID(userID, bankAccountID, transactionID);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Mapper.transactionToTransactionDTO(transaction));
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
     * Adds a new transaction to a specific bank account.
     *
     * @param userID            the ID of the user who owns the bank account
     * @param bankAccountID     the ID of the bank account to which the transaction will be added
     * @param newTransactionDTO the transaction to be added to the bank account
     * @return a ResponseEntity containing the updated bank account or an appropriate HTTP status code
     */
    @PostMapping
    @Operation(summary = "Add transaction", description = "Add a new transaction to a specific bank account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction added successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "User or bank account not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ResponseEntity<?> addTransactionToBankAccount(
            @Parameter(description = "ID of the user who owns the bank account", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account to which the transaction will be added", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "Transaction to be added to the bank account", required = true) @RequestBody NewTransactionDTO newTransactionDTO,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Transaction newTransaction = transactionService.addTransactionToBankAccount(userID, bankAccountID, newTransactionDTO);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Mapper.transactionToTransactionDTO(newTransaction));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDTO(e.getMessage()));
        } catch (AccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDTO(e.getMessage()));
        }
    }
}
