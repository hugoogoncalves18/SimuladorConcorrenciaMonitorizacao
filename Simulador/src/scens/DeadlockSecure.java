package scens;
import monitor.eBPFMonitor;
import resources.CarteiraCliente;

/**
 * Worker seguro
 * Implementa prevenção de deadlock
 * Esta classe vai ordenar sempre os recursos independentemente da ordem no construtor
 * adquire o menor primeiro, sendo a = menor, b = maior
 */
public class DeadlockSecure implements Runnable{
    /** A carteira que será bloqueada em primeiro lugar (a menor alfabeticamente). */
    private CarteiraCliente origem, destino;

    /** Identificador da operação/transação. */
    private String id;

    /**
     * Instancia uma nova transferência segura.
     * <p>
     * O construtor aplica imediatamente a lógica de ordenação: compara os nomes dos titulares
     * e atribui a carteira "menor" à variável {@code origem} (primeiro lock) e a "maior"
     * à variável {@code destino} (segundo lock).
     *
     * @param id Identificador da thread.
     * @param r1 Uma das carteiras envolvidas na transação.
     * @param r2 A outra carteira envolvida.
     */
    public DeadlockSecure(String id, CarteiraCliente r1, CarteiraCliente r2) {
        this.id = id;

        //vai comparar os nomes para decidir quem bloqueia 1
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
            // 1. Adquire sempre o recurso "Menor" primeiro
            monitor.log(threadName, "LOCK_TRY", "A verificar conta:  " + origem.getTitular());
            origem.getLock().acquire();
            monitor.log(threadName, "LOCK_HELD", "Conta validada: " + origem.getTitular());

            // Mesmo com sleep, o deadlock não ocorre porque a outra thread também está à espera do "Menor" ou já o tem.
            Thread.sleep(100);

            // 2. Adquire o recurso "Maior"
            monitor.log(threadName, "LOCK_TRY", "A verificar conta " + destino.getTitular());
            destino.getLock().acquire();

            try {
                monitor.log(threadName, "SUCCESS", "Transferência realizada com sucesso");
                Thread.sleep(100);
            } finally {
                destino.getLock().release();
                monitor.log(threadName, "RELEASE", "Libertar conta " + destino.getTitular());
            }

        } catch (InterruptedException e) {
            monitor.log(threadName, "INTERRUPT", "Transferência abortada.");
        } finally {
            // Libertar o primeiro recurso
            origem.getLock().release();
            monitor.log(threadName, "RELEASE", "Libertar conta " + origem.getTitular());
        }
    }
}
