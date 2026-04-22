import model.LeilaoServer;

import java.util.Scanner;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   SISTEMA DE LEILÃO ELETRÔNICO            ║");
        System.out.println("║   SERVIDOR DE LEILÕES                     ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        int porta = 5000;
        if (args.length > 0) {
            try {
                porta = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Porta invalida: " + args[0]);
                System.err.println("Uso: java ServerMain [porta]");
                return;
            }
        }

        LeilaoServer servidor = new LeilaoServer(porta);
        if (!servidor.isAtivo()) {
            System.err.println("Não foi possível subir o servidor.");
            System.err.println("Feche o processo que já está usando a porta " + servidor.getPorta() + " e tente novamente.");
            System.err.println("Se preferir, suba em outra porta: java ServerMain 5001");
            return;
        }

        Thread servidorThread = new Thread(servidor::iniciar);
        servidorThread.setDaemon(false);
        servidorThread.start();

        Scanner scanner = new Scanner(System.in);
        Thread commandThread = new Thread(() -> {
            System.out.println("\n[Digite 'help' para ver comandos disponíveis]\n");
            while (servidor.isAtivo()) {
                System.out.print("> ");

                String comando;
                try {
                    comando = scanner.nextLine().trim().toLowerCase();
                } catch (Exception e) {
                    break;
                }

                switch (comando) {
                    case "help":
                        System.out.println("\nComandos disponíveis:");
                        System.out.println("  status     - Mostra status do servidor");
                        System.out.println("  clientes   - Mostra clientes conectados");
                        System.out.println("  help       - Mostra esta mensagem");
                        System.out.println("  sair       - Encerra o servidor\n");
                        break;

                    case "status":
                        System.out.println("\n☑ Servidor rodando na porta " + servidor.getPorta());
                        System.out.println("☑ Clientes conectados: " + servidor.getClientesConectados() + "\n");
                        break;

                    case "clientes":
                        System.out.println("\n☑ Total de clientes conectados: " + servidor.getClientesConectados() + "\n");
                        break;

                    case "sair":
                        System.out.println("\n✓ Encerrando servidor...");
                        servidor.encerrar();
                        return;

                    default:
                        if (!comando.isEmpty()) {
                            System.out.println("✗ Comando desconhecido. Digite 'help' para ajuda.\n");
                        }
                }
            }
        });
        commandThread.setDaemon(true);
        commandThread.start();
    }
}
