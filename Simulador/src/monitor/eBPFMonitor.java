package monitor;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exceção de segurança personalizada utilizada para interromper a execução de uma thread.
 */
class SecurityViolationException extends RuntimeException {
    public SecurityViolationException(String msg) { super(msg); }
}

/**
 * Monitor de Sistema Simulado (estilo eBPF) com capacidades de SIEM e IPS.
 * <p>
 * Funcionalidades Principais:
 * <ul>
 * <li><b>Consola Limpa / Modo Silencioso:</b> Adaptável para testes de carga (Stress Tests).</li>
 * <li><b>Logging Híbrido:</b> JSON centralizado para SIEM + Ficheiros de Alerta individuais por Thread.</li>
 * <li><b>IPS:</b> Deteta anomalias e termina threads agressoras.</li>
 * </ul>
 */
public class eBPFMonitor {

    private static eBPFMonitor instance;
    private PrintWriter writer; // Log geral (JSON)

    // Flag para controlar a saída na consola durante Stress Tests
    private boolean silentMode = false;

    // Estatísticas e Timers
    private final Map<String, Integer> accessStats = new ConcurrentHashMap<>();
    private final Map<String, Long> waitTimers = new ConcurrentHashMap<>();
    private static final long STARVATION_THRESHOLD_MS = 5000;

    //Caminho para a pasta logs
    private static final String LOG_DIR = "logs/";

    private eBPFMonitor() {
        try {

            File directory = new File(LOG_DIR);
            if(!directory.exists()) {
                directory.mkdirs();
            }
            // Ficheiro geral do sistema (comportamento completo em JSON)
            FileWriter fw = new FileWriter(LOG_DIR + "eBPFlogs.json", true);
            writer = new PrintWriter(fw, true);
        } catch (IOException e) {
            System.err.println("CRITICAL: Falha ao iniciar sistema de logs.");
        }
    }

    public static synchronized eBPFMonitor getInstance() {
        if (instance == null) instance = new eBPFMonitor();
        return instance;
    }

    /**
     * Útil para testes de carga onde o output da consola degrada a performance.
     * @param silent true para esconder logs informativos da consola.
     */
    public void setSilentMode(boolean silent) {
        this.silentMode = silent;
    }

    /**
     * Regista eventos, gere logs e atua sobre ameaças.
     * Utiliza {@link EventType} para maior segurança de tipos e código limpo.
     *
     * @param threadName Nome da thread.
     * @param eventType Tipo de evento (Enum).
     * @param message Mensagem descritiva.
     */
    public synchronized void log(String threadName, EventType eventType, String message) {
        // 1. Timestamp curto para a consola, longo para o JSON
        String timeFull = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 2. CONSOLA INTELIGENTE (Silent Mode)
        // Se estiver em silentMode, só imprimimos se for CRÍTICO (Deadlocks ou Starvation confirmados)
        boolean isCritical = (eventType == EventType.DEADLOCK_DETECTED ||
                eventType == EventType.ALERT_STARVATION ||
                eventType == EventType.IPS_BLOCK);

        if (!silentMode || isCritical) {
            System.out.println(threadName + " -> " + message);        }

        // 3. Lógica de Severidade e Ação
        String severity = determineSeverity(eventType);
        String action = (isCritical) ? "BLOCK" : "ALLOW";

        // 4. LOG GERAL (JSON para SIEM) - Sempre escrito, independente do modo silencioso
        String jsonLog = String.format(
                "{\"timestamp\": \"%s\", \"severity\": \"%s\", \"event\": \"%s\", \"thread\": \"%s\", \"msg\": \"%s\", \"action\": \"%s\"}",
                timeFull, severity, eventType, threadName, message, action
        );
        if (writer != null) writer.println(jsonLog);

        // 5. SEGREGAÇÃO DE LOGS (Requisito: Alertas por utilizador)
        // Se for HIGH ou CRITICAL, escreve também num ficheiro exclusivo desta thread
        if (severity.equals("HIGH") || severity.equals("CRITICAL")) {
            writeUserAlertLog(threadName, jsonLog);
        }

        // 6. Análise Comportamental (Não analisa os próprios alertas para evitar loop)
        if (!eventType.name().startsWith("ALERT")) {
            updateStats(threadName, eventType);
            checkAnomalies(threadName, eventType);
        }

        // 7. IPS - Atuação
        if (action.equals("BLOCK")) {
            killThread(threadName, "Violação de SLA detectada: " + eventType);
        }
    }

    // --- Métodos Auxiliares ---

    private String determineSeverity(EventType type) {
        if (type == EventType.DEADLOCK_DETECTED || type == EventType.ALERT_STARVATION || type == EventType.IPS_BLOCK) {
            return "CRITICAL";
        }
        if (type == EventType.ERROR || type == EventType.INTERRUPT) {
            return "HIGH";
        }
        return "INFO";
    }

    /**
     * Escreve num ficheiro separado específico para a thread em questão.
     * Útil para isolar o comportamento de um utilizador problemático.
     */
    private void writeUserAlertLog(String threadName, String logContent) {
        // Limpa caracteres especiais do nome da thread para criar um ficheiro válido
        String safeName = threadName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String fileName = LOG_DIR + "alert_" + safeName + ".log";

        try (FileWriter fw = new FileWriter(fileName, true);
             PrintWriter pw = new PrintWriter(fw, true)) {
            pw.println(logContent);
        } catch (IOException e) {
            System.err.println("Erro ao escrever log individual: " + e.getMessage());
        }
    }

    private void killThread(String threadId, String reason) {
        throw new SecurityViolationException("IPS ACTION: " + threadId + " terminada. " + reason);
    }

    private void updateStats(String thread, EventType type) {
        // Usa o Enum para comparação (mais eficiente e limpo)
        if (type == EventType.WORK || type == EventType.LOCK_ACQUIRED || type == EventType.SUCCESS) {
            accessStats.put(thread, accessStats.getOrDefault(thread, 0) + 1);
        }
    }

    private void checkAnomalies(String thread, EventType type) {
        long now = System.currentTimeMillis();

        if (type == EventType.WAIT) {
            waitTimers.put(thread, now);
        }
        else if (type == EventType.LOCK_ACQUIRED) {
            if (waitTimers.containsKey(thread)) {
                long duration = now - waitTimers.remove(thread);

                if (duration > STARVATION_THRESHOLD_MS) {
                    // Chama o log recursivamente com o evento de Alerta
                    log(thread, EventType.ALERT_STARVATION, "Latência excessiva: " + duration + "ms");
                }
            }
        }
    }

    public synchronized void print() {
        System.out.println("\n=== Resumo de Execução ===");
        if (accessStats.isEmpty()) System.out.println("Sem dados registados.");
        else accessStats.forEach((k, v) -> System.out.println("THREAD: " + k + " | ACESSOS: " + v));
        System.out.println("==========================\n");
        accessStats.clear();
        waitTimers.clear();
    }
}