import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatClient {
    private String serverIP = "localhost";
    private int serverPort = 12345;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JFrame frame = new JFrame("Java Chat");
    private JTextArea chatArea = new JTextArea();
    private JTextField inputField = new JTextField();
    private String userName;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ChatClient().start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void start() throws IOException {
        socket = new Socket(serverIP, serverPort);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Handle login loop
        while (true) {
            String line = in.readLine();
            if (line == null) {
                JOptionPane.showMessageDialog(frame, "Connection closed by server.");
                return;
            }

            if (line.equals("LOGIN")) {
                JTextField userField = new JTextField();
                JPasswordField passField = new JPasswordField();
                Object[] fields = {
                        "Username:", userField,
                        "Password:", passField
                };

                int option = JOptionPane.showConfirmDialog(
                        frame, fields, "Login", JOptionPane.OK_CANCEL_OPTION);

                if (option != JOptionPane.OK_OPTION) {
                    return;
                }

                out.println(userField.getText());
                out.println(new String(passField.getPassword()));

                String result = in.readLine();
                if (result == null) {
                    JOptionPane.showMessageDialog(frame, "Login failed. Server disconnected.");
                    return;
                }

                if (result.startsWith("Welcome")) {
                    userName = userField.getText(); // store logged-in username
                    break; // Login successful
                } else {
                    JOptionPane.showMessageDialog(frame, result);
                }
            }
        }

        // ✅ Now initialize the UI after successful login
        setupUI();
        chatArea.append("Connected as " + userName + "\n");

        // ✅ Start message listener thread
        new MessageReceiver().start();

        // ✅ Send message on Enter key
        inputField.addActionListener(e -> {
            String msg = inputField.getText().trim();
            if (!msg.isEmpty()) {
                out.println(msg);
                inputField.setText("");
            }
        });
    }

    private void setupUI() {
        frame.setLayout(new BorderLayout());
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));

        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.add(inputField, BorderLayout.SOUTH);
        frame.setSize(400, 400);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    class MessageReceiver extends Thread {
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    final String message = msg;
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(message + "\n");
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("Connection to server lost.\n");
                });
            }
        }
    }

}
