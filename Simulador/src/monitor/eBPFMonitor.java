package monitor;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitor estilo eBPF
 * Responsável por:
 * 1. Registar logs
 * 2.Manter estatisticas de acesso por thread
 * 3.Detetar tempo de espera
 */
public class eBPFMonitor {
    private static eBPFMonitor instance;
    private PrintWriter writer;

    //contador de acessos por Thread
    private final Map<String, Integer> accessStats = new ConcurrentHashMap<>();

    //deteçao de starvation
    private final  Map<String, Long> waitTimers = new ConcurrentHashMap<>();

    //limite para starvation
    private static final long STARVATION_HOLD = 200;

    private eBPFMonitor() {
        try{
            FileWriter fw = new FileWriter("eBPFlogs.txt", true);
            writer = new PrintWriter(fw, true);
        } catch (IOException e) {
            System.out.println("Erro ao criar o ficheiro: " + e.getMessage());
        }
    }

    public static synchronized eBPFMonitor getInstance() {
        if (instance == null) {
            instance = new eBPFMonitor();
        }
        return instance;
    }

    /**
     * Regista um evento, atualiza estatistics e verifica anomalias
     * @param nomeThread Nome thread
     * @param eventType type Tipo de evento( WAIT, ACCESS, WRITE, ALERT)
     * @param message Mensagem descritiva
     */
    //registo no ficheiro de logs
    public synchronized void log(String nomeThread, String eventType, String message) {
        //timestamp
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        //logica de estatisticas e deteção
        checkAnomalies(nomeThread, eventType);
        updateStats(nomeThread, eventType);

        //escrita e log
        String line = String.format(time + " Thread ->" + nomeThread + " - " + eventType + " - " + message);

        System.out.println(line);
        if (writer != null) {
            writer.println(line);
        }
    }

    private void updateStats(String thread, String type) {
        //conta apenas acessos efetivos ou tentativas criticas
        if (type.equals("ACCESS") || type.equals("WRITE") || type.equals("WORK") || type.equals("LOCK_HELD")) {
            accessStats.put(thread, accessStats.getOrDefault(thread, 0) + 1);
        }
    }

    private void checkAnomalies(String thread, String type) {
        long now = System.currentTimeMillis();

        if (type.equals("WAIT") || type.equals("LOCK_TRY")) {
            waitTimers.put(thread, now);
        } else if (type.equals("ACCESS") || type.equals("LOCK_HELD") || type.equals("WORK")) {
            if (waitTimers.containsKey(thread)) {
                long startWait = waitTimers.remove(thread);
                long duration = now - startWait;

                if (duration > STARVATION_HOLD) {
                    String alert = String.format("Alerta, tempo de espera anormal" + duration + " possivel starvation");
                    log(thread, "ALERT_STARVATION", alert);
                }
            }
        }
    }

    /**
     * Relatório final
     */
    public synchronized  void print() {
        System.out.println("-----Estatisticas de eBPF-----");
        System.out.println("Total de acessos a recursos por thread");
        if (accessStats.isEmpty()) {
            System.out.println("Não existem dados");
        } else {
            accessStats.forEach((k, v) -> System.out.println(" - " + k + ": " + v + " acessos"));
        }
        System.out.println("---------------------");

        accessStats.clear();
        waitTimers.clear();
    }
}
