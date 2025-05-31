package de.fherfurt.clevercash.storage.repositories;

import de.fherfurt.clevercash.storage.models.Saving;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SavingRepository extends JpaRepository<Saving, Integer> {
    List<Saving> findByPayDayLessThanEqualAndActive(LocalDate payDay, boolean active);
}

