package ClientSide;

import javax.swing.*;
import java.awt.*;

public class GUI extends JPanel {

    private JTextArea messageArea;
    private JTextField messageField;
    private JButton sendButton;
    private String username;

    public GUI(String username) {
        this.username = username;

        // Layout instellen
        setLayout(new BorderLayout());

        // Message Area (Chat history)
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        // Message Field (Text input)
        messageField = new JTextField();

        // Send Button
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        // Voeg componenten toe aan het paneel
        add(scrollPane, BorderLayout.CENTER);
        add(messageField, BorderLayout.SOUTH);
        add(sendButton, BorderLayout.EAST);
    }

    // Methode om het bericht te verzenden
    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            messageArea.append(username + ": " + message + "\n");
            messageField.setText(""); // Maak het invoerveld leeg

            // Hier zou je de code toevoegen om berichten naar de server te sturen
        }
    }

    // Methode om binnenkomende berichten weer te geven
    public void displayIncomingMessage(String message) {
        messageArea.append("Other: " + message + "\n");
    }
}
