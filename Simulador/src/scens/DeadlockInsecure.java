package scens;

import monitor.eBPFMonitor;
import resources.CarteiraCliente;

/**
 * Worker inseguro, suscetível a deadlock.
 * Simula uma transferência bancária sem ordem de bloqueio definida.
 */
public class DeadlockInsecure implements Runnable {
    /** Carteira de onde os fundos serão retirados. */
    private CarteiraCliente origem, destino;

    /** Identificador da operação. */
    private String id;

    /**
     * Construtor
     * @param id Identificador da Thread
     * @param r1 Carteira de origem
     * @param r2 Carteira de destino
     */
    public DeadlockInsecure(String id, CarteiraCliente r1, CarteiraCliente r2) {
        this.id = id;
        this.origem = r1;
        this.destino = r2;
    }

/**
 * Executa a tentativa de transferência.
 * <p>
 * O fluxo de execução demonstra a falha:
 */
    @Override
    public void run() {
        String nomeThread = Thread.currentThread().getName();
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        try{
            // 1. Bloqueia carteira de origem
            monitor.log(nomeThread, "LOCK_TRY", "Validando origem: " + origem.getTitular());
            origem.getLock().acquire();
            monitor.log(nomeThread, "LOCK_HELD", "Origem bloqueada: " + origem.getTitular());

            // Pausa para garantir que a outra thread bloqueia a outra carteira (provocando Deadlock)
            Thread.sleep(500);

            // 2. Tenta bloquear carteira de destino
            monitor.log(nomeThread, "LOCK_TRY", "A tentar validar destino: " + destino.getTitular());
            destino.getLock().acquire();

            try{
                monitor.log(nomeThread, "SUCCESS", "Transferência realizada com sucesso!");
                Thread.sleep(100);
            } finally {
                destino.getLock().release();
            }
        } catch (InterruptedException e) {
            monitor.log(nomeThread, "INTERRUPT", "Transferência abortada.");
        } finally {
            // Liberta a origem caso tenha ficado presa
            if (origem.getLock().availablePermits() == 0) {
                origem.getLock().release();
            }
        }
    }
}