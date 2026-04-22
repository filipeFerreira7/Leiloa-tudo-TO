import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CenarioTeste {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5000;
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path HISTORICO = ROOT.resolve("historico_leiloes.json");
    private static final Path HISTORICO_TESTE = ROOT.resolve("historico_leiloes_teste.json");

    private int sucessos;
    private int falhas;

    public static void main(String[] args) {
        new CenarioTeste().executar();
    }

    private void executar() {
        System.out.println("============================================================");
        System.out.println("AUTOMACAO DE CENARIOS - SISTEMA DE LEILAO ELETRONICO");
        System.out.println("============================================================");
        System.out.println("Este runner sobe o ServerMain, inicia clientes reais e");
        System.out.println("executa validacoes automaticas de admin e compradores.");
        System.out.println();

        byte[] historicoOriginal = lerHistoricoOriginal();
        ManagedProcess server = null;
        List<ManagedProcess> clientes = new ArrayList<>();

        try {
            server = iniciarServidor();

            executarCenarioAutenticacao();
            executarCenarioPermissoes();

            ManagedProcess admin = iniciarCliente("ADMIN", "admin", "admin123");
            ManagedProcess comprador1 = iniciarCliente("COMPRADOR1", "comprador1", "123456");
            ManagedProcess comprador2 = iniciarCliente("COMPRADOR2", "comprador2", "123456");

            clientes.add(admin);
            clientes.add(comprador1);
            clientes.add(comprador2);

            executarFluxoCompleto(server, admin, comprador1, comprador2);

            encerrarCliente(comprador1);
            encerrarCliente(comprador2);
            encerrarCliente(admin);

            enviarComandoServidor(server, "status");
            assertProcessOutput(server, "status do servidor", "Clientes conectados: 0", Duration.ofSeconds(5));
        } catch (Exception e) {
            registrarFalha("execucao geral", e.getMessage());
        } finally {
            for (ManagedProcess cliente : clientes) {
                encerrarSilenciosamente(cliente);
            }
            encerrarServidorSilenciosamente(server);
            preservarEReporHistorico(historicoOriginal);
        }

        System.out.println();
        System.out.println("============================================================");
        System.out.println("RESUMO");
        System.out.println("============================================================");
        System.out.println("Sucessos: " + sucessos);
        System.out.println("Falhas: " + falhas);
        System.out.println("Historico de teste: " + HISTORICO_TESTE.getFileName());
        System.out.println();

        if (falhas > 0) {
            System.out.println("Alguns cenarios falharam. Revise o log acima.");
            System.exit(1);
        }

        System.out.println("Todos os cenarios executaram com sucesso.");
    }

    private ManagedProcess iniciarServidor() throws Exception {
        System.out.println("[INFO] Iniciando ServerMain...");
        ManagedProcess server = ManagedProcess.start(
                "SERVER",
                new ProcessBuilder(javaCommand(), "-cp", classpathAtual(), "ServerMain"),
                ROOT
        );

        if (server.waitForOutput("Address already in use", Duration.ofSeconds(3))
                || server.waitForOutput("Nao foi possivel subir o servidor.", Duration.ofSeconds(3))) {
            throw new Exception("A porta 5000 ja esta em uso. Encerre o servidor atual antes de rodar o CenarioTeste.");
        }

        aguardarPorta();
        assertProcessOutput(server, "boot do servidor", "Servidor aguardando conex", Duration.ofSeconds(10));
        return server;
    }

    private void executarCenarioAutenticacao() throws Exception {
        System.out.println();
        System.out.println("[CENARIO] Autenticacao");

        try (ProtocolSession invalido = new ProtocolSession()) {
            invalido.connect();
            invalido.send("LOGIN|admin|senha_errada");
            String resposta = invalido.readLine();
            assertContains("login invalido", resposta, "LOGIN_FALHA");
        }

        try (ProtocolSession admin = new ProtocolSession()) {
            admin.connect();
            admin.send("LOGIN|admin|admin123");
            assertContains("login admin", admin.readLine(), "LOGIN_SUCESSO|ADMIN");
            assertContains("status inicial admin", admin.readLine(), "STATUS|");
        }

        try (ProtocolSession comprador = new ProtocolSession()) {
            comprador.connect();
            comprador.send("LOGIN|comprador3|123456");
            assertContains("login comprador", comprador.readLine(), "LOGIN_SUCESSO|COMPRADOR");
            assertContains("status inicial comprador", comprador.readLine(), "STATUS|");
        }
    }

    private void executarCenarioPermissoes() throws Exception {
        System.out.println();
        System.out.println("[CENARIO] Permissoes de comprador");

        try (ProtocolSession comprador = new ProtocolSession()) {
            comprador.connect();
            comprador.send("LOGIN|comprador1|123456");
            comprador.readLine();
            comprador.readLine();

            comprador.send("CADASTRAR_ITEM|Teste Indevido|1000");
            assertContains("comprador nao cadastra item", comprador.readLine(), "ERRO|Apenas admin pode cadastrar itens");

            comprador.send("ENCERRAR");
            assertContains("comprador nao encerra leilao", comprador.readLine(), "ERRO|Apenas admin pode encerrar leil");
        }
    }

    private void executarFluxoCompleto(
            ManagedProcess server,
            ManagedProcess admin,
            ManagedProcess comprador1,
            ManagedProcess comprador2
    ) throws Exception {
        System.out.println();
        System.out.println("[CENARIO] Fluxo completo com ClientMain real");

        assertProcessOutput(admin, "login admin na UI", "Tipo: ADMIN", Duration.ofSeconds(10));
        assertProcessOutput(comprador1, "login comprador1 na UI", "Tipo: COMPRADOR", Duration.ofSeconds(10));
        assertProcessOutput(comprador2, "login comprador2 na UI", "Tipo: COMPRADOR", Duration.ofSeconds(10));

        enviarComandoServidor(server, "status");
        assertProcessOutput(server, "status com clientes conectados", "Clientes conectados: 3", Duration.ofSeconds(5));

        cadastrarItem(admin, "Vaca Holstein", "1000");
        assertProcessOutput(comprador1, "broadcast novo item para comprador1", "Vaca Holstein", Duration.ofSeconds(8));
        assertProcessOutput(comprador2, "broadcast novo item para comprador2", "Vaca Holstein", Duration.ofSeconds(8));

        consultarStatus(comprador1);
        assertProcessOutput(comprador1, "status do primeiro leilao", "Lance atual: R$1000.00", Duration.ofSeconds(5));

        darLance(comprador1, "1100");
        assertProcessOutput(comprador1, "lance inicial aceito", "Lance aceito", Duration.ofSeconds(5));

        darLance(comprador2, "800");
        assertProcessOutput(comprador2, "lance invalido recusado", "Lance recusado", Duration.ofSeconds(5));

        darLance(comprador2, "1200");
        assertProcessOutput(comprador2, "segundo lance aceito", "Lance aceito", Duration.ofSeconds(5));

        consultarMeusLances(comprador1);
        assertProcessOutput(comprador1, "consulta meus lances", "comprador1: R$1100.00", Duration.ofSeconds(5));

        darLance(comprador1, "1500");
        assertProcessOutput(comprador1, "lance vencedor aceito", "Lance aceito", Duration.ofSeconds(5));

        consultarStatus(admin);
        assertProcessOutput(admin, "status antes de encerrar", "Lance atual: R$1500.00", Duration.ofSeconds(5));

        encerrarLeilao(admin);
        assertProcessOutput(admin, "encerramento primeiro leilao", "Vencedor: comprador1", Duration.ofSeconds(8));
        assertProcessOutput(comprador2, "broadcast encerramento primeiro leilao", "Valor final: R$1500.00", Duration.ofSeconds(8));

        cadastrarItem(admin, "Cavalo Arabe", "2000");
        darLance(comprador1, "2500");
        darLance(comprador2, "3000");
        encerrarLeilao(admin);

        assertProcessOutput(admin, "encerramento segundo leilao", "Vencedor: comprador2", Duration.ofSeconds(8));

        consultarHistorico(admin);
        assertProcessOutput(admin, "historico com primeiro leilao", "Item: Vaca Holstein", Duration.ofSeconds(8));
        assertProcessOutput(admin, "historico com segundo leilao", "Item: Cavalo Arabe", Duration.ofSeconds(8));
    }

    private ManagedProcess iniciarCliente(String nome, String usuario, String senha) throws Exception {
        System.out.println("[INFO] Iniciando ClientMain para " + nome + "...");
        ManagedProcess cliente = ManagedProcess.start(
                nome,
                new ProcessBuilder(javaCommand(), "-cp", classpathAtual(), "ClientMain"),
                ROOT
        );

        cliente.sendLine("local");
        pausar(200);
        cliente.sendLine("");
        pausar(200);
        cliente.sendLine(usuario);
        pausar(200);
        cliente.sendLine(senha);

        return cliente;
    }

    private void cadastrarItem(ManagedProcess admin, String nomeItem, String valorInicial) throws Exception {
        System.out.println("[PASSO] Admin cadastrando item " + nomeItem);
        admin.sendLine("1");
        pausar(250);
        admin.sendLine(nomeItem);
        pausar(250);
        admin.sendLine(valorInicial);
        assertProcessOutput(admin, "confirmacao cadastro " + nomeItem, "Item cadastrado com sucesso", Duration.ofSeconds(5));
    }

    private void darLance(ManagedProcess comprador, String valor) throws IOException, InterruptedException {
        System.out.println("[PASSO] " + comprador.name + " ofertando R$" + valor);
        comprador.sendLine("1");
        pausar(250);
        comprador.sendLine(valor);
    }

    private void consultarStatus(ManagedProcess processo) throws IOException {
        System.out.println("[PASSO] Consultando status em " + processo.name);
        processo.sendLine("2");
    }

    private void consultarHistorico(ManagedProcess processo) throws IOException {
        System.out.println("[PASSO] Consultando historico em " + processo.name);
        processo.sendLine("3");
    }

    private void consultarMeusLances(ManagedProcess processo) throws IOException {
        System.out.println("[PASSO] Consultando meus lances em " + processo.name);
        processo.sendLine("3");
    }

    private void encerrarLeilao(ManagedProcess admin) throws IOException, InterruptedException {
        System.out.println("[PASSO] Admin encerrando leilao");
        admin.sendLine("4");
        pausar(250);
        admin.sendLine("S");
    }

    private void encerrarCliente(ManagedProcess cliente) throws IOException, InterruptedException {
        if (cliente == null || !cliente.isAlive()) {
            return;
        }
        System.out.println("[PASSO] Encerrando cliente " + cliente.name);
        cliente.sendLine("5");
        pausar(250);
        cliente.sendLine("S");
        cliente.waitForExit(Duration.ofSeconds(5));
    }

    private void enviarComandoServidor(ManagedProcess server, String comando) throws IOException {
        System.out.println("[PASSO] Servidor executando comando: " + comando);
        server.sendLine(comando);
    }

    private void assertProcessOutput(ManagedProcess processo, String cenario, String trecho, Duration timeout) throws Exception {
        if (processo.waitForOutput(trecho, timeout)) {
            registrarSucesso(cenario);
            return;
        }
        throw new Exception("Nao encontrou '" + trecho + "' na saida de " + processo.name);
    }

    private void assertContains(String cenario, String valor, String esperado) throws Exception {
        if (valor != null && valor.contains(esperado)) {
            registrarSucesso(cenario);
            return;
        }
        if (valor != null && normalizar(valor).contains(normalizar(esperado))) {
            registrarSucesso(cenario);
            return;
        }
        throw new Exception("Esperado conter '" + esperado + "', mas veio: " + valor);
    }

    private void registrarSucesso(String descricao) {
        sucessos++;
        System.out.println("[OK] " + descricao);
    }

    private void registrarFalha(String descricao, String detalhe) {
        falhas++;
        System.out.println("[FALHA] " + descricao + " -> " + detalhe);
    }

    private void aguardarPorta() throws Exception {
        Instant limite = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(limite)) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(HOST, PORT), 500);
                return;
            } catch (IOException e) {
                pausar(250);
            }
        }
        throw new Exception("Servidor nao abriu a porta " + PORT + " a tempo");
    }

    private byte[] lerHistoricoOriginal() {
        try {
            if (Files.exists(HISTORICO)) {
                return Files.readAllBytes(HISTORICO);
            }
        } catch (IOException e) {
            System.out.println("[WARN] Nao foi possivel ler o historico original: " + e.getMessage());
        }
        return null;
    }

    private void preservarEReporHistorico(byte[] historicoOriginal) {
        try {
            if (Files.exists(HISTORICO)) {
                Files.copy(HISTORICO, HISTORICO_TESTE, StandardCopyOption.REPLACE_EXISTING);
            }

            if (historicoOriginal != null) {
                Files.write(HISTORICO, historicoOriginal);
            }
        } catch (IOException e) {
            System.out.println("[WARN] Nao foi possivel preservar/restaurar o historico: " + e.getMessage());
        }
    }

    private void encerrarSilenciosamente(ManagedProcess processo) {
        if (processo == null) {
            return;
        }
        try {
            if (processo.isAlive()) {
                processo.destroy();
                processo.waitForExit(Duration.ofSeconds(3));
            }
        } catch (Exception ignored) {
        }
        processo.destroyForcibly();
    }

    private void encerrarServidorSilenciosamente(ManagedProcess server) {
        if (server == null) {
            return;
        }
        try {
            if (server.isAlive()) {
                server.sendLine("sair");
                server.waitForExit(Duration.ofSeconds(5));
            }
        } catch (Exception ignored) {
        }
        server.destroyForcibly();
    }

    private static String javaCommand() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    private static String classpathAtual() {
        return System.getProperty("java.class.path");
    }

    private static void pausar(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    private static String normalizar(String valor) {
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcento.replace(',', '.').toLowerCase(Locale.ROOT);
    }

    private static class ProtocolSession implements Closeable {
        private final Socket socket = new Socket();
        private BufferedReader in;
        private BufferedWriter out;

        void connect() throws IOException {
            socket.connect(new InetSocketAddress(HOST, PORT), 2000);
            socket.setSoTimeout(4000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void send(String comando) throws IOException {
            out.write(comando);
            out.newLine();
            out.flush();
        }

        String readLine() throws IOException {
            return in.readLine();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    private static class ManagedProcess {
        private final String name;
        private final Process process;
        private final BufferedWriter stdin;
        private final List<String> output = new ArrayList<>();
        private final Thread outputThread;

        private ManagedProcess(String name, Process process) {
            this.name = name;
            this.process = process;
            this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            this.outputThread = new Thread(this::pumpOutput, "pump-" + name.toLowerCase(Locale.ROOT));
            this.outputThread.setDaemon(true);
            this.outputThread.start();
        }

        static ManagedProcess start(String name, ProcessBuilder builder, Path workdir) throws IOException {
            builder.directory(workdir.toFile());
            builder.redirectErrorStream(true);
            return new ManagedProcess(name, builder.start());
        }

        void sendLine(String line) throws IOException {
            stdin.write(line);
            stdin.newLine();
            stdin.flush();
        }

        boolean waitForOutput(String trecho, Duration timeout) throws InterruptedException {
            String trechoNormalizado = normalizar(trecho);
            Instant limite = Instant.now().plus(timeout);
            while (Instant.now().isBefore(limite)) {
                synchronized (output) {
                    for (String linha : output) {
                        if (linha.contains(trecho) || normalizar(linha).contains(trechoNormalizado)) {
                            return true;
                        }
                    }
                }

                if (!process.isAlive()) {
                    synchronized (output) {
                        for (String linha : output) {
                            if (linha.contains(trecho) || normalizar(linha).contains(trechoNormalizado)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

                Thread.sleep(150);
            }
            return false;
        }

        boolean isAlive() {
            return process.isAlive();
        }

        void waitForExit(Duration timeout) throws InterruptedException {
            process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        void destroy() {
            process.destroy();
        }

        void destroyForcibly() {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }

        private void pumpOutput() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (output) {
                        output.add(line);
                    }
                    System.out.println("[" + name + "] " + line);
                }
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                System.out.println("[" + name + "] erro ao ler saida: " + e.getMessage());
            }
        }
    }
}
