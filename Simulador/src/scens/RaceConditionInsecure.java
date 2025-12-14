package scens;

import monitor.eBPFMonitor;
import resources.ContaConjunta;
import java.util.Random;

/**
 * Worker inseguro, implementação vulnerável a raceConditions
 * Esta Thread acede a recurso partilhado sem o semaforo.
 * Vai simular a falha
 */
public class RaceConditionInsecure implements Runnable {
    private final ContaConjunta conta;
    private final int valor;
    private final Random random;

    /**
     * Construtor do worker inseguro
     * @param conta Referência para a conta partilhada
     * @param valor Valor a ser add ao saldo
     */
    public RaceConditionInsecure(ContaConjunta conta, int valor) {
        if (conta == null) {
            throw new IllegalArgumentException("A conta não pode ser nula");
        }
        this.conta = conta;
        this.valor = valor;
        this.random = new Random();
    }

    @Override
    public void run() {
        String nomeThread = Thread.currentThread().getName();
        eBPFMonitor.getInstance().log(nomeThread,"INIT" ,"Iniciar depósito de  " + valor + "€");

        try{
            //Secção critica desprotegida
            eBPFMonitor.getInstance().log(nomeThread, "READ", "A ler saldo");
            int saldoTemp = conta.getSaldo();

            //processamento
            Thread.sleep(10 + random.nextInt(90));

            //escrita
            conta.setSaldo(saldoTemp + valor);
            eBPFMonitor.getInstance().log(nomeThread, "WRITE", "Saldo atualizado para: " + conta.getSaldo());
        } catch (InterruptedException e) {
            eBPFMonitor.getInstance().log(nomeThread, "ERROR", "Thread interrompida");
            Thread.currentThread().interrupt();
        }
    }
}


