package de.fherfurt.clevercash.api.controller;

import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.output.BankAccountDTO;
import de.fherfurt.clevercash.api.models.output.ErrorDTO;
import de.fherfurt.clevercash.api.models.output.InstallmentDTO;
import de.fherfurt.clevercash.api.models.input.NewInstallmentDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.storage.models.Installment;
import de.fherfurt.clevercash.api.service.InstallmentService;
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
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Controller class for managing operations related to installments associated with bank accounts.
 * Provides endpoints for creating, retrieving, updating, and deleting installments.
 * Handles HTTP requests and interacts with the {@link InstallmentService} to perform operations
 * on installments. Ensures that access control is enforced using {@link AuthenticationService}.
 *
 * <p>Endpoints available in this controller:</p>
 * <ul>
 *     <li><b>GET /api/users/{userID}/bankAccounts/{bankAccountID}/installments</b> - Retrieve a list of installments with optional filters.</li>
 *     <li><b>GET /api/users/{userID}/bankAccounts/{bankAccountID}/installments/{installmentID}</b> - Retrieve a specific installment by its ID.</li>
 *     <li><b>POST /api/users/{userID}/bankAccounts/{bankAccountID}/installments</b> - Add a new installment to a bank account.</li>
 *     <li><b>DELETE /api/users/{userID}/bankAccounts/{bankAccountID}/installments/{installmentID}</b> - Remove a specific installment from a bank account.</li>
 * </ul>
 *
 * <p>Handles exceptions and returns appropriate HTTP status codes and error messages for various error conditions:</p>
 * <ul>
 *     <li>400 Bad Request - When request parameters or body are invalid.</li>
 *     <li>403 Forbidden - When the user does not have the required permissions.</li>
 *     <li>404 Not Found - When the requested resource (user, bank account, or installment) is not found.</li>
 *     <li>409 Conflict - When there is a conflict, such as an existing installment with the same name.</li>
 * </ul>
 *
 * @author Richard Prax, Jakob Roch
 */

@AllArgsConstructor
@RestController
@RequestMapping(path = "/api/users/{userID}/bankAccounts/{bankAccountID}/installments")
@Tag(name = "Installment Controller", description = "Endpoints for managing installments related to bank accounts")
public class InstallmentController {
    private final InstallmentService installmentService;
    private final AuthenticationService authenticationService;

    /**
     * Retrieves installments for a specific user and bank account with optional filters.
     *
     * @param userID        the ID of the user whose installments are to be retrieved
     * @param bankAccountID the ID of the bank account to filter installments for
     * @param startDate     optional start date of the period to filter installments (inclusive), in the format yyyy-MM-dd
     * @param endDate       optional end date of the period to filter installments (inclusive), in the format yyyy-MM-dd
     * @param description   optional substring to filter installments by their description
     * @return a ResponseEntity containing the list of filtered installments, or an appropriate error status if an exception occurs
     */
    @Operation(summary = "Retrieves installments for a specific user and bank account with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved installments", content = @Content(array = @ArraySchema(schema = @Schema(implementation = InstallmentDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<?>> findInstallments(
            @Parameter(description = "ID of the user whose installments are to be retrieved", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account to filter installments for", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "Optional start date of the period to filter installments (inclusive), in the format yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "Optional end date of the period to filter installments (inclusive), in the format yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "Optional substring to filter installments by their description") @RequestParam(required = false) String description,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {

        try {
            authenticationService.verifyUserAccess(userID, request);
            List<Installment> installments = installmentService.findAllInstallmentsForUserWithFilters(userID, bankAccountID, startDate, endDate, description);
            List<InstallmentDTO> installmentDTOs = installments
                    .stream()
                    .map(Mapper::installtmentToInstallmentDTO)
                    .collect(Collectors.toList());
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(installmentDTOs);
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonList(new ErrorDTO(e.getMessage())));
        } catch (DateTimeParseException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonList(new ErrorDTO(e.getMessage())));
        } catch (AccessException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonList(new ErrorDTO(e.getMessage())));
        }
    }

    /**
     * Retrieves a specific installment associated with the specified user, bank account, and installment ID.
     *
     * @param userID        the unique identifier of the user
     * @param bankAccountID the unique identifier of the bank account
     * @param installmentID the unique identifier of the installment
     * @return a ResponseEntity containing the installment if found,
     * or ResponseEntity with status code 404 (Not Found) if the user, bank account, or installment is not found
     */
    @Operation(summary = "Retrieves a specific installment associated with the specified user, bank account, and installment ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved installment", content = @Content(schema = @Schema(implementation = InstallmentDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Installment not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @GetMapping("/{installmentID}")
    public ResponseEntity<?> findInstallmentByID(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "ID of the installment to retrieve", required = true) @PathVariable int installmentID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Installment installment = installmentService.findInstallmentByID(userID, bankAccountID, installmentID);
            InstallmentDTO installmentDTO = Mapper.installtmentToInstallmentDTO(installment);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(installmentDTO);
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
     * Adds an installment to a specific bank account associated with the specified user.
     *
     * @param userID            the unique identifier of the user
     * @param bankAccountID     the unique identifier of the bank account to which the installment will be added
     * @param newInstallmentDTO the installment to be added
     * @return a ResponseEntity containing the created installment if successful,
     * or ResponseEntity with status code 400 (Bad Request) if the user input is invalid,
     * or a ResponseEntity with status code 403 (Forbidden) when the request is made by a not authenticated user,
     * or ResponseEntity with status code 404 (Not Found) if the user, bank account, or installment is not found,
     * or ResponseEntity with status code 409 (Conflict) if there is already an active installment with this name.
     */
    @Operation(summary = "Adds an installment to a specific bank account associated with the specified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Installment added successfully", content = @Content(schema = @Schema(implementation = BankAccountDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @PostMapping
    public ResponseEntity<?> addInstallmentToBankAccount(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "The new installment that should be added", required = true) @RequestBody NewInstallmentDTO newInstallmentDTO,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            Installment newInstallment = installmentService.addInstallmentToBankAccount(userID, bankAccountID, newInstallmentDTO);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Mapper.installtmentToInstallmentDTO(newInstallment));
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
     * Removes an installment from a specific bank account associated with the specified user.
     *
     * @param userID        the unique identifier of the user
     * @param bankAccountID the unique identifier of the bank account from which the installment will be removed
     * @param installmentID the unique identifier of the installment to be removed
     * @return a ResponseEntity with status code 204 (No Content) if successful,
     * or ResponseEntity with status code 404 (Not Found) if the user, bank account, or installment is not found
     */
    @Operation(summary = "Removes an installment from a specific bank account associated with the specified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Installment removed successfully", content = @Content(schema = @Schema(implementation = BankAccountDTO.class))),
            @ApiResponse(responseCode = "403", description = "No access", content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    @DeleteMapping("/{installmentID}")
    public ResponseEntity<?> removeInstallmentFromBankAccount(
            @Parameter(description = "ID of the user", required = true) @PathVariable int userID,
            @Parameter(description = "ID of the bank account", required = true) @PathVariable int bankAccountID,
            @Parameter(description = "ID of the installment to be removed", required = true) @PathVariable int installmentID,
            @Parameter(description = "HTTP-Request which contains HTTP-Header to verify JSONWebToken", required = true) HttpServletRequest request) {
        try {
            authenticationService.verifyUserAccess(userID, request);
            installmentService.removeInstallmentFromBankAccount(userID, bankAccountID, installmentID);
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
