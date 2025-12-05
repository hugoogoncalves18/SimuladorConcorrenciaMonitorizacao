package scens;
import monitor.eBPFMonitor;
import resources.AuditServices;

/**
 * Worker seguro, prevenção de starvation
 */
public class StarvationSecure implements Runnable {
    private AuditServices services;
    private int loopCount;

    public StarvationSecure(AuditServices s, int loopCount) {
        this.services = s;
        this.loopCount = loopCount;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        //loop de acesso
        for(int i = 0; i < loopCount; i++) {
            try{
                services.getSem().acquire();

                try{
                    eBPFMonitor.getInstance().log(name, "WORK", "A usar o serviço");
                    Thread.sleep(100);
                } finally {
                    services.getSem().release();
                }
            }catch (InterruptedException e) {
                eBPFMonitor.getInstance().log(name, "INTERRUPT", "Interrompida");
            }
        }
    }
}
