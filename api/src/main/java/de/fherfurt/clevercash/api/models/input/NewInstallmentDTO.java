package de.fherfurt.clevercash.api.models.input;

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
public class NewInstallmentDTO {
    private String name;
    private BigDecimal amount;
    private BigDecimal amountPerRate;
    private LocalDate startDate;
    private int durationInMonths;
    private String description;
}

