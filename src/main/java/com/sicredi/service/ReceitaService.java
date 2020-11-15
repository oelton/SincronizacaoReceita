package com.sicredi.service;

import com.sicredi.config.AsyncConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gabriel_stabel<gabriel_stabel@sicredi.com.br>
 */
public class ReceitaService {

    // Esta é a implementação interna do "servico" do banco central. Veja o código fonte abaixo para ver os formatos esperados pelo Banco Central neste cenário.

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceitaService.class);

    public boolean atualizarConta(String agencia, String conta, double saldo, String status)
            throws RuntimeException, InterruptedException {


        // Formato agencia: 0000
        if (agencia == null || agencia.length() != 4) {
            LOGGER.info("false 1: {}", agencia);
            return false;
        }

        // Formato conta: 000000
        if (conta == null || conta.length() != 7) {
            LOGGER.info("false 2: {}", conta);
            return false;
        }

        // Tipos de status validos:
        List tipos = new ArrayList();
        tipos.add("A");
        tipos.add("I");
        tipos.add("B");
        tipos.add("P");

        if (status == null || !tipos.contains(status)) {
            LOGGER.info("false 3: {}", status);
            return false;
        }

        // Simula tempo de resposta do serviço (entre 1 e 5 segundos)
        long wait = Math.round(Math.random() * 4000) + 1000;
        Thread.sleep(wait);
        LOGGER.info("wait");

        // Simula cenario de erro no serviço (0,1% de erro)
        long randomError = Math.round(Math.random() * 1000);
        if (randomError == 500) {
            LOGGER.info("error");
            throw new RuntimeException("Error");
        }

        LOGGER.info("true");
        return true;
    }
}
