package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.storage.models.Saving;
import de.fherfurt.clevercash.storage.repositories.SavingRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for scheduling and processing savings.
 * The SavingScheduler processes active savings on their payday or if the payday is in the past.
 *
 * @author Jakob Roch
 */
@AllArgsConstructor
@Service
public class SavingScheduler {
    private final SavingRepository savingRepository;
    private final SavingService savingService;

    /**
     * Scheduled method that runs daily at midnight.
     * Processes all active savings where the payday is today or in the past.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void processSavings() {
        LocalDate today = LocalDate.now();
        // Get all active savings where payday is today or in the past
        List<Saving> savings = savingRepository.findByPayDayLessThanEqualAndActive(today, true);
        // Process each saving
        savings.forEach(savingService::processSaving);
    }
}
