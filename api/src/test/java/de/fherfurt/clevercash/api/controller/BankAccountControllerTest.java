package de.fherfurt.clevercash.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewBankAccountDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.api.service.BankAccountService;
import de.fherfurt.clevercash.api.service.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BankAccountControllerTest {

    private final String BANK_ACCOUNT_CONTROLLER_BASE_URL = "/api/users/{userID}/bankAccounts";

    private BankAccount bankAccount;

    private User user;

    private List<BankAccount> bankAccounts;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private int userID;
    private int bankAccountID;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    UserService userService;

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private BankAccountController bankAccountController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(bankAccountController).build();
        user = TestUtils.getTestUser1();
        bankAccount = TestUtils.getTestBankAccount1();
        bankAccounts = user.getBankAccounts();
        userID = user.getId();
        bankAccountID = user.getBankAccounts().get(0).getId();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testFindBankAccounts() throws Exception {
        // Arrange
        when(bankAccountService.findAllBankAccounts(userID)).thenReturn(bankAccounts);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(BANK_ACCOUNT_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(bankAccounts.get(0).getId()))
                .andExpect(jsonPath("$[0].name").value(bankAccounts.get(0).getName()))
                .andExpect(jsonPath("$[1].id").value(bankAccounts.get(1).getId()))
                .andExpect(jsonPath("$[1].name").value(bankAccounts.get(1).getName()));
    }

    @Test
    void testFindBankAccountsNotFound() throws Exception {
        // Arrange
        when(bankAccountService.findAllBankAccounts(userID)).thenThrow(new NoSuchElementException("User not found"));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(BANK_ACCOUNT_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testFindBankAccountsNoAccess() throws Exception {
        // Arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(BANK_ACCOUNT_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testFindBankAccountByID() throws Exception {
        // Arrange
        when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bankAccountID))
                .andExpect(jsonPath("$.name").value(bankAccount.getName()));
    }

    @Test
    void testFindBankAccountByIDNotFound() throws Exception {
        // Arrange
        when(bankAccountService.findBankAccountByID(userID, bankAccountID))
                .thenThrow(new NoSuchElementException("BankAccount not found for User with ID" + userID));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testFindBankAccountByIDNoAccess() throws Exception {
        // Arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddBankAccountToUser() throws Exception {
        // Arrange
        String newName = "new name";
        BigDecimal newBalance = new BigDecimal("100.00");
        BigDecimal newDailyLimit = new BigDecimal("100.00");
        NewBankAccountDTO newBankAccountDTO = new NewBankAccountDTO();
        newBankAccountDTO.setName(newName);
        newBankAccountDTO.setBalance(newBalance);
        newBankAccountDTO.setDailyLimit(newDailyLimit);

        BankAccount newBankAccount = Mapper.newBankAccountDTOToBankAccount(newBankAccountDTO, user);

        when(bankAccountService.addBankAccountToUser(eq(userID), any(NewBankAccountDTO.class)))
                .thenReturn(newBankAccount);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(BANK_ACCOUNT_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBankAccountDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.balance").value(newBalance.doubleValue()))
                .andExpect(jsonPath("$.dailyLimit").value(newDailyLimit.doubleValue()));
    }

    @Test
    void testAddBankAccountToUserUserNotFound() throws Exception {
        // Arrange
        when(bankAccountService.addBankAccountToUser(anyInt(), any(NewBankAccountDTO.class)))
                .thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(BANK_ACCOUNT_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewBankAccountDTO())))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddBankAccountToUserUserConflict() throws Exception {
        // Arrange
        when(bankAccountService.addBankAccountToUser(anyInt(), any(NewBankAccountDTO.class)))
                .thenThrow(new AlreadyExistsException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(BANK_ACCOUNT_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewBankAccountDTO())))
                .andExpect(status().isConflict());
    }

    @Test
    void testAddBankAccountToUserBankAccountBadRequest() throws Exception {
        // Arrange
        when(bankAccountService.addBankAccountToUser(anyInt(), any(NewBankAccountDTO.class)))
                .thenThrow(new IllegalArgumentException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(BANK_ACCOUNT_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewBankAccountDTO())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddBankAccountToUserUserNoAccess() throws Exception {
        // Arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(BANK_ACCOUNT_CONTROLLER_BASE_URL, userID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewBankAccountDTO())))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateBankAccount() throws Exception {
        // Arrange
        NewBankAccountDTO updatedBankAccountDTO = new NewBankAccountDTO();
        updatedBankAccountDTO.setName("new Account Name");
        updatedBankAccountDTO.setBalance(new BigDecimal("420.00"));
        updatedBankAccountDTO.setDailyLimit(new BigDecimal("420.00"));

        BankAccount updatedBankAccount = Mapper.newBankAccountDTOToBankAccount(updatedBankAccountDTO, user);

        when(bankAccountService.updateBankAccount(eq(userID), eq(bankAccountID), any(NewBankAccountDTO.class)))
                .thenReturn(updatedBankAccount);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(put(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedBankAccountDTO)))
                        .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(updatedBankAccount.getId()))
                .andExpect(jsonPath("$.name").value("new Account Name"))
                .andExpect(jsonPath("$.balance").value(420.00));
            }

    @Test
    void testUpdateBankAccountBadRequest() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException()).when(
                bankAccountService).updateBankAccount(anyInt(), anyInt(), any(NewBankAccountDTO.class));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(put(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewBankAccountDTO())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateBankAccountUserNotFound() throws Exception {
        // Arrange
        doThrow(new NoSuchElementException()).when(
                bankAccountService).updateBankAccount(anyInt(), anyInt(), any(NewBankAccountDTO.class));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(put(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewBankAccountDTO())))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateBankAccountConflictName() throws Exception {
        // Arrange
        doThrow(new AlreadyExistsException()).when(
                bankAccountService).updateBankAccount(anyInt(), anyInt(), any(NewBankAccountDTO.class));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(put(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewBankAccountDTO())))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateBankAccountNoAccess() throws Exception {
        // Arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(put(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NewBankAccountDTO())))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteUserBankAccount() throws Exception {
        // Arrange
        doNothing().when(bankAccountService).deleteUserBankAccount(userID, bankAccountID);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        user.getBankAccounts().remove(bankAccount);

        // Act & Assert
        mockMvc.perform(delete(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteUserBankAccountNotFound() throws Exception {
        // Arrange
        doThrow(new NoSuchElementException()).when(bankAccountService).deleteUserBankAccount(userID, bankAccountID);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(delete(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteUserBankAccountNoAccess() throws Exception {
        // Arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(delete(BANK_ACCOUNT_CONTROLLER_BASE_URL + "/{bankAccountID}", userID, bankAccountID))
                .andExpect(status().isForbidden());
    }
}