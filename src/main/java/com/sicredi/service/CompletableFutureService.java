package com.sicredi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class CompletableFutureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompletableFutureService.class);

    private List<List<String>> linesCompletableFuture = new ArrayList<>();
    public static final String PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR = "Problema na importacao para o registro -> Agencia: {}, Conta: {}, saldo: {}, status: {}, error: {}";

    @Autowired
    private ImportFile importFile;
    @Autowired
    private ApplicationContext appContext;

    public void init(List<List<String>> lines) {
        linesCompletableFuture = lines;

        List<CompletableFuture<List<String>>> futures = new ArrayList<>();

        linesCompletableFuture.stream().skip(1).forEach(line -> {
            try {
                futures.add(importFile.importCVS(line));
            } catch (ParseException e) {
                line.add(Boolean.FALSE.toString());
                LOGGER.error(PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR,
                        line.get(0), line.get(1), line.get(2), line.get(3), e.getLocalizedMessage());
            }
        });

        generateFile(futures);

    }

    //TODO  utilizar esse metodo para controlar o tempo de execucao das threads,
    // caso seja excedido definir o false para o resultado da analise.
    private static void interruptExecution(AtomicBoolean cancelled, List<String> line, CompletableFuture<?> result) {
        // Interromper a execucao se chegar no tempo limite
        try {
            result.get(10, TimeUnit.MINUTES);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            cancelled.set(true);
            line.add(Boolean.FALSE.toString());
            Thread.currentThread().interrupt();
        }
    }

    private void generateFile(List<CompletableFuture<List<String>>> futures) {
        try {
            List<List<String>> retorno = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenApply(unused ->
                    futures.stream().map(CompletableFuture::join).collect(Collectors.toList())
            ).thenApply(lists ->
                    lists
            ).get();

            writeCVSCompletableFuture(retorno);
        } catch (InterruptedException e) {
            LOGGER.error("Erro na atualizacao do arquivio. Erro: {}", e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error("Erro na execucao. Erro: {}", e.getLocalizedMessage());
        }
    }

    private void writeCVSCompletableFuture(List<List<String>> strings) {
        File csvFile = new File("resultCompletableFuture.csv");
        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(csvFile));) {
            linesCompletableFuture.get(0).add("processado");
            csvWriter.println(linesCompletableFuture.get(0).stream().map(String::trim).collect(Collectors.joining(";")));
            for (List<String> item : strings) {
                csvWriter.println(item.stream().map(String::trim).collect(Collectors.joining(";")));
            }
        } catch (IOException e) {
            LOGGER.error("Erro na escrita do arquivio. Erro: {}", e.getLocalizedMessage());
        } finally {
            initiateShutdown(0);
        }
    }

    private void initiateShutdown(int returnCode) {
        LOGGER.info("Shutting Down Servico");
        int exitCode = SpringApplication.exit(appContext, () -> returnCode);
        System.exit(exitCode);
    }
}
