package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewSavingDTO;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Saving;
import de.fherfurt.clevercash.storage.models.Transaction;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.storage.repositories.SavingRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class SavingServiceTest {
    BankAccount bankAccount;

    User user;

    Saving saving1;

    Saving saving2;

    @Mock
    SavingRepository savingRepository;

    @Mock
    BankAccountService bankAccountService;

    @InjectMocks
    SavingService savingService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = TestUtils.getTestUser1();
        bankAccount = TestUtils.getTestBankAccount1();
        saving1 = TestUtils.getTestSaving1(bankAccount);
        saving2 = TestUtils.getTestSaving2(bankAccount);
        bankAccount.getSavings().add(saving1);
        bankAccount.getSavings().add(saving2);
    }

    @Test
    void testCreateOrUpdateSaving() {
        // arrange
        saving1.setAmount(new BigDecimal("300.00"));

        when(savingService.createOrUpdateSaving(saving1)).thenReturn(saving1);

        // act
        Saving createdSaving = savingService.createOrUpdateSaving(saving1);

        // assert
        assertEquals(saving1, createdSaving);
        assertEquals(saving1.getAmount(), new BigDecimal("300.00"));
    }

    @Test
    void testFindAllSavingsWithValidFilters() {
        // arrange
        String startDate = LocalDate.now().toString();
        String endDate = LocalDate.now().plusMonths(1).toString();
        String description = "greece";

        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act
        List<Saving> savings = savingService.findAllSavingForUserWithFilters(
                user.getId(), bankAccount.getId(), startDate, endDate, description);

        // assert
        assertEquals(1, savings.size());
        assertTrue(savings.contains(saving2));
    }

    @Test
    void testFindAllSavingsWithNoFilters() {
        // arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act
        List<Saving> allSavings = savingService.findAllSavingForUserWithFilters(
                user.getId(), bankAccount.getId(), null, null, null);

        // assert
        assertEquals(2, allSavings.size());
        assertTrue(allSavings.contains(saving1));
        assertTrue(allSavings.contains(saving2));
    }

    @Test
    void testFindAllSavingsWithInvalidDate() {
        // arrange
        String invalidDate = "invalidDateString";
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act & assert
        assertThrows(DateTimeParseException.class , () -> savingService.findAllSavingForUserWithFilters(
                user.getId(), bankAccount.getId(), invalidDate, invalidDate, null));
    }

    @Test
    void testFindSavingByID (){
        // arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act
        Saving result = savingService.findSavingByID(user.getId(), bankAccount.getId(), saving2.getId());

        // assert
        assertEquals(saving2, result);
    }

    @Test
    void testFindSavingByIDNotFound (){
        // arrange
        int invalidSavingID = 999;

        when(bankAccountService.findBankAccountByID(
                user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act & assert
        assertThrows(NoSuchElementException.class, () -> savingService.findSavingByID(
                user.getId(), bankAccount.getId(), invalidSavingID));
    }

    @Test
    void testRemoveSavingFromBankAccountSavingDeactivated() {
        // arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);

        // act
        savingService.removeSavingFromBankAccount(
                user.getId(), bankAccount.getId(), bankAccount.getSavings().get(1).getId());

        // assert
        assertTrue(bankAccount.getSavings().contains(saving2));
        assertFalse(bankAccount.getSavings().get(1).isActive());
    }

    @Test
    void testRemoveSavingFromBankAccountSuccessSavingRemoved() {
        // arrange
        int initialSize = bankAccount.getSavings().size();
        LocalDate sameDate = LocalDate.of(2024, 12, 12);
        bankAccount.getSavings().get(1).setPayDay(sameDate);
        bankAccount.getSavings().get(1).setStartDate(sameDate);

        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);

        // act
        savingService.removeSavingFromBankAccount(
                user.getId(), bankAccount.getId(), bankAccount.getSavings().get(1).getId());

        // assert
        assertThrows(IndexOutOfBoundsException.class, () -> bankAccount.getSavings().get(1));
        assertEquals(initialSize - 1, bankAccount.getSavings().size());
    }

    @Test
    void testFindActiveSaving() {
        // arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act
        Saving activeSaving = savingService.findActiveSaving(user.getId(), bankAccount.getId());

        // assert
        assertEquals(saving2, activeSaving);
    }

    @Test
    void testFindActiveSavingNotFound() {
        // arrange
        bankAccount.getSavings().get(1).setActive(false);

        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act & assert
        assertThrows(NoSuchElementException.class, () -> savingService.findActiveSaving(user.getId(), bankAccount.getId()));
    }

    @Test
    void testAddSavingToBankAccount() throws AlreadyExistsException {
        // arrange
        NewSavingDTO newSaving = new NewSavingDTO(
                "name", null, new BigDecimal("100000"),
                LocalDate.of(2025, 2, 2), 1);

        int initialSize = bankAccount.getSavings().size();
        bankAccount.getSavings().get(1).setActive(false);

        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);

        // act
        savingService.addSavingToBankAccount(user.getId(), bankAccount.getId(), newSaving);

        // assert
        assertEquals(initialSize + 1, bankAccount.getSavings().size());
        assertTrue(bankAccount.getSavings().contains(Mapper.newSavingDTOToSaving(newSaving, bankAccount)));
    }

    @Test
    void testAddSavingToBankAccountAlreadyHasActiveSaving() {
        // arrange
        NewSavingDTO newSaving = new NewSavingDTO(
                "name", null, new BigDecimal("100000"),
                LocalDate.of(2025, 2, 2), 1);
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act & assert
        assertThrows(AlreadyExistsException.class, () -> savingService.addSavingToBankAccount(
                user.getId(), bankAccount.getId(), newSaving));
    }

    @Test
    void testRemoveActiveSavingHasActiveSaving() {
        // arrange
        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(savingService.createOrUpdateSaving(savingService.findActiveSaving(
                user.getId(), bankAccount.getId()))).thenReturn(saving2);


        // act
        boolean activeSaving = savingService.removeActiveSaving(user.getId(), bankAccount.getId());

        // assert
        assertTrue(activeSaving);
        assertFalse(bankAccount.getSavings().get(1).isActive());
    }

    @Test
    void testRemoveActiveSavingHasNoActiveSaving() {
        // arrange
        bankAccount.getSavings().get(1).setActive(false);

        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);

        // act
        boolean activeSaving = savingService.removeActiveSaving(user.getId(), bankAccount.getId());

        // assert
        assertFalse(activeSaving);
        assertFalse(bankAccount.getSavings().get(1).isActive());
    }

    @Test
    void testTransferFromSavingToBalance() {
        // arrange
        int transferSum = 100;
        BigDecimal newSavingsAmount = bankAccount.getSavingsAmount().subtract(BigDecimal.valueOf(transferSum));
        BigDecimal newBalance = bankAccount.getBalance().add(BigDecimal.valueOf(transferSum));

        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);

        // act
        BankAccount updateBankAccount = savingService.transferFromSavingToBalance(
                user.getId(), bankAccount.getId(), transferSum);

        // assert
        assertEquals(0, updateBankAccount.getSavingsAmount().compareTo(newSavingsAmount));
        assertEquals(0, updateBankAccount.getBalance().compareTo(newBalance));
    }

    @Test
    void testTransferFromSavingToBalanceNotEnoughSaving() {
        // arrange
        int transferSumToBig = 10000000;

        when(bankAccountService.findBankAccountByID(user.getId(), bankAccount.getId())).thenReturn(bankAccount);
        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);

        // act & assert
        assertThrows(IllegalArgumentException.class, () -> savingService.transferFromSavingToBalance(
                user.getId(), bankAccount.getId(), transferSumToBig));
    }

    @Test
    void testProcessSavingWillNotExpire() {
        // arrange
        int initialTransactionSize = bankAccount.getTransactions().size();
        LocalDate initialPayday = bankAccount.getSavings().get(1).getPayDay();
        BigDecimal newSavingsAmount = bankAccount.getSavingsAmount().add(saving2.getAmount());
        BigDecimal newBalance = bankAccount.getBalance().subtract(saving2.getAmount());

        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);
        when(bankAccountService.canMakeTransaction(eq(bankAccount), any(Transaction.class))).thenReturn(true);

        // act
        savingService.processSaving(bankAccount.getSavings().get(1));

        // assert
        assertEquals(0, bankAccount.getSavingsAmount().compareTo(newSavingsAmount));
        assertEquals(0, bankAccount.getBalance().compareTo(newBalance));
        assertEquals(initialTransactionSize + 1 ,bankAccount.getTransactions().size());
        assertEquals(initialPayday.plusMonths(1), bankAccount.getSavings().get(1).getPayDay());
        assertTrue(bankAccount.getSavings().get(1).isActive());
    }

    @Test
    void testProcessSavingSavingWillExpire() {
        // arrange
        int initialTransactionSize = bankAccount.getTransactions().size();
        bankAccount.getSavings().get(1).setPayDay(bankAccount.getSavings().get(1).getPayDay().plusMonths(2));
        LocalDate initialPayday = bankAccount.getSavings().get(1).getPayDay();
        BigDecimal newSavingsAmount = bankAccount.getSavingsAmount().add(saving2.getAmount());
        BigDecimal newBalance = bankAccount.getBalance().subtract(saving2.getAmount());

        when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);
        when(bankAccountService.canMakeTransaction(eq(bankAccount), any(Transaction.class))).thenReturn(true);

        // act
        savingService.processSaving(bankAccount.getSavings().get(1));

        // assert
        assertEquals(0, bankAccount.getSavingsAmount().compareTo(newSavingsAmount));
        assertEquals(0, bankAccount.getBalance().compareTo(newBalance));
        assertEquals(initialTransactionSize + 1 ,bankAccount.getTransactions().size());
        assertEquals(initialPayday, bankAccount.getSavings().get(1).getPayDay());
        assertFalse(bankAccount.getSavings().get(1).isActive());
    }

    @Test
    void testProcessSavingSavingCannotMakeTransaction() {
            // arrange
            bankAccount.getSavings().get(1).setAmount(new BigDecimal("999999999999999999.99"));
            int initialTransactionSize = bankAccount.getTransactions().size();
            BigDecimal newSavingsAmount = bankAccount.getSavingsAmount();
            BigDecimal newBalance = bankAccount.getBalance();

            when(bankAccountService.createOrUpdateBankAccount(bankAccount)).thenReturn(bankAccount);
            when(bankAccountService.canMakeTransaction(eq(bankAccount), any(Transaction.class))).thenReturn(false);

            // act
            savingService.processSaving(bankAccount.getSavings().get(1));

            // assert
            assertEquals(0, bankAccount.getSavingsAmount().compareTo(newSavingsAmount));
            assertEquals(0, bankAccount.getBalance().compareTo(newBalance));
            assertEquals(initialTransactionSize ,bankAccount.getTransactions().size());
            assertTrue(bankAccount.getSavings().get(1).isActive());
        }
}