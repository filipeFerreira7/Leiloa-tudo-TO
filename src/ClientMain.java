import java.util.Scanner;
import model.LeilaoClient;

public class ClientMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║   SISTEMA DE LEILÃO ELETRÔNICO              ║");
        System.out.println("║   CLIENTE DE LEILÕES                       ║");
        System.out.println("╚════════════════════════════════════════════╝\n");
        
        System.out.println("Credenciais disponíveis:");
        System.out.println("  Admin: user='admin', senha='admin123'");
        System.out.println("  Comprador: user='comprador1/2/3', senha='123456'\n");
        
        System.out.print("Host do servidor (Digite 'local' para localhost): ");
        String host = scanner.nextLine().trim();
        
        if (host.equalsIgnoreCase("local")) {
            host = "127.0.0.1";
        }
        
        System.out.print("Porta do servidor (Padrão 5000): ");
        int porta = 5000;
        try {
            String portaStr = scanner.nextLine().trim();
            if (!portaStr.isEmpty()) {
                porta = Integer.parseInt(portaStr);
            }
        } catch (NumberFormatException e) {
            System.out.println("Porta inválida, usando padrão 5000");
        }
        
        System.out.print("Usuário: ");
        String usuario = scanner.nextLine().trim();
        
        if (usuario.isEmpty()) {
            System.out.println("Usuário não pode estar vazio.");
            scanner.close();
            return;
        }
        
        System.out.print("Senha: ");
        String senha = scanner.nextLine().trim();
        
        if (senha.isEmpty()) {
            System.out.println("Senha não pode estar vazia.");
            scanner.close();
            return;
        }

        System.out.println("\n⟳ Conectando ao servidor " + host + ":" + porta + "...\n");
        
        LeilaoClient cliente = new LeilaoClient();
        cliente.conectar(host, porta, usuario, senha);

        scanner.close();
    }
}