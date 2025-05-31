package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.exceptions.AlreadyExistsException;
import de.fherfurt.clevercash.api.mapping.Mapper;
import de.fherfurt.clevercash.api.models.input.NewInstallmentDTO;
import de.fherfurt.clevercash.api.util.HelperFunctions;
import de.fherfurt.clevercash.storage.models.*;
import de.fherfurt.clevercash.storage.repositories.InstallmentRepository;
import de.fherfurt.clevercash.storage.util.TransactionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Service class for managing Installments.
 * Provides methods to create, update, find, add, remove, and process installments for a bank account.
 *
 * @author Jakob Roch, Richard Prax
 */
@AllArgsConstructor
@Service
public class InstallmentService {
    private final InstallmentRepository installmentRepository;
    private final BankAccountService bankAccountService;

    /**
     * Creates or updates an Installment.
     *
     * @param installment the installment to be created or updated
     * @return the saved installment
     */
    public Installment createOrUpdateInstallment(Installment installment) {
        return installmentRepository.save(installment);
    }

    /**
     * Finds all installments for a user with optional filters.
     *
     * @param userID          the user ID
     * @param bankAccountID   the bank account ID
     * @param startDateString optional start date filter (format: yyyy-MM-dd)
     * @param endDateString   optional end date filter (format: yyyy-MM-dd)
     * @param description     optional description filter
     * @return a list of installments matching the filters
     * @throws DateTimeParseException if date parsing fails
     * @throws NoSuchElementException if the bank account is not found
     */
    public List<Installment> findAllInstallmentsForUserWithFilters(int userID, int bankAccountID, String startDateString, String endDateString, String description) throws DateTimeParseException, NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        List<Installment> Installment = bankAccount.getInstallments();

        LocalDate startDate = (startDateString == null || startDateString.isEmpty()) ? null : LocalDate.parse(startDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate endDate = (endDateString == null || endDateString.isEmpty()) ? null : LocalDate.parse(endDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        return Installment.stream()
                .filter(installment -> (startDate == null || !installment.getStartDate().isBefore(startDate)) &&
                        (endDate == null || !installment.getStartDate().isAfter(endDate)) &&
                        (description == null || installment.getDescription().contains(description)))
                .collect(Collectors.toList());
    }

    /**
     * Finds an installment by its ID.
     *
     * @param userID        the user ID
     * @param bankAccountID the bank account ID
     * @param installmentID the installment ID
     * @return the found installment
     * @throws NoSuchElementException if the installment is not found
     */
    public Installment findInstallmentByID(int userID, int bankAccountID, int installmentID) throws NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        return bankAccount.getInstallments().stream()
                .filter(installment -> installment.getId() == installmentID)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Installment not found for bankAccount with id " + bankAccountID));
    }

    /**
     * Adds a new installment to a bank account.
     *
     * @param userID            the user ID
     * @param bankAccountID     the bank account ID
     * @param newInstallmentDTO the new installment data transfer object
     * @return the added installment
     * @throws IllegalArgumentException if the installment data is invalid
     * @throws NoSuchElementException   if the bank account is not found
     * @throws AlreadyExistsException   if an active installment with the same name already exists
     */
    public Installment addInstallmentToBankAccount(int userID, int bankAccountID, NewInstallmentDTO newInstallmentDTO) throws IllegalArgumentException, NoSuchElementException, AlreadyExistsException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        Installment installment = Mapper.newInstallmentDTOToInstallment(newInstallmentDTO, bankAccount);
        HelperFunctions.validateInstallment(installment);

        if (hasAlreadyAnActiveInstallmentWithThisName(bankAccount, installment)) {
            throw new AlreadyExistsException("Installment with provided name already exists for bankAccount with id " + bankAccountID);
        }

        bankAccount.getInstallments().add(installment);
        return bankAccountService.createOrUpdateBankAccount(bankAccount).getInstallments().getLast();
    }

    /**
     * Checks if there is already an active installment with the same name in the bank account.
     *
     * @param bankAccount the bank account
     * @param installment the installment to check
     * @return true if an active installment with the same name exists, false otherwise
     */
    private boolean hasAlreadyAnActiveInstallmentWithThisName(BankAccount bankAccount, Installment installment) {
        return bankAccount.getInstallments()
                .stream()
                .anyMatch(tempInstallment -> tempInstallment.isActive() && tempInstallment.getName().equals(installment.getName()));
    }

    /**
     * Removes an installment from a bank account.
     *
     * @param userID        the user ID
     * @param bankAccountID the bank account ID
     * @param installmentID the installment ID
     * @throws NoSuchElementException if the installment is not found
     */
    public void removeInstallmentFromBankAccount(int userID, int bankAccountID, int installmentID) throws NoSuchElementException {
        BankAccount bankAccount = bankAccountService.findBankAccountByID(userID, bankAccountID);
        Installment installmentToRemove = findInstallmentByID(userID, bankAccountID, installmentID);
        if (!installmentToRemove.getAlreadyPaidAmount().toBigInteger().equals(BigInteger.ZERO)) {
            installmentToRemove.setActive(false);
        } else {
            bankAccount.getInstallments().remove(installmentToRemove);
        }
        bankAccountService.createOrUpdateBankAccount(bankAccount);
    }

    /**
     * Processes an installment, performing the necessary transactions.
     *
     * @param installment the installment to be processed
     */
    public void processInstallment(Installment installment) {
        BigDecimal installmentAmountPerRate = installment.getAmountPerRate();
        BankAccount bankAccount = installment.getBankAccount();

        Transaction transaction = new Transaction();
        transaction.setAmount(installmentAmountPerRate.negate());
        transaction.setDescription(installment.getName());
        transaction.setDate(installment.getPayDay().atStartOfDay());
        transaction.setTransactionType(TransactionType.INSTALLMENT);
        transaction.setBankAccount(bankAccount);

        if (bankAccountService.canMakeTransaction(bankAccount, transaction)) {
            bankAccount.setBalance(bankAccount.getBalance().subtract(installmentAmountPerRate));
            installment.setAlreadyPaidAmount(installment.getAlreadyPaidAmount().add(installmentAmountPerRate));
            bankAccount.getTransactions().add(transaction);

            if (isFinished(installment)) {
                installment.setActive(false);
            } else {
                installment.setPayDay(installment.getPayDay().plusMonths(1));
            }

            createOrUpdateInstallment(installment);
            bankAccountService.createOrUpdateBankAccount(bankAccount);
        }
    }

    /**
     * Checks if an installment is finished.
     *
     * @param installment the installment to check
     * @return true if the installment is finished, false otherwise
     */
    private boolean isFinished(Installment installment) {
        return installment.getAlreadyPaidAmount().compareTo(installment.getAmount()) >= 0;
    }
}
