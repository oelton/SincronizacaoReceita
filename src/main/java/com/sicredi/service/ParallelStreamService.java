package com.sicredi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ParallelStreamService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelStreamService.class);
    private static final String PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR = "Problema na importacao para o registro -> Agencia: {}, Conta: {}, saldo: {}, status: {}, error: {}";

    public void init(List<List<String>> linesStream) {
        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());

        linesStream.parallelStream().skip(1).forEach(line -> {
            try {
                line.add(importCVSAsync(line.get(0), line.get(1), format.parse(line.get(2)).doubleValue(), line.get(3)).toString());
            } catch (ParseException e) {
                line.add(Boolean.FALSE.toString());
                LOGGER.error(PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR,
                        line.get(0), line.get(1), line.get(2), line.get(3), e.getLocalizedMessage());
            }
        });

        writeCVSStream(linesStream);
    }

    private void writeCVSStream(List<List<String>> strings) {
        File csvFile = new File("resultStream.csv");
        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(csvFile));) {
            strings.get(0).add("processado");
            for (List<String> item : strings) {
                csvWriter.println(item.stream().map(String::trim).collect(Collectors.joining(";")));
            }
        } catch (IOException e) {
            LOGGER.error("Erro na escrita do arquivo. Erro: {}", e.getLocalizedMessage());
        }
    }

    public Boolean importCVSAsync(String agencia, String conta, double saldo, String status) {
        try {
            return new ReceitaService().atualizarConta(agencia, conta, saldo, status);
        } catch (RuntimeException e) {
            LOGGER.error(PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR,
                    agencia, conta, saldo, status, e.getLocalizedMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOGGER.info("return error");
        return Boolean.FALSE;
    }
}
