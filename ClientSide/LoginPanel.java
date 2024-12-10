package ClientSide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginPanel extends JPanel {

    private JTextField usernameField;
    private JButton loginButton;
    private ClientMain clientMain; // ClientMain instantie
    private JFrame frame; // JFrame instantie

    public LoginPanel(ClientMain clientMain, JFrame frame) {
        this.clientMain = clientMain;
        this.frame = frame;

        // Layout instellen voor het inlogpaneel
        setLayout(new BorderLayout());

        // Panel voor de invoer van de gebruikersnaam
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());

        JLabel usernameLabel = new JLabel("Enter Username: ");
        usernameField = new JTextField(20);
        loginButton = new JButton("Login");

        // Voeg componenten toe aan het input panel
        inputPanel.add(usernameLabel);
        inputPanel.add(usernameField);
        inputPanel.add(loginButton);

        // Voeg input panel toe aan het hoofdpaneel
        add(inputPanel, BorderLayout.CENTER);

        // Wanneer de gebruiker op de login-knop klikt
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    login();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    // Login logica
    private void login() throws Exception {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            clientMain.setUsername(username); // Zet de username in ClientMain
            clientMain.login(frame); // Roep de login methode aan in ClientMain met het frame als parameter
        }
    }
}
