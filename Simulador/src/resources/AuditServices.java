package resources;
import java.util.concurrent.Semaphore;
public class AuditServices {
    private  Semaphore sem; // se fair for true, garante FIFO

    public AuditServices(boolean fair) {
        this.sem = new Semaphore(1, fair);
    }

    public Semaphore getSem() {
        return sem;
    }
}
