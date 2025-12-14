package resources;
import java.util.concurrent.Semaphore;

/**
 * Representa uma Conta Bancária Conjunta.
 * Se não for protegida, dois titulares podem movimentar a conta ao mesmo tempo,
 * causando erros no saldo final.
 */
public class ContaConjunta {
    private int saldo = 0;

    private final Semaphore mutex = new Semaphore(1, true);

    public int getSaldo() {
        return saldo;
    }

    public void setSaldo(int saldo) {
        this.saldo = saldo;
    }

    public Semaphore getMutex() {
        return mutex;
    }
}