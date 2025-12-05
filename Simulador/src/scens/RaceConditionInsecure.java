package scens;

import monitor.eBPFMonitor;
import resources.Dtbase;
import java.util.Random;

/**
 * Worker inseguro, implementação vulnerável a raceConditions
 * Esta Thread acede a recurso partilhado sem o semaforo.
 * Vai simular a falha
 */
public class RaceConditionInsecure implements Runnable {
    private final Dtbase db;
    private final int valor;
    private final Random random;

    /**
     * Construtor do worker inseguro
     * @param db Referência para a DB partilhada
     * @param valor Valor a ser add ao saldo
     */
    public RaceConditionInsecure(Dtbase db, int valor) {
        if (db == null) {
            throw new IllegalArgumentException("A bd não pode ser nula");
        }
        this.db = db;
        this.valor = valor;
        this.random = new Random();
    }

    @Override
    public void run() {
        String nomeThread = Thread.currentThread().getName();
        eBPFMonitor.getInstance().log(nomeThread,"INIT" ,"Inicio da transição " + valor);

        try{
            //leitura
            eBPFMonitor.getInstance().log(nomeThread, "READ", "A ler saldo");
            int saldoTemp = db.getSaldo();

            //processamento
            Thread.sleep(10 + random.nextInt(90));

            //escrita
            db.setSaldo(saldoTemp + valor);
            eBPFMonitor.getInstance().log(nomeThread, "WRITE", "Saldo atualizado para: " + db.getSaldo());
        } catch (InterruptedException e) {
            eBPFMonitor.getInstance().log(nomeThread, "ERROR", "Thread interrompida");
            Thread.currentThread().interrupt();
        }
    }
}


