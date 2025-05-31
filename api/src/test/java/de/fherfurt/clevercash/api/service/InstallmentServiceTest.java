package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewInstallmentDTO;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Installment;
import de.fherfurt.clevercash.storage.models.Transaction;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.storage.repositories.InstallmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstallmentServiceTest {
    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private InstallmentService installmentService;


    private Installment installment;
    private BankAccount bankAccount;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = TestUtils.getTestUser1();
        bankAccount = TestUtils.getTestBankAccount1();
        installment = TestUtils.getTestInstallment1(bankAccount);
        bankAccount.getInstallments().add(installment);
    }

    @Test
    void testCreateOrUpdateInstallmentCreateNew() {
        // Arrange
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.empty());
        when(installmentRepository.save(installment)).thenReturn(installment);

        // Act
        Installment result = installmentService.createOrUpdateInstallment(installment);

        // Assert
        assertNotNull(result);
        assertEquals(installment.getId(), result.getId());
        verify(installmentRepository, times(1)).save(installment);
    }

    @Test
    void testCreateOrUpdateInstallmentUpdateExisting() {
        // Arrange
        when(installmentRepository.findById(installment.getId())).thenReturn(Optional.of(installment));
        when(installmentRepository.save(installment)).thenReturn(installment);

        // Act
        Installment result = installmentService.createOrUpdateInstallment(installment);

        // Assert
        assertNotNull(result);
        assertEquals(installment.getId(), result.getId());
        verify(installmentRepository, times(1)).save(installment);
    }

    @Test
    void testFindAllInstallmentsWithOutFilters() {
        // Arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // Act
        List<Installment> result = installmentService.findAllInstallmentsForUserWithFilters(user.getId(), bankAccount.getId(), null, null, null);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.contains(installment));
    }

    @Test
    void testFindAllInstallmentsMultipleInstallmentsValidFilters() {
        // Arrange
        Installment installment2 = new Installment();
        installment2.setId(2);
        installment2.setBankAccount(bankAccount);
        installment2.setAmount(new BigDecimal("200.00"));
        installment2.setStartDate(LocalDate.now().plusMonths(1));
        installment2.setPayDay(LocalDate.now().plusMonths(2));
        installment2.setDescription("Hello");
        bankAccount.getInstallments().add(installment2);

        String description = installment.getDescription();
        String startDate = installment.getStartDate().toString();
        String endDate = installment.getStartDate().plusMonths(1).toString();

        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // Act
        List<Installment> result = installmentService.findAllInstallmentsForUserWithFilters(user.getId(), bankAccount.getId(), startDate, endDate, description);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(installment));
        assertFalse(result.contains(installment2));
    }

    @Test
    void testFindAllSavingsWithInvalidDate() {
        // arrange
        String invalidDate = "invalidDateString";
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act & assert
        assertThrows(DateTimeParseException.class , () -> installmentService.findAllInstallmentsForUserWithFilters(user.getId(), bankAccount.getId(), invalidDate, invalidDate, null));
    }

    @Test
    void testFindInstallmentByIDSuccess() {
        // Arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // Act
        Installment result = installmentService.findInstallmentByID(user.getId(), bankAccount.getId(), installment.getId());

        // Assert
        assertEquals(installment.getId(), result.getId());
    }

    @Test
    void testFindInstallmentByIDInvalidID() {
        // Arrange
        int invalidID = 999;
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // Act & assert
        assertThrows(NoSuchElementException.class, () -> installmentService.findInstallmentByID(user.getId(), bankAccount.getId(), invalidID));
    }

    @Test
    void testAddInstallmentToBankAccountSuccess() throws AlreadyExistsException {
        // Arrange
        NewInstallmentDTO newInstallment = new NewInstallmentDTO();
        newInstallment.setName("TestInstallment");
        newInstallment.setAmount(new BigDecimal("200.00"));
        newInstallment.setStartDate(LocalDate.now());
        newInstallment.setDurationInMonths(2);
        newInstallment.setAmountPerRate(BigDecimal.valueOf(100));
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);

        // Act
        Installment result = installmentService.addInstallmentToBankAccount(user.getId(), bankAccount.getId(), newInstallment);

        // Assert
        assertNotNull(result);
        assertTrue(bankAccount.getInstallments().contains(Mapper.newInstallmentDTOToInstallment(newInstallment, bankAccount)));
    }

    @Test
    void testAddInstallmentToBankAccountInValidInstallment() {
        // Arrange
        NewInstallmentDTO newInstallment = new NewInstallmentDTO();
        newInstallment.setName("testInstallment");
        newInstallment.setAmount(new BigDecimal("200.00"));
        newInstallment.setStartDate(LocalDate.now());
        newInstallment.setDurationInMonths(-2); // invalid duration
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // Act & assert
        assertThrows(IllegalArgumentException.class, () -> installmentService.addInstallmentToBankAccount(user.getId(), bankAccount.getId(), newInstallment));
    }

    @Test
    void testAddInstallmentToBankAccountAlreadyActiveInstallmentWithName() {
        // Arrange
        NewInstallmentDTO newInstallment = new NewInstallmentDTO();
        newInstallment.setName(installment.getName()); // same name
        newInstallment.setAmount(new BigDecimal("200.00"));
        newInstallment.setStartDate(LocalDate.now());
        newInstallment.setDurationInMonths(3);
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // Act & assert
        assertThrows(AlreadyExistsException.class, () -> installmentService.addInstallmentToBankAccount(user.getId(), bankAccount.getId(), newInstallment));
    }

    @Test
    void testRemoveInstallmentFromBankAccountNotStarted() {
        // arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);
        installment.setAlreadyPaidAmount(BigDecimal.ZERO);

        // act
        installmentService.removeInstallmentFromBankAccount(user.getId(), bankAccount.getId(), installment.getId());

        // assert
        assertFalse(bankAccount.getInstallments().contains(installment));
        verify(bankAccountService).createOrUpdateBankAccount(bankAccount);
    }

    @Test
    void testRemoveInstallmentFromBankAccountAlreadyStarted() {
        // arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);
        installment.setAlreadyPaidAmount(installment.getAmountPerRate());

        // act
        installmentService.removeInstallmentFromBankAccount(user.getId(), bankAccount.getId(), installment.getId());

        // assert
        assertTrue(bankAccount.getInstallments().contains(installment));
        assertFalse(installment.isActive());
        verify(bankAccountService).createOrUpdateBankAccount(bankAccount);
    }

    @Test
    void testRemoveInstallmentFromBankAccountInstallmentNotFound() {
        // arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(installmentService.findInstallmentByID(user.getId(), bankAccount.getId(), installment.getId())).thenThrow(new NoSuchElementException());

        // act & assert
        assertThrows(NoSuchElementException.class, () -> {
            installmentService.removeInstallmentFromBankAccount(user.getId(), bankAccount.getId(), installment.getId());
        });
    }

    @Test
    void testProcessInstallmentSufficientBalance() {
        // arrange
        BigDecimal oldBalance = bankAccount.getBalance();
        when(bankAccountService.canMakeTransaction(any(BankAccount.class), any(Transaction.class))).thenReturn(true);

        // When
        installmentService.processInstallment(installment);

        // Then
        assertEquals(oldBalance.subtract(installment.getAmountPerRate()), bankAccount.getBalance());
        assertEquals(installment.getAmountPerRate(), installment.getAlreadyPaidAmount());
        assertEquals(1, bankAccount.getTransactions().size());
        verify(bankAccountService).createOrUpdateBankAccount(bankAccount);
    }

    @Test
    void testProcessInstallmentInsufficientBalance() {
        // arrange
        BigDecimal oldBalance = bankAccount.getBalance();
        when(bankAccountService.canMakeTransaction(any(BankAccount.class), any(Transaction.class))).thenReturn(false);

        // act
        installmentService.processInstallment(installment);

        // assert
        assertEquals(oldBalance, bankAccount.getBalance());
        assertEquals(BigDecimal.ZERO, installment.getAlreadyPaidAmount());
        assertEquals(0, bankAccount.getTransactions().size());
        verify(bankAccountService, never()).createOrUpdateBankAccount(bankAccount);
    }

    @Test
    void testProcessInstallmentInstallmentFinished() {
        // arrange
        BigDecimal oldBalance = bankAccount.getBalance();
        installment.setAlreadyPaidAmount(installment.getAmount().subtract(installment.getAmountPerRate()));
        when(bankAccountService.canMakeTransaction(any(BankAccount.class), any(Transaction.class))).thenReturn(true);

        // When
        installmentService.processInstallment(installment);

        // Then
        assertFalse(installment.isActive());
        assertEquals(oldBalance.subtract(installment.getAmountPerRate()), bankAccount.getBalance());
        assertEquals(installment.getAmount(), installment.getAlreadyPaidAmount());
        assertEquals(1, bankAccount.getTransactions().size());
        verify(bankAccountService).createOrUpdateBankAccount(bankAccount);
    }
}