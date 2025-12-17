package scens;

import monitor.EventType;
import monitor.eBPFMonitor;
import resources.DepartamentoCreditoSync;

/**
 * Worker seguro que utiliza o sistema de Ticket Lock.
 */
public class StarvationSynchronized implements Runnable {

    private final DepartamentoCreditoSync departamento;
    private final int loopCount;

    public StarvationSynchronized(DepartamentoCreditoSync dep, int loopCount) {
        this.departamento = dep;
        this.loopCount = loopCount;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        for (int i = 0; i < loopCount; i++) {
            try {
                // 1. Tirar Senha
                monitor.log(name, EventType.WAIT, "Tirou senha e aguarda...");
                int minhaSenha = departamento.tirarSenha();

                // 2. Wait (Guard)
                departamento.aguardarVez(minhaSenha);

                try {
                    // 3. Trabalho (Secção Crítica)
                    monitor.log(name, EventType.LOCK_ACQUIRED, "A ser atendido (Senha " + minhaSenha + ")");

                    Thread.sleep(100);

                } finally {
                    // 4. Notify (Sair)
                    departamento.sair();
                    monitor.log(name, EventType.LOCK_RELEASE, "Atendimento concluído");
                }
            } catch (InterruptedException e) {
                monitor.log(name, EventType.INTERRUPT, "Desistiu da fila");
                return;
            }
        }
    }
}