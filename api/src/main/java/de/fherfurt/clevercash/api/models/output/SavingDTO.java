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
public class SavingDTO {
    private int id;
    private String name;
    private String description;
    private BigDecimal amount;
    private LocalDate startDate;
    private int durationInMonths;
    private LocalDate payDay;
    private int bankAccountID;
    private boolean active;
}


