package com.sicredi;

import com.sicredi.service.ImportFile;
import com.sicredi.service.ReceitaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@SpringBootApplication
public class SincronizacaoReceitaApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SincronizacaoReceitaApplication.class);
    public static final String PROBLEMA_NA_IMPORTACAO_PARA_O_REGISTRO_AGENCIA_CONTA_SALDO_STATUS_ERROR = "Problema na importacao para o registro -> Agencia: {}, Conta: {}, saldo: {}, status: {}, error: {}";

    private static List<List<String>> linesCompletableFuture = new ArrayList<>();
    private static List<List<String>> linesStream = new ArrayList<>();

    @Autowired
    private ImportFile importFile;
    @Autowired
    private ApplicationContext appContext;

    public static void main(String[] args) {
        ApplicationContext context
                = new AnnotationConfigApplicationContext(SincronizacaoReceitaApplication.class);
        for(String arg:args) {
            LOGGER.info("argumento: {}", arg);
        }
        SincronizacaoReceitaApplication p = context.getBean(SincronizacaoReceitaApplication.class);
        p.start(args);
    }

    private void start(String[] args){
        ClassLoader classLoader = SincronizacaoReceitaApplication.class.getClassLoader();
        String fileName = classLoader.getResource("receita.csv").getFile();

        if (args.length > 0 && args[0]!= null){
            fileName = args[0];
        }else {
            LOGGER.info("Caminho do arquivo nao informado");
            initiateShutdown(0);
        }

        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());

        try (Scanner scanner = new Scanner(new File(fileName))) {
            while (scanner.hasNextLine()) {
                List<String> newLine = readCVS(scanner.nextLine());
                linesCompletableFuture.add(new ArrayList<>(newLine));
                linesStream.add(new ArrayList<>(newLine));
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Arquivo nao encontrado. Erro: {}", e.getLocalizedMessage());
        }
        if (linesStream.isEmpty()) {
            LOGGER.info("Arquivo nao encontrado!");
            initiateShutdown(0);
        } else {
            //Exemplo usando parallel stream
            usingParallelStream(format);

            //Exemplo usando completable future
            usingCompletableFutureRefactor();
        }
    }

    private static List<String> readCVS(String line) {
        List<String> values = new ArrayList<>();
        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(";");
            while (rowScanner.hasNext()) {
                values.add(rowScanner.next());
            }
        }
        return values;
    }

    private void usingCompletableFutureRefactor() {

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

    private void generateFile(List<CompletableFuture<List<String>>> futures) {
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
        }finally {
            initiateShutdown(0);
        }
    }

    private void usingParallelStream(NumberFormat format) {
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

    public void initiateShutdown(int returnCode)
    {
        LOGGER.info("Shutting Down Servico");
        int exitCode = SpringApplication.exit(appContext, () -> returnCode);
        System.exit(exitCode);
    }
}
