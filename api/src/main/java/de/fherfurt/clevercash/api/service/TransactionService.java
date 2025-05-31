package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewTransactionDTO;
import de.fherfurt.clevercash.api.util.HelperFunctions;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Transaction;
import de.fherfurt.clevercash.storage.util.TransactionType;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Service class for handling transactions related to bank accounts.
 * Provides functionalities to find, add, and manage transactions.
 *
 * @author Richard Prax
 */
@AllArgsConstructor
@Service
public class TransactionService {
    @Lazy
    private final BankAccountService bankAccountService;

    /**
     * Finds all transactions for a user with optional filters.
     *
     * @param userID                The ID of the user.
     * @param bankAccountID         The ID of the bank account.
     * @param startDateString       The start date for filtering transactions (yyyy-MM-dd).
     * @param endDateString         The end date for filtering transactions (yyyy-MM-dd).
     * @param transactionTypeString The type of transaction for filtering.
     * @param description           The description filter for transactions.
     * @return A list of filtered transactions.
     * @throws DateTimeParseException   If the date format is invalid.
     * @throws IllegalArgumentException If the transaction type is invalid.
     * @throws NoSuchElementException   If the bank account is not found.
     */
    public List<Transaction> findAllTransactionsForUserWithFilters(int userID, int bankAccountID, String startDateString, String endDateString, String transactionTypeString, String description) throws DateTimeParseException, IllegalArgumentException, NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        List<Transaction> transactions = bankAccount.getTransactions();

        LocalDate startDate = (startDateString == null || startDateString.isEmpty()) ? null : LocalDate.parse(startDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate endDate = (endDateString == null || endDateString.isEmpty()) ? null : LocalDate.parse(endDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        if (!isValidTransactionType(transactionTypeString)) {
            throw new IllegalArgumentException("Invalid transaction type!");
        }

        TransactionType transactionType = (transactionTypeString == null || transactionTypeString.isEmpty()) ? null : TransactionType.valueOf(transactionTypeString);

        return transactions.stream()
                .filter(transaction -> (startDate == null || !transaction.getDate().toLocalDate().isBefore(startDate)) &&
                        (endDate == null || !transaction.getDate().toLocalDate().isAfter(endDate)) &&
                        (transactionType == null || transaction.getTransactionType().equals(transactionType)) &&
                        (description == null || transaction.getDescription().contains(description)))
                .collect(Collectors.toList());
    }

    /**
     * Validates if the provided transaction type is valid.
     *
     * @param transactionType The type of transaction.
     * @return True if valid, false otherwise.
     */
    private boolean isValidTransactionType(String transactionType) {
        if (transactionType == null) {
            return true;
        }
        try {
            TransactionType.valueOf(transactionType);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * Finds a specific transaction by its ID.
     *
     * @param userID        The ID of the user.
     * @param bankAccountID The ID of the bank account.
     * @param transactionID The ID of the transaction.
     * @return The transaction with the specified ID.
     * @throws NoSuchElementException If the transaction is not found.
     */
    public Transaction findTransactionByID(int userID, int bankAccountID, int transactionID) throws NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        return bankAccount.getTransactions().stream()
                .filter(transaction -> transaction.getId() == transactionID)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Transaction not found for bankAccount with id " + bankAccountID));
    }

    /**
     * Adds a new transaction to a bank account.
     *
     * @param userID            The ID of the user.
     * @param bankAccountID     The ID of the bank account.
     * @param newTransactionDTO The data transfer object containing transaction details.
     * @return The added transaction.
     * @throws IllegalArgumentException If the transaction is invalid or cannot be added.
     * @throws NoSuchElementException   If the bank account is not found.
     */
    public Transaction addTransactionToBankAccount(int userID, int bankAccountID, NewTransactionDTO newTransactionDTO) throws IllegalArgumentException, NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        HelperFunctions.validateTransaction(newTransactionDTO);
        Transaction transaction = Mapper.newTransactionDTOToTransaction(newTransactionDTO, bankAccount);

        if (!bankAccountService.canMakeTransaction(bankAccount, transaction)) {
            throw new IllegalArgumentException("Transaction cannot be added to the bank account");
        }

        BigDecimal transactionAmount = transaction.getAmount();

        transaction.setAmount(transactionAmount.negate());
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionType(TransactionType.PAYMENT);

        BigDecimal newBalance = bankAccount.getBalance().subtract(transactionAmount);
        bankAccount.setBalance(newBalance);

        List<Transaction> updatedTransactions = new ArrayList<>(bankAccount.getTransactions());
        updatedTransactions.add(transaction);
        bankAccount.setTransactions(updatedTransactions);

        return bankAccountService.createOrUpdateBankAccount(bankAccount).getTransactions().getLast();
    }
}
