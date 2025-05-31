package de.fherfurt.clevercash.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewSavingDTO;
import de.fherfurt.clevercash.api.service.AuthenticationService;
import de.fherfurt.clevercash.api.service.SavingService;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Saving;
import de.fherfurt.clevercash.storage.models.User;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SavingControllerTest {
    private final String SAVING_CONTROLLER_BASE_URL = "/api/users/{userID}/bankAccounts/{bankAccountID}/savings";

    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private SavingService savingService;

    @InjectMocks
    private SavingController savingController;

    private BankAccount bankAccount;
    private List<Saving> savings;
    private Saving saving1;
    private ObjectMapper objectMapper;
    private int userID;
    private int bankAccountID;
    private int savingID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(savingController).build();
        User user = TestUtils.getTestUser1();
        bankAccount = TestUtils.getTestBankAccount1();
        saving1 = TestUtils.getTestSaving1(bankAccount);
        savings = Arrays.asList(saving1, TestUtils.getTestSaving2(bankAccount));
        bankAccount.getSavings().addAll(savings);
        userID = user.getId();
        bankAccountID = bankAccount.getId();
        savingID = saving1.getId();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void testFindSavings() throws Exception {
        // arrange
        when(savingService.findAllSavingForUserWithFilters(
                userID,bankAccountID, null, null, null))
                .thenReturn(savings);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(savings.get(0).getId()))
                .andExpect(jsonPath("$[0].name").value(savings.get(0).getName()))
                .andExpect(jsonPath("$[1].id").value(savings.get(1).getId()))
                .andExpect(jsonPath("$[1].name").value(savings.get(1).getName()));
    }

    @Test
    public void testFindSavingsBadRequest() throws Exception {
        // arrange
        when(savingService.findAllSavingForUserWithFilters(
                userID,bankAccountID, null, null, null))
                .thenThrow(new DateTimeParseException(null,"text", 1, null));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFindSavingsNotFound() throws Exception {
        // arrange
        when(savingService.findAllSavingForUserWithFilters(
                userID,bankAccountID, null, null, null))
                .thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindSavingsNoAccess() throws Exception {
        // arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFindSavingByID() throws Exception {
        // arrange
        when(savingService.findSavingByID(userID,bankAccountID, savingID)).thenReturn(saving1);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL + "/{savingID}", userID, bankAccountID,savingID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saving1.getId()))
                .andExpect(jsonPath("$.name").value(saving1.getName()));
    }

    @Test
    public void testFindSavingByIDNotFound() throws Exception {
        // arrange
        when(savingService.findSavingByID(userID,bankAccountID,savingID)).thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL+ "/{savingID}", userID, bankAccountID,savingID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindSavingByIDNoAccess() throws Exception {
        // arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL + "/{savingID}", userID, bankAccountID, savingID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAddSavingToBankAccount() throws Exception {
        // arrange
        NewSavingDTO newSavingDTO = new NewSavingDTO(
                "Name", "Description", new BigDecimal("1000"), LocalDate.now(), 2);
        Saving newSaving = Mapper.newSavingDTOToSaving(newSavingDTO, bankAccount);

        when(savingService.addSavingToBankAccount(anyInt(), anyInt(), any(NewSavingDTO.class))).thenReturn(newSaving);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(SAVING_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSavingDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(newSaving.getId()))
                .andExpect(jsonPath("$.name").value(newSaving.getName()));
    }

    @Test
    public void testAddSavingToBankAccountBadRequest() throws Exception {
        // arrange
        NewSavingDTO newSavingDTO = new NewSavingDTO(
                "Name", "Description", new BigDecimal("1000"), LocalDate.now(), 2);

        when(savingService.addSavingToBankAccount(anyInt(), anyInt(), any(NewSavingDTO.class)))
                .thenThrow(new IllegalArgumentException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(SAVING_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSavingDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddSavingToBankAccountConflict() throws Exception {
        // arrange
        NewSavingDTO newSavingDTO = new NewSavingDTO(
                "Name", "Description", new BigDecimal("1000"), LocalDate.now(), 2);

        when(savingService.addSavingToBankAccount(anyInt(), anyInt(), any(NewSavingDTO.class)))
                .thenThrow(new AlreadyExistsException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(SAVING_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSavingDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    public void testAddSavingToBankAccountNotFound() throws Exception {
        // arrange
        NewSavingDTO newSavingDTO = new NewSavingDTO(
                "Name", "Description", new BigDecimal("1000"), LocalDate.now(), 2);

        when(savingService.addSavingToBankAccount(anyInt(), anyInt(), any(NewSavingDTO.class)))
                .thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(SAVING_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSavingDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAddSavingToBankAccountNoAccess() throws Exception {
        // arrange
        NewSavingDTO newSavingDTO = new NewSavingDTO(
                "Name", "Description", new BigDecimal("1000"), LocalDate.now(), 2);

        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(post(SAVING_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newSavingDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testRemoveSavingFromBankAccount() throws Exception {
        // arrange
        doNothing().when(savingService).removeSavingFromBankAccount(userID, bankAccountID, savingID);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(SAVING_CONTROLLER_BASE_URL + "/{savingID}", userID, bankAccountID, savingID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(savingService, times(1))
                .removeSavingFromBankAccount(userID, bankAccountID, savingID);
    }

    @Test
    public void testRemoveSavingFromBankAccountNotFound() throws Exception {
        // arrange
        doThrow(new NoSuchElementException()).when(savingService)
                .removeSavingFromBankAccount(userID, bankAccountID, savingID);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(SAVING_CONTROLLER_BASE_URL + "/{savingID}", userID, bankAccountID, savingID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testRemoveSavingFromBankAccountNoAccess() throws Exception {
        // arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(SAVING_CONTROLLER_BASE_URL + "/{savingID}", userID, bankAccountID, savingID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFindActiveSaving() throws Exception {
        // arrange
        Saving saving2 = bankAccount.getSavings().get(1);

        when(savingService.findActiveSaving(userID, bankAccountID))
                .thenReturn(saving2);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL + "/activeSaving", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saving2.getId()))
                .andExpect(jsonPath("$.active").value(saving2.isActive()));
    }

    @Test
    public void testFindActiveSavingNotFound() throws Exception {
        // arrange
        when(savingService.findActiveSaving(userID, bankAccountID)).thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL + "/activeSaving", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindActiveSavingNoAccess() throws Exception {
        // arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(get(SAVING_CONTROLLER_BASE_URL + "/activeSaving", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testRemoveActiveSaving() throws Exception {
        // arrange
        boolean savingIsActive = true;

        when(savingService.removeActiveSaving(userID, bankAccountID)).thenReturn(savingIsActive);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(SAVING_CONTROLLER_BASE_URL + "/activeSaving", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testRemoveActiveSavingNoActiveSaving() throws Exception {
        // arrange
        boolean savingIsActive = false;

        when(savingService.removeActiveSaving(userID, bankAccountID)).thenReturn(savingIsActive);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(SAVING_CONTROLLER_BASE_URL + "/activeSaving", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testRemoveActiveSavingNotFound() throws Exception {
        // arrange
        when(savingService.removeActiveSaving(userID, bankAccountID)).thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(SAVING_CONTROLLER_BASE_URL + "/activeSaving", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testRemoveActiveSavingNoAccess() throws Exception {
        // arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(delete(SAVING_CONTROLLER_BASE_URL + "/activeSaving", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testTransferFromSavingToBalance() throws Exception {
        // arrange
        int amountInt = 100;

        when(savingService.transferFromSavingToBalance(userID, bankAccountID, amountInt))
                .thenReturn(bankAccount);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(put(SAVING_CONTROLLER_BASE_URL + "/credit", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("amountInt", String.valueOf(amountInt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bankAccount.getId()))
                .andExpect(jsonPath("$.balance").value(bankAccount.getBalance().doubleValue()));
    }

    @Test
    public void testTransferFromSavingToBalanceBadRequest() throws Exception {
        // arrange
        int amountInt = 100;

        when(savingService.transferFromSavingToBalance(userID, bankAccountID, amountInt))
                .thenThrow(new IllegalArgumentException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(put(SAVING_CONTROLLER_BASE_URL + "/credit", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("amountInt", String.valueOf(amountInt)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testTransferFromSavingToBalanceNotFound() throws Exception {
        // arrange
        int amountInt = 100;

        when(savingService.transferFromSavingToBalance(userID, bankAccountID, amountInt))
                .thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(put(SAVING_CONTROLLER_BASE_URL + "/credit", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("amountInt", String.valueOf(amountInt)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testTransferFromSavingToBalanceNoAccess() throws Exception {
        // arrange
        int amountInt = 100;
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // act & assert
        mockMvc.perform(put(SAVING_CONTROLLER_BASE_URL + "/credit", userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("amountInt", String.valueOf(amountInt)))
                .andExpect(status().isForbidden());
    }
}