# Guia Técnico - Sistema de Leilão Eletrônico

## ✅ Checklist de Requisitos

### Requisitos Técnicos
- [x] **Comunicação via Sockets TCP/UDP**
  - Implementado: TCP em `LeilaoServer` (port 5000)
  - Cliente conecta via `Socket` e `ServerSocket`

- [x] **Gerenciamento de Threads para Conexões Concorrentes**
  - `ExecutorService.newCachedThreadPool()` em `LeilaoServer`
  - `ClientHandler extends Thread` para cada cliente
  - Estruturas thread-safe: `ConcurrentHashMap.newKeySet()`

### Funcionalidades Principais

#### Servidor de Leilão
- [x] Gerenciar estado do leilão
  - `Leilao` armazena: nomeItem, valorInicial, lanceAtual, ultimoLanceador, historicoLances
  
- [x] Cadastrar item de leilão
  - Admin pode cadastrar via `CADASTRAR_ITEM` command
  - Notifica todos clientes em tempo real
  
- [x] Receber e armazenar lances
  - `receberLance()` com identificação do cliente
  - Validação: novo lance > lance anterior
  - Armazenado em `Map<String, List<Double>>` por cliente
  
- [x] Ao receber lance, informar compradores
  - `notificarTodos()` envia `NOVO_LANCE|...` para todos clientes
  
- [x] Encerrar leilão
  - Admin pode encerrar via `ENCERRAR` command
  - Notifica vencedor e valor pago
  
- [x] Validar lances
  - `validarLance()` verifica se valor > atual
  
- [x] Notificar todos participantes
  - Thread receptor em cliente recebe notificações em tempo real

#### Clientes
- [x] Conexão com servidor
  - `LeilaoClient.conectar()` cria Socket
  - Login com autenticação
  
- [x] Interface para enviar lances
  - Menu comprador: opção 1 para dar lance
  - Validação de valor em cliente
  
- [x] Painel de monitoramento em tempo real
  - Thread receptor para notificações
  - Display formatado de eventos

#### Persistência
- [x] Registro histórico completo
  - Salvo em `historico_leiloes.json`
  - Inclui: item, participantes, lances, vencedor, data
  - `Json.java` gerencia serialização

#### Segurança (BÔNUS)
- [x] Autenticação de participantes
  - Credenciais verificadas em `autenticar()`
  - Admin: `admin/admin123`
  - Compradores: `comprador1-3/123456`
  
- [x] Controle de acesso
  - Somente admin pode `CADASTRAR_ITEM` e `ENCERRAR`
  - Somente comprador pode `LANCE`

---

## 🏗️ Arquitetura

### Diagrama de Componentes

```
┌─────────────────────────────────────┐
│        ClientMain                    │
│  (Entrada do Cliente)                │
└──────────────┬──────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│        LeilaoClient                  │
│                                      │
│  - conectar()                        │
│  - iniciarReceptor()                 │
│  - menuAdmin() / menuComprador()     │
│  - processarMensagem()               │
│  - Thread receptor (notificações)    │
└──────────────┬───────────────────────┘
               │
               │ Socket TCP
               │ Port 5000
               │
┌──────────────▼───────────────────────┐
│        ServerMain                    │
│  (Controle do Servidor)              │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│        LeilaoServer                  │
│                                      │
│  - serverSocket (TCP Port 5000)      │
│  - Set<ClientHandler> clients        │
│  - Map<String, Leilao> leiloes       │
│  - ExecutorService threadPool        │
│  - iniciar() [main loop]             │
│  - cadastrarItem()                   │
│  - receberLance()                    │
│  - encerrarLeilao()                  │
│  - notificarTodos()                  │
└──────────────┬───────────────────────┘
               │
               ├─ spawned for each client
               ▼
        ┌──────────────────────────────┐
        │    ClientHandler (Thread)    │
        │                              │
        │  - Socket socket             │
        │  - BufferedReader in          │
        │  - PrintWriter out           │
        │  - processarMensagem()       │
        │  - run() [message loop]      │
        │  - enviarMensagem()          │
        └──────────────────────────────┘
               │
               │ uses
               ▼
        ┌──────────────────────────────┐
        │        Leilao (Model)        │
        │                              │
        │  - nomeItem                  │
        │  - lanceAtual                │
        │  - ultimoLanceador           │
        │  - historicoLances           │
        │  - lancesPerCliente          │
        │  - adicionarLance()          │
        │  - getLancesDoCliente()      │
        └──────────────────────────────┘
```

### Fluxo de Dados

```
Cliente Admin:
  1. LOGIN|admin|admin123
     ↓
  2. Recebe: LOGIN_SUCESSO|ADMIN
     ↓
  3. Envia: CADASTRAR_ITEM|Vaca|1000
     ↓
  4. Server envia para TODOS: NOVO_ITEM|Vaca|1000
     ↓
  5. Envia: ENCERRAR
     ↓
  6. Server envia para TODOS: LEILAO_ENCERRADO|...
     ↓
  7. Salva em historico_leiloes.json

Cliente Comprador:
  1. LOGIN|comprador1|123456
     ↓
  2. Recebe: LOGIN_SUCESSO|COMPRADOR
     ↓
  3. Ouve NOVO_ITEM (notificação em tempo real)
     ↓
  4. Envia: LANCE|1100
     ↓
  5. Recebe: LANCE_ACEITO|1100|comprador1
     ↓
  6. Ouve novos lances (notificação em tempo real)
     ↓
  7. Ouve LEILAO_ENCERRADO (notificação em tempo real)
```

---

## 🔀 Protocolo de Comunicação

### Formato Geral
```
COMANDO|parametro1|parametro2|...
```

### Comandos Cliente → Servidor

| Comando | Formato | Requerimento | Resposta |
|---------|---------|--------------|----------|
| LOGIN | `LOGIN\|usuario\|senha` | - | `LOGIN_SUCESSO\|tipo` ou `LOGIN_FALHA` |
| CADASTRAR_ITEM | `CADASTRAR_ITEM\|nome\|valor` | Admin | `ITEM_CADASTRADO` |
| LANCE | `LANCE\|valor` | Comprador | `LANCE_ACEITO\|valor\|lanceador` ou `LANCE_RECUSADO\|motivo` |
| ENCERRAR | `ENCERRAR` | Admin | `LEILAO_ENCERRADO\|...` |
| STATUS | `STATUS` | - | `STATUS\|...` |
| HISTORICO | `HISTORICO` | - | `HISTORICO_LEILOES\|...` |
| LANCES_CLIENTE | `LANCES_CLIENTE` | - | `LANCES_CLIENTE\|...` |

### Comandos Servidor → Cliente (Broadcast)

| Comando | Formato | Quando |
|---------|---------|--------|
| NOVO_ITEM | `NOVO_ITEM\|Item: X\|Valor: Y` | Após cadastro |
| NOVO_LANCE | `NOVO_LANCE\|Item: X\|usuario\|R$valor` | Após novo lance válido |
| LEILAO_ENCERRADO | `LEILAO_ENCERRADO\|Item: X\|Vencedor: Y\|Valor: Z` | Após encerramento |
| STATUS | `STATUS\|...` | Na conexão |

---

## 🔐 Segurança

### Autenticação
```java
private Map<String, String> autenticar(String usuario, String senha) {
    // Retorna tipo (ADMIN/COMPRADOR) ou null
}
```

Credenciais:
- **admin / admin123** → ADMIN
- **comprador1,2,3 / 123456** → COMPRADOR

### Controle de Acesso
```java
if (autenticado && tipoUsuario.equals("ADMIN")) {
    // Permitir cadastro/encerramento
}
if (autenticado && tipoUsuario.equals("COMPRADOR")) {
    // Permitir apenas lances
}
```

### Validações
- ✅ Senha requerida para login
- ✅ Apenas usuários autenticados realizam ações
- ✅ Admin vs Comprador diferenciados
- ✅ Valores positivos para lances
- ✅ Lances maiores que anterior

---

## 🧪 Testes

### Teste 1: Conexão Básica
```
Servidor: rodando em 5000
Cliente 1: conecta como admin
Resultado: LOGIN_SUCESSO
```

### Teste 2: Cadastro de Item
```
Admin: CADASTRAR_ITEM|Cavalo|5000
Todos clientes: recebem NOVO_ITEM|Cavalo|5000
```

### Teste 3: Validação de Lance
```
Cliente: LANCE|4999 (menor que inicial 5000)
Servidor: LANCE_RECUSADO|Lance deve ser maior que 5000
```

### Teste 4: Lance Válido
```
Cliente1: LANCE|5500
Resultado: LANCE_ACEITO
Todos clientes: NOVO_LANCE|Item: Cavalo|cliente1|R$5500.00
```

### Teste 5: Encerramento
```
Admin: ENCERRAR
Todos: LEILAO_ENCERRADO|Item: Cavalo|Vencedor: cliente1|Valor: 5500
Histórico: salvo em historico_leiloes.json
```

---

## 📊 Estruturas de Dados

### LeilaoServer
```java
private ServerSocket serverSocket;                    // Listener TCP
private final Set<ClientHandler> clients;             // Clientes conectados
private final Map<String, Leilao> leiloes;            // Histórico de leilões
private Leilao leilaoAtual;                           // Leilão em progresso
private final ExecutorService threadPool;             // Pool de threads
```

### ClientHandler
```java
private final Socket socket;                          // Conexão TCP
private final LeilaoServer servidor;                  // Referência servidor
private String clienteId;                             // ID do usuário
private String tipoUsuario;                           // ADMIN ou COMPRADOR
private boolean autenticado;                          // Status autenticação
private PrintWriter out;                              // Envio de mensagens
```

### Leilao
```java
private final String nomeItem;                        // Nome do item
private final double valorInicial;                    // Valor inicial
private double lanceAtual;                            // Maior lance
private String ultimoLanceador;                       // Nome do lanceador
private boolean encerrado;                            // Status
private final List<String> historicoLances;           // Histórico geral
private final Map<String, List<Double>> lancesporCliente;  // Lances por cliente
```

---

## 🚀 Performance

### Recursos Utilizados
- **Threads**: 1 (aceitar conexões) + N (clientes) + 1 (menu servidor)
- **Memória**: Mínima (apenas string/double por lance)
- **Rede**: TCP keepalive, sem heartbeat necessário

### Escalabilidade
- ✅ `ExecutorService` garante pool eficiente
- ✅ `ConcurrentHashMap` permite acesso concorrente
- ✅ Métodos sincronizados em seções críticas
- ✅ Sem deadlocks (operações rápidas)

---

## 🔧 Melhorias Futuras

1. **Criptografia TLS/SSL** - Encriptar canal TCP
2. **Base de Dados** - Trocar JSON por SQL
3. **WebSocket** - Interface web em tempo real
4. **Múltiplos Leilões** - Leilões paralelos
5. **Notificações Email** - Alertar vencedores
6. **Estatísticas** - API de relatórios

---

## 📚 Referências

- Java Socket Programming: https://docs.oracle.com/javase/tutorial/networking/sockets/
- Java Concurrency: https://docs.oracle.com/javase/tutorial/essential/concurrency/
- JSON in Java: https://github.com/stleary/JSON-java

---

Desenvolvido conforme especificação de Sistema Distribuído 📚✅
