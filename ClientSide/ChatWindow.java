package ClientSide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ChatWindow {
    private static final Map<String, ChatWindow> instances = new HashMap<>();

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;

    private String sender;
    private String receiver;

    private ClientMain client;

    ChatWindow(String sender, String receiver, ClientMain client) {
        this.client = client;
        this.sender = sender;
        this.receiver = receiver;

        String title = "Chat: " + sender + " -> " + receiver;
        // Frame instellen
        frame = new JFrame(title);
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // JTextArea voor de berichten
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Paneel voor invoer
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());

        inputField = new JTextField();
        sendButton = new JButton("SEND");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Actie voor de SEND-knop
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText().trim();
                if (!message.isEmpty()) {
                    client.sendMessage(sender, receiver, message);
                    addMessage(sender + ": " + message);
                    inputField.setText("");
                }
            }
        });

        frame.setVisible(true);
    }

    public void receiveMessage(String message) {
        addMessage(receiver + ": " + message);
    }

    public static synchronized ChatWindow getInstance(String sender, String receiver, ClientMain client) {
        String key = sender + "->" + receiver;
        if (!instances.containsKey(key)) {
            instances.put(key, new ChatWindow(sender, receiver, client));
        }
        return instances.get(key);
    }

    public void addMessage(String message) {
        chatArea.append(message + "\n");
    }
}

interface ClientMain {
    void sendMessage(String sender, String receiver, String message);
}
