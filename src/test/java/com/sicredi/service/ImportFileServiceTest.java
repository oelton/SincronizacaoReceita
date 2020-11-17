package com.sicredi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ImportFileServiceTest {

    @InjectMocks
    ImportFile importFile;

    @Mock
    ReceitaService receitaService;

    @BeforeEach
    public void creatMocck() throws ParseException, InterruptedException {
        List<String> row = Arrays.asList(new String[]{"0101", "12225-6", "100,00", "A"});
        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
        String agencia = row.get(0);
        String conta = row.get(1);
        double saldo = format.parse(row.get(2)).doubleValue();
        String status = row.get(3);
        Mockito.when(receitaService.atualizarConta(agencia, conta, saldo, status)).thenReturn(true);
    }

    @DisplayName("Test importCVS")
    @Test
    void testImportCVS() throws ParseException {
        List<String> line = new ArrayList<>(Arrays.asList(new String[]{"0101", "12225-6", "100,00", "A"}));
        assertNotNull(importFile.importCVS(line));
    }

    @DisplayName("Test importCVS addvalue")
    @Test
    void testImportCVSAddResult() throws ParseException {
        List<String> line = new ArrayList<>(Arrays.asList(new String[]{"0101", "12225-6", "100,00", "A"}));
        importFile.importCVS(line);
        assertEquals(line.size(), 5);
    }

    @DisplayName("Test importCVS result true")
    @Test
    void testImportCVSResultTrue() throws ParseException {
        List<String> line = new ArrayList<>(Arrays.asList(new String[]{"0101", "12225-6", "100,00", "A"}));
        importFile.importCVS(line);
        assertTrue(Boolean.valueOf(line.get(4)));
    }

    @DisplayName("Test importCVS result false")
    @Test
    void testImportCVSResultFalse() throws ParseException {
        List<String> line = new ArrayList<>(Arrays.asList(new String[]{"0101", "122250-6", "100,00", "A"}));
        importFile.importCVS(line);
        assertFalse(Boolean.valueOf(line.get(4)));
    }
}
