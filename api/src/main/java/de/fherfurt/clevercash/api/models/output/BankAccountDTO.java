package de.fherfurt.clevercash.api.models.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankAccountDTO {
    private int id;
    private int owner;
    private String name;
    private BigDecimal balance;
    private BigDecimal dailyLimit;
    private BigDecimal savingsAmount;
    private List<SavingDTO> savings;
    private List<InstallmentDTO> installments;
    private List<TransactionDTO> transactions;
}
