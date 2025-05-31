package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.storage.models.Transaction;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for filtering transactions based on date range.
 *
 * @author Anton Götz, Lilou Steffen
 */
@AllArgsConstructor
@Service
public class FilterService {

    /**
     * Filters a list of transactions to include only those within a specified date range.
     *
     * @param transactions    the list of transactions to be filtered
     * @param startDateString the start date of the range in ISO-8601 format (yyyy-MM-dd)
     * @param endDateString   the end date of the range in ISO-8601 format (yyyy-MM-dd)
     * @return a list of transactions that fall within the specified date range
     * @throws DateTimeParseException if the startDateString or endDateString cannot be parsed
     */
    public List<Transaction> filterTransactionsByDate(List<Transaction> transactions, String startDateString, String endDateString) throws DateTimeParseException {
        LocalDate startDate = LocalDate.parse(startDateString);
        LocalDate endDate = LocalDate.parse(endDateString);

        return transactions.stream()
                .filter(transaction -> {
                    LocalDate transactionDate = transaction.getDate().toLocalDate();
                    return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }
}
