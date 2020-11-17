package com.sicredi;

import com.sicredi.service.CompletableFutureService;
import com.sicredi.service.ParallelStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class SincronizacaoReceitaApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SincronizacaoReceitaApplication.class);
    private static List<List<String>> linesCompletableFuture = new ArrayList<>();
    private static List<List<String>> linesStream = new ArrayList<>();

    @Autowired
    private ApplicationContext appContext;
    @Autowired
    private CompletableFutureService completableFutureService;
    @Autowired
    private ParallelStreamService parallelStreamService;

    public static void main(String[] args) {
        ApplicationContext context
                = new AnnotationConfigApplicationContext(SincronizacaoReceitaApplication.class);
        for (String arg : args) {
            LOGGER.info("argumento: {}", arg);
        }
        SincronizacaoReceitaApplication p = context.getBean(SincronizacaoReceitaApplication.class);
        p.start(args);
    }

    private void start(String[] args) {
        ClassLoader classLoader = SincronizacaoReceitaApplication.class.getClassLoader();
        String fileName = classLoader.getResource("receita.csv").getFile();

        if (args == null) {
            LOGGER.info("Caminho do arquivo nao informado");
            initiateShutdown(0);
        } else if (args.length > 0 && args[0] != null) {
            fileName = args[0];
        }

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
            parallelStreamService.init(linesStream);

            //Exemplo usando completable future
            completableFutureService.init(linesCompletableFuture);
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

    private void initiateShutdown(int returnCode) {
        LOGGER.info("Shutting Down Servico");
        int exitCode = SpringApplication.exit(appContext, () -> returnCode);
        System.exit(exitCode);
    }
}
