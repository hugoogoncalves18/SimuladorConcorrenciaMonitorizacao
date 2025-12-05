package scens;

import monitor.eBPFMonitor;
import resources.Dtbase;
import java.util.Random;

/**
 * Worker seguro: Implementação corrigida com os semáforos
 * Garante exclusão mútua na secção critica, impedindo as RaceConditions
 */
public class RaceConditionsSecure implements Runnable {
    private final Dtbase db;
    private final int valor;
    private final Random random;

    public RaceConditionsSecure(Dtbase db, int valor) {
        if (db == null)
            throw  new IllegalArgumentException("A DB não pode ser nula");
        this.db = db;
        this.valor = valor;
        this.random = new Random();
    }

    @Override
    public  void run() {
        String nomeThread = Thread.currentThread().getName();
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        monitor.log(nomeThread, "INIT", "A iniciar a transação" + valor);

        try{
            //solicita permissão
            monitor.log(nomeThread, "WAIT", "a aguardar");
            db.getMutex().acquire();

            try{
                monitor.log(nomeThread, "LOCK", "permissão obtida");
                int saldoTemp = db.getSaldo();

                //simular latência
                Thread.sleep(10 + random.nextInt(90));
                db.setSaldo(saldoTemp + valor);

                monitor.log(nomeThread, "WRITE", "Saldo atualizado" + db.getSaldo());
            } finally {
                //liberta permissão
                db.getMutex().release();
                monitor.log(nomeThread, "RELEASE", "permissão libertada");
            }
        } catch (InterruptedException e) {
            monitor.log(nomeThread, "ERROR", "Thread interrompida");
            Thread.currentThread().interrupt();
        }
    }
}
