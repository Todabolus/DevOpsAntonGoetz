package de.fherfurt.clevercash.api.mapping;

import de.fherfurt.clevercash.api.exceptions.MappingException;
import de.fherfurt.clevercash.api.models.input.*;
import de.fherfurt.clevercash.api.models.output.*;
import de.fherfurt.clevercash.storage.models.*;
import de.fherfurt.clevercash.storage.util.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

public class Mapper {
    private static final DateTimeFormatter defaultDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static UserDTO userToUserDTO(User user) {
        UserDTO userDTO = new UserDTO(user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getBirthDate().format(defaultDateTimeFormatter),
                user.getEmail());

        if (user.getAddress() != null) {
            userDTO.setAddress(addressToString(user.getAddress()));
        }

        return userDTO;
    }

    public static String addressToString(Address address) {
        return address.getStreetNumber() +
                " " +
                address.getStreet() +
                ", " +
                address.getCity() +
                " " +
                address.getPostalCode() +
                " " +
                address.getCountry();
    }

    public static AddressDTO addressToAddressDTO(Address address) {
        return new AddressDTO(
                address.getId(),
                address.getStreetNumber(),
                address.getStreet(),
                address.getPostalCode(),
                address.getCity(),
                address.getCountry(),
                address.getState()
        );
    }

    public static Address addressDTOToAddress(NewAddressDTO newAddressDTO) {
        return new Address(
                newAddressDTO.getStreetNumber(),
                newAddressDTO.getStreet(),
                newAddressDTO.getPostalCode(),
                newAddressDTO.getCity(),
                newAddressDTO.getCountry(),
                newAddressDTO.getState()
        );
    }

    public static User newUserDTOToUser(NewUserDTO newUserDTO) throws MappingException {
        // Parse Birthday
        LocalDate parsedBirthday = null;
        try {
            parsedBirthday = LocalDate
                    .parse(newUserDTO.getBirthDate(), defaultDateTimeFormatter);
        } catch (DateTimeParseException dtpe) {
            throw new MappingException("Birthday could not be parsed", dtpe);
        }

        // Create and return User
        return new User(
                newUserDTO.getFirstName(),
                newUserDTO.getLastName(),
                newUserDTO.getEmail(),
                newUserDTO.getPassword(),
                parsedBirthday
        );
    }

    public static SavingDTO savingtoSavingDTO(Saving saving) {
        SavingDTO dto = new SavingDTO();
        dto.setId(saving.getId());
        dto.setName(saving.getName());
        dto.setDescription(saving.getDescription());
        dto.setAmount(saving.getAmount());
        dto.setStartDate(saving.getStartDate());
        dto.setDurationInMonths(saving.getDurationInMonths());
        dto.setPayDay(saving.getPayDay());
        dto.setBankAccountID(saving.getBankAccount().getId());
        dto.setActive(saving.isActive());
        return dto;
    }

    public static Saving newSavingDTOToSaving(NewSavingDTO newSavingDTO, BankAccount bankAccount) {
        Saving saving = new Saving();
        saving.setName(newSavingDTO.getName());
        saving.setDescription(newSavingDTO.getDescription());
        saving.setAmount(newSavingDTO.getAmount());
        saving.setStartDate(newSavingDTO.getStartDate());
        saving.setDurationInMonths(newSavingDTO.getDurationInMonths());
        // set default values
        saving.setPayDay(newSavingDTO.getStartDate());
        saving.setActive(true);
        saving.setBankAccount(bankAccount);
        return saving;
    }

    public static TransactionDTO transactionToTransactionDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setDate(transaction.getDate());
        dto.setTransactionType(transaction.getTransactionType().toString());
        dto.setBankAccountID(transaction.getBankAccount().getId());
        return dto;
    }

    public static Transaction newTransactionDTOToTransaction(NewTransactionDTO newTransactionDTO, BankAccount bankAccount) {
        Transaction transaction = new Transaction();
        transaction.setAmount(newTransactionDTO.getAmount());
        transaction.setDescription(newTransactionDTO.getDescription());
        transaction.setDate(LocalDateTime.now());
        transaction.setTransactionType(TransactionType.PAYMENT);
        transaction.setBankAccount(bankAccount);
        return transaction;
    }

    public static InstallmentDTO installtmentToInstallmentDTO(Installment installment) {
        InstallmentDTO dto = new InstallmentDTO();
        dto.setId(installment.getId());
        dto.setName(installment.getName());
        dto.setAmount(installment.getAmount());
        dto.setAlreadyPaidAmount(installment.getAlreadyPaidAmount());
        dto.setAmountPerRate(installment.getAmountPerRate());
        dto.setStartDate(installment.getStartDate());
        dto.setDurationInMonths(installment.getDurationInMonths());
        dto.setPayDay(installment.getPayDay());
        dto.setDescription(installment.getDescription());
        dto.setBankAccountID(installment.getBankAccount().getId());
        dto.setActive(installment.isActive());
        return dto;
    }

    public static Installment newInstallmentDTOToInstallment(NewInstallmentDTO newInstallmentDTO, BankAccount bankAccount) {
        Installment installment = new Installment();
        installment.setName(newInstallmentDTO.getName());
        installment.setAmount(newInstallmentDTO.getAmount());
        installment.setAmountPerRate(newInstallmentDTO.getAmountPerRate());
        installment.setDurationInMonths(newInstallmentDTO.getDurationInMonths());
        installment.setStartDate(newInstallmentDTO.getStartDate());
        installment.setDescription(newInstallmentDTO.getDescription());
        // set default values
        installment.setPayDay(newInstallmentDTO.getStartDate());
        installment.setAlreadyPaidAmount(BigDecimal.ZERO);
        installment.setActive(true);
        installment.setBankAccount(bankAccount);
        return installment;
    }

    public static BankAccountDTO bankAccountToBankAccountDTO(BankAccount bankAccount) {
        BankAccountDTO dto = new BankAccountDTO();
        dto.setId(bankAccount.getId());
        dto.setOwner(bankAccount.getUser().getId());
        dto.setName(bankAccount.getName());
        dto.setBalance(bankAccount.getBalance());
        dto.setDailyLimit(bankAccount.getDailyLimit());
        dto.setSavingsAmount(bankAccount.getSavingsAmount());

        dto.setSavings(bankAccount.getSavings().stream()
                .map(Mapper::savingtoSavingDTO)
                .collect(Collectors.toList()));
        dto.setInstallments(bankAccount.getInstallments().stream()
                .map(Mapper::installtmentToInstallmentDTO)
                .collect(Collectors.toList()));
        dto.setTransactions(bankAccount.getTransactions().stream()
                .map(Mapper::transactionToTransactionDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    public static BankAccount newBankAccountDTOToBankAccount(NewBankAccountDTO newBankAccountDTO, User user) {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setUser(user);
        bankAccount.setName(newBankAccountDTO.getName());
        bankAccount.setBalance(newBankAccountDTO.getBalance());
        bankAccount.setDailyLimit(newBankAccountDTO.getDailyLimit());
        bankAccount.setSavingsAmount(BigDecimal.ZERO);

        return bankAccount;
    }
}