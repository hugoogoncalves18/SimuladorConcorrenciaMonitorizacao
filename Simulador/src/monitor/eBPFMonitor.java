package monitor;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class eBPFMonitor {
    private static eBPFMonitor instance;
    private PrintWriter writer;

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

    //registo no ficheiro de logs
    public synchronized void log(String nomeThread, String eventType, String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String line = String.format(time + " Thread ->" + nomeThread + " - " + eventType + " - " + message);

        System.out.println(line);
        if (writer != null) {
            writer.println(line);
        }
    }
}
