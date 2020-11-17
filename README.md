# SincronizacaoReceita
  O programa recebe como parametro o nome do arquivo csv que sera processado.
  
  O *ReceitaService* poderia estar num microservico a parte e ter uma api expondo o endpoint para receber os dados que serao processados. O mapeamento deste servico no *SincronizacaoReceita* seria feito utilizando o webflux webclient tornando as chamadas assincronas, poderia tambem utilizar o Eureka e garantir a escalabilidade horizontal para aumentar o processamento paralelo.
  
  Foram usadas duas abordagens para a geracao do arquivo de resultado e ambas poderiam resultar e pragramas distinstos, mas para fins didaticos, estao no mesmo programas e em classes separadas:
  
  * Na classe *ParallelStreamService* 
    - Resulta no arquivo *resultStream.csv*
    - Uma abordagem mais simples para executa de forma paralela, utilizando o parallelStream, a chamada da classe *ReceitaService*.
  * Na classe *CompletableFutureService*     
    - Resulta no arquivo *resultCompletableFuture.csv*
    - Executa de forma assincrona e as threads sao gerenciadas pela classe de configuracao *AsyncConfiguration*.
      Existe a possibilidade de utilizar um recurso para interromper a execucao da thread que passar de um tempo de execucao pre-determinado.
        Existe um TO-DO para o metodo *interruptExecution*
    - Foi criado um metodo que finaliza a execucao do programa ao completar a escrita do arquivo(*initiateShutdown*), porem poderia utilizar o actuator/shutdown
      caso fosse uma aplicacao restfull. Ou criar um scheduling definindo o cron para executar todos os dias a partir das 06 horas e nao ter a necessidade de interromper a execucao e para isso, teria que definir um local especifico de onde ler os arquivos que serao processados.
  * Foram criados alguns testes unitarios
  * A classe principal *SincronizacaoReceitaApplication* faz a leitura do arquivo e chama os services com abordagens diferentes para o processamento dos dados.

# instrucao para executar:
  java -jar SincronizacaoReceita-0.0.1.jar "path/file.csv"
  Onde path/file.csv eh o caminho e nome do arquivo que sera lido
