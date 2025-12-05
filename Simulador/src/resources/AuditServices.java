package resources;
import java.util.concurrent.Semaphore;

/**
 * Recurso partilhado
 */
public class AuditServices {
    private  Semaphore sem; // se fair for true, garante FIFO

    /**
     * Construtor
     * @param fair se true, ativa a politica FIFO, se false permite starvation
     */
    public AuditServices(boolean fair) {
        this.sem = new Semaphore(1, fair);
    }

    public Semaphore getSem() {
        return sem;
    }
}
