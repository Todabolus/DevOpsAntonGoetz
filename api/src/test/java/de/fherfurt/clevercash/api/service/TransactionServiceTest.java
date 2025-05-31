package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.models.input.NewTransactionDTO;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Transaction;
import de.fherfurt.clevercash.storage.util.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class TransactionServiceTest {

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private TransactionService transactionService;

    private BankAccount bankAccount;
    private List<Transaction> transactions;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bankAccount = TestUtils.getTestBankAccount1();
        transactions = TestUtils.getTestTransactionList(bankAccount);
        bankAccount.setTransactions(transactions);
    }

    @Test
    void testFindAllTransactionsWithValidFilters() {
        // arrange
        int userID = 1;
        int bankAccountID = bankAccount.getId();
        String startDate = LocalDate.now().toString();
        String endDate = LocalDate.now().plusMonths(1).toString();
        String transactionType = "PAYMENT";
        String description = "Payment";

        when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);

        // act
        List<Transaction> result = transactionService.findAllTransactionsForUserWithFilters(userID, bankAccountID, startDate, endDate, transactionType, description);

        // assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        for (Transaction transaction : result) {
            assertFalse(transaction.getDate().toLocalDate().isBefore(LocalDate.parse(startDate)));
            assertFalse(transaction.getDate().toLocalDate().isAfter(LocalDate.parse(endDate)));
            assertEquals(TransactionType.valueOf(transactionType), transaction.getTransactionType());
            assertTrue(transaction.getDescription().contains(description));
        }
    }

    @Test
    void testFindAllTransactionsWithInvalidDate() {
        int userID = 1;
        int bankAccountID = bankAccount.getId();
        String invalidDate = "invalid-date";

        Mockito.when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);

        assertThrows(DateTimeParseException.class, () -> {
            transactionService.findAllTransactionsForUserWithFilters(userID, bankAccountID, invalidDate, null, null, null);
        });
    }

    @Test
    void testFindAllTransactionsWithInvalidTransactionType() {
        int userID = 1;
        int bankAccountID = bankAccount.getId();
        String transactionType = "INVALID_TYPE";

        Mockito.when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);

        assertThrows(IllegalArgumentException.class, () -> {
            transactionService.findAllTransactionsForUserWithFilters(userID, bankAccountID, null, null, transactionType, null);
        });
    }

    @Test
    void testFindAllTransactionsWithoutFilters() {
        // arrange
        int userID = 1;
        int bankAccountID = bankAccount.getId();

        Mockito.when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);

        // act
        List<Transaction> result = transactionService.findAllTransactionsForUserWithFilters(userID, bankAccountID, null, null, null, null);

        // arrange
        assertNotNull(result);
        assertEquals(transactions.size(), result.size());
    }

    @Test
    void testFindTransactionByID() {
        // arrange
        when(bankAccountService.findBankAccountByID(1, 1)).thenReturn(bankAccount);

        // act
        Transaction result = transactionService.findTransactionByID(1, 1, 2);

        // assert
        assertEquals(transactions.get(1), result);
    }

    @Test
    void testFindTransactionByIDNotFound() {
        // arrange
        when(bankAccountService.findBankAccountByID(1, 1)).thenReturn(bankAccount);

        // act
        Exception exception = assertThrows(NoSuchElementException.class, () -> {
            transactionService.findTransactionByID(1, 1, 99);
        });

        // assert
        assertEquals("Transaction not found for bankAccount with id 1", exception.getMessage());
    }

    @Test
    void testAddTransactionToBankAccount() {
        // arrange
        when(bankAccountService.findBankAccountByID(1, 1)).thenReturn(bankAccount);
        when(bankAccountService.canMakeTransaction(any(BankAccount.class), any(Transaction.class))).thenReturn(true);
        when(bankAccountService.createOrUpdateBankAccount(any(BankAccount.class))).thenReturn(bankAccount);

        NewTransactionDTO newTransaction = new NewTransactionDTO(BigDecimal.valueOf(100), "TestTransaction");
        BigDecimal balanceAfterTransaction = bankAccount.getBalance().subtract(newTransaction.getAmount());

        // act
        Transaction addedTransaction = transactionService.addTransactionToBankAccount(1, 1, newTransaction);

        // assert
        assertEquals(balanceAfterTransaction, bankAccount.getBalance());
        assertEquals(new BigDecimal("-100"), addedTransaction.getAmount()); // ensure amount is negative

        verify(bankAccountService).createOrUpdateBankAccount(any(BankAccount.class));
    }

    @Test
    void testAddTransactionToBankAccountCannotMakeTransaction() {
        // arrange
        when(bankAccountService.findBankAccountByID(1, 1)).thenReturn(bankAccount);
        when(bankAccountService.canMakeTransaction(any(BankAccount.class), any(Transaction.class))).thenReturn(false);

        NewTransactionDTO newTransaction = new NewTransactionDTO(BigDecimal.valueOf(100), "Test");
        BigDecimal balanceBeforeTransaction = bankAccount.getBalance();

        // act
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.addTransactionToBankAccount(1, 1, newTransaction);
        });

        // assert
        assertEquals("Transaction cannot be added to the bank account", exception.getMessage());
        assertEquals(balanceBeforeTransaction, bankAccount.getBalance());
    }
}