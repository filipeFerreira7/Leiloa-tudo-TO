package model;

import java.util.*;

public class Leilao {
    private final String nomeItem;
    private final double valorInicial;
    private double lanceAtual;
    private String ultimoLanceador;
    private boolean encerrado;
    private final List<String> historicoLances;
    private final Map<String, List<Double>> lancesporCliente;
    private final Map<String, String> nomesPorCliente;

    public Leilao(String nomeItem, double valorInicial) {
        this.nomeItem = nomeItem;
        this.valorInicial = valorInicial;
        this.lanceAtual = valorInicial;
        this.ultimoLanceador = "Nenhum";
        this.encerrado = false;
        this.historicoLances = new ArrayList<>();
        this.lancesporCliente = new HashMap<>();
        this.nomesPorCliente = new HashMap<>();
    }

    public String getNomeItem() {
        return nomeItem;
    }

    public double getValorInicial() {
        return valorInicial;
    }

    public double getLanceAtual() {
        return lanceAtual;
    }

    public String getUltimoLanceador() {
        return ultimoLanceador;
    }

    public boolean isEncerrado() {
        return encerrado;
    }

    public List<String> getHistoricoLances() {
        return new ArrayList<>(historicoLances);
    }

    public synchronized void adicionarLance(String clienteId, double valor, String nomeCliente) {
        if (!encerrado && valor > lanceAtual) {
            lanceAtual = valor;
            ultimoLanceador = nomeCliente;
            
            // Registrar no histórico geral
            historicoLances.add(nomeCliente + ": R$" + String.format("%.2f", valor));
            
            // Rastrear lances por cliente
            lancesporCliente.computeIfAbsent(clienteId, k -> new ArrayList<>()).add(valor);
            nomesPorCliente.put(clienteId, nomeCliente);
        }
    }

    public List<String> getLancesDoCliente(String clienteId) {
        List<String> lances = new ArrayList<>();
        List<Double> valores = lancesporCliente.get(clienteId);
        String nome = nomesPorCliente.getOrDefault(clienteId, clienteId);
        
        if (valores != null) {
            for (Double valor : valores) {
                lances.add(nome + ": R$" + String.format("%.2f", valor));
            }
        }
        
        return lances;
    }

    public void encerrar() {
        this.encerrado = true;
    }
}