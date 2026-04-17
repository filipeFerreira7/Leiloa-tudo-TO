package model;

import java.io.*;
import java.net.*;
import java.util.*;

public class LeilaoClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private String usuario;
    private String tipoUsuario;
    private boolean conectado = false;
    private Thread receptor;

    public LeilaoClient() {
        scanner = new Scanner(System.in);
    }

    public void conectar(String host, int porta, String usuario, String senha) {
        try {
            socket = new Socket(host, porta);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.usuario = usuario;

            out.println("LOGIN|" + usuario + "|" + senha);
            out.flush();
            String resposta = in.readLine();

            if (resposta != null && resposta.startsWith("LOGIN_SUCESSO")) {
                String[] partes = resposta.split("\\|");
                if (partes.length >= 2) {
                    tipoUsuario = partes[1];
                }
                conectado = true;
                System.out.println("\n✓ Conexão estabelecida com sucesso!");
                System.out.println("  Usuário: " + usuario);
                System.out.println("  Tipo: " + tipoUsuario + "\n");
                
                iniciarReceptor();
                
                if ("ADMIN".equals(tipoUsuario)) {
                    menuAdmin();
                } else {
                    menuComprador();
                }
            } else {
                System.out.println("✗ Erro na autenticação. Verifique credenciais.");
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
            System.err.println("✗ Erro ao conectar: " + e.getMessage());
        }
    }

    private void iniciarReceptor() {
        receptor = new Thread(() -> {
            try {
                String mensagem;
                while (conectado && (mensagem = in.readLine()) != null) {
                    processarMensagem(mensagem);
                }
            } catch (IOException e) {
                // Desconectado normalmente
            }
        });
        receptor.setDaemon(true);
        receptor.start();
    }

    private void processarMensagem(String mensagem) {
        if (mensagem == null || mensagem.trim().isEmpty()) return;
        
        String[] partes = mensagem.split("\\|", 2);
        String comando = partes[0];

        switch (comando) {
            case "NOVO_LANCE":
                if (partes.length >= 2) {
                    System.out.println("\n" + construirSeparador(50));
                    System.out.println("🔔 NOVO LANCE: " + partes[1]);
                    System.out.println(construirSeparador(50));
                    System.out.print("\n> ");
                }
                break;

            case "NOVO_ITEM":
                if (partes.length >= 2) {
                    System.out.println("\n" + construirSeparador(50));
                    System.out.println("📢 NOVO ITEM: " + partes[1]);
                    System.out.println(construirSeparador(50));
                    System.out.print("\n> ");
                }
                break;

            case "LEILAO_ENCERRADO":
                if (partes.length >= 2) {
                    System.out.println("\n" + construirSeparador(50));
                    System.out.println("🏁 LEILÃO ENCERRADO!");
                    System.out.println(partes[1]);
                    System.out.println(construirSeparador(50));
                    System.out.print("\n> ");
                }
                break;

            case "STATUS":
                System.out.println("\n" + construirSeparador(50));
                System.out.println(partes[1]);
                System.out.println(construirSeparador(50));
                System.out.print("\n> ");
                break;

            case "HISTORICO_LEILOES":
                if (partes.length >= 2) {
                    System.out.println("\n" + construirSeparador(60));
                    System.out.println("📋 HISTÓRICO DE LEILÕES");
                    System.out.println(construirSeparador(60));
                    if (partes[1].contains(":::")) {
                        String[] leiloes = partes[1].split(":::");
                        for (String leilao : leiloes) {
                            if (!leilao.isEmpty()) {
                                String[] info = leilao.split("\\|");
                                if (info.length >= 5) {
                                    System.out.printf("Item: %s | Inicial: %s | Final: %s | Vencedor: %s | Lances: %s\n",
                                            info[0], info[1], info[2], info[3], info[4]);
                                }
                            }
                        }
                    } else {
                        System.out.println(partes[1]);
                    }
                    System.out.println(construirSeparador(60));
                    System.out.print("\n> ");
                }
                break;

            case "LANCES_CLIENTE":
                if (partes.length >= 2) {
                    System.out.println("\n" + construirSeparador(50));
                    System.out.println("💰 MEUS LANCES");
                    System.out.println(construirSeparador(50));
                    if (partes[1].contains(":::")) {
                        String[] lances = partes[1].split(":::");
                        for (String lance : lances) {
                            if (!lance.isEmpty()) {
                                System.out.println("  " + lance);
                            }
                        }
                    } else {
                        System.out.println(partes[1]);
                    }
                    System.out.println(construirSeparador(50));
                    System.out.print("\n> ");
                }
                break;

            case "LANCE_ACEITO":
                System.out.println("✓ Lance aceito!");
                break;

            case "LANCE_RECUSADO":
                System.out.println("✗ Lance recusado: " + (partes.length > 1 ? partes[1] : "Motivo desconhecido"));
                break;

            case "ITEM_CADASTRADO":
                System.out.println("✓ Item cadastrado com sucesso!");
                break;

            case "ERRO":
                System.out.println("✗ Erro: " + (partes.length > 1 ? partes[1] : "Erro desconhecido"));
                break;

            default:
                if (!comando.isEmpty()) {
                    System.out.println("\n> " + mensagem);
                }
        }
    }

    private void menuAdmin() {
        System.out.println("╔════════════════════════════════╗");
        System.out.println("║  PAINEL DE ADMINISTRAÇÃO       ║");
        System.out.println("╚════════════════════════════════╝");
        mostrarMenuAdmin();

        while (conectado) {
            System.out.print("\n> ");
            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1":
                    cadastrarItem();
                    break;
                case "2":
                    out.println("STATUS");
                    break;
                case "3":
                    out.println("HISTORICO");
                    break;
                case "4":
                    encerrarLeilao();
                    break;
                case "5":
                    desconectar();
                    break;
                default:
                    System.out.println("✗ Opção inválida.");
                    mostrarMenuAdmin();
            }
        }
    }

    private void mostrarMenuAdmin() {
        System.out.println("\n╔════════════════════════════════╗");
        System.out.println("║ MENU ADMINISTRATIVO            ║");
        System.out.println("║ 1. Cadastrar Item              ║");
        System.out.println("║ 2. Ver Status do Leilão        ║");
        System.out.println("║ 3. Histórico de Leilões        ║");
        System.out.println("║ 4. Encerrar Leilão             ║");
        System.out.println("║ 5. Sair                        ║");
        System.out.println("╚════════════════════════════════╝");
    }

    private void menuComprador() {
        System.out.println("╔════════════════════════════════╗");
        System.out.println("║  PAINEL DO COMPRADOR           ║");
        System.out.println("╚════════════════════════════════╝");
        mostrarMenuComprador();

        while (conectado) {
            System.out.print("\n> ");
            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1":
                    darLance();
                    break;
                case "2":
                    out.println("STATUS");
                    break;
                case "3":
                    out.println("LANCES_CLIENTE");
                    break;
                case "4":
                    out.println("HISTORICO");
                    break;
                case "5":
                    desconectar();
                    break;
                default:
                    System.out.println("✗ Opção inválida.");
                    mostrarMenuComprador();
            }
        }
    }

    private void mostrarMenuComprador() {
        System.out.println("\n╔════════════════════════════════╗");
        System.out.println("║ MENU DO COMPRADOR              ║");
        System.out.println("║ 1. Dar Lance                   ║");
        System.out.println("║ 2. Ver Status do Leilão        ║");
        System.out.println("║ 3. Meus Lances                 ║");
        System.out.println("║ 4. Histórico de Leilões        ║");
        System.out.println("║ 5. Sair                        ║");
        System.out.println("╚════════════════════════════════╝");
    }

    private void cadastrarItem() {
        System.out.print("\nNome do item: ");
        String nome = scanner.nextLine().trim();
        System.out.print("Valor inicial (R$): ");
        try {
            double valor = Double.parseDouble(scanner.nextLine());
            out.println("CADASTRAR_ITEM|" + nome + "|" + valor);
        } catch (NumberFormatException e) {
            System.out.println("✗ Valor inválido.");
        }
    }

    private void darLance() {
        System.out.print("\nValor do lance (R$): ");
        try {
            double valor = Double.parseDouble(scanner.nextLine());
            if (valor <= 0) {
                System.out.println("✗ O valor deve ser positivo.");
                return;
            }
            out.println("LANCE|" + valor);
        } catch (NumberFormatException e) {
            System.out.println("✗ Valor inválido.");
        }
    }

    private void encerrarLeilao() {
        System.out.print("\nTem certeza que deseja encerrar o leilão? (S/N): ");
        String confirm = scanner.nextLine().trim().toUpperCase();
        if ("S".equals(confirm)) {
            out.println("ENCERRAR");
        } else {
            System.out.println("Operação cancelada.");
        }
    }

    private void desconectar() {
        System.out.print("\nTem certeza que deseja desconectar? (S/N): ");
        String confirm = scanner.nextLine().trim().toUpperCase();
        if ("S".equals(confirm)) {
            try {
                conectado = false;
                socket.close();
                System.out.println("\n✓ Desconectado com sucesso.");
                System.exit(0);
            } catch (IOException e) {
                System.err.println("✗ Erro ao desconectar: " + e.getMessage());
            }
        }
    }

    private String construirSeparador(int tamanho) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tamanho; i++) {
            sb.append("═");
        }
        return sb.toString();
    }
}