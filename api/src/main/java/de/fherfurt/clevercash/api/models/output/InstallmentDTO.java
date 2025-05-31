package de.fherfurt.clevercash.api.models.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InstallmentDTO {
    private int id;
    private String name;
    private BigDecimal amount;
    private BigDecimal alreadyPaidAmount;
    private BigDecimal amountPerRate;
    private LocalDate startDate;
    private int durationInMonths;
    private LocalDate payDay;
    private String description;
    private int bankAccountID;
    private boolean active;
}

