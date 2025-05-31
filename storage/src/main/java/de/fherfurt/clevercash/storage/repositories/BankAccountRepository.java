package de.fherfurt.clevercash.storage.repositories;

import de.fherfurt.clevercash.storage.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.fherfurt.clevercash.storage.models.BankAccount;

import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Integer> {

    List<BankAccount> findBankAccountByUser(User user);
}
