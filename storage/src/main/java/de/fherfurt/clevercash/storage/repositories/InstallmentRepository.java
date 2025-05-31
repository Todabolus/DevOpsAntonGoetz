package de.fherfurt.clevercash.storage.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.fherfurt.clevercash.storage.models.Installment;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Integer> {
    List<Installment> findByPayDayLessThanEqualAndActive(LocalDate payDay, boolean active);
}

