import java.util.Scanner;
import monitor.EventType;
import monitor.eBPFMonitor;
import resources.DepartamentoCredito;
import resources.DepartamentoCreditoSync;
import resources.ContaConjunta;
import resources.CarteiraCliente;

// Importação dos Cenários Inseguros
import scens.DeadlockInsecure;
import scens.RaceConditionInsecure;
import scens.StarvationInsecure;

// Importação dos Cenários Seguros (Semáforos)
import scens.DeadlockSecure;
import scens.RaceConditionsSecure;
import scens.StarvationSecure;

// Importação dos Cenários Seguros (Synchronized/Wait-Notify)
import scens.DeadlockSynchronized;
import scens.RaceConditionSynchronized;
import scens.StarvationSynchronized;

/**
 * Ponto de entrada (Entry Point) do Simulador de Sistema Bancário.
 * <p>
 * Esta classe é responsável pela orquestração dos diferentes cenários de ataque e defesa.
 * Apresenta um menu interativo (CLI) que permite ao utilizador escolher entre:
 * <ol>
 * <li><b>Demonstrar vulnerabilidades</b> (Modo Inseguro).</li>
 * <li><b>Mitigação via Semáforos</b> (High-level concurrency - {@code java.util.concurrent}).</li>
 * <li><b>Mitigação via Monitores/Wait-Notify</b> (Low-level synchronization - {@code synchronized}).</li>
 * </ol>
 * <p>
 * Inclui também um modo de <b>Stress Test</b> para validação de integridade sob carga elevada.
 */
public class Main {

    /**
     * Método principal da aplicação.
     * Inicializa o Monitor eBPF e gere o ciclo de vida do menu de opções.
     *
     * @param args Argumentos de linha de comando (não utilizados nesta versão).
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        while (true) {
            System.out.println("\n#############################################");
            System.out.println("#    SIMULADOR DE SISTEMA BANCÁRIO (v2.0)   #");
            System.out.println("#############################################");
            System.out.println("1. Race Condition (Corrupção de Saldo)");
            System.out.println("2. Deadlock (Denial of Service)");
            System.out.println("3. Starvation (Negação de Serviços)");
            System.out.println("4. Stress Test (Carga Elevada)");
            System.out.println("0. Sair");
            System.out.print("\nSelecione o cenário: ");

            int opcao;
            try {
                String input = scanner.nextLine();
                if (input.isEmpty()) continue;
                opcao = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Opção inválida");
                continue;
            }

            if (opcao == 0) {
                System.out.println("A encerrar sistema...");
                break;
            }

            if (opcao < 1 || opcao > 4) {
                System.out.println("Opção inválida");
                continue;
            }

            // Seleção do Modo de Segurança
            System.out.println("O que deseja executar?");
            System.out.println("0 - Inseguro (Demonstrar Ataque)");
            System.out.println("1 - Seguro (Ativar Defesas)");
            System.out.print("Opção: ");

            boolean seguro = false;
            try {
                String input = scanner.nextLine();
                if (!input.isEmpty()) {
                    seguro = (Integer.parseInt(input) == 1);
                }
            } catch (NumberFormatException e) {
                System.out.println("Input inválido, a assumir inseguro.");
            }

            System.out.println("------------------------");
            monitor.log("MAIN", EventType.SYSTEM_START, "Cenário " + opcao + " [Seguro: " + seguro + "]");

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
                case 4:
                    runStressTest(seguro);
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
     * Permite comparar duas estratégias de defesa:
     * <ul>
     * <li><b>Semáforos:</b> Uso de {@code Semaphore(1)} para exclusão mútua.</li>
     * <li><b>Synchronized:</b> Uso de blocos {@code synchronized(obj)} (Monitores Intrínsecos).</li>
     * </ul>
     *
     * @param seguro {@code true} para ativar a proteção; {@code false} para permitir a vulnerabilidade TOCTOU.
     */
    private static void runRaceCondition(boolean seguro) {
        Scanner scanner = new Scanner(System.in);
        ContaConjunta conta = new ContaConjunta();
        Thread[] threads = new Thread[5];
        int valor = 10;

        int tipoDefesa = 1; // 1 = Semáforo (Default)

        if (seguro) {
            System.out.println("\n--- Escolha a Técnica de Sincronização ---");
            System.out.println("1. Semáforos (java.util.concurrent)");
            System.out.println("2. Monitores Intrínsecos (synchronized block)");
            System.out.print("Opção: ");
            try {
                String input = scanner.nextLine();
                if (!input.isEmpty()) tipoDefesa = Integer.parseInt(input);
            } catch (NumberFormatException e) {}
        }

        System.out.println(">>> Cenário: Depósitos Simultâneos. Modo: " + (seguro ? (tipoDefesa==1?"Semáforo":"Synchronized") : "INSEGURO"));

        for (int i = 0; i < 5; i++) {
            Runnable worker;
            if (seguro) {
                if (tipoDefesa == 2) {
                    worker = new RaceConditionSynchronized(conta, valor);
                } else {
                    worker = new RaceConditionsSecure(conta, valor);
                }
            } else {
                worker = new RaceConditionInsecure(conta, valor);
            }
            threads[i] = new Thread(worker, "MB-" + i);
            threads[i].start();
        }

        for (Thread t : threads) {
            try{ t.join(); } catch (InterruptedException e) {}
        }

        eBPFMonitor.getInstance().log("MAIN", EventType.RESULT, "Saldo final: " + conta.getSaldo());
        System.out.println("Saldo final: " + conta.getSaldo());
    }

    /**
     * Executa o cenário de <b>Deadlock</b> (Bloqueio Mútuo).
     * <p>
     * Demonstra a eficácia da <b>Ordenação de Recursos</b> para prevenir a "Espera Circular".
     * Permite validar que a lógica algorítmica funciona independentemente da primitiva usada (Semáforo ou Synchronized).
     *
     * @param seguro {@code true} para usar ordenação de recursos; {@code false} para permitir Deadlock.
     */
    private static void runDeadLock(boolean seguro) {
        Scanner scanner = new Scanner(System.in);
        CarteiraCliente a = new CarteiraCliente("Cliente A");
        CarteiraCliente b = new CarteiraCliente("Cliente B");

        int tipoDefesa = 1;

        if (seguro) {
            System.out.println("\n--- Escolha a Técnica de Sincronização ---");
            System.out.println("1. Semáforos (Ordenado)");
            System.out.println("2. Synchronized Blocks (Ordenado)");
            System.out.print("Opção: ");
            try {
                String input = scanner.nextLine();
                if (!input.isEmpty()) tipoDefesa = Integer.parseInt(input);
            } catch (NumberFormatException e) {}
        }

        System.out.println(">>> DeadLock. Modo: " + (seguro ? (tipoDefesa==1?"Semáforo":"Synchronized") : "INSEGURO"));

        Thread t1, t2;

        if (seguro) {
            if (tipoDefesa == 2) {
                t1 = new Thread(new DeadlockSynchronized("Agente-1", a, b), "Agente-1");
                t2 = new Thread(new DeadlockSynchronized("Agente-2", b, a), "Agente-2");
            } else {
                t1 = new Thread(new DeadlockSecure("Agente-1", a, b), "Agente-1");
                t2 = new Thread(new DeadlockSecure("Agente-2", b, a), "Agente-2");
            }
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
                eBPFMonitor.getInstance().log("MAIN", EventType.DEADLOCK_DETECTED, "TIMEOUT: Deadlock confirmado");
                System.out.println("Alerta, DeadLock detetado. A encerrar threads...");
                t1.interrupt();
                t2.interrupt();
                t1.join(500);
                t2.join(500);
            } else {
                eBPFMonitor.getInstance().log("MAIN", EventType.SUCCESS, "Execução terminada");
            }
        } catch (InterruptedException e) {}
    }

    /**
     * Executa o cenário de <b>Starvation</b> (Inanição).
     * <p>
     * Compara duas abordagens para garantir Justiça (Fairness):
     * <ul>
     * <li><b>Semáforo Justo:</b> {@code new Semaphore(1, true)} (Fila FIFO gerida pela API).</li>
     * <li><b>Ticket Lock:</b> Implementação manual de um sistema de senhas usando {@code wait() / notifyAll()}.</li>
     * </ul>
     *
     * @param seguro {@code true} para ativar a política de Justiça; {@code false} para permitir Starvation.
     */
    private static void runStarvation(boolean seguro) {
        Scanner scanner = new Scanner(System.in);

        int tipoDefesa = 1;

        if (seguro) {
            System.out.println("\n--- Escolha a Técnica de Sincronização ---");
            System.out.println("1. Semáforo Justo (java.util.concurrent)");
            System.out.println("2. Wait/Notify com Ticket Lock (Primitivas Monitor)");
            System.out.print("Opção: ");
            try {
                String input = scanner.nextLine();
                if (!input.isEmpty()) tipoDefesa = Integer.parseInt(input);
            } catch (NumberFormatException e) {}
        }

        System.out.println(">>> Starvation. Modo: " + (seguro ? (tipoDefesa==1?"Semáforo":"Ticket Lock") : "INSEGURO"));

        // Instanciar Recursos (Depende da estratégia escolhida)
        DepartamentoCredito depSem = null;
        DepartamentoCreditoSync depSync = null;

        if (seguro && tipoDefesa == 2) {
            depSync = new DepartamentoCreditoSync(); // Usar o novo Ticket Monitor
        } else {
            depSem = new DepartamentoCredito(seguro); // Usar Semáforo (Fair ou Unfair)
        }

        Thread poor;
        Thread[] rich = new Thread[3];

        // Instanciar Threads
        if (seguro) {
            if (tipoDefesa == 2) {
                // Nova implementação wait/notify
                poor = new Thread(new StarvationSynchronized(depSync, 1), "Cliente-Normal");
                for (int i = 0; i < 3; i++) {
                    rich[i] = new Thread(new StarvationSynchronized(depSync, 10), "Cliente-VIP-" + i);
                }
            } else {
                // Implementação original Semáforo
                poor = new Thread(new StarvationSecure(depSem, 1), "Cliente-Normal");
                for (int i = 0; i < 3; i++) {
                    rich[i] = new Thread(new StarvationSecure(depSem, 10), "Cliente-VIP-" + i);
                }
            }
        } else {
            // Modo Inseguro
            poor = new Thread(new StarvationInsecure(depSem, 1), "Cliente-Normal");
            for (int i = 0; i < 3; i++) {
                rich[i] = new Thread(new StarvationInsecure(depSem, 10), "Cliente-VIP-" + i);
            }
        }

        // Definir Prioridades SO
        poor.setPriority(Thread.MIN_PRIORITY);
        for (Thread t : rich) {
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
        }

        poor.start();

        try {
            poor.join(5000);

            if (poor.isAlive()) {
                eBPFMonitor.getInstance().log("MAIN", EventType.ALERT_STARVATION, "Cliente Normal bloqueado (Timeout)");
                System.out.println("Alerta: Starvation. Cliente Normal não conseguiu terminar.");
            } else {
                eBPFMonitor.getInstance().log("MAIN", EventType.SUCCESS, "Cliente Normal executou com sucesso");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("A limpar processos em background...");
            for (Thread t : rich) {
                if (t.isAlive()) t.interrupt();
            }
            for (Thread t : rich) {
                try { t.join(1000); } catch (InterruptedException e) {}
            }
        }
    }

    /**
     * Executa um <b>Stress Test</b> (Teste de Carga).
     *
     * <p>
     * Lança um elevado número de threads para validar:
     * <ol>
     * <li><b>Integridade:</b> Se o saldo se mantém correto sob alta contenção.</li>
     * <li><b>Performance:</b> Calcula o Throughput (transações/segundo).</li>
     * </ol>
     * Ativa o "Modo Silencioso" do Monitor para evitar overhead de I/O na consola.
     *
     * @param seguro {@code true} para ativar defesas; {@code false} para demonstrar corrupção massiva de dados.
     */
    private static void runStressTest(boolean seguro) {
        Scanner scanner = new Scanner(System.in);
        eBPFMonitor monitor = eBPFMonitor.getInstance();
        ContaConjunta conta = new ContaConjunta();

        System.out.println("\n>>> STRESS TEST MONITOR <<<");
        System.out.print("Quantas threads deseja lançar? (Rec: 100-1000): ");
        int numThreads = 100;
        try {
            String in = scanner.nextLine();
            if(!in.isEmpty()) numThreads = Integer.parseInt(in);
        } catch (NumberFormatException e) {
            System.out.println("Valor inválido, a usar 100.");
        }

        int tipoDefesa = 1;
        if (seguro) {
            System.out.println("Técnica: 1-Semáforo | 2-Synchronized");
            try {
                String input = scanner.nextLine();
                if (!input.isEmpty()) tipoDefesa = Integer.parseInt(input);
            } catch (NumberFormatException e) {}
        }

        System.out.println("A iniciar " + numThreads + " threads...");
        System.out.println("Consola em modo SILENCIOSO para não afetar a performance (Ver logs JSON).");

        // Ativa modo silencioso
        monitor.setSilentMode(true);

        Thread[] threads = new Thread[numThreads];
        int valorPorThread = 1; // Cada thread deposita 1€

        long startTime = System.currentTimeMillis();

        // Lançamento das Threads
        for (int i = 0; i < numThreads; i++) {
            Runnable worker;
            if (seguro) {
                if (tipoDefesa == 2) worker = new RaceConditionSynchronized(conta, valorPorThread);
                else worker = new RaceConditionsSecure(conta, valorPorThread);
            } else {
                worker = new RaceConditionInsecure(conta, valorPorThread);
            }
            threads[i] = new Thread(worker, "StressWorker-" + i);
            threads[i].start();
        }

        // Aguardar conclusão (Join)
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException e) {}
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Reativa a consola para mostrar resultados
        monitor.setSilentMode(false);

        System.out.println("\n=== RESULTADOS DO STRESS TEST ===");
        System.out.println("Threads executadas: " + numThreads);
        System.out.println("Tempo de execução:  " + duration + " ms");
        System.out.println("Saldo Esperado:     " + (numThreads * valorPorThread));
        System.out.println("Saldo Real:         " + conta.getSaldo());

        if (conta.getSaldo() != (numThreads * valorPorThread)) {
            System.out.println("STATUS: [FALHA CRÍTICA] Corrupção de dados detetada.");
            int diferenca = (numThreads * valorPorThread) - conta.getSaldo();
            System.out.println("Perda financeira: " + diferenca + "€");
        } else {
            System.out.println("STATUS: [SUCESSO] Integridade mantida.");
        }

        // Métrica de Performance (Throughput)
        if (duration > 0) {
            double throughput = (double) numThreads / (duration / 1000.0);
            System.out.printf("Throughput: %.2f transações/segundo\n", throughput);
        }

        monitor.log("MAIN", EventType.RESULT, "Stress Test Finalizado. Duration: " + duration + "ms");
    }
}