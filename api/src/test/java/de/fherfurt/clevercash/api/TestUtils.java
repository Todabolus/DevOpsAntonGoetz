package de.fherfurt.clevercash.api;

import de.fherfurt.clevercash.api.models.input.NewAddressDTO;
import de.fherfurt.clevercash.api.models.input.NewUserDTO;
import de.fherfurt.clevercash.storage.models.*;
import de.fherfurt.clevercash.storage.util.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class TestUtils {

    /* New Test user 1*/

    private static final String NEW_USER1_FIRST_NAME = "Max";
    private static final String NEW_USER1_LAST_NAME = "Mustermann";
    private static final String NEW_USER1_EMAIL = "maxMustermann@gmail.com";
    private static final String NEW_USER1_PASSWORD = "MaxMustermann1!";
    private static final String NEW_USER1_BIRTH_DATE = "2000-01-01";

    public static NewUserDTO getNewTestUser1() {
        NewUserDTO newUserDTO = new NewUserDTO();
        newUserDTO.setFirstName(NEW_USER1_FIRST_NAME);
        newUserDTO.setLastName(NEW_USER1_LAST_NAME);
        newUserDTO.setEmail(NEW_USER1_EMAIL);
        newUserDTO.setPassword(NEW_USER1_PASSWORD);
        newUserDTO.setBirthDate(NEW_USER1_BIRTH_DATE);
        return newUserDTO;
    }

    /* Test user 1*/

    private static final int USER1_USER_ID = 1;
    private static final String USER1_FIRST_NAME = "Max";
    private static final String USER1_LAST_NAME = "Mustermann";
    private static final String USER1_EMAIL = "maxMustermann@gmail.com";
    private static final String USER1_PASSWORD = "MaxMustermann1!";
    private static final LocalDate USER1_BIRTH_DATE = LocalDate.of(2000, 1, 1);

    public static User getTestUser1() {
        User user = new User();
        user.setId(USER1_USER_ID);
        user.setFirstName(USER1_FIRST_NAME);
        user.setLastName(USER1_LAST_NAME);
        user.setEmail(USER1_EMAIL);
        user.setPassword(USER1_PASSWORD);
        user.setBirthDate(USER1_BIRTH_DATE);
        user.setBankAccounts(getTestBankAccountList());
        return user;
    }

    /* Test user 2*/

    private static final int USER2_USER_ID = 2;
    private static final String USER2_FIRST_NAME = "Rainer";
    private static final String USER2_LAST_NAME = "Zufall";
    private static final String USER2_EMAIL = "rainerZufall@gmail.com";
    private static final String USER2_PASSWORD = "RainerZufall1!";
    private static final LocalDate USER2_BIRTH_DATE = LocalDate.of(2000, 1, 1);

    public static User getTestUser2() {
        User user = new User();
        user.setId(USER2_USER_ID);
        user.setFirstName(USER2_FIRST_NAME);
        user.setLastName(USER2_LAST_NAME);
        user.setEmail(USER2_EMAIL);
        user.setPassword(USER2_PASSWORD);
        user.setBirthDate(USER2_BIRTH_DATE);
        return user;
    }

    public static List<User> getTestUserList() {
        List<User> users = new ArrayList<>();
        users.add(getTestUser1());
        users.add(getTestUser2());
        return users;
    }

    /* Test address new*/

    private static final String POSTAL_CODE = "11111";
    private static final String COUNTRY = "TestCounty";
    private static final String CITY = "TestCity";
    private static final String STREET = "TestStreet";
    private static final String STREET_NUMBER = "1";
    private static final String STATE = "TestState";

    public static NewAddressDTO getNewTestAddress() {
        NewAddressDTO newAddressDTO = new NewAddressDTO();
        newAddressDTO.setPostalCode(POSTAL_CODE);
        newAddressDTO.setCountry(COUNTRY);
        newAddressDTO.setCity(CITY);
        newAddressDTO.setStreet(STREET);
        newAddressDTO.setStreetNumber(STREET_NUMBER);
        newAddressDTO.setState(STATE);
        return newAddressDTO;
    }

    /* Test address */

    public static Address getTestAddress() {
        Address address = new Address();
        address.setId(1);
        address.setPostalCode(POSTAL_CODE);
        address.setCountry(COUNTRY);
        address.setCity(CITY);
        address.setStreet(STREET);
        address.setStreetNumber(STREET_NUMBER);
        address.setState(STATE);
        return address;
    }


    /* Test Installment 1 */
    private static final int INSTALLMENT1_ID = 1;
    private static final String INSTALLMENT1_NAME = "Smartphone";
    private static final String INSTALLMENT1_DESCRIPTION = "New smartphone";
    private static final BigDecimal INSTALLMENT1_AMOUNT = new BigDecimal("1200.00");
    private static final BigDecimal INSTALLMENT1_AMOUNT_PER_RATE = new BigDecimal("100.00");
    private static final LocalDate INSTALLMENT1_START_DATE = LocalDate.now();
    private static final int INSTALLMENT1_DURATION_IN_MONTHS = 12;
    private static final LocalDate INSTALLMENT1_PAYDAY = LocalDate.now();
    private static boolean INSTALLMENT1_ACTIVE = true;

    public static Installment getTestInstallment1(BankAccount bankAccount) {
        return new Installment(INSTALLMENT1_ID, INSTALLMENT1_NAME, INSTALLMENT1_AMOUNT, BigDecimal.ZERO,
                INSTALLMENT1_AMOUNT_PER_RATE, INSTALLMENT1_START_DATE, INSTALLMENT1_DURATION_IN_MONTHS,
                INSTALLMENT1_PAYDAY, INSTALLMENT1_DESCRIPTION, bankAccount, INSTALLMENT1_ACTIVE);
    }

    /* Test Installment 2 */
    private static final int INSTALLMENT2_ID = 2;
    private static final String INSTALLMENT2_NAME = "Laptop";
    private static final String INSTALLMENT2_DESCRIPTION = "New laptop";
    private static final BigDecimal INSTALLMENT2_AMOUNT = new BigDecimal("1500.00");
    private static final BigDecimal INSTALLMENT2_AMOUNT_PER_RATE = new BigDecimal("125.00");
    private static final LocalDate INSTALLMENT2_START_DATE = LocalDate.now().plusMonths(1); // Example: Starts next month
    private static final int INSTALLMENT2_DURATION_IN_MONTHS = 10;
    private static final LocalDate INSTALLMENT2_PAYDAY = LocalDate.now().plusMonths(1); // Example: Payday next month
    private static final boolean INSTALLMENT2_ACTIVE = true;

    public static Installment getTestInstallment2(BankAccount bankAccount) {
        return new Installment(INSTALLMENT2_ID, INSTALLMENT2_NAME, INSTALLMENT2_AMOUNT, BigDecimal.ZERO,
                INSTALLMENT2_AMOUNT_PER_RATE, INSTALLMENT2_START_DATE, INSTALLMENT2_DURATION_IN_MONTHS,
                INSTALLMENT2_PAYDAY, INSTALLMENT2_DESCRIPTION, bankAccount, INSTALLMENT2_ACTIVE);
    }

    /* Test bank account 1 */
    private static final int BANK_ACCOUNT1_ID = 1;
    private static final User TEST_USER = TestUtils.getTestUser1();
    private static final String BANK_ACCOUNT1_NAME = "Mein erster Bankkonto";
    private static final BigDecimal BANK_ACCOUNT_BALANCE = new BigDecimal("5000.00");
    private static final BigDecimal BANK_ACCOUNT_DAILY_LIMIT = new BigDecimal("1000.00");
    private static final BigDecimal BANK_ACCOUNT_SAVINGS_AMOUNT = new BigDecimal("2100.00");

    public static BankAccount getTestBankAccount1() {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setId(BANK_ACCOUNT1_ID);
        bankAccount.setUser(TEST_USER);
        bankAccount.setName(BANK_ACCOUNT1_NAME);
        bankAccount.setBalance(BANK_ACCOUNT_BALANCE);
        bankAccount.setDailyLimit(BANK_ACCOUNT_DAILY_LIMIT);
        bankAccount.setSavingsAmount(BANK_ACCOUNT_SAVINGS_AMOUNT);
        return bankAccount;
    }

    /* Test bank account 2 */
    private static final int BANK_ACCOUNT2_ID = 2;
    private static final String BANK_ACCOUNT2_NAME = "Mein zweiter Bankkonto";

    public static BankAccount getTestBankAccount2() {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setId(BANK_ACCOUNT2_ID);
        bankAccount.setUser(TEST_USER);
        bankAccount.setName(BANK_ACCOUNT2_NAME);
        bankAccount.setBalance(BANK_ACCOUNT_BALANCE);
        bankAccount.setDailyLimit(BANK_ACCOUNT_DAILY_LIMIT);
        bankAccount.setSavingsAmount(BANK_ACCOUNT_SAVINGS_AMOUNT);
        return bankAccount;
    }

    public static List<BankAccount> getTestBankAccountList() {
        List<BankAccount> bankAccounts = new ArrayList<>();
        bankAccounts.add(getTestBankAccount1());
        bankAccounts.add(getTestBankAccount2());
        return bankAccounts;
    }

    /* Test Transaction 1 */
    private static final int TRANSACTION1_ID = 1;
    private static final BigDecimal TRANSACTION1_AMOUNT = new BigDecimal("100.00");
    private static final String TRANSACTION1_DESCRIPTION = "Payment for services";
    private static final LocalDateTime TRANSACTION1_DATE = LocalDateTime.now();
    private static final TransactionType TRANSACTION1_TYPE = TransactionType.PAYMENT;

    public static Transaction getTestTransaction1(BankAccount bankAccount) {
        return new Transaction(TRANSACTION1_ID, TRANSACTION1_AMOUNT, TRANSACTION1_DESCRIPTION, TRANSACTION1_DATE, TRANSACTION1_TYPE, bankAccount);
    }

    /* Test Transaction 2 */
    private static final int TRANSACTION2_ID = 2;
    private static final BigDecimal TRANSACTION2_AMOUNT = new BigDecimal("50.00");
    private static final String TRANSACTION2_DESCRIPTION = "Savings deposit";
    private static final LocalDateTime TRANSACTION2_DATE = LocalDateTime.now().plusDays(1);
    private static final TransactionType TRANSACTION2_TYPE = TransactionType.SAVING;

    public static Transaction getTestTransaction2(BankAccount bankAccount) {
        return new Transaction(TRANSACTION2_ID, TRANSACTION2_AMOUNT, TRANSACTION2_DESCRIPTION, TRANSACTION2_DATE, TRANSACTION2_TYPE, bankAccount);
    }

    /* Test Transaction 3 */
    private static final int TRANSACTION3_ID = 3;
    private static final BigDecimal TRANSACTION3_AMOUNT = new BigDecimal("200.00");
    private static final String TRANSACTION3_DESCRIPTION = "Installment payment";
    private static final LocalDateTime TRANSACTION3_DATE = LocalDateTime.now().plusDays(2);
    private static final TransactionType TRANSACTION3_TYPE = TransactionType.INSTALLMENT;

    public static Transaction getTestTransaction3(BankAccount bankAccount) {
        return new Transaction(TRANSACTION3_ID, TRANSACTION3_AMOUNT, TRANSACTION3_DESCRIPTION, TRANSACTION3_DATE, TRANSACTION3_TYPE, bankAccount);
    }

    /* Test Transaction 4 */
    private static final int TRANSACTION4_ID = 4;
    private static final BigDecimal TRANSACTION4_AMOUNT = new BigDecimal("150.00");
    private static final String TRANSACTION4_DESCRIPTION = "Payment for goods";
    private static final LocalDateTime TRANSACTION4_DATE = LocalDateTime.now().plusDays(2);
    private static final TransactionType TRANSACTION4_TYPE = TransactionType.PAYMENT;

    public static Transaction getTestTransaction4(BankAccount bankAccount) {
        return new Transaction(TRANSACTION4_ID, TRANSACTION4_AMOUNT, TRANSACTION4_DESCRIPTION, TRANSACTION4_DATE, TRANSACTION4_TYPE, bankAccount);
    }

    public static List<Transaction> getTestTransactionList(BankAccount bankAccount) {
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(getTestTransaction1(bankAccount));
        transactions.add(getTestTransaction2(bankAccount));
        transactions.add(getTestTransaction3(bankAccount));
        transactions.add(getTestTransaction4(bankAccount));
        return transactions;
    }

    /* Test Saving 1 */
    private static final int SAVING1_ID = 1;
    private static final String SAVING1_NAME = "Retirement Funds";
    private static final String SAVING1_DESCRIPTION = "Savings for when i'm old";
    private static final BigDecimal SAVING1_AMOUNT = new BigDecimal("1500.00");
    private static final LocalDate SAVING1_START_DATE = LocalDate.now().plusMonths(1);
    private static final int SAVING1_DURATION_IN_MONTHS = 42;
    private static final LocalDate SAVING1_PAYDAY = LocalDate.now().plusMonths(2);
    private static boolean SAVING1_ACTIVE = false;

    public static Saving getTestSaving1(BankAccount bankAccount) {
        return new Saving(SAVING1_ID, SAVING1_NAME, SAVING1_DESCRIPTION, SAVING1_AMOUNT, SAVING1_START_DATE, SAVING1_DURATION_IN_MONTHS, SAVING1_PAYDAY, bankAccount, SAVING1_ACTIVE);
    }

    /* Test Saving 2 */
    private static final int SAVING2_ID = 2;
    private static final String SAVING2_NAME = "Vacation Funds";
    private static final String SAVING2_DESCRIPTION = "Savings for an trip to greece";
    private static final BigDecimal SAVING2_AMOUNT = new BigDecimal("600.00");
    private static final LocalDate SAVING2_START_DATE = LocalDate.now();
    private static final int SAVING2_DURATION_IN_MONTHS = 3;
    private static final LocalDate SAVING2_PAYDAY = LocalDate.now().plusMonths(1);
    private static boolean SAVING2_ACTIVE = true;

    public static Saving getTestSaving2(BankAccount bankAccount) {
        return new Saving(SAVING2_ID, SAVING2_NAME, SAVING2_DESCRIPTION, SAVING2_AMOUNT, SAVING2_START_DATE, SAVING2_DURATION_IN_MONTHS, SAVING2_PAYDAY, bankAccount, SAVING2_ACTIVE);
    }
}
