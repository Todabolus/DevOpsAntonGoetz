package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewSavingDTO;
import de.fherfurt.clevercash.api.util.HelperFunctions;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Saving;
import de.fherfurt.clevercash.storage.models.Transaction;
import de.fherfurt.clevercash.storage.util.TransactionType;
import de.fherfurt.clevercash.storage.repositories.SavingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Service for managing savings associated with bank accounts.
 * Provides functionalities to create, update, find, and process savings.
 *
 * @author Jakob Roch, Richard Prax, Anton Götz
 */
@AllArgsConstructor
@Service
public class SavingService {
    private final SavingRepository savingRepository;
    private final BankAccountService bankAccountService;

    /**
     * Creates or updates a saving.
     *
     * @param saving the saving to create or update
     * @return the created or updated saving
     */
    public Saving createOrUpdateSaving(Saving saving) {
        return savingRepository.save(saving);
    }

    /**
     * Finds all savings for a user with optional filters.
     *
     * @param userID          the user ID
     * @param bankAccountID   the bank account ID
     * @param startDateString the start date filter (optional)
     * @param endDateString   the end date filter (optional)
     * @param description     the description filter (optional)
     * @return the list of filtered savings
     * @throws DateTimeParseException if date parsing fails
     * @throws NoSuchElementException if bank account is not found
     */
    public List<Saving> findAllSavingForUserWithFilters(int userID, int bankAccountID, String startDateString, String endDateString, String description) throws DateTimeParseException, NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        List<Saving> savings = bankAccount.getSavings();

        LocalDate startDate = (startDateString == null || startDateString.isEmpty()) ? null : LocalDate.parse(startDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate endDate = (endDateString == null || endDateString.isEmpty()) ? null : LocalDate.parse(endDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return savings.stream()
                .filter(saving -> (startDate == null || !saving.getStartDate().isBefore(startDate)) &&
                        (endDate == null || !saving.getStartDate().isAfter(endDate)) &&
                        (description == null || saving.getDescription().contains(description)))
                .collect(Collectors.toList());
    }

    /**
     * Finds a saving by its ID.
     *
     * @param userID        the user ID
     * @param bankAccountID the bank account ID
     * @param savingID      the saving ID
     * @return the found saving
     * @throws NoSuchElementException if saving is not found
     */
    public Saving findSavingByID(int userID, int bankAccountID, int savingID) throws NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        return bankAccount.getSavings().stream()
                .filter(saving -> saving.getId() == savingID)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Saving not found for bankAccount with id " + bankAccountID));
    }

    /**
     * Removes a saving from a bank account.
     *
     * @param userID        the user ID
     * @param bankAccountID the bank account ID
     * @param savingID      the saving ID
     * @throws NoSuchElementException if saving or bank account is not found
     */
    public void removeSavingFromBankAccount(int userID, int bankAccountID, int savingID) throws NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        Saving savingToRemove = findSavingByID(userID, bankAccountID, savingID);
        if (savingToRemove.getPayDay().isAfter(savingToRemove.getStartDate())) {
            savingToRemove.setActive(false);
        } else {
            bankAccount.getSavings().remove(savingToRemove);
            bankAccount.setSavingsAmount(bankAccount.getSavingsAmount().subtract(savingToRemove.getAmount()));
        }
        bankAccountService.createOrUpdateBankAccount(bankAccount);
    }

    /**
     * Finds the active saving for a bank account.
     *
     * @param userID        the user ID
     * @param bankAccountID the bank account ID
     * @return the active saving
     * @throws NoSuchElementException if no active saving is found
     */
    public Saving findActiveSaving(int userID, int bankAccountID) throws NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        return bankAccount.getSavings().stream()
                .filter(Saving::isActive)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No active saving found for bankAccount with id " + bankAccountID));
    }

    /**
     * Adds a saving to a bank account.
     *
     * @param userID        the user ID
     * @param bankAccountID the bank account ID
     * @param newSavingDTO  the new saving data transfer object
     * @return the added saving
     * @throws IllegalArgumentException if saving validation fails
     * @throws NoSuchElementException   if bank account is not found
     * @throws AlreadyExistsException   if an active saving already exists
     */
    public Saving addSavingToBankAccount(int userID, int bankAccountID, NewSavingDTO newSavingDTO) throws IllegalArgumentException, NoSuchElementException, AlreadyExistsException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        Saving saving = Mapper.newSavingDTOToSaving(newSavingDTO, bankAccount);
        HelperFunctions.validateSaving(saving);

        if (hasActiveSaving(bankAccount)) {
            throw new AlreadyExistsException("Saving already exists for bankAccount with id " + bankAccountID);
        }

        bankAccount.getSavings().add(saving);
        return bankAccountService.createOrUpdateBankAccount(bankAccount).getSavings().getLast();
    }

    /**
     * Removes the active saving from a bank account.
     *
     * @param userID        the user ID
     * @param bankAccountID the bank account ID
     * @return true if an active saving was removed, false otherwise
     * @throws NoSuchElementException if no active saving is found
     */
    public boolean removeActiveSaving(int userID, int bankAccountID) throws NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        if (hasActiveSaving(bankAccount)) {
            Saving saving = findActiveSaving(userID, bankAccountID);
            saving.setActive(false);
            createOrUpdateSaving(saving);
            return true;
        }
        return false;
    }

    /**
     * Transfers an amount from savings to balance.
     *
     * @param userID        the user ID
     * @param bankAccountID the bank account ID
     * @param amountInt     the amount to transfer
     * @return the updated bank account
     * @throws NoSuchElementException   if bank account is not found
     * @throws IllegalArgumentException if there are not enough savings
     */
    public BankAccount transferFromSavingToBalance(int userID, int bankAccountID, int amountInt) throws NoSuchElementException, IllegalArgumentException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        BigDecimal amount = BigDecimal.valueOf(amountInt);

        if (!hasEnoughSavings(bankAccount, amount)) {
            throw new IllegalArgumentException("Not enough savings for bankAccount with id " + bankAccountID);
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Credit from savings");
        transaction.setDate(LocalDateTime.now());
        transaction.setTransactionType(TransactionType.SAVING);
        transaction.setBankAccount(bankAccount);

        bankAccount.setBalance(bankAccount.getBalance().add(amount));
        bankAccount.setSavingsAmount(bankAccount.getSavingsAmount().subtract(amount));

        return bankAccountService.createOrUpdateBankAccount(bankAccount);
    }

    /**
     * Processes a saving by creating a transaction and updating the saving and bank account.
     *
     * @param saving the saving to process
     */
    public void processSaving(Saving saving) {
        BigDecimal savingAmount = saving.getAmount();
        BankAccount bankAccount = saving.getBankAccount();

        Transaction transaction = new Transaction();
        transaction.setAmount(savingAmount.negate());
        transaction.setDescription(saving.getName());
        transaction.setDate(saving.getPayDay().atStartOfDay());
        transaction.setTransactionType(TransactionType.SAVING);
        transaction.setBankAccount(bankAccount);

        if (bankAccountService.canMakeTransaction(bankAccount, transaction)) {
            bankAccount.setBalance(bankAccount.getBalance().subtract(savingAmount));
            bankAccount.setSavingsAmount(bankAccount.getSavingsAmount().add(savingAmount));
            bankAccount.getTransactions().add(transaction);

            if (willNotExpire(saving)) {
                saving.setPayDay(saving.getPayDay().plusMonths(1));
            } else {
                saving.setActive(false);
            }

            createOrUpdateSaving(saving);
            bankAccountService.createOrUpdateBankAccount(bankAccount);
        }
    }

    /**
     * Checks if a saving will not expire in the next month.
     *
     * @param saving the saving to check
     * @return true if the saving will not expire, false otherwise
     */
    private boolean willNotExpire(Saving saving) {
        return saving.getPayDay().plusMonths(1).isBefore(saving.getStartDate().plusMonths(saving.getDurationInMonths()));
    }

    /**
     * Checks if a bank account has an active saving.
     *
     * @param bankAccount the bank account to check
     * @return true if the bank account has an active saving, false otherwise
     */
    private boolean hasActiveSaving(BankAccount bankAccount) {
        return bankAccount.getSavings()
                .stream()
                .anyMatch(Saving::isActive);
    }

    /**
     * Checks if a bank account has enough savings for a specified amount.
     *
     * @param bankAccount the bank account to check
     * @param amount      the amount to check against
     * @return true if the bank account has enough savings, false otherwise
     */
    private boolean hasEnoughSavings(BankAccount bankAccount, BigDecimal amount) {
        return bankAccount.getSavingsAmount().compareTo(amount) >= 0;
    }
}
