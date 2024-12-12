package ClientSide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private int sendIndex;
    private String sendTag;
    private String encodedKeySend;

    private boolean waitingOnNextIndex = false;

    ChatWindow(String sender, String receiver, ClientMain client, int sendIndex, String sendTag, String encodedKeySend) {
        this.client = client;
        this.sender = sender;
        this.receiver = receiver;

        this.sendIndex = sendIndex;
        this.sendTag = sendTag;
        this.encodedKeySend = encodedKeySend;

        String title = "Chat: " + sender + " -> " + receiver;
        // Frame instellen
        frame = new JFrame(title);
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.setLocationRelativeTo(null);

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
                try {
                    sendMessage();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        // Actie voor de ENTER-toets
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    try {
                        sendMessage();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                client.closeChatWith(receiver);
                instances.remove(sender + "->" + receiver); // Optioneel: verwijder de instance
                e.getWindow().dispose();
            }
        });

        frame.setVisible(true);
    }

    private void sendMessage() throws Exception {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            
            String timestamp = new SimpleDateFormat("HH'u'mm").format(new Date());
            String originalSendtag = sendTag;

            sendIndex = client.sendMessage(receiver, message, sendIndex, sendTag, encodedKeySend, this);
            
            if(originalSendtag.equals(sendTag)){
                System.out.println("ERROR: Sendtag is the same -----------------------------------------------------------------------------");
            }


            addMessage(timestamp + " - " + sender + ": " + message);
            inputField.setText("");
            
        }
    }

    public void setSendTag(String sendTag) {
        this.sendTag = sendTag;
    }

    public void setEncodedKeySend(String encodedKeySend) {
        this.encodedKeySend = encodedKeySend;
    }

    public void receiveMessage(String message) {
        String timestamp = new SimpleDateFormat("HH'u'mm").format(new Date());
        addMessage(timestamp + " - " + receiver + ": " + message);
    }

    public static synchronized ChatWindow getInstance(String sender, String receiver, ClientMain client, int sendIndex, String sendTag, String encodedKeySend) {
        String key = sender + "->" + receiver;
        if (!instances.containsKey(key)) {
            instances.put(key, new ChatWindow(sender, receiver, client, sendIndex, sendTag, encodedKeySend));
        }
        return instances.get(key);
    }

    public void addMessage(String message) {
        chatArea.append(message + "\n");
    }

    public void close() {
        frame.dispose();
        instances.remove(sender + "->" + receiver);
    }

    public boolean isVisible() {
        return frame.isVisible();
    }
}

