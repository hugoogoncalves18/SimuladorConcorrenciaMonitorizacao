package resources;
import java.util.concurrent.Semaphore;

/**
 * Representa a carteira individual de um cliente.
 * Para transferir dinheiro, é preciso bloquear a carteira de origem e a de destino.
 */
public class CarteiraCliente {
    private String titular;
    private Semaphore lock = new Semaphore(1);

    public CarteiraCliente(String titular) {
        this.titular = titular;
    }

    public String getTitular() {
        return titular;
    }

    public Semaphore getLock() {
        return lock;
    }
}