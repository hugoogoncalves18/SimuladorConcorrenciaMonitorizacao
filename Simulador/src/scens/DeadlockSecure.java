package scens;
import monitor.eBPFMonitor;
import resources.ServerResource;

/**
 * Worker seguro
 * Implementa prevenção de deadlock
 * Esta classe vai ordenar sempre os recursos independentemente da ordem no construtor
 * adquire o menor primeiro, sendo a = menor, b = maior
 */
public class DeadlockSecure implements Runnable{
    private ServerResource a, b;
    private String id;

    public DeadlockSecure(String id, ServerResource r1, ServerResource r2) {
        this.id = id;

        //vai comparar os nomes para decidir quem bloqueia 1
        if (r1.getNome().compareTo(r2.getNome()) < 0) {
            this.a = r1;
            this.b = r2;
        } else {
            this.a = r2;
            this.b = r1;
        }
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        try {
            // 1. Adquire sempre o recurso "Menor" primeiro
            monitor.log(threadName, "LOCK_TRY", "Ordenação forçada:  " + a.getNome());
            a.getSem().acquire();
            monitor.log(threadName, "LOCK_HELD", "Obteve " + a.getNome());

            // Mesmo com sleep, o deadlock não ocorre porque a outra thread também está à espera do "Menor" ou já o tem.
            Thread.sleep(100);

            // 2. Adquire o recurso "Maior"
            monitor.log(threadName, "LOCK_TRY", "Ordenação forçada: A tentar " + b.getNome());
            b.getSem().acquire();

            try {
                monitor.log(threadName, "SUCCESS", "ACESSO SEGURO a ambos os recursos!");
                Thread.sleep(100);
            } finally {
                b.getSem().release();
                monitor.log(threadName, "RELEASE", "Libertou " + b.getNome());
            }

        } catch (InterruptedException e) {
            monitor.log(threadName, "INTERRUPT", "Interrompida.");
        } finally {
            // Libertar o primeiro recurso
            a.getSem().release();
            monitor.log(threadName, "RELEASE", "Libertou " + a.getNome());
        }
    }
}
