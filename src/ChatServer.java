import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private static JTextArea textArea;

    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("Chat Server");
        textArea = new JTextArea();
        textArea.setEditable(false);
        frame.add(new JScrollPane(textArea));
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        ServerSocket serverSocket = new ServerSocket(PORT);
        textArea.append("Server started on port " + PORT + "\n");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            clients.add(handler);
            handler.start();
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private static Map<String, ClientHandler> userMap = new HashMap<>();

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // LOGIN
                while (true) {
                    out.println("LOGIN");
                    username = in.readLine();
                    String password = in.readLine();
                    if (authenticate(username, password)) {
                        synchronized (userMap) {
                            if (!userMap.containsKey(username)) {
                                userMap.put(username, this);
                                break;
                            } else {
                                out.println("Username already logged in.");
                            }
                        }
                    } else {
                        out.println("Invalid credentials.");
                    }
                }

                out.println("Welcome " + username + "!");
                broadcast(username + " has joined the chat", null);

                String msg;
                while ((msg = in.readLine()) != null) {
                    msg = decodeEmoji(msg);
                    if (msg.startsWith("/msg ")) {
                        handlePrivateMessage(msg);
                    } else {
                        broadcast(username + ": " + msg, this);
                    }
                }

            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {}
                clients.remove(this);
                userMap.remove(username);
                broadcast(username + " has left the chat", null);
            }
        }

        private boolean authenticate(String user, String pass) {
            try (BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\singh\\IdeaProjects\\ChatApp\\src\\users.txt"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2 && parts[0].equals(user) && parts[1].equals(pass)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading users.txt: " + e.getMessage());
            }
            return false;
        }


        private void handlePrivateMessage(String msg) {
            String[] parts = msg.split(" ", 3);
            if (parts.length < 3) {
                out.println("Usage: /msg <username> <message>");
                return;
            }
            String target = parts[1];
            String privateMsg = parts[2];

            ClientHandler recipient = userMap.get(target);
            if (recipient != null) {
                recipient.out.println("[Private] " + username + ": " + privateMsg);
                out.println("[To " + target + "] " + privateMsg);
            } else {
                out.println("User not found or not online.");
            }

        }

        private void broadcast(String message, ClientHandler exclude) {
            textArea.append(message + "\n");
            for (ClientHandler client : userMap.values()) {
                client.out.println(message);

            }
        }

        private String decodeEmoji(String msg) {
            return msg
                    .replace(":)", "😊")
                    .replace(":D", "😄")
                    .replace("<3", "❤️")
                    .replace(":(", "😢");
        }
    }
}
