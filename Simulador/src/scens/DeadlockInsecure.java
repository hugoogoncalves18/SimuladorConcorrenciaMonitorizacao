package scens;

import monitor.eBPFMonitor;
import resources.ServerResource;

/**
 * Worker inseguro, suscetivel a deadlock
 */
public class DeadlockInsecure implements Runnable {
    private ServerResource a, b;
    private String id;

    /**
     * Construtor
     * @param id Identificador da Thread
     * @param r1 primeiro recurso
     * @param r2 segundo recurso
     */
    public DeadlockInsecure(String id, ServerResource r1, ServerResource r2) {
        this.id = id;
        this.a = r1;
        this.b = r2;
    }

    @Override
    public void run() {
        String nomeThread = Thread.currentThread().getName();
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        try{
            //tenta obter o primeiro recurso
            monitor.log(nomeThread, "LOCK_TRY", "A tentar adquirir recurso" + a);
            a.getSem().acquire();
            monitor.log(nomeThread, "LOCK_HELD", "Obteve: " + a.getNome());

            //pausa para aumentar a probabilidade de bloqueio
            Thread.sleep(500);

            //tenta obter o segundo recurso
            monitor.log(nomeThread, "LOCK_TRY", "A tentar adquirir recurso" + b);
            b.getSem().acquire();

            try{
                monitor.log(nomeThread, "SUCCESS", "Acesso concedido a ambos os recursos");
                Thread.sleep(100);
            } finally {
                b.getSem().release();
            }
            } catch (InterruptedException e) {
            monitor.log(nomeThread, "INTERRUPT", "Thread interrompida");
        } finally {
            //Se ocorrer deadlock não atinge esta linha
            if (a.getSem().availablePermits() == 0) {
                a.getSem().release();
            }
        }
        }
}

