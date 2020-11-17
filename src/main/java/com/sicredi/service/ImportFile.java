package com.sicredi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Service
public class ImportFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportFile.class);

    @Async
    public CompletableFuture<List<String>> importCVS(List<String> line) throws ParseException {
        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
        String agencia = line.get(0);
        String conta = line.get(1);
        double saldo = format.parse(line.get(2)).doubleValue();
        String status = line.get(3);
        Boolean retorno = false;
        try {
            retorno = new ReceitaService().atualizarConta(agencia, conta, saldo, status);
        } catch (RuntimeException e) {
            LOGGER.error("Problema na importacao para o registro -> Agencia: {}, Conta: {}, saldo: {}, status: {}, error: {}",
                    agencia, conta,  saldo, status, e.getLocalizedMessage());
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        line.add(retorno.toString());
        return CompletableFuture.completedFuture(line);
    }
}
