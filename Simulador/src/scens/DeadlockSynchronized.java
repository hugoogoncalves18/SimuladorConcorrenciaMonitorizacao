package scens;

import monitor.EventType;
import monitor.eBPFMonitor;
import resources.CarteiraCliente;

/**
 * Worker seguro contra Deadlock utilizando blocos 'synchronized' aninhados.
 * <p>
 * Diferença técnica para Semáforos:
 * O bloqueio é intrínseco ao objeto (Monitor Java). Não há 'release' explícito,
 * o que evita erros de programação onde o programador se esquece de libertar o lock.
 * <p>
 * Estratégia: Ordenação de Recursos (igual à versão Semáforo) para evitar Espera Circular.
 */
public class DeadlockSynchronized implements Runnable {

    private final CarteiraCliente origem;
    private final CarteiraCliente destino;
    private final String id;

    public DeadlockSynchronized(String id, CarteiraCliente r1, CarteiraCliente r2) {
        this.id = id;

        // ORDENAÇÃO DE RECURSOS (Vital para evitar Deadlock)
        // Bloqueamos sempre o objeto "menor" primeiro.
        if (r1.getTitular().compareTo(r2.getTitular()) < 0) {
            this.origem = r1;
            this.destino = r2;
        } else {
            this.origem = r2;
            this.destino = r1;
        }
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        try {
            monitor.log(threadName, EventType.WAIT, "A aguardar monitor 1 (" + origem.getTitular() + ")");

            // 1. Primeiro Bloqueio (Nativo)
            synchronized (origem) {
                monitor.log(threadName, EventType.LOCK_ACQUIRED, "Monitor 1 adquirido");

                Thread.sleep(50); // Simula latência

                monitor.log(threadName, EventType.WAIT, "A aguardar monitor 2 (" + destino.getTitular() + ")");

                // 2. Segundo Bloqueio (Nativo e Aninhado)
                synchronized (destino) {
                    monitor.log(threadName, EventType.LOCK_ACQUIRED, "Monitor 2 adquirido");

                    // --- SECÇÃO CRÍTICA ---
                    monitor.log(threadName, EventType.SUCCESS, "Transferência Sync realizada");
                    Thread.sleep(100);
                }
                // Fim do bloco destino -> Release automático
                monitor.log(threadName, EventType.LOCK_RELEASE, "Monitor 2 libertado");
            }
            // Fim do bloco origem -> Release automático
            monitor.log(threadName, EventType.LOCK_RELEASE, "Monitor 1 libertado");

        } catch (InterruptedException e) {
            monitor.log(threadName, EventType.INTERRUPT, "Interrompido");
        }
    }
}