package model;

import data.Json;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class LeilaoServer {
    private static final int PORT = 5000;

    private ServerSocket serverSocket;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final Map<String, Leilao> leiloes = new ConcurrentHashMap<>();
    private Leilao leilaoAtual;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public LeilaoServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor de Leilão iniciado na porta " + PORT);
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        }
    }

    public void iniciar() {
        System.out.println("Servidor aguardando conexões...\n");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                threadPool.execute(handler);
                System.out.println("Nova conexão aceita. Clientes conectados: " + clients.size());
            } catch (IOException e) {
                System.err.println("Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    public synchronized boolean validarLance(double valor) {
        if (leilaoAtual == null) return false;
        return valor > leilaoAtual.getLanceAtual();
    }

    public synchronized String receberLance(String clienteId, double valor, String nomeCliente) {
        if (leilaoAtual == null) {
            return "LANCE_RECUSADO|Nenhum leilão ativo";
        }
        
        if (leilaoAtual.isEncerrado()) {
            return "LANCE_RECUSADO|Leilão encerrado";
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
            System.out.println("Erro: Já existe um leilão ativo");
            return;
        }
        
        leilaoAtual = new Leilao(nomeItem, valorInicial);
        leiloes.put(nomeItem, leilaoAtual);
        String mensagem = "NOVO_ITEM|Item: " + nomeItem + "|Valor inicial: R$" + String.format("%.2f", valorInicial);
        notificarTodos(mensagem);
        System.out.println("✓ Item cadastrado: " + nomeItem + " - R$" + String.format("%.2f", valorInicial));
    }

    public synchronized String encerrarLeilao() {
        if (leilaoAtual == null) {
            return "ERRO|Nenhum leilão ativo";
        }
        
        leilaoAtual.encerrar();
        String vencedor = leilaoAtual.getUltimoLanceador();
        double valorFinal = leilaoAtual.getLanceAtual();
        String resultado = "LEILAO_ENCERRADO|Item: " + leilaoAtual.getNomeItem() + 
                          "|Vencedor: " + vencedor + "|Valor final: R$" + String.format("%.2f", valorFinal);
        
        notificarTodos(resultado);
        salvarHistorico(leilaoAtual);
        System.out.println("✓ Leilão encerrado - Vencedor: " + vencedor + " - R$" + String.format("%.2f", valorFinal));
        
        Leilao leilaoEncerrado = leilaoAtual;
        leilaoAtual = null;
        
        return "LEILAO_ENCERRADO|" + leilaoEncerrado.getNomeItem() + "|" + vencedor + "|" + valorFinal;
    }

    public synchronized Leilao getLeilaoAtual() {
        return leilaoAtual;
    }

    public synchronized String getStatusLeilao() {
        if (leilaoAtual == null || leilaoAtual.isEncerrado()) {
            return "STATUS|Nenhum leilão ativo no momento";
        }
        return "STATUS|Item: " + leilaoAtual.getNomeItem() + 
               "|Lance atual: R$" + String.format("%.2f", leilaoAtual.getLanceAtual()) + 
               "|Último lanceador: " + leilaoAtual.getUltimoLanceador() +
               "|Total de lances: " + leilaoAtual.getHistoricoLances().size();
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
            return "LANCES_CLIENTE|Nenhum leilão ativo";
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

            System.out.println("✓ Histórico salvo em " + nomeArquivo);
        } catch (IOException e) {
            System.err.println("Erro ao salvar histórico: " + e.getMessage());
        }
    }

    public int getClientesConectados() {
        return clients.size();
    }
}