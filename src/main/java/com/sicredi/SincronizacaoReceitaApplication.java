package com.sicredi;

import com.sicredi.service.ReceitaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SpringBootApplication
public class SincronizacaoReceitaApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SincronizacaoReceitaApplication.class);

    private static List<List<String>> lines = new ArrayList<>();

    public static void main(String[] args) {
        ClassLoader classLoader = SincronizacaoReceitaApplication.class.getClassLoader();

        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());

        SpringApplication.run(SincronizacaoReceitaApplication.class, args);
        try (Scanner scanner = new Scanner(new File(classLoader.getResource("receita.csv").getFile()));) {
            while (scanner.hasNextLine()) {
                lines.add(readCVS(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        lines.stream().skip(1).forEach(line -> {
            try {
                CompletableFuture<Boolean> result = importCVS(line.get(0), line.get(1), format.parse(line.get(2)).doubleValue(), line.get(3));
                CompletableFuture.allOf(result).thenAcceptAsync(unused -> {
                    try {
                        line.add(result.get().toString());
                    } catch (InterruptedException|ExecutionException e) {
                        e.printStackTrace();
                    }
                });
//                line.add(importCVS(line.get(0), line.get(1), format.parse(line.get(2)).doubleValue(), line.get(3)));
//                LOGGER.info("lines: " + line.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });

        lines.size();
        lines.forEach(strings -> LOGGER.info(strings.toString()));
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

    @Async
    public static CompletableFuture<Boolean> importCVS(String agencia, String conta, double saldo, String status){
        try {
//            LOGGER.info("return true");
            return CompletableFuture.completedFuture(new ReceitaService().atualizarConta(agencia, conta, saldo, status));
        } catch (InterruptedException|RuntimeException e) {
            LOGGER.error("Problema na importacao para o registro -> Agencia: {}, Conta: {}, saldo: {}, status: {}, error: {}",
                    agencia, conta,  saldo, status, e.getLocalizedMessage());
        }
//        LOGGER.info("return error");
        return CompletableFuture.completedFuture(Boolean.FALSE);
    }
}
