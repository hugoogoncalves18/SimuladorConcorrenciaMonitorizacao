package resources;
import java.util.concurrent.*;

/**
 * Representa o recurso partilhado (Base de Dados/Conta Bancária) que será alvo de acessos concorrentes.
 * <p>
 * Contém um saldo que pode ser corrompido se não for protegido adequadamente.
 * Inclui um {@link Semaphore} para permitir a implementação da exclusão mútua.
 */
public class Dtbase {

    /**
     * Saldo partilhado entre Threads
     */
    private int saldo = 0;

    /**
     * Semáforo para controlo de acesso á seccção critica
     * Inicia com 1 permissão
     */
    private final Semaphore mutex = new Semaphore(1, true);

    /**
     * Obtém o saldo
     * @return Valor do saldo
     */
    public int getSaldo() {
        return saldo;
    }

    /**
     * Atualiza o saldo
     * @param saldo novo valor de saldo
     */
    public void setSaldo(int saldo) {
        this.saldo = saldo;
    }

    /**
     * Retorna o semáforo para controlo de sincronização
     * @return Objeto Semaphore associado a este recurso
     */
    public Semaphore getMutex() {
        return mutex;
    }
}
