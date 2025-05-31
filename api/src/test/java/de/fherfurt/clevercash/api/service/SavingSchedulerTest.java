package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.storage.models.Saving;
import de.fherfurt.clevercash.storage.repositories.SavingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SavingSchedulerTest {

    @Mock
    private SavingRepository savingRepository;

    @Mock
    private SavingService savingService;

    @InjectMocks
    private SavingScheduler savingScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessSavings() {
        // Arrange
        LocalDate today = LocalDate.now();
        Saving saving1 = mock(Saving.class);
        Saving saving2 = mock(Saving.class);
        List<Saving> savings = List.of(saving1, saving2);

        when(savingRepository.findByPayDayLessThanEqualAndActive(today, true)).thenReturn(savings);

        // Act
        savingScheduler.processSavings();

        // Assert
        ArgumentCaptor<Saving> savingArgumentCaptor = ArgumentCaptor.forClass(Saving.class);
        verify(savingService, times(2)).processSaving(savingArgumentCaptor.capture());

        List<Saving> capturedSavings = savingArgumentCaptor.getAllValues();
        assertEquals(savings, capturedSavings);
    }
}