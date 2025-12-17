package scens;

import monitor.EventType;
import monitor.eBPFMonitor;
import resources.DepartamentoCredito;

/**
 * Worker inseguro que simula um cliente num sistema de atendimento de crédito suscetível a <b>Starvation</b> (Inanição).
 */
public class StarvationInsecure implements Runnable {
    /** Referência para o departamento de crédito (recurso partilhado injusto). */
    private DepartamentoCredito dep;

    /** Número de pedidos de crédito que este cliente vai tentar realizar. */
    private int pedidos;

    /**
     * Instancia um novo cliente vulnerável a Starvation.
     *
     * @param dep O departamento de crédito partilhado. Deve estar configurado com {@code fair=false} para demonstrar o ataque.
     * @param pedidos Quantidade de operações a realizar.
     */
    public StarvationInsecure(DepartamentoCredito dep, int pedidos) {
        this.dep = dep;
        this.pedidos = pedidos;
    }

    /**
     * Executa o ciclo de vida do cliente no banco.
     */
    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        //loop de acesso
        for(int i = 0; i < pedidos; i++) {
            try{
                eBPFMonitor.getInstance().log(name, EventType.WAIT, "A tentar entrar na fila...");

                dep.getSem().acquire();

                try{
                    // Registar entrada (ACQUIRED)
                    eBPFMonitor.getInstance().log(name, EventType.LOCK_ACQUIRED, "A analisar o pedido de crédito");
                    Thread.sleep(100);
                } finally {
                    dep.getSem().release();
                    eBPFMonitor.getInstance().log(name, EventType.LOCK_RELEASE, "Saiu do guiché");
                }
            }catch (InterruptedException e) {
                eBPFMonitor.getInstance().log(name, EventType.INTERRUPT, "Interrompida");
                return;
            }
        }
    }
}