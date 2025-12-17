package monitor;

/**
 * Enumerações para manter o código mais limpo
 */
public enum EventType {
    //Eventos do sistema
    SYSTEM_START, SYSTEM_END,

    //Eventos de trabalho
    WORK, INIT, RESULT,

    //eventos de sincronização
    WAIT,
    LOCK_ACQUIRED,
    LOCK_RELEASE,

    //resultados
    SUCCESS,
    ERROR,
    INTERRUPT,

    //segurança
    ALERT_STARVATION,
    DEADLOCK_DETECTED,
    IPS_BLOCK
}