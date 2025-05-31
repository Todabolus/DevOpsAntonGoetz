package de.fherfurt.clevercash.api.models.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NewBankAccountDTO {
    private String name;
    private BigDecimal balance;
    private BigDecimal dailyLimit;
}

