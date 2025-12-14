package scens;
import monitor.eBPFMonitor;
import resources.DepartamentoCredito;

/**
 * Worker seguro, prevenção de starvation.
 * Simula um cliente num sistema de Crédito Justo (FIFO).
 */
public class StarvationSecure implements Runnable {
    /** Referência para o departamento de crédito (recurso partilhado com política justa). */
    private DepartamentoCredito departamento;

    /** Número de pedidos de crédito que este cliente vai tentar realizar. */
    private int loopCount;

    /**
     * Instancia um novo cliente seguro.
     *
     * @param dep O departamento de crédito partilhado. Deve estar configurado com {@code fair=true}.
     * @param loopCount Quantidade de operações a realizar.
     */
    public StarvationSecure(DepartamentoCredito dep, int loopCount) {
        this.departamento = dep;
        this.loopCount = loopCount;
    }

    /**
     * Executa o ciclo de vida do cliente no banco.
     */
    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        // Loop de tentativas de pedido de crédito
        for(int i = 0; i < loopCount; i++) {
            try{
                // Tenta entrar no guiché (que agora é FIFO/Justo)
                departamento.getSem().acquire();

                try{
                    eBPFMonitor.getInstance().log(name, "WORK", "Atendimento de Crédito em curso (Seguro)");
                    Thread.sleep(100);
                } finally {
                    departamento.getSem().release();
                }
            } catch (InterruptedException e) {
                eBPFMonitor.getInstance().log(name, "INTERRUPT", "Cliente saiu da fila");
                return;
            }
        }
    }
}