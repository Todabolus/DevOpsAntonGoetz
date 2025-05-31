package de.fherfurt.clevercash.storage.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "bankAccounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "userID", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal dailyLimit;

    @Cascade(CascadeType.ALL)
    @OneToMany(mappedBy = "bankAccount", orphanRemoval = true)
    private List<Saving> savings = new ArrayList<>();

    @Cascade(CascadeType.ALL)
    @OneToMany(mappedBy = "bankAccount", orphanRemoval = true)
    private List<Installment> installments = new ArrayList<>();

    @Cascade(CascadeType.ALL)
    @OneToMany(mappedBy = "bankAccount")
    private List<Transaction> transactions = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal savingsAmount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankAccount that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
