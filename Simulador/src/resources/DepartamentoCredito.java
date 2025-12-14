package resources;
import java.util.concurrent.Semaphore;

/**
 * Departamento que aprova créditos.
 * Se 'fair' for false, atende apenas os VIPs (prioridade do SO).
 * Se 'fair' for true, atende por ordem de chegada (fila única).
 */
public class DepartamentoCredito {
    private Semaphore sem;

    public DepartamentoCredito(boolean sistemaJusto) {
        this.sem = new Semaphore(1, sistemaJusto);
    }

    public Semaphore getSem() {
        return sem;
    }
}