package scens;
import monitor.EventType;
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
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        // Loop de tentativas de pedido de crédito
        for(int i = 0; i < loopCount; i++) {
            try{
                monitor.log(name, EventType.WAIT, "A entrar na fila...");
                // Tenta entrar no guiché (que agora é FIFO/Justo)
                departamento.getSem().acquire();

                try{
                    monitor.log(name, EventType.LOCK_ACQUIRED, "Atendimento iniciado");
                    Thread.sleep(100);
                } finally {
                    departamento.getSem().release();
                    monitor.log(name, EventType.LOCK_RELEASE, "Atendimento concluído");
                }
            } catch (InterruptedException e) {
                monitor.log(name, EventType.INTERRUPT, "Saiu da fila");
                return;
            }
        }
    }
}