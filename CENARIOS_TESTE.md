# Cenários de Teste - Sistema de Leilão Eletrônico

## 🎯 Cenários de Teste Implementados

### Cenário 1: Sistema Básico com 1 Admin e 2 Compradores

**Objetivo**: Validar fluxo básico de leilão

#### Setup
```
Terminal 1: java -cp bin ServerMain
Terminal 2: java -cp bin ClientMain  → admin/admin123 (ADMIN)
Terminal 3: java -cp bin ClientMain  → comprador1/123456 (COMPRADOR)
Terminal 4: java -cp bin ClientMain  → comprador2/123456 (COMPRADOR)
```

#### Passos

1. **Admin cadastra item**
   ```
   Terminal 2 (Admin):
   > 1
   Nome do item: Vaca Holstein
   Valor inicial (R$): 1000
   [Sistema]: ✓ Item cadastrado com sucesso!
   
   Terminal 3 (Comprador1):
   [Notificação em tempo real]
   ═══════════════════════════════════════════
   📢 NOVO ITEM: Item: Vaca Holstein|Valor inicial: R$1000.00
   ═══════════════════════════════════════════
   
   Terminal 4 (Comprador2):
   [Notificação em tempo real - igual]
   ```

2. **Comprador1 dá lance inicial**
   ```
   Terminal 3:
   > 1
   Valor do lance (R$): 1100
   [Sistema]: ✓ Lance aceito!
   
   Terminal 4:
   [Notificação em tempo real]
   ═══════════════════════════════════════════════════════════
   🔔 NOVO LANCE: Item: Vaca Holstein|comprador1 ofereceu R$1100.00
   ═══════════════════════════════════════════════════════════
   
   Terminal 2 (Admin):
   [Notificação em tempo real - igual]
   ```

3. **Comprador2 aumenta lance**
   ```
   Terminal 4:
   > 1
   Valor do lance (R$): 1200
   [Sistema]: ✓ Lance aceito!
   
   Terminal 3:
   [Notificação em tempo real]
   ═══════════════════════════════════════════════════════════
   🔔 NOVO LANCE: Item: Vaca Holstein|comprador2 ofereceu R$1200.00
   ═══════════════════════════════════════════════════════════
   ```

4. **Comprador1 contra-oferece**
   ```
   Terminal 3:
   > 1
   Valor do lance (R$): 1500
   [Sistema]: ✓ Lance aceito!
   ```

5. **Admin encerra leilão**
   ```
   Terminal 2:
   > 4
   Tem certeza que deseja encerrar o leilão? (S/N): S
   
   Terminal 3 e 4:
   [Notificação em tempo real]
   ═══════════════════════════════════════════════════════════════
   🏁 LEILÃO ENCERRADO!
   Item: Vaca Holstein|Vencedor: comprador1|Valor final: R$1500.00
   ═══════════════════════════════════════════════════════════════
   ```

---

### Cenário 2: Validação de Lances Inválidos

**Objetivo**: Testar validações do sistema

#### Teste 2.1: Lance menor que o anterior
```
Terminal X (Comprador):
> 1
Valor do lance (R$): 800
[Sistema]: ✗ Lance recusado: Lance deve ser maior que 1100
```

#### Teste 2.2: Lance igual ao anterior
```
Terminal X (Comprador):
> 1
Valor do lance (R$): 1100
[Sistema]: ✗ Lance recusado: Lance deve ser maior que 1100
```

#### Teste 2.3: Valor inválido
```
Terminal X (Comprador):
> 1
Valor do lance (R$): abc
[Sistema]: ✗ Valor inválido.
```

---

### Cenário 3: Controle de Acesso

**Objetivo**: Validar diferenças entre Admin e Comprador

#### Teste 3.1: Comprador tentando cadastrar item
```
Terminal Y (Comprador):
> 1
[Sistema]: ✗ Erro: Apenas admin pode cadastrar itens
```

#### Teste 3.2: Comprador tentando encerrar
```
Terminal Y (Comprador):
> 5
[Sistema]: ✗ Erro: Apenas admin pode encerrar leilões
```

#### Teste 3.3: Admin pode ter menu diferente
```
Terminal X (Admin):
╔════════════════════════════════╗
║ MENU ADMINISTRATIVO            ║
║ 1. Cadastrar Item              ║
║ 2. Ver Status do Leilão        ║
║ 3. Histórico de Leilões        ║
║ 4. Encerrar Leilão             ║
║ 5. Sair                        ║
╚════════════════════════════════╝

Terminal Y (Comprador):
╔════════════════════════════════╗
║ MENU DO COMPRADOR              ║
║ 1. Dar Lance                   ║
║ 2. Ver Status do Leilão        ║
║ 3. Meus Lances                 ║
║ 4. Histórico de Leilões        ║
║ 5. Sair                        ║
╚════════════════════════════════╝
```

---

### Cenário 4: Múltiplos Leilões

**Objetivo**: Testar persistência entre leilões

#### Passo a Passo
```
Leilão 1: Vaca Holstein → comprador1 → R$1500
  ↓ salva em historico_leiloes.json

Leilão 2: Cavalo Árabe → comprador2 → R$3000
  ↓ salva em historico_leiloes.json

Admin: > 3 (Histórico)
[Sistema mostra ambos os leilões]
```

#### Arquivo historico_leiloes.json gerado
```json
[
  {
    "nomeItem": "Vaca Holstein",
    "valorInicial": "1000.00",
    "vencedor": "comprador1",
    "valorFinal": "1500.00",
    "totalLances": 3,
    "lances": [
      "comprador1: R$1100.00",
      "comprador2: R$1200.00",
      "comprador1: R$1500.00"
    ],
    "dataEncerramento": "20/04/2025 14:30:45"
  },
  {
    "nomeItem": "Cavalo Árabe",
    "valorInicial": "2000.00",
    "vencedor": "comprador2",
    "valorFinal": "3000.00",
    "totalLances": 2,
    "lances": [
      "comprador1: R$2500.00",
      "comprador2: R$3000.00"
    ],
    "dataEncerramento": "20/04/2025 15:00:20"
  }
]
```

---

### Cenário 5: Autenticação e Segurança

**Objetivo**: Validar sistema de autenticação

#### Teste 5.1: Credenciais incorretas
```
Terminal X:
User: admin
Password: senha_errada
[Sistema]: ✗ Erro na autenticação. Verifique credenciais.
```

#### Teste 5.2: Usuário inexistente
```
Terminal X:
User: usuario_fake
Password: 123456
[Sistema]: ✗ Erro na autenticação. Verifique credenciais.
```

#### Teste 5.3: Admin com credencial certa
```
Terminal X:
User: admin
Password: admin123
[Sistema]: ✓ Conexão estabelecida com sucesso!
           Usuário: admin
           Tipo: ADMIN
```

---

### Cenário 6: Visualização de Status e Histórico

**Objetivo**: Testar comandos de consulta

#### Teste 6.1: Status durante leilão
```
Terminal X (Qualquer cliente):
> 2
═══════════════════════════════════════════════════════════
STATUS|Item: Vaca Holstein|Lance atual: R$1300.00|Último lanceador: comprador1|Total de lances: 2
═══════════════════════════════════════════════════════════
```

#### Teste 6.2: Meus Lances (Comprador)
```
Terminal Y (comprador1):
> 3
═══════════════════════════════════════════
💰 MEUS LANCES
═══════════════════════════════════════════
  comprador1: R$1100.00
  comprador1: R$1300.00
═══════════════════════════════════════════
```

#### Teste 6.3: Histórico
```
Terminal X:
> 4
═════════════════════════════════════════════════════════════
📋 HISTÓRICO DE LEILÕES
═════════════════════════════════════════════════════════════
Item: Vaca Holstein | Inicial: 1000.00 | Final: 1500.00 | Vencedor: comprador1 | Lances: 3
═════════════════════════════════════════════════════════════
```

---

### Cenário 7: Conexão/Desconexão

**Objetivo**: Testar robustez de conexão

#### Teste 7.1: Cliente desconecta normalmente
```
Terminal X (Comprador):
> 5
Tem certeza que deseja desconectar? (S/N): S
✓ Desconectado com sucesso.

Terminal 1 (Servidor):
Cliente desconectado: comprador1. Total: 2
```

#### Teste 7.2: Múltiplos clientes
```
Terminal 1:
☑ Cliente autenticado: comprador1 (COMPRADOR)
☑ Cliente autenticado: comprador2 (COMPRADOR)
☑ Cliente autenticado: admin (ADMIN)
```

#### Teste 7.3: Status do servidor
```
Terminal 1:
> status
☑ Servidor rodando na porta 5000
☑ Clientes conectados: 3
```

---

### Cenário 8: Fluxo Completo Realista

**Objetivo**: Cenário end-to-end realista

#### Simulação
```
[13:00] Admin conecta, cadastra "Ovelha Merino" por R$500
[13:02] Comprador1 conecta, oferece R$600
[13:03] Comprador2 conecta, vê notificação de R$600
[13:04] Comprador2 oferece R$700
[13:05] Comprador3 conecta, oferece R$800
[13:06] Comprador1 oferece R$900
[13:08] Admin encerra, vencedor é comprador1 com R$900
[13:09] Histórico salvo permanentemente
[13:10] Admin cadastra novo item, novo leilão começa
```

---

## 📊 Casos de Teste - Matriz

| ID | Cenário | Entrada | Saída Esperada | Status |
|----|---------|---------|---------------|--------|
| T1 | Login Admin Sucesso | admin/admin123 | LOGIN_SUCESSO\|ADMIN | ✅ |
| T2 | Login Admin Falha | admin/errado | LOGIN_FALHA | ✅ |
| T3 | Login Comprador | comprador1/123456 | LOGIN_SUCESSO\|COMPRADOR | ✅ |
| T4 | Cadastro Item | CADASTRAR_ITEM\|Item\|1000 | ITEM_CADASTRADO | ✅ |
| T5 | Lance Válido | LANCE\|1100 | LANCE_ACEITO | ✅ |
| T6 | Lance Inválido | LANCE\|900 | LANCE_RECUSADO | ✅ |
| T7 | Notificação Broadcast | Admin cadastra | Todos recebem | ✅ |
| T8 | Encerração | ENCERRAR | LEILAO_ENCERRADO | ✅ |
| T9 | Persistência | Encerramento | historico_leiloes.json | ✅ |
| T10 | Acesso Comprador | CADASTRAR_ITEM | ERRO - permissão | ✅ |

---

## 🔄 Fluxos Esperados

### Fluxo Happy Path
```
[Cliente Admin]         SERVER                [Cliente Compr1]     [Cliente Compr2]
    │                     │                         │                    │
    ├─── LOGIN ──────────→│                         │                    │
    │                     │                         │                    │
    │◄── OK + ADMIN ──────┤                         │                    │
    │                     │                         │                    │
    ├─ CADASTRAR_ITEM ───→│                         │                    │
    │                     ├────── NOVO_ITEM ──────→│                    │
    │                     ├────── NOVO_ITEM ────────────────────────→   │
    │                     │                    [notificação]       [notificação]
    │                     │                         │                    │
    │                     │◄───── LANCE ───────────┤                    │
    │                     ├────── NOVO_LANCE ──────│                    │
    │                     ├────── NOVO_LANCE ────────────────────────→  │
    │                     │                         │                    │
    │                     │                         │←───── LANCE ───────┤
    │                     ├────── NOVO_LANCE ──────│                    │
    │                     ├────── NOVO_LANCE ────────────────────────→  │
    │                     │                         │                    │
    ├─── ENCERRAR ───────→│                         │                    │
    │                     ├────── ENCERRADO ──────→│                    │
    │                     ├────── ENCERRADO ────────────────────────→   │
    │                     │    [salva histórico]    │                    │
```

---

## 🎓 Lições Aprendidas

1. **Thread Safety**: Necessário sincronizar acesso a dados compartilhados
2. **Protocolo de Comunicação**: Design claro evita bugs
3. **Notificação Real-Time**: Thread receptor em paralelo ao input
4. **Persistência**: Importante para manter histórico entre sessões
5. **Controle de Acesso**: Validar tipo de usuário em cada operação

---

Desenvolvido para fins educacionais - Sistema Distribuído 📚
