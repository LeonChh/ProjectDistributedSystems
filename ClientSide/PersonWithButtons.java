package ClientSide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

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

    public PersonWithButtons(String name, ActionListener addListener, ActionListener removeListener, ListElementTypes type) {
        this.name = name;
        if (type == ListElementTypes.ADD) {
            this.buttonOne = new JButton("+");
            this.buttonTwo = new JButton("X");
        } else if (type == ListElementTypes.ACCEPT) {
            this.buttonOne = new JButton("✓");
            this.buttonTwo = new JButton("X");
        } else if (type == ListElementTypes.CHAT) {
            this.buttonOne = new JButton("Chat");
        }

        this.type = type;

        this.buttonOne.addActionListener(addListener);
        if (type != ListElementTypes.CHAT){
            this.buttonTwo.addActionListener(removeListener);
        }
    }

    public String getName() {
        return name;
    }

    public JButton getButtonOne() {
        return buttonOne;
    }

    public JButton getButtonTwo() {
        if (type == ListElementTypes.CHAT) {
            return null;
        }
        return buttonTwo;
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel(name));  // Voeg de naam toe
        panel.add(buttonOne);         // Voeg de "+" knop toe
        if (type != ListElementTypes.CHAT) {
            panel.add(buttonTwo);      // Voeg de "X" knop toe
        }
        return panel;
    }
}
