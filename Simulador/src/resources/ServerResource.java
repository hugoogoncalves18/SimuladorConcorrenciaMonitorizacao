package resources;
import java.util.concurrent.Semaphore;

/**
 * Representa um recurso do servidor
 * Cada recurso possui um nome e um semáforo para controlo de acesso
 */
public class ServerResource {
    private String nome;
    private Semaphore sem = new Semaphore(1);

    /**
     * Construtor
     * @param nome Identificador do recurso
     */
    public ServerResource(String nome) {
        this.nome = nome;
    }

    /**
     * Obtém o nome do recurso
     * @return String com o nome
     */
    public String getNome() {
        return nome;
    }

    /**
     * Obtém o semáforo que protege o recurso
     * @return Objeto semáforo
     */
    public Semaphore getSem() {
        return sem;
    }
}
