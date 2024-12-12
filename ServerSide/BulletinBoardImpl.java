package ServerSide;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.swing.Box;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ClientSide.JsonHandler;
import interfaces.BulletinBoardInterface;

public class BulletinBoardImpl extends UnicastRemoteObject implements BulletinBoardInterface {
    private int bulletinBoardSize;
    private ArrayList<BulletinBoardElement> bulletinBoard;
    private MessageDigest digestSHA256;
    private JsonHandlerServer jsonHandler;

    private final Object lock = new Object();

    public BulletinBoardImpl(int bulletinBoardSize) throws RemoteException {
        super();
        this.bulletinBoardSize = bulletinBoardSize;
        jsonHandler = new JsonHandlerServer("ServerSide/subscribers.json");
        bulletinBoard = new ArrayList<>();
        for (int i = 0; i < bulletinBoardSize; i++) {
            bulletinBoard.add(new BulletinBoardElement(null, null));
        }
        try {
            digestSHA256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean reserveSpot(int index) {
        int indexAfterModulo = index % bulletinBoardSize;
        if (bulletinBoard.get(indexAfterModulo).isEmpty()) {
            bulletinBoard.get(indexAfterModulo).setReserved();
            return true;
        }
        
        return false;
    }

    @Override
    public boolean addMessage(int index, String message, String tag) throws RemoteException {
        int indexAfterModulo  = index % bulletinBoardSize;

        if (bulletinBoard.get(indexAfterModulo).isOccupied()) {
            System.out.println("ERROR: index " + index + "is niet leeg in addMessage, maar " + bulletinBoard.get(indexAfterModulo).getState());
            return false;
        } else if (bulletinBoard.get(indexAfterModulo).isEmpty()) {
            // assert false : "ERROR: index " + index + "is niet gereserveerd in addMessage, maar " + bulletinBoard.get(indexAfterModulo).getState();
            // return false;
        }

        byte[] hashBytes = digestSHA256.digest(tag.getBytes());
        
        bulletinBoard.get(indexAfterModulo).setTag(hashBytes);
        bulletinBoard.get(indexAfterModulo).setMessage(message);
        bulletinBoard.get(indexAfterModulo).setOccupied();

        return true;
    }

    @Override
    public boolean hasMessage(int index) throws RemoteException {
        int indexAfterModulo = index % bulletinBoardSize;
        return bulletinBoard.get(indexAfterModulo).isOccupied();
    }

    @Override
    public boolean isEmpty(int index) throws RemoteException {
        int indexAfterModulo = index % bulletinBoardSize;
        return bulletinBoard.get(indexAfterModulo).isEmpty();
    }

    @Override
    public String getMessage(int index, String tag) throws RemoteException {
        int indexAfterModulo = index % bulletinBoardSize;
        
        if (!bulletinBoard.get(indexAfterModulo).isOccupied()) {
            return null;
        }

        byte[] tagBytesReceiver = digestSHA256.digest(tag.getBytes());
        byte[] tagBytesSender = bulletinBoard.get(indexAfterModulo).getTag();

        if (!Arrays.equals(tagBytesReceiver, tagBytesSender)) {
            System.out.println("ERROR: tag " + tag + " komt niet overeen met tag " + bulletinBoard.get(indexAfterModulo).getTag() + " in getMessage op index " + index);
            return null;
        }

        String message = bulletinBoard.get(indexAfterModulo).getMessage();
        bulletinBoard.get(indexAfterModulo).setEmpty();

        return message;
    }

    @Override
    public boolean newSubscriber(String usernameHash, String publicKeyBase64) throws RemoteException {
        synchronized (lock) {
            jsonHandler.addToNewSubscriber(usernameHash, publicKeyBase64);
            return true;
        }
    }

    @Override
    public HashMap<String, String> getSubscribers() throws RemoteException {
        synchronized (lock) {
            HashMap<String, String> subscribersMap = new HashMap<>();
            JSONObject subscribers = jsonHandler.readJsonFile();
            
            if (subscribers != null) { // Controleer of het JSON-object is geladen
                for (Object key : subscribers.keySet()) { // Itereer over de sleutels van het JSON-object
                    String username = (String) key; // De sleutel is de gebruikersnaam
                    JSONObject subscriberDetails = (JSONObject) subscribers.get(username); // Haal de details op
                    String publicKeyBase64 = (String) subscriberDetails.get("publicKey"); // Haal de public key op
            
                    subscribersMap.put(username, publicKeyBase64); // Voeg toe aan de map
                }
            } else {
                System.out.println("ERROR: subscribers is null in getSubscribers");
            }

            return subscribersMap;
        }
    }

    @Override
    public void addNewFriendTo(String username, String encryptedSymmetricKeyBase64Send, String encryptedSymmetricKeyBase64Receive, String encryptedMessageBase64, String publicKeyBase64) {
        synchronized (lock) {
            jsonHandler.addNewFriendTo(username, encryptedSymmetricKeyBase64Send, encryptedSymmetricKeyBase64Receive, encryptedMessageBase64, publicKeyBase64);
        }
    }

    @Override
    public JSONArray fetchNewFriends(String usernameHash) {
        synchronized (lock) {
            return jsonHandler.fetchNewFriends(usernameHash); // returnt nieuwe vrienden en maakt de lijst leeg
        }
    }

    @Override
    public void clearSpot(int index) {
        int indexAfterModulo = index % bulletinBoardSize;
        bulletinBoard.get(indexAfterModulo).setEmpty();
    }

    @Override
    public boolean isOccupied(int index) {
        int indexAfterModulo = index % bulletinBoardSize;
        return bulletinBoard.get(indexAfterModulo).isOccupied();
    }

    @Override
    public boolean isDeleted(int index) {
        int indexAfterModulo = index % bulletinBoardSize;
        return bulletinBoard.get(indexAfterModulo).isDeleted();
    }

    @Override
    public boolean setDeleted(int index) {
        int indexAfterModulo = index % bulletinBoardSize;
        if (!bulletinBoard.get(indexAfterModulo).isReserved()) {
            System.out.println("ERROR: index " + index + "is niet reserved in setDeleted, maar " + bulletinBoard.get(indexAfterModulo).getState());
            return false;
        }
        bulletinBoard.get(indexAfterModulo).setDeleted();
        return true;
    }

    // CLI methode
    public void startCLI() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Selecteer een optie:");
            System.out.println("1) corrupt een index");
            System.out.println("2) corrupt een tag");
            
            int keuze = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (keuze) {
                case 1:
                    System.out.print("Voer het indexnummer in om te corrupten: ");
                    int index = scanner.nextInt();
                    scanner.nextLine(); // Consume newline
                    index = index % bulletinBoardSize;
                    bulletinBoard.get(index).setEmpty();
                    break;

                case 2:
                    System.out.print("Voer het indexnummer in om de tag te corrupten: ");
                    int indexTag = scanner.nextInt();
                    scanner.nextLine(); // Consume newline
                    indexTag = indexTag % bulletinBoardSize;
                    bulletinBoard.get(indexTag).setTag(new byte[32]);
                    break;
                default:
                    System.out.println("Ongeldige keuze. Probeer opnieuw.");
            }
        }
    }
}
