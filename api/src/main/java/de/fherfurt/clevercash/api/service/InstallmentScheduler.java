package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.storage.models.Installment;
import de.fherfurt.clevercash.storage.repositories.InstallmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for scheduling and processing installments.
 * This service checks for due installments daily and processes them accordingly.
 *
 * @author Jakob Roch, Richard Prax
 */
@AllArgsConstructor
@Service
public class InstallmentScheduler {
    private final InstallmentRepository installmentRepository;
    private final InstallmentService installmentService;

    /**
     * Scheduled method to process installments.
     * This method runs daily at midnight to find and process all active installments
     * that are due on the current day or were due in the past.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void processInstallments() {
        LocalDate today = LocalDate.now();
        // Get all active installments where payday is today or in the past
        List<Installment> installments = installmentRepository.findByPayDayLessThanEqualAndActive(today, true);
        // Process each installment
        installments.forEach(installmentService::processInstallment);
    }
}
