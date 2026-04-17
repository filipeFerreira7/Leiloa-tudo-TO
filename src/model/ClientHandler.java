package model;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final LeilaoServer servidor;
    private String clienteId;
    private String tipoUsuario; // "ADMIN" ou "COMPRADOR"
    private boolean autenticado = false;
    private PrintWriter out;

    public ClientHandler(Socket socket, LeilaoServer servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    public void enviarMensagem(String mensagem) {
        if (out != null) {
            out.println(mensagem);
            out.flush();
        }
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String linha;
            while ((linha = in.readLine()) != null) {
                processarMensagem(linha);
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + clienteId);
        } finally {
            servidor.removerCliente(this);
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    private void processarMensagem(String mensagem) {
        if (mensagem == null || mensagem.trim().isEmpty()) return;
        
        String[] partes = mensagem.split("\\|");
        String comando = partes[0];

        switch (comando) {
            case "LOGIN":
                if (partes.length >= 3) {
                    String usuario = partes[1];
                    String senha = partes[2];
                    Map<String, String> auth = autenticar(usuario, senha);
                    if (auth != null) {
                        clienteId = usuario;
                        tipoUsuario = auth.get("tipo");
                        autenticado = true;
                        out.println("LOGIN_SUCESSO|" + tipoUsuario);
                        out.println(servidor.getStatusLeilao());
                        System.out.println("Cliente autenticado: " + usuario + " (" + tipoUsuario + ")");
                    } else {
                        out.println("LOGIN_FALHA|Credenciais inválidas");
                    }
                }
                break;

            case "LANCE":
                if (autenticado && tipoUsuario.equals("COMPRADOR") && partes.length >= 2) {
                    try {
                        double valor = Double.parseDouble(partes[1]);
                        String resultado = servidor.receberLance(clienteId, valor, clienteId);
                        out.println(resultado);
                    } catch (NumberFormatException e) {
                        out.println("LANCE_RECUSADO|Valor inválido");
                    }
                } else if (!tipoUsuario.equals("COMPRADOR")) {
                    out.println("LANCE_RECUSADO|Apenas compradores podem fazer lances");
                }
                break;

            case "CADASTRAR_ITEM":
                if (autenticado && tipoUsuario.equals("ADMIN") && partes.length >= 3) {
                    try {
                        String nomeItem = partes[1];
                        double valorInicial = Double.parseDouble(partes[2]);
                        servidor.cadastrarItem(nomeItem, valorInicial);
                        out.println("ITEM_CADASTRADO");
                    } catch (NumberFormatException e) {
                        out.println("ERRO|Valor inicial inválido");
                    }
                } else if (!tipoUsuario.equals("ADMIN")) {
                    out.println("ERRO|Apenas admin pode cadastrar itens");
                }
                break;

            case "ENCERRAR":
                if (autenticado && tipoUsuario.equals("ADMIN")) {
                    String resultado = servidor.encerrarLeilao();
                    out.println(resultado);
                } else if (!tipoUsuario.equals("ADMIN")) {
                    out.println("ERRO|Apenas admin pode encerrar leilões");
                }
                break;

            case "STATUS":
                out.println(servidor.getStatusLeilao());
                break;

            case "HISTORICO":
                if (autenticado) {
                    out.println(servidor.getHistoricoLeiloesJson());
                }
                break;

            case "LANCES_CLIENTE":
                if (autenticado) {
                    out.println(servidor.getLancesDoCliente(clienteId));
                }
                break;

            default:
                out.println("COMANDO_DESCONHECIDO");
        }
    }

    private Map<String, String> autenticar(String usuario, String senha) {
        Map<String, String> usuarios = new HashMap<>();
        
        // Admin
        if (usuario.equals("admin") && senha.equals("admin123")) {
            Map<String, String> resultado = new HashMap<>();
            resultado.put("tipo", "ADMIN");
            resultado.put("usuario", usuario);
            return resultado;
        }
        
        // Compradores
        if ((usuario.equals("comprador1") && senha.equals("123456")) ||
            (usuario.equals("comprador2") && senha.equals("123456")) ||
            (usuario.equals("comprador3") && senha.equals("123456")) ||
            (usuario.equals("comprador") && senha.equals("123456"))) {
            Map<String, String> resultado = new HashMap<>();
            resultado.put("tipo", "COMPRADOR");
            resultado.put("usuario", usuario);
            return resultado;
        }
        
        return null;
    }
}