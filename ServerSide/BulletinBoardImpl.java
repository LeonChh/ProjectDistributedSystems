package ServerSide;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Box;
import javax.xml.crypto.Data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ClientSide.JsonHandler;
import interfaces.BulletinBoardInterface;

public class BulletinBoardImpl extends UnicastRemoteObject implements BulletinBoardInterface {
    private int bulletinBoardSize;
    private int aantalDataServers;
    private ArrayList<DataServer> dataServers;
    private MessageDigest digestSHA256;
    private JsonHandlerServer jsonHandler;

    private final Object lockJsonFile = new Object();
    private final Object lockForReserve = new Object();
    private int serverIterator = 0;
    private int indexMultiplyFactor;


    public BulletinBoardImpl(int bulletinBoardSize, int aantalDataServers) throws RemoteException {
        super();
        this.bulletinBoardSize = bulletinBoardSize;
        this.aantalDataServers = aantalDataServers; 
        jsonHandler = new JsonHandlerServer("ServerSide/subscribers.json");

        if (aantalDataServers <= 10) {
            indexMultiplyFactor = 10;
        } else if (aantalDataServers <= 100) {
            indexMultiplyFactor = 100;
        } else {
            indexMultiplyFactor = 1000; // wss niet meer of 1000 servers
        }

        dataServers = new ArrayList<>();
        for (int i = 0; i < aantalDataServers; i++) {
            dataServers.add(new DataServer(i, bulletinBoardSize));
        }
        try {
            digestSHA256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int reserveSpot(int index) {
        synchronized (lockJsonFile) {
            DataServer dataServer = dataServers.get(serverIterator);
            serverIterator = (serverIterator + 1) % aantalDataServers;

            ArrayList<BulletinBoardElement> bulletinBoard = dataServer.getBulletinBoard();

            int indexAfterModulo = index % bulletinBoardSize;
            if (bulletinBoard.get(indexAfterModulo).isEmpty()) {
                bulletinBoard.get(indexAfterModulo).setReserved();
                int newIndex = indexMultiplyFactor * indexAfterModulo + dataServer.getServerID();

                System.out.println("spot gereserveerd op server " + dataServer.getServerID() + " op index " + indexAfterModulo + " met newIndex " + newIndex);

                return newIndex;
            }
        }
        
        return -1;
    }

    @Override
    public boolean addMessage(int index, String message, String tag) throws RemoteException {
        int serverID = index % indexMultiplyFactor;
        int indexOnTheServer = index / indexMultiplyFactor; // rond altijd naar onder af

        ArrayList<BulletinBoardElement> bulletinBoard = dataServers.get(serverID).getBulletinBoard();

        if (bulletinBoard.get(indexOnTheServer).isOccupied()) {
            System.out.println("ERROR: index " + index + "is niet leeg in addMessage, maar " + bulletinBoard.get(indexOnTheServer).getState());
            return false;
        } else if (bulletinBoard.get(indexOnTheServer).isEmpty()) {
            // assert false : "ERROR: index " + index + "is niet gereserveerd in addMessage, maar " + bulletinBoard.get(indexAfterModulo).getState();
            // return false;
        }

        byte[] hashBytes = digestSHA256.digest(tag.getBytes());
        
        bulletinBoard.get(indexOnTheServer).setTag(hashBytes);
        bulletinBoard.get(indexOnTheServer).setMessage(message);
        bulletinBoard.get(indexOnTheServer).setOccupied();

        return true;
    }

    @Override
    public boolean hasMessage(int index) throws RemoteException {
        int serverID = index % indexMultiplyFactor;
        int indexOnTheServer = index / indexMultiplyFactor; // rond altijd naar onder af

        ArrayList<BulletinBoardElement> bulletinBoard = dataServers.get(serverID).getBulletinBoard();
        return bulletinBoard.get(indexOnTheServer).isOccupied();
    }

    @Override
    public boolean isEmpty(int index) throws RemoteException {
        int serverID = index % indexMultiplyFactor;
        int indexOnTheServer = index / indexMultiplyFactor; // rond altijd naar onder af
        ArrayList<BulletinBoardElement> bulletinBoard = dataServers.get(serverID).getBulletinBoard();
        return bulletinBoard.get(indexOnTheServer).isEmpty();
    }

    @Override
    public String getMessage(int index, String tag) throws RemoteException {
        int serverID = index % indexMultiplyFactor;
        int indexOnTheServer = index / indexMultiplyFactor; // rond altijd naar onder af
        ArrayList<BulletinBoardElement> bulletinBoard = dataServers.get(serverID).getBulletinBoard();
        
        if (!bulletinBoard.get(indexOnTheServer).isOccupied()) {
            return null;
        }

        byte[] tagBytesReceiver = digestSHA256.digest(tag.getBytes());
        byte[] tagBytesSender = bulletinBoard.get(indexOnTheServer).getTag();

        if (!Arrays.equals(tagBytesReceiver, tagBytesSender)) {
            System.out.println("ERROR: tag " + tag + " komt niet overeen met tag " + bulletinBoard.get(indexOnTheServer).getTag() + " in getMessage op index " + index);
            return null;
        }

        String message = bulletinBoard.get(indexOnTheServer).getMessage();
        bulletinBoard.get(indexOnTheServer).setEmpty();

        return message;
    }

    @Override
    public boolean newSubscriber(String usernameHash, String publicKeyBase64) throws RemoteException {
        synchronized (lockJsonFile) {
            jsonHandler.addToNewSubscriber(usernameHash, publicKeyBase64);
            return true;
        }
    }

    @Override
    public HashMap<String, String> getSubscribers() throws RemoteException {
        synchronized (lockJsonFile) {
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
        synchronized (lockJsonFile) {
            jsonHandler.addNewFriendTo(username, encryptedSymmetricKeyBase64Send, encryptedSymmetricKeyBase64Receive, encryptedMessageBase64, publicKeyBase64);
        }
    }

    @Override
    public JSONArray fetchNewFriends(String usernameHash) {
        synchronized (lockJsonFile) {
            return jsonHandler.fetchNewFriends(usernameHash); // returnt nieuwe vrienden en maakt de lijst leeg
        }
    }

    @Override
    public void clearSpot(int index) {
        int serverID = index % indexMultiplyFactor;
        int indexOnTheServer = index / indexMultiplyFactor; // rond altijd naar onder af
        ArrayList<BulletinBoardElement> bulletinBoard = dataServers.get(serverID).getBulletinBoard();
        bulletinBoard.get(indexOnTheServer).setEmpty();
    }

    @Override
    public boolean isOccupied(int index) {
        int serverID = index % indexMultiplyFactor;
        int indexOnTheServer = index / indexMultiplyFactor; // rond altijd naar onder af
        ArrayList<BulletinBoardElement> bulletinBoard = dataServers.get(serverID).getBulletinBoard();
        return bulletinBoard.get(indexOnTheServer).isOccupied();
    }

    @Override
    public boolean isDeleted(int index) {
        int serverID = index % indexMultiplyFactor;
        int indexOnTheServer = index / indexMultiplyFactor; // rond altijd naar onder af
        ArrayList<BulletinBoardElement> bulletinBoard = dataServers.get(serverID).getBulletinBoard();
        return bulletinBoard.get(indexOnTheServer).isDeleted();
    }

    @Override
    public boolean setDeleted(int index) {
        int serverID = index % indexMultiplyFactor;
        int indexOnTheServer = index / indexMultiplyFactor; // rond altijd naar onder af
        ArrayList<BulletinBoardElement> bulletinBoard = dataServers.get(serverID).getBulletinBoard();

        if (!bulletinBoard.get(indexOnTheServer).isReserved()) {
            System.out.println("ERROR: index " + index + "is niet reserved in setDeleted, maar " + bulletinBoard.get(indexOnTheServer).getState());
            return false;
        }
        bulletinBoard.get(indexOnTheServer).setDeleted();
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
                    int serverID = index % indexMultiplyFactor;
                    int indexOnTheServer = index / indexMultiplyFactor; // rond altijd naar onder af
                    ArrayList<BulletinBoardElement> bulletinBoard = dataServers.get(serverID).getBulletinBoard();
                    bulletinBoard.get(indexOnTheServer).setEmpty();
                    break;

                case 2:
                    System.out.print("Voer het indexnummer in om de tag te corrupten: ");
                    int indexTag = scanner.nextInt();
                    scanner.nextLine(); // Consume newline
                    int serverIDTag = indexTag % indexMultiplyFactor;
                    int indexOnTheServerTag = indexTag / indexMultiplyFactor; // rond altijd naar onder af
                    ArrayList<BulletinBoardElement> bulletinBoardTag = dataServers.get(serverIDTag).getBulletinBoard();
                    bulletinBoardTag.get(indexOnTheServerTag).setTag(new byte[32]);
                    break;
                default:
                    System.out.println("Ongeldige keuze. Probeer opnieuw.");
            }
        }
    }
}
