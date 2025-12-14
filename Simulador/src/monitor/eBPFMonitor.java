package monitor;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exceção de segurança personalizada utilizada para interromper a execução de uma thread.
 * <p>
 * Esta exceção estende {@link RuntimeException} (unchecked) para simular o comportamento
 * do Kernel a enviar um sinal <b>SIGKILL</b> a um processo mal comportado, forçando
 * a sua paragem imediata sem necessidade de try-catch na lógica de negócio padrão.
 */
class SecurityViolationException extends RuntimeException {
    /**
     * Instancia uma nova violação de segurança.
     * @param msg Mensagem descritiva do motivo do bloqueio (ex: "Violação de SLA").
     */
    public SecurityViolationException(String msg) { super(msg); }
}

/**
 * Monitor de Sistema Simulado (estilo eBPF) com capacidades de SIEM e IPS.
 * Esta classe implementa o padrão <b>Singleton</b> e atua como um observador centralizado
 * de todas as operações críticas do sistema bancário.
 * <b>Funcionalidades Principais:</b>
 * <ul>
 * <li><b>Logging SIEM (JSON):</b> Gera logs estruturados prontos para ingestão em ferramentas como Splunk ou ElasticSearch.</li>
 * <li><b>IPS (Intrusion Prevention System):</b> Deteta anomalias em tempo real e termina threads agressoras.</li>
 * <li><b>Análise Comportamental:</b> Monitoriza tempos de espera para detetar ataques de DoS ou Starvation.</li>
 * </ul>
 */
public class eBPFMonitor {

    /** Instância única do Monitor (Singleton). */
    private static eBPFMonitor instance;

    /** Escritor para o ficheiro de logs persistente. */
    private PrintWriter writer;

    /** * Mapa de estatísticas de acesso.
     * Chave: Nome da Thread | Valor: Quantidade de acessos a recursos.
     */
    private final Map<String, Integer> accessStats = new ConcurrentHashMap<>();

    /** * Mapa de temporizadores para deteção de latência.
     * Chave: Nome da Thread | Valor: Timestamp (ms) do início do pedido.
     */
    private final Map<String, Long> waitTimers = new ConcurrentHashMap<>();

    /** * Limite de Segurança (SLA - Service Level Agreement).
     * Se uma thread esperar mais do que este valor (ms), considera-se Starvation/DoS.
     */
    private static final long STARVATION_THRESHOLD_MS = 200;

    /**
     * Construtor privado para garantir o padrão Singleton.
     * Inicializa o sistema de escrita de logs no ficheiro "eBPFlogs.json".
     */
    private eBPFMonitor() {
        try {
            FileWriter fw = new FileWriter("eBPFlogs.json", true);
            writer = new PrintWriter(fw, true);
        } catch (IOException e) {
            System.err.println("CRITICAL: Falha ao iniciar sistema de logs.");
        }
    }

    /**
     * Obtém a instância única do Monitor eBPF.
     * @return A instância singleton de {@link eBPFMonitor}.
     */
    public static synchronized eBPFMonitor getInstance() {
        if (instance == null) instance = new eBPFMonitor();
        return instance;
    }

    /**
     * Núcleo do Monitor: Regista eventos, analisa comportamentos e atua sobre ameaças.
     * <p>
     * Este método realiza 4 passos fundamentais:
     * <ol>
     * <li><b>Enriquecimento:</b> Adiciona Timestamp e classifica a Severidade do evento.</li>
     * <li><b>Logging SIEM:</b> Formata o evento em JSON e escreve no ficheiro.</li>
     * <li><b>Análise:</b> Verifica se o evento viola regras de anomalia (ex: latência excessiva).</li>
     * <li><b>IPS (Atuação):</b> Se a ação for BLOCK, termina a thread invocadora imediatamente.</li>
     * </ol>
     *
     * @param threadName Nome da thread que gerou o evento (ex: "Cliente-VIP-1").
     * @param eventType Tipo de evento (ex: "LOCK_TRY", "WORK", "ALERT_STARVATION").
     * @param message Descrição detalhada do evento.
     * @throws SecurityViolationException Se o IPS decidir bloquear a thread.
     */
    public synchronized void log(String threadName, String eventType, String message) {
        // 1. Enriquecimento de Dados (Timestamp)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 2. Classificação de Segurança (Severity & Action)
        String severity = "INFO";
        String action = "ALLOW";

        // Define severidade baseada no tipo de evento
        if (eventType.contains("ALERT") || eventType.equals("ERROR")) {
            severity = "HIGH";
        }
        if (eventType.equals("DEADLOCK_CONFIRMED") || eventType.equals("ALERT_STARVATION")) {
            severity = "CRITICAL";
            action = "BLOCK";
        }

        // 3. Formatação SIEM (JSON)
        String jsonLog = String.format(
                "{\"timestamp\": \"%s\", \"severity\": \"%s\", \"event\": \"%s\", \"thread\": \"%s\", \"msg\": \"%s\", \"action\": \"%s\"}",
                timestamp, severity, eventType, threadName, message, action
        );

        // 4. Output (Consola + Ficheiro)
        System.out.println(jsonLog);
        if (writer != null) writer.println(jsonLog);

        // 5. Análise Comportamental
        if (!eventType.startsWith("ALERT")) {
            updateStats(threadName, eventType);
            checkAnomalies(threadName, eventType);
        }

        // 6. IPS - ATUAÇÃO ATIVA
        if (action.equals("BLOCK")) {
            killThread(threadName, "Violação de SLA de Segurança detetada: " + eventType);
        }
    }

    /**
     * Simula a atuação do Kernel ao enviar um sinal de terminação forçada (SIGKILL).
     * * @param threadId O identificador da thread a terminar.
     * @param reason O motivo da terminação para auditoria.
     * @throws SecurityViolationException Exceção unchecked que força a saída da stack de execução.
     */
    private void killThread(String threadId, String reason) {
        throw new SecurityViolationException("IPS ACTION: Thread " + threadId + " terminada. Motivo: " + reason);
    }

    /**
     * Atualiza os contadores estatísticos de acesso aos recursos.
     * * @param thread Nome da thread.
     * @param type Tipo de evento. Apenas eventos de trabalho efetivo ou sucesso são contabilizados.
     */
    private void updateStats(String thread, String type) {
        if (type.equals("WORK") || type.equals("LOCK_HELD") || type.equals("SUCCESS")) {
            accessStats.put(thread, accessStats.getOrDefault(thread, 0) + 1);
        }
    }

    /**
     * Verifica anomalias temporais (Latência).
     * <p>
     * Calcula o tempo decorrido entre um pedido de recurso (WAIT/LOCK_TRY) e a sua obtenção.
     * Se o tempo exceder o {@code STARVATION_THRESHOLD_MS}, gera um alerta de segurança.
     * * @param thread Nome da thread.
     * @param type Tipo de evento atual.
     */
    private void checkAnomalies(String thread, String type) {
        long now = System.currentTimeMillis();

        // Início da espera
        if (type.equals("WAIT") || type.equals("LOCK_TRY")) {
            waitTimers.put(thread, now);
        }
        // Fim da espera (Aquisição)
        else if (type.equals("WORK") || type.equals("LOCK_HELD") || type.equals("SUCCESS")) {
            if (waitTimers.containsKey(thread)) {
                long startWait = waitTimers.remove(thread);
                long duration = now - startWait;

                // Regra de Starvation
                if (duration > STARVATION_THRESHOLD_MS) {
                    log(thread, "ALERT_STARVATION", "Tempo de espera excessivo: " + duration + "ms");
                }
            }
        }
    }

    /**
     * Gera e imprime o relatório final agregado na consola.
     * Limpa as estatísticas após a impressão.
     */
    public synchronized void print() {
        System.out.println("\n=== RELATÓRIO SIEM (Estatísticas Agregadas) ===");
        if (accessStats.isEmpty()) System.out.println("Sem dados registados.");
        else accessStats.forEach((k, v) -> System.out.println("THREAD: " + k + " | ACESSOS: " + v));
        System.out.println("===============================================\n");
        accessStats.clear();
        waitTimers.clear();
    }
}