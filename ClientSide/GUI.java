package ClientSide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class GUI extends JPanel {
    private JTextArea messageArea;
    private JTextField messageField;
    private JButton sendButton;
    private String username;
    private ClientMain client;
    private JsonHandler jsonHandler;

    // Panels voor de kolommen
    private JPanel notificationsPanel;
    private JPanel friendsPanel;
    private JPanel requestsPanel;
    private JPanel newPeoplePanel;

    private static int panelHight = 600;
    private static int panelWidth = 1200;

    // Maak de ActionListeners voor de knoppen
    private ActionListener addListenerAdd = e -> {
        try {
            addNewFriend(((JButton) e.getSource()).getClientProperty("userName").toString());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    };
    private ActionListener removeListenerNotAdd = e -> {
        try {
            removeNewPerson(((JButton) e.getSource()).getClientProperty("userName").toString());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    };
    private ActionListener acceptListener = e -> {
        try {
            acceptRequest(((JButton) e.getSource()).getClientProperty("userName").toString());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    };
    private ActionListener declineListener = e -> {
        try {
            declineRequest(((JButton) e.getSource()).getClientProperty("userName").toString());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    };
    private ActionListener chatListener = e -> {
        try {
            startChat(((JButton) e.getSource()).getClientProperty("userName").toString());
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    };

    public GUI(String username, ClientMain client) {
        this.username = username;
        this.client = client;
        String filename = "ClientSide/jsonFiles/" + username + ".json";
        jsonHandler = new JsonHandler(filename);

        // Layout instellen
        setLayout(new GridLayout(1, 4)); // aantal rijen, aantal kolommen
        // Initialiseer de verschillende panelen
        notificationsPanel = createPanel("Notifications", "↻");
        friendsPanel = createPanel("Friends", "↻");
        requestsPanel = createPanel("Requests", "↻");
        newPeoplePanel = createPanel("New People", "↻");

        // Voeg de panelen toe aan de GUI
        add(notificationsPanel);
        add(friendsPanel);
        add(requestsPanel);
        add(newPeoplePanel);
    }

    // Maak een panel met een titel en een refresh-knop
    private JPanel createPanel(String title, String buttonLabel) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Maak een paneel voor de titel en de knop
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Voeg de titel toe
        JLabel label = new JLabel(title);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(label);

        // Voeg de refresh knop toe
        JButton refreshButton = new JButton(buttonLabel);
        refreshButton.setFont(new Font("Segoe UI Symbol", Font.BOLD, 30)); // Pas de fontgrootte aan
        refreshButton.setContentAreaFilled(false);  // Zorg ervoor dat de achtergrond transparant is
        refreshButton.setBorderPainted(false);  // Verwijder de rand van de knop
        refreshButton.setMargin(new Insets(0, 0, 0, 0));

        refreshButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                refreshButton.setBackground(new Color(220, 220, 220)); // Rgb-waarde voor lichtgrijs
                refreshButton.setContentAreaFilled(true);
            }
        
            public void mouseExited(java.awt.event.MouseEvent evt) {
                refreshButton.setBackground(UIManager.getColor("control"));
                refreshButton.setContentAreaFilled(false);
            }
        });

        refreshButton.addActionListener(e -> {
            try {
                refreshPanel(title);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }); // Verbind met de refresh-methode
        

        titlePanel.add(refreshButton);
        // Voeg de titelpanel toe aan het bovenste deel van het paneel
        panel.add(titlePanel, BorderLayout.NORTH);

        if (title.equals("Notifications")) {
            // Voeg een tekstveld toe voor de notificaties
            JTextArea notificationsArea = new JTextArea();
            notificationsArea.setEditable(true);
            panel.add(notificationsArea, BorderLayout.CENTER);

        } else if (title.equals("Friends")) {
            ArrayList<String> friends = jsonHandler.getUserNamesOfList("friends");

            // Maak een NewPeopleList met de nieuwe mensen
            NewPeopleList friendsList = new NewPeopleList(friends, chatListener, null, PersonWithButtons.ListElementTypes.CHAT);

            // Plaats de NewPeopleList in een JScrollPane zodat het scrollbaar is
            JScrollPane scrollPane = new JScrollPane(friendsList);

            // Voeg het scrollPane toe aan het hoofdpaneel
            panel.add(scrollPane, BorderLayout.CENTER);

            // Herteken het panel om te zorgen dat de UI wordt bijgewerkt
            panel.revalidate();
            panel.repaint();

        } else if (title.equals("Requests")) {
            ArrayList<String> requests = jsonHandler.getUserNamesOfList("friendRequests");

            // Maak een NewPeopleList met de nieuwe mensen
            NewPeopleList newPeopleList = new NewPeopleList(requests, acceptListener, declineListener, PersonWithButtons.ListElementTypes.ACCEPT);

            // Plaats de NewPeopleList in een JScrollPane zodat het scrollbaar is
            JScrollPane scrollPane = new JScrollPane(newPeopleList);

            // Voeg het scrollPane toe aan het hoofdpaneel
            panel.add(scrollPane, BorderLayout.CENTER);

            // Herteken het panel om te zorgen dat de UI wordt bijgewerkt
            panel.revalidate();
            panel.repaint();

        } else if (title.equals("New People")) {
            ArrayList<String> newPeople = jsonHandler.getUserNamesOfList("newPeople");

            // Maak een NewPeopleList met de nieuwe mensen
            NewPeopleList newPeopleList = new NewPeopleList(newPeople, addListenerAdd, removeListenerNotAdd, PersonWithButtons.ListElementTypes.ADD);

            // Plaats de NewPeopleList in een JScrollPane zodat het scrollbaar is
            JScrollPane scrollPane = new JScrollPane(newPeopleList);

            // Voeg het scrollPane toe aan het hoofdpaneel
            panel.add(scrollPane, BorderLayout.CENTER);

            // Herteken het panel om te zorgen dat de UI wordt bijgewerkt
            panel.revalidate();
            panel.repaint();

        }

        return panel;
    }

    public void refreshAllPanels() {
        try {
            refreshPanel("Friends");
            refreshPanel("Requests");
            refreshPanel("New People");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Methode voor de refresh actie
    private void refreshPanel(String panelTitle) throws Exception {
        if (panelTitle.equals("Notifications")) {
            System.out.println("No new notifications");;
        } else if (panelTitle.equals("Friends")) {
            ArrayList<String> friends = jsonHandler.getUserNamesOfList("friends");  // Verkrijg de nieuwe mensen

            // Haal het newPeoplePanel op, index 3 is de 4de kolom
            JPanel friendsPanel = (JPanel) getComponent(1); 

            // Haal de bestaande JScrollPane op die de JList bevat
            JScrollPane scrollPaneFriends = (JScrollPane) friendsPanel.getComponent(1);  // Veronderstel dat de JScrollPane op index 1 staat
            NewPeopleList friendsList = (NewPeopleList) scrollPaneFriends.getViewport().getView();  // Verkrijg de NewPeopleList
            friendsList.setNewPeopleList(friends, chatListener, null, PersonWithButtons.ListElementTypes.CHAT);  // Werk de JList bij

            scrollPaneFriends.revalidate();  // Herteken de JScrollPane
            scrollPaneFriends.repaint();     // Herverf de JScrollPane

        } else if (panelTitle.equals("Requests")) {
            ArrayList<String> requests = jsonHandler.getUserNamesOfList("friendRequests");  // Verkrijg de nieuwe mensen

            // Haal het newPeoplePanel op, index 3 is de 4de kolom
            JPanel requestsPanel = (JPanel) getComponent(2); 

            // Haal de bestaande JScrollPane op die de JList bevat
            JScrollPane scrollPaneRequests = (JScrollPane) requestsPanel.getComponent(1);  // Veronderstel dat de JScrollPane op index 1 staat
            NewPeopleList requestsList = (NewPeopleList) scrollPaneRequests.getViewport().getView();  // Verkrijg de NewPeopleList
            requestsList.setNewPeopleList(requests, acceptListener, declineListener, PersonWithButtons.ListElementTypes.ACCEPT);  // Werk de JList bij

            scrollPaneRequests.revalidate();  // Herteken de JScrollPane
            scrollPaneRequests.repaint();     // Herverf de JScrollPane

        } else if (panelTitle.equals("New People")) {
            // Haal de nieuwe mensen opnieuw op
            client.lookForNewFriends(); // Dit zorgt ervoor dat nieuwe mensen worden opgehaald
            ArrayList<String> newPeople = jsonHandler.getUserNamesOfList("newPeople");  // Verkrijg de nieuwe mensen

            // Haal het newPeoplePanel op, index 3 is de 4de kolom
            JPanel newPeoplePanel = (JPanel) getComponent(3); 

            // Haal de bestaande JScrollPane op die de JList bevat
            JScrollPane scrollPaneNewPeople = (JScrollPane) newPeoplePanel.getComponent(1);  // Veronderstel dat de JScrollPane op index 1 staat
            NewPeopleList newPeopleList = (NewPeopleList) scrollPaneNewPeople.getViewport().getView();  // Verkrijg de NewPeopleList
            newPeopleList.setNewPeopleList(newPeople, addListenerAdd, removeListenerNotAdd, PersonWithButtons.ListElementTypes.ADD);  // Werk de JList bij

            scrollPaneNewPeople.revalidate();  // Herteken de JScrollPane
            scrollPaneNewPeople.repaint();     // Herverf de JScrollPane
        }
    }

    private void addNewFriend(String person) throws Exception {
        sendNotification("Er is een vriendschapsverzoek gestuurd naar " + person);
        client.sendFriendRequest(person);
    }

    private void removeNewPerson(String person) throws Exception {
        sendNotification(person + " is verwijderd uit de lijst van nieuwe mensen");
        client.removePersonOutNewPeople(person);
    }

    private void acceptRequest(String person) throws Exception {
        sendNotification("Vriendschapsverzoek van " + person + " is geaccepteerd");
        client.acceptFriendRequest(person);
    }

    private void declineRequest(String person) throws Exception {
        sendNotification("Vriendschapsverzoek van " + person + " is geweigerd");
        client.declineFriendRequest(person);
    }

    private void startChat(String person) throws Exception {
        sendNotification("Chat gestart met " + person);
        client.startChat(person);
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

    public void sendNotification(String notification) {
        // Zoek het notificationsPanel en de JTextArea binnen dat paneel
        JPanel notificationsPanel = (JPanel) getComponent(0);  // Index 0 is het eerste paneel (Notifications)
        JTextArea notificationsArea = (JTextArea) notificationsPanel.getComponent(1);  // Aannemen dat de JTextArea op index 1 staat in het panel
    
        // Voeg de notificatie toe aan het tekstveld
        notificationsArea.append(notification + "\n");
    
        // Optioneel: Herteken en herverf het notificatiepaneel
        notificationsPanel.revalidate();
        notificationsPanel.repaint();
    }
    

    // Methode om binnenkomende berichten weer te geven
    public void displayIncomingMessage(String message) {
        messageArea.append("Other: " + message + "\n");
    }

    // Main methode om de GUI te testen
    public static void main(String[] args) {
        JFrame frame = new JFrame("Chat GUI");
        // Zorg ervoor dat je ClientMain instantie meegeeft aan de GUI constructor
        ClientMain client = new ClientMain();
        GUI gui = new GUI("User1", client);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(panelWidth, panelHight);
        frame.add(gui);
        frame.setVisible(true);
    }
}
