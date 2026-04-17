# Build & Deploy - Sistema de Leilão Eletrônico

## 🔨 Compilação

### Pré-requisitos
- ✅ Java JDK 8 ou superior
- ✅ Estrutura de diretórios conforme: [README.md](README.md)

### Opção 1: Compilar com Script (Recomendado)

#### Windows (.bat)
```batch
@echo off
REM Criar diretório de build
if not exist bin mkdir bin

REM Compilar todos os arquivos Java
echo Compilando servidor...
javac -d bin src/model/Leilao.java
javac -d bin src/data/Json.java
javac -d bin -cp bin src/model/LeilaoServer.java
javac -d bin -cp bin src/model/ClientHandler.java
javac -d bin -cp bin src/model/LeilaoClient.java
javac -d bin src/ServerMain.java
javac -d bin -cp bin src/ClientMain.java

echo.
echo ✓ Compilação concluída!
echo.
echo Para iniciar:
echo   Servidor: java -cp bin ServerMain
echo   Cliente:  java -cp bin ClientMain
```

Salvar como: `compile.bat`

#### Linux/Mac (.sh)
```bash
#!/bin/bash

# Criar diretório de build
mkdir -p bin

# Compilar todos os arquivos Java
echo "Compilando servidor..."
javac -d bin src/model/Leilao.java
javac -d bin src/data/Json.java
javac -d bin -cp bin src/model/LeilaoServer.java
javac -d bin -cp bin src/model/ClientHandler.java
javac -d bin -cp bin src/model/LeilaoClient.java
javac -d bin src/ServerMain.java
javac -d bin -cp bin src/ClientMain.java

echo ""
echo "✓ Compilação concluída!"
echo ""
echo "Para iniciar:"
echo "  Servidor: java -cp bin ServerMain"
echo "  Cliente:  java -cp bin ClientMain"
```

Salvar como: `compile.sh`
Executar: `chmod +x compile.sh && ./compile.sh`

### Opção 2: Compilar Manual (Terminal)

```bash
# Criar diretório
mkdir -p bin

# Ordem de compilação (dependências)
javac -d bin src/model/Leilao.java
javac -d bin src/data/Json.java
javac -d bin -cp bin src/model/LeilaoServer.java
javac -d bin -cp bin src/model/ClientHandler.java
javac -d bin -cp bin src/model/LeilaoClient.java
javac -d bin src/ServerMain.java
javac -d bin -cp bin src/ClientMain.java
```

### Opção 3: Usando Maven (Avançado)

Criar arquivo `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.leilao</groupId>
    <artifactId>leilao-eletronico</artifactId>
    <version>1.0.0</version>

    <name>Sistema de Leilão Eletrônico</name>
    <description>Plataforma distribuída de leilões com sockets TCP</description>

    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Sem dependências externas -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>ServerMain</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Compilar: `mvn clean package`

---

## 🚀 Execução

### Servidor

#### Windows
```batch
java -cp bin ServerMain
```

#### Linux/Mac
```bash
java -cp bin ServerMain
```

#### Saída esperada
```
╔════════════════════════════════════════════╗
║   SISTEMA DE LEILÃO ELETRÔNICO              ║
║   SERVIDOR DE LEILÕES                      ║
╚════════════════════════════════════════════╝

Servidor de Leilão iniciado na porta 5000
Servidor aguardando conexões...

[Digite 'help' para ver comandos disponíveis]

>
```

### Cliente

#### Windows
```batch
java -cp bin ClientMain
```

#### Linux/Mac
```bash
java -cp bin ClientMain
```

#### Saída esperada
```
╔════════════════════════════════════════════╗
║   SISTEMA DE LEILÃO ELETRÔNICO              ║
║   CLIENTE DE LEILÕES                       ║
╚════════════════════════════════════════════╝

Credenciais disponíveis:
  Admin: user='admin', senha='admin123'
  Comprador: user='comprador1/2/3', senha='123456'

Host do servidor (Digite 'local' para localhost): _
```

---

## 📦 Distribuição (JAR)

### Criar Executáveis

#### Método 1: JAR com Manifest

```bash
# Comprimir tudo em um arquivo JAR
jar cfm leilao-servidor.jar MANIFEST.MF -C bin .
jar cfm leilao-cliente.jar MANIFEST.MF -C bin .
```

Conteúdo de `MANIFEST.MF`:
```
Manifest-Version: 1.0
Main-Class: ServerMain
Built-By: Seu Nome
```

Executar:
```bash
java -jar leilao-servidor.jar
java -jar leilao-cliente.jar
```

#### Método 2: Upload para Repositório

```bash
# Exemplo com GitHub Releases
# 1. Criar JAR
mvn clean package

# 2. Fazer upload do arquivo
# target/leilao-eletronico-1.0.0.jar
```

---

## 🧪 Verificação Pós-Compilação

### Checklist

- [ ] Arquivo `bin/ServerMain.class` existe
- [ ] Arquivo `bin/ClientMain.class` existe
- [ ] Arquivo `bin/model/LeilaoServer.class` existe
- [ ] Arquivo `bin/model/ClientHandler.class` existe
- [ ] Arquivo `bin/model/LeilaoClient.class` existe
- [ ] Arquivo `bin/model/Leilao.class` existe
- [ ] Arquivo `bin/data/Json.class` existe

### Teste de Execução

1. **Terminal 1: Iniciar servidor**
   ```bash
   java -cp bin ServerMain
   ```
   Deve aparecer:
   ```
   Servidor de Leilão iniciado na porta 5000
   Servidor aguardando conexões...
   ```

2. **Terminal 2: Iniciar cliente**
   ```bash
   java -cp bin ClientMain
   ```
   Deve aparecer:
   ```
   Host do servidor (Digite 'local' para localhost): 
   ```

3. **Digitar "local" no cliente**
   Deve conectar ao servidor

---

## 🔧 Troubleshooting

### Erro: "cannot find symbol"
```
[SOLUÇÃO] Compilar em ordem correta:
1. Leilao.java (sem dependências)
2. Json.java (sem dependências)
3. LeilaoServer.java (usa Leilao)
4. ClientHandler.java (usa LeilaoServer)
5. LeilaoClient.java (sem dependências)
6. ServerMain.java (usa LeilaoServer)
7. ClientMain.java (usa LeilaoClient)
```

### Erro: "Port 5000 already in use"
```
Windows:
  netstat -ano | findstr :5000
  taskkill /PID <PID> /F

Linux/Mac:
  lsof -i :5000
  kill -9 <PID>
```

### Erro: "Connection refused"
```
[VERIFICAR]
- Servidor está rodando?
- Host correto? (usar 'local' ou 127.0.0.1 para local)
- Porta correta? (padrão 5000)
- Firewall bloqueando?
```

### Erro ao compilar: "UTF-8"
```bash
# Adicionar encoding:
javac -encoding UTF-8 -d bin src/model/Leilao.java
```

---

## 📊 Informações de Build

### Versão
- **Java**: 1.8+
- **Encoding**: UTF-8
- **Tamanho** (compilado): ~150 KB
- **Memória mínima**: 128 MB

### Dependências
- ✅ Nenhuma dependência externa
- ✅ Apenas biblioteca padrão Java
- ✅ Funciona offline

---

## 📚 Estrutura Final do Projeto

```
leiloa-to/
├── src/
│   ├── ServerMain.java
│   ├── ClientMain.java
│   ├── model/
│   │   ├── LeilaoServer.java
│   │   ├── ClientHandler.java
│   │   ├── Leilao.java
│   │   └── LeilaoClient.java
│   └── data/
│       └── Json.java
├── bin/                          ← Gerado pela compilação
│   ├── ServerMain.class
│   ├── ClientMain.class
│   ├── model/
│   │   ├── LeilaoServer.class
│   │   ├── ClientHandler.class
│   │   ├── Leilao.class
│   │   └── LeilaoClient.class
│   └── data/
│       └── Json.class
├── historico_leiloes.json       ← Gerado em runtime
├── README.md
├── GUIA_TECNICO.md
├── CENARIOS_TESTE.md
├── BUILD_DEPLOY.md              ← Este arquivo
├── compile.bat                  ← (Opcional)
└── compile.sh                   ← (Opcional)
```

---

## 🎓 Dicas de Desenvolvimento

### Adicionar Debug
```java
// Em LeilaoServer.java
System.out.println("[DEBUG] Client " + clienteId + " enviou: " + mensagem);
```

### Profile de Performance
```bash
java -cp bin -Xmx512M -XX:+PrintGCDetails ServerMain
```

### Modo Verbose
```bash
java -verbose:class -cp bin ServerMain
```

---

## 📞 Contato & Suporte

Dúvidas sobre compilação?

1. Verificar Java version: `java -version`
2. Conferir estrutura de diretórios
3. Seguir ordem de compilação
4. Consultar [README.md](README.md)

---

✅ Ready to compile and deploy! 🚀

Desenvolvido para fins educacionais - Sistema Distribuído 📚
