import java.util.Scanner;
import monitor.eBPFMonitor;
import resources.DepartamentoCredito;
import resources.ContaConjunta;
import resources.CarteiraCliente;
import scens.DeadlockInsecure;
import scens.DeadlockSecure;
import scens.RaceConditionInsecure;
import scens.RaceConditionsSecure;
import scens.StarvationInsecure;
import scens.StarvationSecure;

/**
 * Ponto de entrada (Entry Point) do Simulador de Sistema Bancário.
 * Esta classe é responsável pela orquestração dos diferentes cenários de ataque e defesa.
 * Apresenta um menu interativo (CLI) que permite ao utilizador escolher entre
 * demonstrar vulnerabilidades (Modo Inseguro) ou as respetivas mitigações (Modo Seguro).
 */
public class Main {

    /**
     * Método principal da aplicação.
     * @param args Argumentos de linha de comando (não utilizados nesta versão).
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        while (true) {
            System.out.println("\n#############################################");
            System.out.println("#    SIMULADOR DE SISTEMA BANCÁRIO   #");
            System.out.println("#############################################");
            System.out.println("1. Race Condition (Corrupção de Saldo)");
            System.out.println("2. Deadlock (Denial of Service)");
            System.out.println("3. Starvation (Negação de Serviços)");
            System.out.println("0. Sair");
            System.out.print("\nSelecione o cenário: ");

            int opcao;
            try {
                String input = scanner.nextLine();
                if (input.isEmpty())
                    continue;
                opcao = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Opcao invalida");
                continue;
            }

            if (opcao == 0) {
                System.out.println("Fim");
                break;
            }

            if (opcao < 1 || opcao > 3) {
                System.out.println("Opção inválida");
                continue;
            }

            System.out.println("O que deseja executar?");
            System.out.println("0-Inseguro\n1-Seguro: ");

            boolean seguro = false;
            try {
                String input = scanner.nextLine();
                if (!input.isEmpty()) {
                    seguro = (Integer.parseInt(input) == 1);
                }
            } catch (NumberFormatException e) {
                System.out.println("Numero invalido, inseguro por default");
            }

            System.out.println("------------------------");
            monitor.log("MAIN", "SYSTEM_START", "Cenário " + opcao);

            switch (opcao) {
                case 1:
                    runRaceCondition(seguro);
                    break;
                case 2:
                    runDeadLock(seguro);
                    break;
                case 3:
                    runStarvation(seguro);
                    break;
            }
            monitor.print();
            System.out.println("Pressione ENTER para voltar ao menu...");
            scanner.nextLine();
        }
        scanner.close();
    }

    /**
     * Executa o cenário de <b>Race Condition</b> (Condição de Corrida).
     * <p>
     * Simula múltiplos terminais Multibanco (Threads) a tentar depositar dinheiro
     * numa mesma {@link ContaConjunta} simultaneamente.
     * <ul>
     * <li><b>Modo Inseguro:</b> Demonstra a vulnerabilidade TOCTOU (Time-of-Check to Time-of-Use),
     * resultando em perda de dinheiro (corrupção de dados).</li>
     * <li><b>Modo Seguro:</b> Utiliza Semáforos (Mutex) para garantir exclusão mútua na
     * secção crítica, assegurando a integridade do saldo.</li>
     * </ul>
     *
     * @param seguro {@code true} para ativar a proteção por Mutex; {@code false} para permitir a vulnerabilidade.
     */
    private static void runRaceCondition(boolean seguro) {
        ContaConjunta conta = new ContaConjunta();
        Thread[] threads = new Thread[5];
        int valor = 10;

        System.out.println(">>> Cenário: Depósitos Simultâneos [Modo Seguro: " + seguro + "]");

        for (int i = 0; i < 5; i++) {
            Runnable worker;
            if (seguro) {
                worker = new RaceConditionsSecure(conta, valor);
            } else {
                worker = new RaceConditionInsecure(conta, valor);
            }
            threads[i] = new Thread(worker, "MB: " + i);
            threads[i].start();
        }

        for (Thread t : threads) {
            try{
                t.join();
            } catch (InterruptedException e) {}
        }

        eBPFMonitor.getInstance().log("MAIN", "RESULT", "Saldo final: " + conta.getSaldo());
        System.out.println("Saldo final: " + conta.getSaldo());
    }

    /**
     * Executa o cenário de <b>Deadlock</b> (Bloqueio Mútuo).
     * <p>
     * Simula transferências bancárias cruzadas entre dois agentes.
     * <ul>
     * <li><b>Modo Inseguro:</b> Demonstra um ataque de Negação de Serviço (DoS) onde
     * dois agentes bloqueiam recursos (Carteiras) em ordem inversa, causando paragem total.</li>
     * <li><b>Modo Seguro:</b> Implementa a estratégia de <i>Ordenação de Recursos</i>,
     * garantindo que a ordem de aquisição dos locks é consistente para evitar espera circular.</li>
     * </ul>
     *
     * @param seguro {@code true} para usar ordenação de recursos; {@code false} para permitir Deadlock.
     */
    private static void runDeadLock(boolean seguro) {
        CarteiraCliente a = new CarteiraCliente("Cliente A");
        CarteiraCliente b = new CarteiraCliente("Cliente B");

        System.out.println("DeadLock [seguro: " + seguro + "]");

        Thread t1, t2;

        if (seguro) {
            t1 = new Thread(new DeadlockSecure("Agente-1", a, b), "Agente-1");
            t2 = new Thread(new DeadlockSecure("Agente-2", b, a), "Agente-2");
        } else {
            t1 = new Thread(new DeadlockInsecure("Hacker", a, b), "Hacker");
            t2 = new Thread(new DeadlockInsecure("Vitima", b, a), "Vitima");
        }
        t1.start();
        t2.start();

        try{
            t1.join(3000);
            t2.join(3000);

            if (t1.isAlive() || t2.isAlive()) {
                eBPFMonitor.getInstance().log("MAIN", "ALERT", "TIMEOUT: Deadlock detetado");
                System.out.println("Alerta, DeadLock detetado. A encerrar threads...");

                t1.interrupt();
                t2.interrupt();
                t1.join(500);
                t2.join(500);
            } else {
                eBPFMonitor.getInstance().log("MAIN", "SUCCESS", "Execução terminada");
            }
        } catch (InterruptedException e) {}
    }

    /**
     * Executa o cenário de <b>Starvation</b> (Inanição).
     * <p>
     * Simula um {@link DepartamentoCredito} onde clientes VIP competem com clientes normais.
     * <ul>
     * <li><b>Modo Inseguro:</b> O semáforo não garante justiça (Fairness=false). Devido à
     * prioridade do SO, os VIPs monopolizam o guiché e o Cliente Normal sofre Starvation.</li>
     * <li><b>Modo Seguro:</b> O semáforo é configurado com Fairness=true (FIFO), garantindo
     * que o Cliente Normal é atendido pela ordem de chegada, independentemente da prioridade.</li>
     * </ul>
     *
     * @param seguro {@code true} para ativar a política de Justiça (Fairness); {@code false} para permitir Starvation.
     */
    private static void runStarvation(boolean seguro) {
        DepartamentoCredito services = new DepartamentoCredito(seguro);
        System.out.println("Starvation [seguro: " + seguro + "]");

        Thread poor;
        Thread[] rich = new Thread[3];

        if (seguro) {
            poor = new Thread(new StarvationSecure(services, 1), "Cliente-Normal");
            for (int i = 0; i < 3; i++) {
                rich[i] = new Thread(new StarvationSecure(services, 10), "Cliente-VIP-" + i);
            }
        } else {
            poor = new Thread(new StarvationInsecure(services, 1), "Cliente-Normal");
            for (int i = 0; i < 3; i++) {
                rich[i] = new Thread(new StarvationInsecure(services, 10), "Cliente-VIP-" + i);
            }
        }

        poor.setPriority(Thread.MIN_PRIORITY);
        for (Thread t : rich) {
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
        }

        poor.start();

        try {
            poor.join(5000);

            if (poor.isAlive()) {
                eBPFMonitor.getInstance().log("MAIN", "ALERT", "Starvation: Cliente Normal bloqueado");
                System.out.println("Alerta: Starvation. Cliente Normal não conseguiu terminar.");
            } else {
                eBPFMonitor.getInstance().log("MAIN", "SUCCESS", "Cliente Normal executou com sucesso ");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("A limpar processos em background...");
            for (Thread t : rich) {
                if (t.isAlive()) {
                    t.interrupt();
                }
            }
            for (Thread t : rich) {
                try { t.join(1000); } catch (InterruptedException e) {}
            }
        }
    }
}