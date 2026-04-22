# Sistema de Leilão Eletrônico Distribuído

## 🎯 Visão Geral

Sistema distribuído de leilões eletrônicos implementado em Java com comunicação TCP/IP, gerenciamento de threads para conexões concorrentes e notificação em tempo real de todos os participantes.

---

## 📋 Requisitos Implementados

### ✅ Requisitos Técnicos
- **Comunicação via Sockets TCP**: Implementado em `LeilaoServer` e `LeilaoClient`
- **Gerenciamento de Threads**: Usando `ExecutorService` para pool de threads e `ConcurrentHashMap` para estruturas thread-safe
- **Sincronização**: Métodos sincronizados em operações críticas

### ✅ Funcionalidades Principais

#### 🖥️ Servidor de Leilão
- Gerenciar estado do leilão (item, lance atual)
- Cadastrar itens de leilão
- Receber e armazenar lances (identificando o autor)
- Notificar automaticamente todos os compradores sobre novos lances
- Encerrar leilão e notificar vencedor
- Validar lances (deve ser maior que o anterior)
- Histórico completo de leilões

#### 👥 Clientes
- Conexão autenticada com servidor
- Interface diferenciada para Admin e Comprador
- **Painel em tempo real** com notificações de novos lances
- Visualizar status atual do leilão
- **Admin**: Cadastrar itens, encerrar leilões
- **Comprador**: Dar lances, visualizar meus lances, histórico

#### 💾 Persistência
- Registro histórico completo em arquivo `historico_leiloes.json`
- Salva automaticamente ao encerrar cada leilão
- Inclui: item, valor inicial, vencedor, valor final, lances, data

#### 🔐 Segurança (BÔNUS)
- Autenticação de participantes (Admin vs Comprador)
- Múltiplos usuários suportados
- Controle de acesso (somente admin pode cadastrar/encerrar)
- Validação de credenciais

---

## 📁 Estrutura de Arquivos

```
leiloa-to/
├── src/
│   ├── ServerMain.java              # Ponto de entrada do servidor
│   ├── ClientMain.java              # Ponto de entrada do cliente
│   ├── model/
│   │   ├── LeilaoServer.java        # Servidor principal
│   │   ├── ClientHandler.java       # Manipulador de cliente (thread)
│   │   ├── Leilao.java              # Modelo de leilão
│   │   └── LeilaoClient.java        # Cliente
│   └── data/
│       └── Json.java                # Persistência JSON
├── historico_leiloes.json           # Arquivo de histórico (gerado)
└── README.md                        # Este arquivo
```

---

## 🚀 Como Usar

### Compilar

```bash
javac -d bin src/model/*.java src/data/*.java src/*.java
```

### Rodar cenarios automatizados

```bash
javac -encoding UTF-8 -d out/production/leiloa-to src/model/*.java src/data/*.java src/*.java
java -cp out/production/leiloa-to CenarioTeste
```

Esse runner:
- sobe o `ServerMain`
- inicia `ClientMain` reais para admin e compradores
- executa cadastros, lances, consultas e encerramentos
- valida autenticacao e permissoes via protocolo
- gera uma copia do historico em `historico_leiloes_teste.json`

### 1️⃣ Iniciar o Servidor

```bash
java -cp bin ServerMain
```

Ou, se a porta `5000` ja estiver em uso:

```bash
java -cp bin ServerMain 5001
```

**Saída esperada:**
```
╔════════════════════════════════════════════╗
║   SISTEMA DE LEILÃO ELETRÔNICO              ║
║   SERVIDOR DE LEILÕES                      ║
╚════════════════════════════════════════════╝

Servidor de Leilão iniciado na porta 5000
Servidor aguardando conexões...

[Digite 'help' para ver comandos disponíveis]

> _
```

**Comandos do servidor:**
- `status` - Mostra status do servidor
- `clientes` - Mostra número de clientes conectados
- `help` - Lista todos os comandos
- `sair` - Encerra o servidor

### 2️⃣ Iniciar Clientes

Em outro terminal:

```bash
java -cp bin ClientMain
```

**Credenciais disponíveis:**

**Admin:**
- Usuário: `admin`
- Senha: `admin123`

**Compradores:**
- Usuário: `comprador1`, `comprador2`, `comprador3`
- Senha: `123456`

---

## 📝 Fluxo de Uso

### Como Admin

1. **Conectar**
   ```
   Connect to: local
   Port: 5000
   User: admin
   Password: admin123
   ```

2. **Painel de Administração**
   ```
   ╔════════════════════════════════╗
   ║ MENU ADMINISTRATIVO            ║
   ║ 1. Cadastrar Item              ║
   ║ 2. Ver Status do Leilão        ║
   ║ 3. Histórico de Leilões        ║
   ║ 4. Encerrar Leilão             ║
   ║ 5. Sair                        ║
   ╚════════════════════════════════╝
   ```

3. **Cadastrar Item**
   - Opção 1
   - Nome do item: `Vaca Holstein`
   - Valor inicial: `1000.00`

4. **Encerrar**
   - Opção 4
   - Confirma encerramento

### Como Comprador

1. **Conectar**
   ```
   Connect to: local
   Port: 5000
   User: comprador1
   Password: 123456
   ```

2. **Painel do Comprador**
   ```
   ╔════════════════════════════════╗
   ║ MENU DO COMPRADOR              ║
   ║ 1. Dar Lance                   ║
   ║ 2. Ver Status do Leilão        ║
   ║ 3. Meus Lances                 ║
   ║ 4. Histórico de Leilões        ║
   ║ 5. Sair                        ║
   ╚════════════════════════════════╝
   ```

3. **Dar Lance** (Opção 1)
   - Valor: `1100.00`
   - Recebe confirmação em tempo real

4. **Receber Notificações**
   - Novos lances são notificados automaticamente
   - Encerramento do leilão com vencedor

---

## 🔄 Fluxo Técnico

```
┌─────────────┐
│ ClientMain  │
└──────┬──────┘
       │ conectar()
       ▼
┌─────────────────┐      ┌──────────────────┐
│ LeilaoClient    │─────▶│ LeilaoServer     │
│                 │      │                  │
│ Scanner         │      │ ServerSocket     │
│ socket TCP      │      │ Set<ClientHandler│
│ Thread receptor │      │ Map<Leilao>      │
└─────────────────┘      │ ExecutorService  │
       ▲                 └────────┬─────────┘
       │                          │
       │ notificações            │ socket TCP
       │ em tempo real           │
       │                          ▼
       │                 ┌──────────────────┐
       │                 │ ClientHandler    │
       │                 │                  │
       │                 │ autenticado      │
       │                 │ tipoUsuario      │
       └─────────────────│ run()            │
                         │ processarMsg()   │
                         └──────────────────┘
```

---

## 📊 Estrutura de Dados

### Protocolo de Comunicação

**Login:**
```
Client → Server: LOGIN|usuario|senha
Server → Client: LOGIN_SUCESSO|ADMIN ou COMPRADOR
```

**Cadastrar Item:**
```
Client → Server: CADASTRAR_ITEM|nome|valor
Server → Todos:  NOVO_ITEM|nome|valor
```

**Dar Lance:**
```
Client → Server: LANCE|valor
Server → Todos:  NOVO_LANCE|Item: X|usuario|R$valor
```

**Encerrar:**
```
Client → Server: ENCERRAR
Server → Todos:  LEILAO_ENCERRADO|Item: X|Vencedor: Y|Valor: Z
```

---

## 💾 Arquivo de Histórico

`historico_leiloes.json`:

```json
[
  {
    "nomeItem": "Vaca Holstein",
    "valorInicial": "1000.00",
    "vencedor": "comprador1",
    "valorFinal": "1500.00",
    "totalLances": 5,
    "lances": [
      "comprador1: R$1100.00",
      "comprador2: R$1200.00",
      "comprador1: R$1300.00",
      "comprador3: R$1400.00",
      "comprador1: R$1500.00"
    ],
    "dataEncerramento": "20/04/2025 14:30:45"
  }
]
```

---

## 🔧 Melhorias Implementadas

### vs. Especificação Original

| Requisito | Implementação |
|-----------|---------------|
| Comunicação TCP | ✅ Sockets TCP com protocolo texto |
| Gerenciamento Thread | ✅ Pool de threads + ConcurrentHashMap |
| Cadastrar item | ✅ Menu admin |
| Receber lances | ✅ Validação + armazenamento |
| Notificação de lances | ✅ Broadcast em tempo real |
| Encerrar leilão | ✅ Menu admin com confirmação |
| Autenticação | ✅ Admin vs Comprador |
| Persistência | ✅ JSON com histórico completo |
| Interface | ✅ Menu amigável com emojis |

---

## 🐛 Tratamento de Erros

- ✅ Validação de valores (positivos, maiores que anterior)
- ✅ Verificação de autenticação
- ✅ Controle de acesso (Admin/Comprador)
- ✅ Reconexão de clientes
- ✅ Sincronização thread-safe

---

## 📱 Exemplo de Interação Completa

### Terminal 1 - Servidor
```
> status
☑ Servidor rodando na porta 5000
☑ Clientes conectados: 2
```

### Terminal 2 - Admin
```
✓ Conexão estabelecida com sucesso!
  Usuário: admin
  Tipo: ADMIN

> 1
Nome do item: Cavalo árabe
Valor inicial (R$): 5000
✓ Item cadastrado com sucesso!
```

### Terminal 3 - Comprador 1
```
✓ Conexão estabelecida com sucesso!
  Usuário: comprador1
  Tipo: COMPRADOR

═══════════════════════════════════════════
📢 NOVO ITEM: Item: Cavalo árabe|Valor inicial: R$5000.00
═══════════════════════════════════════════

> 1
Valor do lance (R$): 5500
✓ Lance aceito!
```

### Terminal 3 - Comprador 2 (Notificação em tempo real)
```
═══════════════════════════════════════════════════════════
🔔 NOVO LANCE: Item: Cavalo árabe|comprador1 ofereceu R$5500.00
═══════════════════════════════════════════════════════════
```

---

## 📚 Classes Principais

### `LeilaoServer`
- Gerencia servidor principal
- Thread pool de conexões
- Notificação broadcast
- Persistência

### `ClientHandler`
- Manipula cada cliente (extends Thread)
- Autentica usuário
- Processa mensagens
- Envia notificações

### `LeilaoClient`
- Cliente com interface menu
- Thread receptor para notificações
- Menus diferenciados (Admin/Comprador)

### `Leilao`
- Modelo de dados do leilão
- Armazena lances
- Rastreia por cliente

---

## 🎓 Conceitos Aprendidos

1. **Sockets TCP** - Comunicação cliente-servidor
2. **Threads** - Execução concorrente (`Thread`, `ExecutorService`)
3. **Sincronização** - `synchronized`, `ConcurrentHashMap`
4. **Protocolos** - Design de protocolo de comunicação
5. **Persistência** - Salvar dados em JSON
6. **Autenticação** - Controle de acesso baseado em papel

---

## 📞 Suporte

**Erros comuns:**

1. `Port already in use` → Mudar porta ou encerrar processo anterior
2. `Connection refused` → Verificar se servidor está rodando
3. `Login failed` → Verificar credenciais na tabela acima

---

**Desenvolvido como sistema distribuído para fins educacionais** 📚

