package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewBankAccountDTO;
import de.fherfurt.clevercash.api.util.HelperFunctions;
import de.fherfurt.clevercash.storage.models.*;
import de.fherfurt.clevercash.storage.repositories.BankAccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service class for managing bank accounts.
 *
 * @author Richard Prax, Jakob Roch, Anton Götz
 */
@AllArgsConstructor
@Service
public class BankAccountService {
    private final BankAccountRepository bankAccountRepository;
    private final UserService userService;
    private final FilterService filterService;

    /**
     * Finds all bank accounts associated with a user.
     *
     * @param userID the ID of the user
     * @return list of bank accounts
     * @throws NoSuchElementException if the user is not found
     */
    public List<BankAccount> findAllBankAccounts(int userID) throws NoSuchElementException {
        User user = userService.findUserByID(userID);
        return user.getBankAccounts();
    }

    /**
     * Finds a specific bank account by ID for a user.
     *
     * @param userID        the ID of the user
     * @param bankAccountID the ID of the bank account
     * @return the bank account
     * @throws NoSuchElementException if the user or bank account is not found
     */
    public BankAccount findBankAccountByID(int userID, int bankAccountID) throws NoSuchElementException {
        User user = userService.findUserByID(userID);

        return user.getBankAccounts()
                .stream()
                .filter(bankAccount -> bankAccount.getId() == bankAccountID)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("BankAccount not found for User with ID" + userID));
    }

    /**
     * Adds a new bank account to a user.
     *
     * @param userID            the ID of the user
     * @param newBankAccountDTO the new bank account data
     * @return the added bank account
     * @throws NoSuchElementException   if the user is not found
     * @throws AlreadyExistsException   if a bank account with the same name already exists
     * @throws IllegalArgumentException if the bank account data is invalid
     */
    public BankAccount addBankAccountToUser(int userID, NewBankAccountDTO newBankAccountDTO) throws NoSuchElementException, AlreadyExistsException, IllegalArgumentException {
        User user = userService.findUserByID(userID);
        HelperFunctions.validateBankAccount(newBankAccountDTO);
        BankAccount bankAccount = Mapper.newBankAccountDTOToBankAccount(newBankAccountDTO, user);
        if (!isValidBankAccountName(bankAccount, user)) {
            throw new AlreadyExistsException("Bank account with this name is already in use");
        }
        user.getBankAccounts().add(bankAccount);
        bankAccount.setUser(user);
        createOrUpdateBankAccount(bankAccount);

        return userService.createOrUpdateUser(user).getBankAccounts().getLast();
    }

    /**
     * Updates an existing bank account.
     *
     * @param userID            the ID of the user
     * @param bankAccountID     the ID of the bank account
     * @param newBankAccountDTO the updated bank account data
     * @return the updated bank account
     * @throws NoSuchElementException   if the user or bank account is not found
     * @throws IllegalArgumentException if the bank account data is invalid
     * @throws AlreadyExistsException   if another bank account with the same name already exists
     */
    public BankAccount updateBankAccount(int userID, int bankAccountID, NewBankAccountDTO newBankAccountDTO) throws NoSuchElementException, IllegalArgumentException, AlreadyExistsException {
        User user = userService.findUserByID(userID);
        HelperFunctions.validateBankAccount(newBankAccountDTO);
        BankAccount bankAccount = Mapper.newBankAccountDTOToBankAccount(newBankAccountDTO, user);

        BankAccount bankAccountToUpdate = user.getBankAccounts()
                .stream()
                .filter(tempBankAccount -> tempBankAccount.getId() == bankAccountID)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Bank account not found with id: " + bankAccountID));

        assert bankAccountToUpdate != null;

        if (bankAccount.getName().equals(bankAccountToUpdate.getName()) || isValidBankAccountName(bankAccount, user)) {
            bankAccountToUpdate.setName(bankAccount.getName());
            bankAccountToUpdate.setDailyLimit(bankAccount.getDailyLimit());
            bankAccountToUpdate.setBalance(bankAccount.getBalance());
            bankAccountToUpdate = createOrUpdateBankAccount(bankAccountToUpdate);
        } else {
            throw new AlreadyExistsException("Other Bank account with this name is already in use");
        }

        return bankAccountToUpdate;
    }

    /**
     * Checks if the bank account name is valid for a user.
     *
     * @param bankAccount the bank account to check
     * @param user        the user
     * @return true if the name is valid, false otherwise
     */
    private boolean isValidBankAccountName(BankAccount bankAccount, User user) {
        return user.getBankAccounts()
                .stream()
                .noneMatch(tempBankAccount -> tempBankAccount.getName().equals(bankAccount.getName()));
    }

    /**
     * Creates or updates a bank account in the repository.
     *
     * @param bankAccount the bank account to create or update
     * @return the created or updated bank account
     */
    public BankAccount createOrUpdateBankAccount(BankAccount bankAccount) {
        return bankAccountRepository.save(bankAccount);
    }

    /**
     * Deletes a bank account from a user.
     *
     * @param userID        the ID of the user
     * @param bankAccountID the ID of the bank account
     * @throws NoSuchElementException if the user or bank account is not found
     */
    public void deleteUserBankAccount(int userID, int bankAccountID) throws NoSuchElementException {
        User user = userService.findUserByID(userID);
        BankAccount bankAccountToRemove = findBankAccountByID(userID, bankAccountID);
        user.getBankAccounts().remove(bankAccountToRemove);
        deleteBankAccount(bankAccountToRemove);
        userService.createOrUpdateUser(user);
    }

    /**
     * Deletes a bank account from the repository.
     *
     * @param bankAccount the bank account to delete
     */
    private void deleteBankAccount(BankAccount bankAccount) {
        bankAccountRepository.delete(bankAccount);
    }

    /**
     * Checks if a transaction can be made from a bank account.
     *
     * @param bankAccount the bank account
     * @param transaction the transaction to check
     * @return true if the transaction can be made, false otherwise
     */
    public boolean canMakeTransaction(BankAccount bankAccount, Transaction transaction) {
        BigDecimal transactionAmount = transaction.getAmount();
        BigDecimal availableDailyBudget = getAvailableDailyBudget(bankAccount);

        return transactionAmount.compareTo(bankAccount.getDailyLimit()) <= 0
                && isEnoughBalance(bankAccount, transactionAmount)
                && !hasReachedDailyLimit(bankAccount)
                && availableDailyBudget.subtract(transactionAmount).compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * Gets the amount spent today from a bank account.
     *
     * @param bankAccount the bank account
     * @return the amount spent today
     */
    private BigDecimal getTodaySpentAmount(BankAccount bankAccount) {
        String today = LocalDate.now().toString();
        return filterService.filterTransactionsByDate(bankAccount.getTransactions(), today, today)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .negate();
    }

    /**
     * Gets the available daily budget for a bank account.
     *
     * @param bankAccount the bank account
     * @return the available daily budget
     */
    private BigDecimal getAvailableDailyBudget(BankAccount bankAccount) {
        BigDecimal todaySpentAmount = getTodaySpentAmount(bankAccount);
        return bankAccount.getDailyLimit().subtract(todaySpentAmount);
    }

    /**
     * Checks if the daily limit has been reached for a bank account.
     *
     * @param bankAccount the bank account
     * @return true if the daily limit has been reached, false otherwise
     */
    private boolean hasReachedDailyLimit(BankAccount bankAccount) {
        BigDecimal todaySpentAmount = getTodaySpentAmount(bankAccount);
        return todaySpentAmount.compareTo(bankAccount.getDailyLimit()) >= 0;
    }

    /**
     * Checks if there is enough balance in a bank account for a given amount.
     *
     * @param bankAccount the bank account
     * @param amount      the amount to check
     * @return true if there is enough balance, false otherwise
     */
    private boolean isEnoughBalance(BankAccount bankAccount, BigDecimal amount) {
        return bankAccount.getBalance().compareTo(amount) >= 0;
    }
}
