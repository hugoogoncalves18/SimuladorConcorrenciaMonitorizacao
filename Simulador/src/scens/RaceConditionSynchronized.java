package scens;

import monitor.EventType;
import monitor.eBPFMonitor;
import resources.ContaConjunta;
import java.util.Random;

/**
 * Worker seguro que utiliza blocos 'synchronized' (Monitores Intrínsecos).
 * Comparação para o Relatório:
 * - Vantagem: Mais simples de escrever, menos overhead de memória que Semáforos.
 * - Desvantagem: Menos flexível (não tem tryAcquire com timeout, nem fairness garantido).
 */
public class RaceConditionSynchronized implements Runnable {

    private final ContaConjunta conta;
    private final int valor;
    private final Random random;

    public RaceConditionSynchronized(ContaConjunta conta, int valor) {
        this.conta = conta;
        this.valor = valor;
        this.random = new Random();
    }

    @Override
    public void run() {
        String nomeThread = Thread.currentThread().getName();
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        monitor.log(nomeThread, EventType.INIT, "Transação (Sync) de " + valor + "€");

        try {
            monitor.log(nomeThread, EventType.WAIT, "A aguardar monitor...");

            // --- DIFERENÇA PRINCIPAL AQUI ---
            // Em vez de Semáforo, usamos o Monitor do objeto 'conta'.
            // Isto cria uma barreira de exclusão mútua nativa da JVM.
            synchronized (conta) {

                monitor.log(nomeThread, EventType.LOCK_ACQUIRED, "Monitor adquirido");

                // Secção Crítica
                int saldoTemp = conta.getSaldo();
                Thread.sleep(10 + random.nextInt(90));
                conta.setSaldo(saldoTemp + valor);

                monitor.log(nomeThread, EventType.WORK, "Saldo atualizado: " + conta.getSaldo());

                // O 'release' é automático quando o bloco fecha
                monitor.log(nomeThread, EventType.LOCK_RELEASE, "Monitor libertado");
            }

        } catch (InterruptedException e) {
            monitor.log(nomeThread, EventType.ERROR, "Interrompida");
        }
    }
}