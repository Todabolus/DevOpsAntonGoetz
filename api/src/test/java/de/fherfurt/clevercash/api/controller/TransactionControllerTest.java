package de.fherfurt.clevercash.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewTransactionDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Transaction;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.api.service.BankAccountService;
import de.fherfurt.clevercash.api.service.TransactionService;
import de.fherfurt.clevercash.storage.repositories.BankAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionControllerTest {
    private final String TRANSACTION_CONTROLLER_BASE_URL = "/api/users/{userID}/bankAccounts/{bankAccountID}/transactions";

    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private BankAccountService bankAccountService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private TransactionController transactionController;

    private User user;
    private BankAccount bankAccount;
    private List<Transaction> transactions;
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        user = TestUtils.getTestUser1();
        bankAccount = TestUtils.getTestBankAccount1();
        transactions = TestUtils.getTestTransactionList(bankAccount);
        bankAccount.getTransactions().addAll(transactions);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void testFindTransactionsSuccess() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();

        when(transactionService.findAllTransactionsForUserWithFilters(
                userID, bankAccountID, null, null, null, null)).thenReturn(transactions);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(TRANSACTION_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(transactions.size()));
    }

    @Test
    public void testFindTransactionsNotFound() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();

        when(transactionService.findAllTransactionsForUserWithFilters(
                userID, bankAccountID, null, null, null, null))
                .thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(TRANSACTION_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindTransactionsInvalidDate() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();
        String invalidStartDate = "invalidStartDate";

        when(transactionService.findAllTransactionsForUserWithFilters(
                userID, bankAccountID, invalidStartDate, null, null, null))
                .thenThrow(new IllegalArgumentException("Invalid transaction type!"));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(TRANSACTION_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .param("startDate", invalidStartDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFindTransactionsInvalidParams() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();
        String invalidTransactionType = "INVALID_TYPE";

        when(transactionService.findAllTransactionsForUserWithFilters(
                userID, bankAccountID, null, null, invalidTransactionType, null))
                .thenThrow(new IllegalArgumentException("Invalid transaction type!"));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(TRANSACTION_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .param("transactionType", invalidTransactionType)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFindTransactionsNoAccess() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();

        when(transactionService.findAllTransactionsForUserWithFilters(
                userID, bankAccountID, null, null, null, null)).thenReturn(transactions);
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(TRANSACTION_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFindTransactionByIdSuccess() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();
        Transaction transaction = transactions.getFirst();
        int transactionID = transaction.getId();

        when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);
        when(transactionService.findTransactionByID(userID, bankAccountID, transactionID)).thenReturn(transaction);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(TRANSACTION_CONTROLLER_BASE_URL + "/{transactionID}", userID, bankAccountID, transactionID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionID));
    }

    @Test
    public void testFindTransactionByIdNotFound() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();
        int transactionID = 999;

        when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);
        when(transactionService.findTransactionByID(userID, bankAccountID, transactionID))
                .thenThrow(new NoSuchElementException("Transaction not found!"));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(TRANSACTION_CONTROLLER_BASE_URL + "/{transactionID}", userID, bankAccountID, transactionID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindTransactionNoAccess() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();
        Transaction transaction = transactions.getFirst();
        int transactionID = transaction.getId();

        when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);
        when(transactionService.findTransactionByID(userID, bankAccountID, transactionID)).thenReturn(transaction);
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(TRANSACTION_CONTROLLER_BASE_URL + "/{transactionID}", userID, bankAccountID, transactionID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAddTransactionToBankAccount() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();

        NewTransactionDTO newTransactionDTO = new NewTransactionDTO(new BigDecimal("2000"), "PAYMENT" );
        Transaction newTransaction = Mapper.newTransactionDTOToTransaction(newTransactionDTO, bankAccount);

        when(transactionService.addTransactionToBankAccount(anyInt(),anyInt(), any(NewTransactionDTO.class)))
                .thenReturn(newTransaction);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(TRANSACTION_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTransactionDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(newTransactionDTO.getAmount()))
                .andExpect(jsonPath("$.description").value(newTransactionDTO.getDescription()));
    }

    @Test
    public void testAddTransactionToBankAccountBadRequest() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();
        NewTransactionDTO newTransactionDTO = new NewTransactionDTO(new BigDecimal("2000"), "PAYMENT" );

        when(transactionService.addTransactionToBankAccount(anyInt(),anyInt(), any(NewTransactionDTO.class)))
                .thenThrow(new IllegalArgumentException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(TRANSACTION_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTransactionDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddTransactionToBankAccountNotFound() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();
        NewTransactionDTO newTransactionDTO = new NewTransactionDTO(new BigDecimal("2000"), "PAYMENT" );

        when(transactionService.addTransactionToBankAccount(anyInt(),anyInt(), any(NewTransactionDTO.class)))
                .thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(TRANSACTION_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTransactionDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAddTransactionToBankAccountNoAccess() throws Exception {
        // arrange
        int userID = user.getId();
        int bankAccountID = bankAccount.getId();
        NewTransactionDTO newTransactionDTO = new NewTransactionDTO(new BigDecimal("2000"), "PAYMENT" );

        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(TRANSACTION_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTransactionDTO)))
                .andExpect(status().isForbidden());
    }


}