package ClientSide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class NewPeopleList extends JPanel {
    private ArrayList<PersonWithButtons> peopleWithButtons;

    public NewPeopleList(ArrayList<String> newPeople, ActionListener listenerButtonOne, ActionListener listenerButtonTwo, PersonWithButtons.ListElementTypes type) {
        peopleWithButtons = new ArrayList<>();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // Verticaal gerangschikt

        // Maak een PersonItem voor elke persoon in de lijst
        for (String person : newPeople) {
            PersonWithButtons personWithButton = new PersonWithButtons(person, listenerButtonOne, listenerButtonTwo, type);
            peopleWithButtons.add(personWithButton); // Voeg de personItem toe aan de lijst
            add(personWithButton.createPanel()); // Voeg het paneel van de persoon toe aan de lijst}
        }
    }

    public void setNewPeopleList(ArrayList<String> newPeople, ActionListener addListener, ActionListener removeListener, PersonWithButtons.ListElementTypes type){
        removeAll(); // Verwijder alle personen uit de lijst
        peopleWithButtons.clear(); // Maak de lijst leeg

        // Maak een PersonItem voor elke persoon in de lijst
        for (String person : newPeople) {
            PersonWithButtons personWithButton = new PersonWithButtons(person, addListener, removeListener, type);
            peopleWithButtons.add(personWithButton); // Voeg de personItem toe aan de lijst
            add(personWithButton.createPanel()); // Voeg het paneel van de persoon toe aan de lijst
        }
    }

}

