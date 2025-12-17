package resources;

/**
 * Monitor que implementa um algoritmo de Ticket Lock (Sistema de Senhas)
 * usando wait() e notifyAll().
 * <p>
 * Isto prova que é possível implementar Justiça (Fairness) manualmente
 * sem depender de classes prontas como Semaphore(true).
 */
public class DepartamentoCreditoSync {
    private int senhaAtual = 0;      // Número que está a ser atendido
    private int proximaSenha = 0;    // Próximo número a ser distribuído

    /**
     * Retira uma senha da máquina.
     * É synchronized para garantir que dois clientes não tiram o mesmo número.
     */
    public synchronized int tirarSenha() {
        return proximaSenha++;
    }

    /**
     * Bloqueia a thread até que a sua senha seja chamada.
     */
    public synchronized void aguardarVez(int minhaSenha) throws InterruptedException {
        // Enquanto não for a minha vez, durmo.
        while (minhaSenha != senhaAtual) {
            wait(); // Primitiva que larga o lock e suspende a thread
        }
    }

    /**
     * Termina o atendimento e chama o próximo número.
     */
    public synchronized void sair() {
        senhaAtual++;
        notifyAll(); // Acorda todas as threads para verificarem as suas senhas
    }
}