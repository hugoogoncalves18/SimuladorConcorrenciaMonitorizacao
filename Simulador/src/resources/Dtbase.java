package resources;
import java.util.concurrent.*;
public class Dtbase {
    private int saldo = 0;
    //semaforo para garantir a exclusao mutua
    private Semaphore mutex = new Semaphore(1);

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
