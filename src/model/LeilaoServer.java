package model;

import data.Json;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeilaoServer {
    private static final int DEFAULT_PORT = 5000;

    private final int port;
    private ServerSocket serverSocket;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final Map<String, Leilao> leiloes = new ConcurrentHashMap<>();
    private Leilao leilaoAtual;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public LeilaoServer() {
        this(DEFAULT_PORT);
    }

    public LeilaoServer(int port) {
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Servidor de Leilao iniciado na porta " + port);
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
            serverSocket = null;
        }
    }

    public void iniciar() {
        if (!isAtivo()) {
            System.err.println("Servidor nao foi iniciado. Verifique se a porta " + port + " ja esta em uso.");
            return;
        }

        System.out.println("Servidor aguardando conexoes...\n");
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                threadPool.execute(handler);
                System.out.println("Nova conexao aceita. Clientes conectados: " + clients.size());
            } catch (SocketException e) {
                if (serverSocket.isClosed()) {
                    System.out.println("Servidor encerrado.");
                    break;
                }
                System.err.println("Erro ao aceitar conexao: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Erro ao aceitar conexao: " + e.getMessage());
            }
        }
    }

    public boolean isAtivo() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    public int getPorta() {
        return port;
    }

    public synchronized void encerrar() {
        for (ClientHandler client : new ArrayList<>(clients)) {
            client.interrupt();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao encerrar servidor: " + e.getMessage());
        }

        threadPool.shutdownNow();
    }

    public synchronized boolean validarLance(double valor) {
        if (leilaoAtual == null) return false;
        return valor > leilaoAtual.getLanceAtual();
    }

    public synchronized String receberLance(String clienteId, double valor, String nomeCliente) {
        if (leilaoAtual == null) {
            return "LANCE_RECUSADO|Nenhum leilao ativo";
        }

        if (leilaoAtual.isEncerrado()) {
            return "LANCE_RECUSADO|Leilao encerrado";
        }

        if (valor <= leilaoAtual.getLanceAtual()) {
            return "LANCE_RECUSADO|Lance deve ser maior que " + leilaoAtual.getLanceAtual();
        }

        leilaoAtual.adicionarLance(clienteId, valor, nomeCliente);
        double ultimoLance = leilaoAtual.getLanceAtual();
        String ultimoLanceador = leilaoAtual.getUltimoLanceador();
        String mensagem = "NOVO_LANCE|Item: " + leilaoAtual.getNomeItem() + "|" + ultimoLanceador + " ofereceu R$" + String.format("%.2f", ultimoLance);

        notificarTodos(mensagem);
        System.out.println("Lance recebido: " + nomeCliente + " - R$" + String.format("%.2f", valor));

        return "LANCE_ACEITO|" + ultimoLance + "|" + ultimoLanceador;
    }

    public synchronized void cadastrarItem(String nomeItem, double valorInicial) {
        if (leilaoAtual != null && !leilaoAtual.isEncerrado()) {
            System.out.println("Erro: Ja existe um leilao ativo");
            return;
        }

        leilaoAtual = new Leilao(nomeItem, valorInicial);
        leiloes.put(nomeItem, leilaoAtual);
        String mensagem = "NOVO_ITEM|Item: " + nomeItem + "|Valor inicial: R$" + String.format("%.2f", valorInicial);
        notificarTodos(mensagem);
        System.out.println("Item cadastrado: " + nomeItem + " - R$" + String.format("%.2f", valorInicial));
    }

    public synchronized String encerrarLeilao() {
        if (leilaoAtual == null) {
            return "ERRO|Nenhum leilao ativo";
        }

        leilaoAtual.encerrar();
        String vencedor = leilaoAtual.getUltimoLanceador();
        double valorFinal = leilaoAtual.getLanceAtual();
        String resultado = "LEILAO_ENCERRADO|Item: " + leilaoAtual.getNomeItem()
                + "|Vencedor: " + vencedor + "|Valor final: R$" + String.format("%.2f", valorFinal);

        notificarTodos(resultado);
        salvarHistorico(leilaoAtual);
        System.out.println("Leilao encerrado - Vencedor: " + vencedor + " - R$" + String.format("%.2f", valorFinal));

        Leilao leilaoEncerrado = leilaoAtual;
        leilaoAtual = null;

        return "LEILAO_ENCERRADO|" + leilaoEncerrado.getNomeItem() + "|" + vencedor + "|" + valorFinal;
    }

    public synchronized Leilao getLeilaoAtual() {
        return leilaoAtual;
    }

    public synchronized String getStatusLeilao() {
        if (leilaoAtual == null || leilaoAtual.isEncerrado()) {
            return "STATUS|Nenhum leilao ativo no momento";
        }
        return "STATUS|Item: " + leilaoAtual.getNomeItem()
                + "|Lance atual: R$" + String.format("%.2f", leilaoAtual.getLanceAtual())
                + "|Ultimo lanceador: " + leilaoAtual.getUltimoLanceador()
                + "|Total de lances: " + leilaoAtual.getHistoricoLances().size();
    }

    public synchronized String getHistoricoLeiloesJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("HISTORICO_LEILOES|");

        int i = 0;
        for (Map.Entry<String, Leilao> entry : leiloes.entrySet()) {
            Leilao l = entry.getValue();
            if (i > 0) sb.append(":::");
            sb.append(l.getNomeItem()).append("|")
                    .append(String.format("%.2f", l.getValorInicial())).append("|")
                    .append(String.format("%.2f", l.getLanceAtual())).append("|")
                    .append(l.getUltimoLanceador()).append("|")
                    .append(l.getHistoricoLances().size());
            i++;
        }

        return sb.toString();
    }

    public synchronized String getLancesDoCliente(String clienteId) {
        if (leilaoAtual == null) {
            return "LANCES_CLIENTE|Nenhum leilao ativo";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("LANCES_CLIENTE|");

        List<String> lances = leilaoAtual.getLancesDoCliente(clienteId);
        if (lances.isEmpty()) {
            sb.append("Nenhum lance registrado");
        } else {
            for (String lance : lances) {
                sb.append(lance).append(":::");
            }
        }

        return sb.toString();
    }

    public void notificarTodos(String mensagem) {
        for (ClientHandler client : clients) {
            client.enviarMensagem(mensagem);
        }
    }

    public void removerCliente(ClientHandler client) {
        clients.remove(client);
        System.out.println("Cliente desconectado: " + client.getClienteId() + ". Total: " + clients.size());
    }

    private synchronized void salvarHistorico(Leilao leilao) {
        try {
            String nomeArquivo = "historico_leiloes.json";
            Json historico = new Json(nomeArquivo);

            Map<String, Object> leilaoJson = new LinkedHashMap<>();
            leilaoJson.put("nomeItem", leilao.getNomeItem());
            leilaoJson.put("valorInicial", String.format("%.2f", leilao.getValorInicial()));
            leilaoJson.put("vencedor", leilao.getUltimoLanceador());
            leilaoJson.put("valorFinal", String.format("%.2f", leilao.getLanceAtual()));
            leilaoJson.put("totalLances", leilao.getHistoricoLances().size());
            leilaoJson.put("lances", leilao.getHistoricoLances());
            leilaoJson.put("dataEncerramento", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));

            historico.adicionarLeilao(leilaoJson);
            historico.salvar();

            System.out.println("Historico salvo em " + nomeArquivo);
        } catch (IOException e) {
            System.err.println("Erro ao salvar historico: " + e.getMessage());
        }
    }

    public int getClientesConectados() {
        return clients.size();
    }
}
