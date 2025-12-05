package scens;

import monitor.eBPFMonitor;
import resources.Dtbase;

class Worker implements Runnable {
    private Dtbase db;
    private boolean isSecure;
    private int valor;
    private String id; //Utilizamos para logs

    public Worker(Dtbase db, boolean isSecure, int valor, String id) {
        this.db = db;
        this.isSecure = isSecure;
        this.valor = valor;
        this.id = id;
    }

    @Override
    public void run() {
        String nomeThread = Thread.currentThread().getName();
        eBPFMonitor.getInstance().log(nomeThread,"INIT" ,"Inicio da transição " + valor);

        try{
            if (isSecure) {
                eBPFMonitor.getInstance().log(nomeThread, "WAIT", "semáforo... ");
                db.getMutex().acquire(); //Permite bloquear até ter permissão
            }
            //Secção critica
            try {
                eBPFMonitor.getInstance().log(nomeThread, "ACCESS", "A verificar o saldo");
                int temp = db.getSaldo();

                //simular tempo de processamento
                Thread.sleep(500);

                db.setSaldo(temp + valor);
                eBPFMonitor.getInstance().log(nomeThread, "WRITE", "Novo saldo guardado" + (temp + valor));
            } finally {
                if (isSecure) {
                    db.getMutex().release();
                    eBPFMonitor.getInstance().log(nomeThread, "RELEASE", "Semáforo libertado" );
                }
            }
        } catch (InterruptedException e) {
            eBPFMonitor.getInstance().log(nomeThread, "ERROR", "Interrompido");
        }
    }
}

public class RaceConditions {
    public static void run(boolean seguro) {
        Dtbase db = new Dtbase();
        Thread[] threads = new Thread[5];

        System.out.println("A executar RaceCondition " + seguro);

        for (int i = 0; i < 5; i++) {
            Worker worker = new Worker(db, seguro, 10, "Utilizador: " + i); //Cria runnable
            threads[i] = new Thread(worker, "Th - " + i);
            threads[i].start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {}
            }
        eBPFMonitor.getInstance().log("MAIN", "RESULT", "Saldo final: " + db.getSaldo());
        System.out.println("Saldo esperado: 50");
        System.out.println("Saldo obtido: " + db.getSaldo());
    }
}

