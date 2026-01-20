🛡️ Simulador de Concorrência com Monitorização (Estilo eBPF)
Unidade Curricular: Sistemas Operativos

Linguagem: Java (JDK 21)

Conceito: Simulação de Infraestrutura Bancária Crítica com SIEM e IPS integrado.

📋 Sobre o Projeto
Este projeto não é apenas um simulador bancário; é uma ferramenta de engenharia de segurança desenhada para demonstrar, detetar e mitigar vulnerabilidades clássicas de sistemas operativos e concorrência.

O sistema opera em duas camadas:

Camada de Execução: Threads (Workers) que simulam operações bancárias (Depósitos, Transferências, Crédito).

Camada de Observabilidade (Kernel Space simulado): Um Monitor eBPF (Singleton) que interceta eventos, gera logs estruturados (JSON) e atua ativamente (IPS) para terminar processos que violem as regras de segurança (SLA).

🚀 Funcionalidades Principais
Simulação de Ataques: Demonstração prática de Race Conditions (Corrupção de dados), Deadlocks (DoS) e Starvation (Negação de Serviço).

Monitorização SIEM: Geração de logs estruturados em JSON para auditoria (logs/eBPFlogs.json).

Segregação de Logs: Criação automática de ficheiros de alerta individuais por thread em caso de erro crítico.

IPS (Intrusion Prevention System): Sistema de defesa ativa que "mata" threads (SecurityViolationException) se excederem o tempo de espera permitido (SLA de 5s).

Stress Testing: Modo de carga elevada para validação de escalabilidade e throughput .

Auto-Cura (Self-Healing): O orquestrador (Main) recupera automaticamente de falhas críticas (como Deadlocks forçados) sem crashar a aplicação.


🛠️ Arquitetura e Tecnologias
Estrutura do Projeto
src/
├── Main.java                   # Orquestrador e Menu CLI (Com Self-Healing)
├── monitor/
│   ├── eBPFMonitor.java        # Singleton: SIEM, IPS e Análise Comportamental
│   └── EventType.java          # Enumeração de eventos de sistema
├── resources/                  # Recursos Partilhados (Secções Críticas)
│   ├── ContaConjunta.java      # Recurso para Race Condition
│   ├── CarteiraCliente.java    # Recurso para Deadlock
│   ├── DepartamentoCredito.java# Recurso (Semáforo) para Starvation
│   └── DepartamentoCreditoSync.java # Recurso (Wait/Notify) para Starvation
└── scens/                      # Cenários (Workers/Threads)
    ├── *Insecure.java          # Implementações vulneráveis
    ├── *Secure.java            # Implementações com Semáforos (High-level)
    └── *Synchronized.java      # Implementações com Monitores (Low-level)


Comparação Técnica de Sincronização
O projeto implementa duas estratégias de defesa para comparação:
Características - Semáforos (java.util.concurrent),  Monitores (synchronized / wait-notify)
Abstração - Alto Nível (API),  Baixo Nível (Nativo)
Justiça (Fairness) - "Automático (new Semaphore(1, true))",  Manual (Implementação de Ticket Lock)
Performance - Ligeiro Overhead,  Otimizado pela JVM (Intrinsic Locks)
Uso - Cenários Padrão,  Controlo Fino e Stress Test


🚦 Cenários Implementados
1. Race Condition (Integridade) 🏎️
O Problema: Vulnerabilidade TOCTOU (Time-of-Check to Time-of-Use). Múltiplas threads leem e escrevem o saldo simultaneamente, resultando em perda de dinheiro.

A Solução: Atomicidade garantida via Exclusão Mútua (Mutex ou synchronized block).

2. Deadlock (Disponibilidade) 🔒
O Problema: Espera Circular. Thread A bloqueia recurso X e quer Y; Thread B bloqueia Y e quer X. O sistema congela (DoS).

A Solução: Algoritmo de Ordenação de Recursos. O sistema obriga a adquirir sempre o recurso com "menor ID" primeiro, tornando o Deadlock matematicamente impossível.

3. Starvation (Justiça/QoS) 🍽️
O Problema: Threads com prioridade alta (VIP) monopolizam a CPU, impedindo a execução de threads normais.

A Solução 1 (Semáforo): Política Fair=true (Fila FIFO).

A Solução 2 (Engenharia): Implementação manual de um Ticket Lock (Sistema de Senhas) usando wait() e notifyAll() para garantir ordem estrita de chegada.


📊 Como Interpretar os Logs
Os logs são gerados na pasta logs/ no formato JSON. Exemplo de um bloqueio por IPS:
{
  "timestamp": "2025-12-18 17:24:33",
  "severity": "CRITICAL",
  "event": "DEADLOCK_DETECTED",
  "thread": "MAIN",
  "msg": "TIMEOUT: Deadlock confirmado",
  "action": "BLOCK"
}

Severity: INFO, HIGH, CRITICAL.
Action: ALLOW (Permitido), BLOCK (Thread terminada pelo IPS).

▶️ Como Executar
1.Compilar: Certifique-se de que tem o JDK 21 instalado.
javac -d out src/Main.java src/monitor/*.java src/resources/*.java src/scens/*.java

2.Correr:
java -cp out Main

3.Menu Interativo: Escolha o cenário (1-4) e o modo (0-Inseguro, 1-Seguro). No modo seguro, poderá escolher entre Semáforos ou Synchronized.

🧪 Stress Test (Cenário 4)
Para validar a robustez, execute a opção 4.

Lança 100-1000 threads simultâneas.

Ativa o "Silent Mode" no Monitor para reduzir I/O de consola.

Apresenta métricas finais de Integridade (Saldo Correto?) e Throughput (Transações/segundo).

Autor: Hugo Gonçalves
