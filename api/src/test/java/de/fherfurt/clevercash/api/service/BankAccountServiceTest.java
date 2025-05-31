package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewBankAccountDTO;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Transaction;
import de.fherfurt.clevercash.storage.models.User;
import de.fherfurt.clevercash.storage.repositories.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class BankAccountServiceTest {

    BankAccount bankAccount1;

    User user;

    @Mock
    FilterService filterService;

    @Mock
    UserService userService;

    @Mock
    BankAccountRepository bankAccountRepository;

    @InjectMocks
    BankAccountService bankAccountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bankAccount1 = TestUtils.getTestBankAccount1();
        user = TestUtils.getTestUser1();
    }

    @Test
    void testFindAllBankAccounts (){
        // arrange
        BankAccount bankAccount2 = TestUtils.getTestBankAccount2();

        when(userService.findUserByID(user.getId())).thenReturn(user);

        // act
        List<BankAccount> bankAccounts = bankAccountService.findAllBankAccounts(user.getId());

        // assert
        assertEquals(2, bankAccounts.size());
        assertEquals(bankAccount1, bankAccounts.get(0));
        assertEquals(bankAccount2, bankAccounts.get(1));
    }

    @Test
    void testFindBankAccountByID (){
        // arrange
        when(userService.findUserByID(user.getId())).thenReturn(user);

        // act
        BankAccount foundBankAccount = bankAccountService.findBankAccountByID(user.getId(), bankAccount1.getId());

        // assert
        assertEquals(bankAccount1, foundBankAccount);
    }

    @Test
    void testFindBankAccountByIDBankAccountNotFound (){
        // arrange
        when(userService.findUserByID(user.getId())).thenReturn(user);

        // act & assert
        assertThrows(NoSuchElementException.class, () -> bankAccountService.findBankAccountByID(user.getId(), 9999));
    }

    @Test
    void testAddBankAccountToUser () throws AlreadyExistsException {
        // arrange
        int oldNumberOfBankAccounts = user.getBankAccounts().size();
        NewBankAccountDTO newBankAccountDTO = new NewBankAccountDTO("TestAccount", new BigDecimal("10000"), new BigDecimal("1000"));

        when(userService.findUserByID(user.getId())).thenReturn(user);
        when(userService.createOrUpdateUser(any(User.class))).thenReturn(user);

        // act
        BankAccount newBankAccount = bankAccountService.addBankAccountToUser(user.getId(), newBankAccountDTO);

        // assert
        assertEquals(newBankAccount.getName(), newBankAccountDTO.getName());
        assertEquals(newBankAccount.getBalance(), newBankAccountDTO.getBalance());
        assertEquals(oldNumberOfBankAccounts + 1, user.getBankAccounts().size());
        assertTrue(user.getBankAccounts().contains(newBankAccount));
    }

    @Test
    void testAddBankAccountToUserInvalidName () {
        // arrange
        int oldNumberOfBankAccounts = user.getBankAccounts().size();
        NewBankAccountDTO newBankAccountDTO = new NewBankAccountDTO("Mein erster Bankkonto", new BigDecimal("10000"), new BigDecimal("1000"));

        when(userService.findUserByID(user.getId())).thenReturn(user);
        when(userService.createOrUpdateUser(any(User.class))).thenReturn(user);

        // act & assert
        assertThrows(AlreadyExistsException.class, () -> bankAccountService.addBankAccountToUser(user.getId(), newBankAccountDTO));
        assertEquals(oldNumberOfBankAccounts, user.getBankAccounts().size());
    }

    @Test
    void testUpdateBankAccount () throws Exception {
        NewBankAccountDTO newBankAccountDTO = new NewBankAccountDTO("TestAccount", new BigDecimal("10000"), new BigDecimal("1000"));
        BankAccount newBankAccount = Mapper.newBankAccountDTOToBankAccount(newBankAccountDTO, user);

        // arrange
        when(userService.findUserByID(user.getId())).thenReturn(user);
        when(bankAccountRepository.save(bankAccount1)).thenReturn(newBankAccount);

        // act
        BankAccount updatedBankAccount = bankAccountService.updateBankAccount(user.getId(), bankAccount1.getId(), newBankAccountDTO);

        // assert
        assertEquals(newBankAccount, updatedBankAccount);
    }

    @Test
    void testUpdateBankAccountInvalidBankAccount (){
        NewBankAccountDTO newBankAccountDTO = new NewBankAccountDTO("TestAccount", new BigDecimal("10000"), new BigDecimal("1000"));
        newBankAccountDTO.setName("");

        // arrange
        when(userService.findUserByID(user.getId())).thenReturn(user);

        // act & assert
        assertThrows(IllegalArgumentException.class, () -> bankAccountService.updateBankAccount(user.getId(), bankAccount1.getId(), newBankAccountDTO));
    }

    @Test
    void testUpdateBankAccountNameAlreadyExists () {
        NewBankAccountDTO newBankAccountDTO = new NewBankAccountDTO("Mein zweiter Bankkonto", new BigDecimal("10000"), new BigDecimal("1000"));

        // arrange
        when(userService.findUserByID(user.getId())).thenReturn(user);

        // act & assert
        assertThrows(AlreadyExistsException.class, () -> bankAccountService.updateBankAccount(user.getId(), bankAccount1.getId(), newBankAccountDTO));
    }

    @Test
    public void testCreateOrUpdateBankAccount() {
        // Arrange
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(bankAccount1);

        // Act
        BankAccount result = bankAccountService.createOrUpdateBankAccount(bankAccount1);

        // Assert
        assertEquals(bankAccount1, result);
    }

    @Test
    void testDeleteUserBankAccount (){
        // arrange
        int oldNumberOfBankAccounts = user.getBankAccounts().size();

        when(userService.findUserByID(user.getId())).thenReturn(user);
        when(userService.createOrUpdateUser(any(User.class))).thenReturn(user);

        // act
        bankAccountService.deleteUserBankAccount(user.getId(), bankAccount1.getId());

        // assert
        assertEquals(oldNumberOfBankAccounts - 1, user.getBankAccounts().size());
        assertFalse(user.getBankAccounts().contains(bankAccount1));
    }

    @Test
    void testCanMakeTransaction (){
        // arrange
        Transaction transaction = TestUtils.getTestTransaction1(bankAccount1);

        when(filterService.filterTransactionsByDate(anyList(), anyString(), anyString()))
                .thenReturn(TestUtils.getTestTransactionList(bankAccount1));

        // act
        boolean canMakeTransaction = bankAccountService.canMakeTransaction(bankAccount1, transaction);

        // assert
        assertTrue(canMakeTransaction);
    }

    @Test
    void testCanMakeTransactionNotEnoughBalance (){
        // arrange
        Transaction transaction = TestUtils.getTestTransaction1(bankAccount1);
        transaction.setAmount(new BigDecimal("10000000"));

        when(filterService.filterTransactionsByDate(any(), any(), any()))
                .thenReturn(TestUtils.getTestTransactionList(bankAccount1));

        // act
        boolean canMakeTransaction = bankAccountService.canMakeTransaction(bankAccount1, transaction);

        // assert
        assertFalse(canMakeTransaction);
    }

    @Test
    void testCanMakeTransactionDailyLimitAlreadyReached (){
        // arrange
        Transaction transaction = TestUtils.getTestTransaction1(bankAccount1);
        bankAccount1.setDailyLimit(new BigDecimal("0"));

        when(filterService.filterTransactionsByDate(any(), any(), any()))
                .thenReturn(TestUtils.getTestTransactionList(bankAccount1));

        // act
        boolean canMakeTransaction = bankAccountService.canMakeTransaction(bankAccount1, transaction);

        // assert
        assertFalse(canMakeTransaction);
    }

    @Test
    void testCanMakeTransactionExceedingDailyLimit (){
        // arrange
        Transaction transaction = TestUtils.getTestTransaction1(bankAccount1);
        transaction.setAmount(new BigDecimal("1100"));

        when(filterService.filterTransactionsByDate(any(), any(), any()))
                .thenReturn(TestUtils.getTestTransactionList(bankAccount1));

        // act
        boolean canMakeTransaction = bankAccountService.canMakeTransaction(bankAccount1, transaction);

        // assert
        assertFalse(canMakeTransaction);
    }
}
