package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.api.TestUtils;
import de.fherfurt.clevercash.storage.models.BankAccount;
import de.fherfurt.clevercash.storage.models.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilterServiceTest {

    @InjectMocks
    private FilterService filterService;

    private List<Transaction> transactions;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); 
        transactions = TestUtils.getTestTransactionList(new BankAccount());
    }

    @Test
    public void testFilterTransactionsByDate() {
        // arrange & act
        List<Transaction> result = filterService.filterTransactionsByDate(transactions, LocalDate.now().toString(), LocalDate.now().plusDays(1).toString());

        // assert
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals(2, result.get(1).getId());
    }
}