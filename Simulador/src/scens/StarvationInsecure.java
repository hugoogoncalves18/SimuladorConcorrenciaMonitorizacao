package scens;
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
                dep.getSem().acquire();

                try{
                    eBPFMonitor.getInstance().log(name, "WORK", "A Analisar o pedido de crédito: ");
                    Thread.sleep(100);
                } finally {
                    dep.getSem().release();
                }
            }catch (InterruptedException e) {
                eBPFMonitor.getInstance().log(name, "INTERRUPT", "Interrompida");
                return;
            }
        }
    }
}
