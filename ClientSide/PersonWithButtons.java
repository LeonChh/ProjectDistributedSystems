package ClientSide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Type;

public class PersonWithButtons {
    private String name;
    private JButton buttonOne;
    private JButton buttonTwo;
    ListElementTypes type;

    public enum ListElementTypes {
        ADD, 
        ACCEPT,
        CHAT
    }

    public PersonWithButtons(String name, ActionListener listenerButtonOne, ActionListener listenerButtonTwo, ListElementTypes type) {
        this.name = name;
        if (type == ListElementTypes.ADD) {
            this.buttonOne = new JButton("+");
            this.buttonOne.putClientProperty("userName", name);
            this.buttonTwo = new JButton("X");
            this.buttonTwo.putClientProperty("userName", name);
        } else if (type == ListElementTypes.ACCEPT) {
            this.buttonOne = new JButton("✓");
            this.buttonOne.putClientProperty("userName", name);
            this.buttonTwo = new JButton("X");
            this.buttonTwo.putClientProperty("userName", name);
        } else if (type == ListElementTypes.CHAT) {
            this.buttonOne = new JButton("Chat");
            this.buttonOne.putClientProperty("userName", name);
            this.buttonTwo = new JButton("X");
            this.buttonTwo.putClientProperty("userName", name);
        }

        
        this.type = type;

        this.buttonOne.addActionListener(listenerButtonOne);
        this.buttonTwo.addActionListener(listenerButtonTwo);
        
    }

    public String getName() {
        return name;
    }

    public JButton getButtonOne() {
        return buttonOne;
    }

    public JButton getButtonTwo() {
        return buttonTwo;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel(name));  // Voeg de naam toe
        panel.add(buttonOne);         // Voeg de "+" knop toe
        panel.add(buttonTwo);      // Voeg de "X" knop toe
        
        return panel;
    }
}
