package com.sicredi;

import com.sicredi.service.ImportFile;
import com.sicredi.service.ReceitaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@SpringBootApplication
public class SincronizacaoReceitaApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SincronizacaoReceitaApplication.class);
    public static final String PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR = "Problema na importacao para o registro -> Agencia: {}, Conta: {}, saldo: {}, status: {}, error: {}";

    private static List<List<String>> lines = new ArrayList<>();
    private static List<List<String>> newLines = new ArrayList<>();

    private static ImportFile importFile;

    @Autowired
    public SincronizacaoReceitaApplication(ImportFile file){
        importFile = file;
    }

    public static void main(String[] args) {
        ClassLoader classLoader = SincronizacaoReceitaApplication.class.getClassLoader();

        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());

        SpringApplication.run(SincronizacaoReceitaApplication.class, args);
        try (Scanner scanner = new Scanner(new File(classLoader.getResource("receita.csv").getFile()))) {
            while (scanner.hasNextLine()) {
                lines.add(readCVS(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Arquivo nao encontrado. Erro: {}", e.getLocalizedMessage());
        }
        usingParallelStream(format);

        usingCompletableFutureRefactor();

    }

    private static List<String> readCVS(String line){
        List<String> values = new ArrayList<>();
        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(";");
            while (rowScanner.hasNext()) {
                values.add(rowScanner.next());
            }
        }
        return values;
    }

    private static void usingCompletableFutureRefactor() {

        List<CompletableFuture<List<String>>> futures = new ArrayList<>();

        lines.stream().skip(1).forEach(line -> {
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

    private static void generateFile(List<CompletableFuture<List<String>>> futures) {
        try {
            List<List<String>> retorno = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenApply(unused -> {
                return futures.stream().map(listCompletableFuture -> listCompletableFuture.join()).collect(Collectors.toList());
            }).thenApply(lists -> {
                LOGGER.info(String.valueOf(lists));
                return lists;
            }).get();

            writeCVSCompletableFuture(retorno);
        } catch (InterruptedException e) {
            LOGGER.error("Erro na atualizacao do arquivio. Erro: {}", e.getLocalizedMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOGGER.error("Erro na execucao. Erro: {}", e.getLocalizedMessage());
        }
    }

    private static void writeCVSCompletableFuture(List<List<String>> strings) {
        File csvFile = new File("result.csv");
        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(csvFile));){
            lines.get(0).add("processado");
            csvWriter.println(lines.get(0).stream().map(s -> s.trim()).collect(Collectors.joining(";")));
            for(List<String> item : strings){
                csvWriter.println(item.stream().map(s -> s.trim()).collect(Collectors.joining(";")));
            }
        } catch (IOException e) {
            LOGGER.error("Erro na escrita do arquivio. Erro: {}", e.getLocalizedMessage());
        }
    }

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

    private static void usingParallelStream(NumberFormat format) {
        lines.parallelStream().skip(1).forEach(line -> {
            try {
                line.add(importCVSAsync(line.get(0), line.get(1), format.parse(line.get(2)).doubleValue(), line.get(3)).toString());
            } catch (ParseException e) {
                line.add(Boolean.FALSE.toString());
                LOGGER.error(PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR,
                        line.get(0), line.get(1), line.get(2), line.get(3), e.getLocalizedMessage());
            }
        });

        writeCVSStream(lines);
    }
    private static void writeCVSStream(List<List<String>> strings) {
        File csvFile = new File("resultStream.csv");
        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(csvFile));){
            strings.get(0).add("processado");
            for(List<String> item : strings){
                csvWriter.println(item.stream().map(s -> s.trim()).collect(Collectors.joining(";")));
            }
        } catch (IOException e) {
            LOGGER.error("Erro na escrita do arquivo. Erro: {}", e.getLocalizedMessage());
        }
    }

    public static Boolean importCVSAsync(String agencia, String conta, double saldo, String status){
        try {
            return new ReceitaService().atualizarConta(agencia, conta, saldo, status);
        } catch (RuntimeException e) {
            LOGGER.error(PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR,
                    agencia, conta,  saldo, status, e.getLocalizedMessage());
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        LOGGER.info("return error");
        return Boolean.FALSE;
    }

    private static void usingCompletableFuture(NumberFormat format) {
        AtomicBoolean cancelled = new AtomicBoolean(false);

        lines.stream().skip(1).forEach(line -> {
            try {
                CompletableFuture<Boolean> result = importFile.importCVS(line.get(0), line.get(1), format.parse(line.get(2)).doubleValue(), line.get(3));
                CompletableFuture.allOf(result).thenAcceptAsync(unused -> {
                    try {
                        line.add(result.get().toString());
                        newLines.addAll(lines);
                    } catch (InterruptedException | ExecutionException e){
                        Thread.currentThread().interrupt();
                        line.add(Boolean.FALSE.toString());
                    }
                });

                interruptExecution(cancelled, line, result);

            } catch (ParseException e) {
                line.add(Boolean.FALSE.toString());
                LOGGER.error(PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR,
                        line.get(0), line.get(1), line.get(2), line.get(3), e.getLocalizedMessage());
            }
        });
    }
}
