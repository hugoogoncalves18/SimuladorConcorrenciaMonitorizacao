package scens;

import monitor.eBPFMonitor;
import resources.ContaConjunta;
import java.util.Random;

/**
 * Worker seguro: Implementação corrigida com os semáforos
 * Garante exclusão mútua na secção critica, impedindo as RaceConditions
 */
public class RaceConditionsSecure implements Runnable {
    /** Referência para a conta bancária partilhada onde será feito o depósito. */
    private final ContaConjunta conta;

    /** Valor monetário a depositar na conta. */
    private final int valor;

    /** Gerador de aleatoriedade para simular latência de rede/processamento. */
    private final Random random;

    /**
     * Instancia um novo worker para realizar uma transação segura.
     *
     * @param conta A conta bancária partilhada (recurso crítico). Não pode ser nula.
     * @param valor O valor a depositar na conta.
     * @throws IllegalArgumentException Se a conta fornecida for nula.
     */
    public RaceConditionsSecure(ContaConjunta conta, int valor) {
        if (conta == null)
            throw  new IllegalArgumentException("A conta não pode ser nula");
        this.conta = conta;
        this.valor = valor;
        this.random = new Random();
    }

    /**
    * Executa a lógica da transação segura.
    */
    @Override
    public  void run() {
        String nomeThread = Thread.currentThread().getName();
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        monitor.log(nomeThread, "INIT", "A iniciar a transação" + valor);

        try{
            //solicita permissão
            monitor.log(nomeThread, "WAIT", "a aguardar");
            conta.getMutex().acquire();

            try{
                monitor.log(nomeThread, "LOCK", "permissão obtida");
                int saldoTemp = conta.getSaldo();

                //simular latência
                Thread.sleep(10 + random.nextInt(90));
                conta.setSaldo(saldoTemp + valor);

                monitor.log(nomeThread, "WRITE", "Saldo atualizado" + conta.getSaldo());
            } finally {
                //liberta permissão
                conta.getMutex().release();
                monitor.log(nomeThread, "RELEASE", "permissão libertada");
            }
        } catch (InterruptedException e) {
            monitor.log(nomeThread, "ERROR", "Thread interrompida");
            Thread.currentThread().interrupt();
        }
    }
}
