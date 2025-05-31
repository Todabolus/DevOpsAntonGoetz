package de.fherfurt.clevercash.storage.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "savings")
public class Saving {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private LocalDate startDate;

    @Column(nullable = false)
    private int durationInMonths;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private LocalDate payDay;

    @ManyToOne
    @JoinColumn(name = "bankAccountID", nullable = false)
    private BankAccount bankAccount;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Saving saving)) return false;
        return id == saving.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
