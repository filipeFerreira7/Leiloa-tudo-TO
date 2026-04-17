import model.LeilaoServer;
import java.util.Scanner;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   SISTEMA DE LEILÃO ELETRÔNICO              ║");
        System.out.println("║   SERVIDOR DE LEILÕES                      ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        LeilaoServer servidor = new LeilaoServer();
        
        // Iniciar servidor em thread separada
        Thread servidorThread = new Thread(() -> servidor.iniciar());
        servidorThread.setDaemon(false);
        servidorThread.start();
        
        // Menu de administração
        Scanner scanner = new Scanner(System.in);
        Thread commandThread = new Thread(() -> {
            System.out.println("\n[Digite 'help' para ver comandos disponíveis]\n");
            while (true) {
                System.out.print("> ");
                String comando = scanner.nextLine().trim().toLowerCase();
                
                switch (comando) {
                    case "help":
                        System.out.println("\nComandos disponíveis:");
                        System.out.println("  status     - Mostra status do servidor");
                        System.out.println("  clientes   - Mostra clientes conectados");
                        System.out.println("  help       - Mostra esta mensagem");
                        System.out.println("  sair       - Encerra o servidor\n");
                        break;
                    
                    case "status":
                        System.out.println("\n☑ Servidor rodando na porta 5000");
                        System.out.println("☑ Clientes conectados: " + servidor.getClientesConectados() + "\n");
                        break;
                    
                    case "clientes":
                        System.out.println("\n☑ Total de clientes conectados: " + servidor.getClientesConectados() + "\n");
                        break;
                    
                    case "sair":
                        System.out.println("\n✓ Encerrando servidor...");
                        System.exit(0);
                        break;
                    
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