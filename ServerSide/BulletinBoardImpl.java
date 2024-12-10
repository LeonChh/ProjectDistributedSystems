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

import javax.swing.Box;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ClientSide.JsonHandler;
import interfaces.BulletinBoardInterface;

public class BulletinBoardImpl extends UnicastRemoteObject implements BulletinBoardInterface {
    private int bulletinBoardSize;
    ArrayList<BulletinBoardElement> bulletinBoard;
    MessageDigest digestSHA256;
    JsonHandlerServer jsonHandler;

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
    public boolean checkIfIndexIsEmpty(int index) {
        index = index % bulletinBoardSize;
        System.out.println("Checking if index " + index + " is empty");
        return bulletinBoard.get(index).isEmpty();
    }

    @Override
    public boolean addMessage(int index, String message, String tag) throws RemoteException {
        index = index % bulletinBoardSize;
        if (!bulletinBoard.get(index).isEmpty()) {
            System.out.println("ERROR: index " + index + "is niet leeg in addMessage");
            return false;
        }

        byte[] hashBytes = digestSHA256.digest(tag.getBytes());
        
        bulletinBoard.get(index).setTag(hashBytes);
        bulletinBoard.get(index).setMessage(message);
        bulletinBoard.get(index).setEmpty(false);
        return true;
    }

    @Override
    public String getMessage(int index, String tag) throws RemoteException {
        index = index % bulletinBoardSize;
        if (bulletinBoard.get(index).isEmpty()) {
            System.out.println("ERROR: index " + index + "is leeg in getMessage");
            return null;
        }

        byte[] tagBytesReceiver = digestSHA256.digest(tag.getBytes());
        byte[] tagBytesSender = bulletinBoard.get(index).getTag();

        if (!Arrays.equals(tagBytesReceiver, tagBytesSender)) {
            System.out.println("ERROR: tag " + tag + " komt niet overeen met tag " + bulletinBoard.get(index).getTag() + " in getMessage");
            return null;
        }

        String message = bulletinBoard.get(index).getMessage();
        bulletinBoard.get(index).setEmpty(true);

        return message;
    }

    @Override
    public boolean newSubscriber(String username, String publicKeyBase64) throws RemoteException {
        jsonHandler.addToNewSubscriber(username, publicKeyBase64);
        return true;
    }

    @Override
    public HashMap<String, String> getSubscribers() throws RemoteException {
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

    @Override
    public void addNewFriendTo(String username, String nameFriend, String encryptedSymmetricKeyBase64, String encryptedMessageBase64, String publicKeyBase64) {
        jsonHandler.addNewFriendTo(username, nameFriend, encryptedSymmetricKeyBase64, encryptedMessageBase64, publicKeyBase64);
    }

    @Override
    public JSONArray fetchNewFriends(String username) {
        return jsonHandler.fetchNewFriends(username);
    }
}
