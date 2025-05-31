package de.fherfurt.clevercash.api.service;

import de.fherfurt.clevercash.storage.models.Installment;
import de.fherfurt.clevercash.storage.repositories.InstallmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class InstallmentSchedulerTest {
    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private InstallmentService installmentService;

    @InjectMocks
    private InstallmentScheduler installmentScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessInstallments() {
        // Arrange
        LocalDate today = LocalDate.now();
        Installment installment1 = mock(Installment.class);
        Installment installment2 = mock(Installment.class);
        List<Installment> installments = List.of(installment1, installment2);

        when(installmentRepository.findByPayDayLessThanEqualAndActive(today, true)).thenReturn(installments);

        // Act
        installmentScheduler.processInstallments();

        // Assert
        ArgumentCaptor<Installment> installmentArgumentCaptor = ArgumentCaptor.forClass(Installment.class);
        verify(installmentService, times(2)).processInstallment(installmentArgumentCaptor.capture());

        List<Installment> capturedInstallments = installmentArgumentCaptor.getAllValues();
        assertEquals(installments, capturedInstallments);
    }
}