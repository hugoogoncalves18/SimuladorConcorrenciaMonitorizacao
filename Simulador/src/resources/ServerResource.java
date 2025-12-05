package resources;
import java.util.concurrent.Semaphore;

/**
 * Para simulação de deadlock
 */
public class ServerResource {
    private String nome;
    private Semaphore sem = new Semaphore(1);

    public ServerResource(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public Semaphore getSem() {
        return sem;
    }
}
