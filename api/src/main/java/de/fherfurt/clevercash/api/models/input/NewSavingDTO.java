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
public class NewSavingDTO {
    private String name;
    private String description;
    private BigDecimal amount;
    private LocalDate startDate;
    private int durationInMonths;
}


