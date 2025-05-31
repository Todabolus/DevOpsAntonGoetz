package de.fherfurt.clevercash.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.AccessException;
import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewInstallmentDTO;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Installment;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.api.service.BankAccountService;
import de.fherfurt.clevercash.api.service.InstallmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import de.fherfurt.clevercash.api.service.AuthenticationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstallmentControllerTest {
    private final String INSTALLMENT_CONTROLLER_BASE_URL = "/api/users/{userID}/bankAccounts/{bankAccountID}/installments";

    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private InstallmentService installmentService;

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private InstallmentController installmentController;

    private BankAccount bankAccount;
    private List<Installment> installments;
    private Installment installment1;
    private ObjectMapper objectMapper;
    private int userID;
    private int bankAccountID;
    private int installmentID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(installmentController).build();
        User user = TestUtils.getTestUser1();
        bankAccount = TestUtils.getTestBankAccount1();
        installment1 = TestUtils.getTestInstallment1(bankAccount);
        installments = Arrays.asList(installment1, TestUtils.getTestInstallment2(bankAccount));
        bankAccount.getInstallments().addAll(installments);
        userID = user.getId();
        bankAccountID = bankAccount.getId();
        installmentID = installment1.getId();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testFindInstallmentsWithOutFilters() throws Exception {
        // Arrange
        when(installmentService.findAllInstallmentsForUserWithFilters(
                userID, bankAccountID, null, null, null)).thenReturn(installments);
        when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(installments.get(0).getId()))
                .andExpect(jsonPath("$[0].name").value(installments.get(0).getName()))
                .andExpect(jsonPath("$[1].id").value(installments.get(1).getId()))
                .andExpect(jsonPath("$[1].name").value(installments.get(1).getName()));
    }

    @Test
    void testFindInstallmentsWithFilters() throws Exception {
        // Arrange
        String startDate = LocalDate.now().toString();
        String description = "smartphone";

        when(installmentService.findAllInstallmentsForUserWithFilters(
                userID, bankAccountID, startDate, null, description))
                .thenReturn(Collections.singletonList(installment1));
        when(bankAccountService.findBankAccountByID(eq(userID), eq(bankAccountID)))
                .thenReturn(bankAccount);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .param("startDate", startDate)
                        .param("description", description)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(installment1.getId()))
                .andExpect(jsonPath("$[0].description").value(installment1.getDescription()));
    }

    @Test
    void testFindInstallmentsNotFound() throws Exception {
        // Arrange
        when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);
        when(installmentService.findAllInstallmentsForUserWithFilters(
                userID, bankAccountID, null, null, null))
                .thenThrow(new NoSuchElementException("Installments not found"));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindInstallmentsInvalidDate() throws Exception {
        String invalidDate = "invalidDate";

        when(installmentService.findAllInstallmentsForUserWithFilters(
                userID, bankAccountID, invalidDate, null, null)).
                thenThrow(new DateTimeParseException("Invalid date!", invalidDate, 0));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        mockMvc.perform(get(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .param("startDate", invalidDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testFindInstallmentsWithNoAccess() throws Exception {
        // Arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testFindInstallmentByID() throws Exception {
        // Arrange
        Installment installment = installments.getFirst();
        int firstInstallmentID = installment.getId();

        when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);
        when(installmentService.findInstallmentByID(userID, bankAccountID, firstInstallmentID)).thenReturn(installment);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(INSTALLMENT_CONTROLLER_BASE_URL + "/{installmentID}", userID, bankAccountID, firstInstallmentID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstInstallmentID))
                .andExpect(jsonPath("$.name").value(installment.getName()));
    }

    @Test
    void testFindInstallmentByIDNotFound() throws Exception {
        // Arrange
        int invalidInstallmentID = 999;

        when(bankAccountService.findBankAccountByID(userID, bankAccountID)).thenReturn(bankAccount);
        when(installmentService.findInstallmentByID(userID, bankAccountID, invalidInstallmentID))
                .thenThrow(new NoSuchElementException("Installment not found"));
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(INSTALLMENT_CONTROLLER_BASE_URL + "/{installmentID}", userID, bankAccountID, invalidInstallmentID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testFindInstallmentByIDNoAccess() throws Exception {
        // Arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(get(INSTALLMENT_CONTROLLER_BASE_URL + "/{installmentID}", userID, bankAccountID, installmentID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAddInstallmentToBankAccount() throws Exception {
        // Arrange
        NewInstallmentDTO newInstallmentDTO = new NewInstallmentDTO(
                "name",new  BigDecimal("1000"), new BigDecimal("100"), LocalDate.now(), 2, "New Installment");
        Installment newInstallment = Mapper.newInstallmentDTOToInstallment(newInstallmentDTO, bankAccount);

        when(installmentService.addInstallmentToBankAccount(anyInt(), anyInt(), any(NewInstallmentDTO.class)))
                .thenReturn(newInstallment);
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInstallmentDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(newInstallment.getName()))
                .andExpect(jsonPath("$.description").value(newInstallment.getDescription()));
    }

    @Test
    void testAddInstallmentToBankAccountBadRequest() throws Exception {
        // Arrange
        NewInstallmentDTO newInstallmentDTO = new NewInstallmentDTO(
                "name",new  BigDecimal("1000"), new BigDecimal("100"), LocalDate.now(), 2, "New Installment");

        when(installmentService.addInstallmentToBankAccount(anyInt(), anyInt(), any(NewInstallmentDTO.class)))
                .thenThrow(new IllegalArgumentException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInstallmentDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddInstallmentToBankAccountNotFound() throws Exception {
        // Arrange
        NewInstallmentDTO newInstallmentDTO = new NewInstallmentDTO(
                "name",new  BigDecimal("1000"), new BigDecimal("100"), LocalDate.now(), 2, "New Installment");

        when(installmentService.addInstallmentToBankAccount(anyInt(), anyInt(), any(NewInstallmentDTO.class)))
                .thenThrow(new NoSuchElementException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInstallmentDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddInstallmentToBankAccountConflict() throws Exception {
        // Arrange
        NewInstallmentDTO newInstallmentDTO = new NewInstallmentDTO(
                "name",new  BigDecimal("1000"), new BigDecimal("100"), LocalDate.now(), 2, "New Installment");

        when(installmentService.addInstallmentToBankAccount(anyInt(), anyInt(), any(NewInstallmentDTO.class)))
                .thenThrow(new AlreadyExistsException());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInstallmentDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    void testAddInstallmentToBankAccountNoAccess() throws Exception {
        // Arrange
        NewInstallmentDTO newInstallmentDTO = new NewInstallmentDTO("name",new  BigDecimal("1000"),
                new BigDecimal("100"), LocalDate.now(), 2, "name");

        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(post(INSTALLMENT_CONTROLLER_BASE_URL, userID, bankAccountID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInstallmentDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRemoveInstallmentFromBankAccount() throws Exception {
        // Arrange
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));


        // Act & Assert
        mockMvc.perform(delete(INSTALLMENT_CONTROLLER_BASE_URL + "/{installmentID}", userID, bankAccountID, installmentID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void testRemoveInstallmentFromBankAccountNotFound() throws Exception {
        // Arrange
        doThrow(new NoSuchElementException()).when(
                installmentService).removeInstallmentFromBankAccount(anyInt(), anyInt(), anyInt());
        doNothing().when(authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(delete(INSTALLMENT_CONTROLLER_BASE_URL + "/{installmentID}", userID, bankAccountID, installmentID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testRemoveInstallmentFromBankAccountNoAccess() throws Exception {
        // Arrange
        doThrow(new AccessException()).when(
                authenticationService).verifyUserAccess(anyInt(), any(HttpServletRequest.class));

        // Act & Assert
        mockMvc.perform(delete(INSTALLMENT_CONTROLLER_BASE_URL + "/{installmentID}", userID, bankAccountID, installmentID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}