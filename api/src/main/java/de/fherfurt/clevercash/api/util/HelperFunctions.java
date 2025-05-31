package de.fherfurt.clevercash.api.util;

import de.fherfurt.clevercash.api.models.input.NewAddressDTO;
import de.fherfurt.clevercash.api.models.input.NewBankAccountDTO;
import de.fherfurt.clevercash.api.models.input.NewTransactionDTO;
import de.fherfurt.clevercash.storage.models.Installment;
import de.fherfurt.clevercash.storage.models.Saving;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HelperFunctions {
    /**
     * Checks if an email address is valid based on a given regular expression.
     *
     * @param eMail The email address to validate.
     * @see <a href="https://www.akto.io/tools/email-regex-Javascript-tester"></a>
     */
    public static void validateEmail(String eMail) throws IllegalArgumentException {
        String validEMailRegex = "^[A-Za-z0-9\\._%+\\-]+@[A-Za-z0-9\\.\\-]+\\.[A-Za-z]{2,}$";
        if (eMail == null || !eMail.matches(validEMailRegex)) {
            throw new IllegalArgumentException("Invalid email address");
        }
    }

    /**
     * Checks if a password follows the defined rules.
     *
     * @param password The password to validate.
     * @see <a href="https://www.akto.io/tools/password-regex-Javascript-tester"></a>
     */
    public static void validatePassword(String password) throws IllegalArgumentException {
        String validPasswordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!password.matches(validPasswordRegex)) {
            throw new IllegalArgumentException("Invalid password");
        }
    }

    /**
     * Checks if a saving is valid based on the provided criteria.
     *
     * @param saving the saving to be validated
     */
    public static void validateSaving(Saving saving) throws IllegalArgumentException {
        boolean isValidName = !saving.getName().isEmpty() && !saving.getName().isBlank();
        boolean isValidAmount = saving.getAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean isValidStartDate = !saving.getStartDate().isBefore(LocalDate.now());
        boolean isValidDuration = HelperFunctions.isValidDuration(saving.getDurationInMonths());

        if (!(isValidName && isValidAmount && isValidStartDate && isValidDuration)) {
            throw new IllegalArgumentException("Invalid saving");
        }
    }

    /**
     * Checks if a duration is valid (greater than 0).
     *
     * @param durationInMonths The duration to validate.
     * @return true if the duration is valid (greater than 0), otherwise false.
     */
    private static boolean isValidDuration(int durationInMonths) {
        return durationInMonths > 0;
    }

    /**
     * Checks if an installment is valid based on the provided criteria.
     *
     * @param installment the installment to be validated
     */
    public static void validateInstallment(Installment installment) throws IllegalArgumentException {
        boolean isValidName = !installment.getName().isEmpty() && !installment.getName().isBlank();
        boolean isValidAmount = installment.getAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean isValidStartDate = !installment.getStartDate().isBefore(LocalDate.now());
        boolean isValidDuration = HelperFunctions.isValidDuration(installment.getDurationInMonths());

        if (!(isValidName && isValidAmount && isValidStartDate && isValidDuration)) {
            throw new IllegalArgumentException("Invalid saving");
        }
    }

    /**
     * Checks if an address is valid based on the provided criteria.
     *
     * @param newAddressDTO the address to be validated
     */
    public static void validateAddress(NewAddressDTO newAddressDTO) throws IllegalArgumentException {
        if (newAddressDTO == null ||
                newAddressDTO.getStreetNumber() == null || newAddressDTO.getStreetNumber().isEmpty() ||
                newAddressDTO.getStreet() == null || newAddressDTO.getStreet().isEmpty() ||
                newAddressDTO.getPostalCode() == null || newAddressDTO.getPostalCode().length() != 5 ||
                newAddressDTO.getCity() == null || newAddressDTO.getCity().isEmpty() ||
                newAddressDTO.getCountry() == null || newAddressDTO.getCountry().isEmpty() ||
                newAddressDTO.getState() == null || newAddressDTO.getState().isEmpty()) {
            throw new IllegalArgumentException("Invalid Address");
        }
    }

    /**
     * Checks if an bankAccount is valid based on the provided criteria.
     *
     * @param newBankAccountDTO the bankAccount to be validated
     */
    public static void validateBankAccount(NewBankAccountDTO newBankAccountDTO) throws IllegalArgumentException {
        if (newBankAccountDTO == null ||
                newBankAccountDTO.getName() == null || newBankAccountDTO.getName().isEmpty() ||
                newBankAccountDTO.getBalance() == null ||
                newBankAccountDTO.getDailyLimit() == null)
            throw new IllegalArgumentException("Invalid bank account params");
    }

    /**
     * Checks if a transaction is valid based on the provided criteria.
     *
     * @param newTransactionDTO the transaction to be validated
     */
    public static void validateTransaction(NewTransactionDTO newTransactionDTO) throws IllegalArgumentException {
        if (newTransactionDTO == null ||
                newTransactionDTO.getAmount() == null || newTransactionDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0 ||
                newTransactionDTO.getDescription() == null || newTransactionDTO.getDescription().isEmpty())
            throw new IllegalArgumentException("Invalid transaction params");
    }
}

