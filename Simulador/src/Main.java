import java.util.Scanner;
import monitor.eBPFMonitor;
import resources.AuditServices;
import resources.Dtbase;
import resources.ServerResource;
import scens.DeadlockInsecure;
import scens.DeadlockSecure;
import scens.RaceConditionInsecure;
import scens.RaceConditionsSecure;
import scens.StarvationInsecure;
import scens.StarvationSecure;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        eBPFMonitor monitor = eBPFMonitor.getInstance();

        while (true) {
            System.out.println("\n#############################################");
            System.out.println("#    SIMULADOR DE CIBERSEGURANÇA   #");
            System.out.println("#############################################");
            System.out.println("1. Race Condition (Corrupção de Dados)");
            System.out.println("2. Deadlock (Denial of Service)");
            System.out.println("3. Starvation (Bloqueio de Serviços)");
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

            //Executa a logica
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

    private static void runRaceCondition(boolean seguro) {
        Dtbase db = new Dtbase();
        Thread[] threads = new Thread[5];
        int valor = 10;

        System.out.println("Race Condition -> [seguro: " + seguro + "]");

        for (int i = 0; i < 5; i++) {
            Runnable worker;
            if (seguro) {
                worker = new RaceConditionsSecure(db, valor);
            } else {
                worker = new RaceConditionInsecure(db, valor);
            }
            threads[i] = new Thread(worker, "Th-" + i);
            threads[i].start();
        }

        for (Thread t : threads) {
            try{
                t.join();
            } catch (InterruptedException e) {}
        }

        eBPFMonitor.getInstance().log("MAIN", "RESULT", "Saldo final: " + db.getSaldo());
        System.out.println("Saldo final: " +db.getSaldo()); // esperado 50
    }

    private static void runDeadLock(boolean seguro) {
        ServerResource a = new ServerResource("FileA");
        ServerResource b = new ServerResource("File B");

        System.out.println("DeadLock [seguro: " + seguro + "]");

        Thread t1, t2;

        if (seguro) {
            //Modo seguro utiliza a classe que ordena os recursos
            t1 = new Thread(new DeadlockSecure("P1", a, b), "P1");
            t2 = new Thread(new DeadlockSecure("P2", b, a), "P2");
        } else {
            //Modo inseguro a classe aceita processos desordenados
            t1 = new Thread(new DeadlockInsecure("Hack", a, b), "Hack");
            t2 = new Thread(new DeadlockInsecure("Vitima", b, a), "Vitima");
        }
        t1.start();
        t2.start();

        //tempo para detetar deadlock
        try{
            t1.join(3000); // 3 segundos
            t2.join(3000);

            if (t1.isAlive() || t2.isAlive()) {
                eBPFMonitor.getInstance().log("MAIN", "ALERT", "TIMEOUT: Deadlock detetado");
                System.out.println("Alerta, DeadLock detetado. A encerrar threads...");

                // Matar as threads presas para não sujarem o menu seguinte
                t1.interrupt();
                t2.interrupt();
                // Pequena pausa para garantir que os logs de interrupção saem
                t1.join(500);
                t2.join(500);
            } else {
                eBPFMonitor.getInstance().log("MAIN", "SUCCESS", "Execução terminada");
            }
        } catch (InterruptedException e) {}
    }

    private static void runStarvation(boolean seguro) {
        AuditServices services = new AuditServices(seguro);
        System.out.println("Starvation [seguro: " + seguro + "]");

        Thread poor;
        Thread[] rich = new Thread[3];

        if (seguro) {
            poor = new Thread(new StarvationSecure(services, 1), "Pobre");
            for (int i = 0; i < 3; i++) {
                rich[i] = new Thread(new StarvationSecure(services, 10), "Rica-" + i);
            }
        } else {
            poor = new Thread(new StarvationInsecure(services, 1), "Pobre");
            for (int i = 0; i < 3; i++) {
                rich[i] = new Thread(new StarvationInsecure(services, 10), "Rica-" + i);
            }
        }

        poor.setPriority(Thread.MIN_PRIORITY);
        for (Thread t : rich) {
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
        }

        poor.start();

        try {
            poor.join(5000); // Espera 5 segundos pela thread Pobre

            if (poor.isAlive()) {
                eBPFMonitor.getInstance().log("MAIN", "ALERT", "Starvation: Thread pobre bloqueada");
                System.out.println("Alerta: Starvation. Thread pobre não conseguiu terminar.");
                // Removemos System.exit(0) para permitir voltar ao menu
            } else {
                eBPFMonitor.getInstance().log("MAIN", "SUCCESS", "Thread pobre executou com sucesso ");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("A limpar processos em background...");
            for (Thread t : rich) {
                if (t.isAlive()) {
                    t.interrupt(); // Força a saída do sleep()
                }
            }
            // Esperar que elas terminem efetivamente
            for (Thread t : rich) {
                try { t.join(500); } catch (InterruptedException e) {}
            }
        }
    }
}