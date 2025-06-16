package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewSavingDTO;
import de.fherfurt.clevercash.api.util.HelperFunctions;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Saving;
import de.fherfurt.clevercash.storage.models.Transaction;
import de.fherfurt.clevercash.storage.repositories.SavingRepository;
import de.fherfurt.clevercash.storage.util.TransactionType;
import jakarta.transaction.Transactional;
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

@AllArgsConstructor
@Service
public class SavingService {

    private final SavingRepository   savingRepository;
    private final BankAccountService bankAccountService;

    /* ------------------------------------------------------------------
     *  Create / Update
     * ------------------------------------------------------------------ */
    public Saving createOrUpdateSaving(Saving saving) {
        return savingRepository.save(saving);
    }

    /* ------------------------------------------------------------------
     *  Read
     * ------------------------------------------------------------------ */
    public List<Saving> findAllSavingForUserWithFilters(int userID,
                                                        int bankAccountID,
                                                        String startDateString,
                                                        String endDateString,
                                                        String description)
            throws DateTimeParseException {

        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        List<Saving> savings    = bankAccount.getSavings();

        LocalDate startDate = (startDateString == null || startDateString.isEmpty())
                ? null
                : LocalDate.parse(startDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        LocalDate endDate = (endDateString == null || endDateString.isEmpty())
                ? null
                : LocalDate.parse(endDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return savings.stream()
                .filter(s -> (startDate == null || !s.getStartDate().isBefore(startDate)) &&
                             (endDate == null || !s.getStartDate().isAfter(endDate)) &&
                             (description == null || s.getDescription().contains(description)))
                .collect(Collectors.toList());
    }

    public Saving findSavingByID(int userID, int bankAccountID, int savingID) {
        BankAccount account = bankAccountService.findBankAccountByID(userID, bankAccountID);
        return account.getSavings().stream()
                .filter(s -> s.getId() == savingID)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Saving not found for bankAccount with id " + bankAccountID));
    }

    public Saving findActiveSaving(int userID, int bankAccountID) {
        BankAccount account = bankAccountService.findBankAccountByID(userID, bankAccountID);
        return account.getSavings().stream()
                .filter(Saving::isActive)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No active saving found for bankAccount with id " + bankAccountID));
    }

    /* ------------------------------------------------------------------
     *  Delete / Deactivate
     * ------------------------------------------------------------------ */
    public void removeSavingFromBankAccount(int userID, int bankAccountID, int savingID) {
        BankAccount account = bankAccountService.findBankAccountByID(userID, bankAccountID);
        Saving savingToRemove = findSavingByID(userID, bankAccountID, savingID);

        if (savingToRemove.getPayDay().isAfter(savingToRemove.getStartDate())) {
            savingToRemove.setActive(false);
        } else {
            account.getSavings().remove(savingToRemove);
            account.setSavingsAmount(
                    account.getSavingsAmount().subtract(savingToRemove.getAmount()));
        }
        bankAccountService.createOrUpdateBankAccount(account);
    }

    public boolean removeActiveSaving(int userID, int bankAccountID) {
        BankAccount account = bankAccountService.findBankAccountByID(userID, bankAccountID);
        if (hasActiveSaving(account)) {
            Saving saving = findActiveSaving(userID, bankAccountID);
            saving.setActive(false);
            createOrUpdateSaving(saving);
            return true;
        }
        return false;
    }

    /* ------------------------------------------------------------------
     *  Business action – ADD SAVING
     * ------------------------------------------------------------------ */
    @Transactional
    public Saving addSavingToBankAccount(int userID,
                                         int bankAccountID,
                                         NewSavingDTO newSavingDTO)
            throws AlreadyExistsException {

        BankAccount account = bankAccountService.findBankAccountByID(userID, bankAccountID);

        if (hasActiveSaving(account)) {
            throw new AlreadyExistsException(
                    "Saving already exists for bankAccount with id " + bankAccountID);
        }

        Saving saving = Mapper.newSavingDTOToSaving(newSavingDTO, account);

        /* fallback-Korrektur für payDay – erst *nach* fehlgeschlagener Validierung */
        if (!tryValidate(saving)) {
            LocalDate minPayDay = saving.getStartDate().plusMonths(1);
            saving.setPayDay(minPayDay);
            HelperFunctions.validateSaving(saving);           // zweite (und letzte) Validierung
        }

        saving.setActive(true);
        account.getSavings().add(saving);
        return bankAccountService.createOrUpdateBankAccount(account)
                                 .getSavings()
                                 .getLast();
    }

    /* ------------------------------------------------------------------
     *  weitere Business-Methoden
     * ------------------------------------------------------------------ */
    public BankAccount transferFromSavingToBalance(int userID,
                                                   int bankAccountID,
                                                   int amountInt) {

        BankAccount account = bankAccountService.findBankAccountByID(userID, bankAccountID);
        BigDecimal amount   = BigDecimal.valueOf(amountInt);

        if (!hasEnoughSavings(account, amount)) {
            throw new IllegalArgumentException("Not enough savings for bankAccount with id " + bankAccountID);
        }

        Transaction tx = new Transaction();
        tx.setAmount(amount);
        tx.setDescription("Credit from savings");
        tx.setDate(LocalDateTime.now());
        tx.setTransactionType(TransactionType.SAVING);
        tx.setBankAccount(account);

        account.setBalance(account.getBalance().add(amount));
        account.setSavingsAmount(account.getSavingsAmount().subtract(amount));

        return bankAccountService.createOrUpdateBankAccount(account);
    }

    public void processSaving(Saving saving) {
        BigDecimal   amount  = saving.getAmount();
        BankAccount  account = saving.getBankAccount();

        Transaction tx = new Transaction();
        tx.setAmount(amount.negate());
        tx.setDescription(saving.getName());
        tx.setDate(saving.getPayDay().atStartOfDay());
        tx.setTransactionType(TransactionType.SAVING);
        tx.setBankAccount(account);

        if (bankAccountService.canMakeTransaction(account, tx)) {
            account.setBalance(account.getBalance().subtract(amount));
            account.setSavingsAmount(account.getSavingsAmount().add(amount));
            account.getTransactions().add(tx);

            if (willNotExpire(saving)) {
                saving.setPayDay(saving.getPayDay().plusMonths(1));
            } else {
                saving.setActive(false);
            }

            createOrUpdateSaving(saving);
            bankAccountService.createOrUpdateBankAccount(account);
        }
    }

    /* ------------------------------------------------------------------
     *  Helper
     * ------------------------------------------------------------------ */
    private boolean tryValidate(Saving saving) {
        try {
            HelperFunctions.validateSaving(saving);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean hasActiveSaving(BankAccount account) {
        return account.getSavings().stream().anyMatch(Saving::isActive);
    }

    private boolean hasEnoughSavings(BankAccount account, BigDecimal amount) {
        return account.getSavingsAmount().compareTo(amount) >= 0;
    }

    private boolean willNotExpire(Saving saving) {
        return saving.getPayDay().plusMonths(1)
                .isBefore(saving.getStartDate().plusMonths(saving.getDurationInMonths()));
    }
}
