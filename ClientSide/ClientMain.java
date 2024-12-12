package ClientSide;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import interfaces.*;

public class ClientMain {
    private static String host = "localhost";
    private static int port = 12345;

    private String username;
    private File file;
    private JsonHandler jsonHandler;
    private BulletinBoardInterface bulletinBoard;
    private static MessageDigest digestSHA256; // SHA-256 message digest voor te hashen

    private ConcurrentHashMap<Integer, String> indexesToFetch = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    private static int chatHistorySize = 50;
    private static int panelWidth = 800;
    private static int panelHight = 400;
    private GUI chatGUI;
    private boolean change = true;
    
    public static void main(String[] args) {
        try {
            // connect to the server
            ClientMain client = new ClientMain();
            BulletinBoardInterface bulletinBoard = client.connectToServer();
            client.setBulletinBoard(bulletinBoard);

            // Maak een nieuw JFrame voor de applicatie
            JFrame frame = new JFrame("Chat System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 120);

            // Start met het inlogpaneel en geef ClientMain door aan het LoginPanel
            LoginPanel loginPanel = new LoginPanel(client, frame);
            frame.setContentPane(loginPanel);

            // Plaats het JFrame in het midden van het scherm
            frame.setLocationRelativeTo(null);

            // Maak het frame zichtbaar
            frame.setVisible(true);

        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public ClientMain() {
        try {
            digestSHA256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private BulletinBoardInterface connectToServer() throws RemoteException, NotBoundException{
        System.out.println("Client connecting to server at " + host + ":" + port);

        // Zoek de registry
        Registry registry = LocateRegistry.getRegistry(host, port);
        System.out.println("RMI registry found.");

        // Zoek het object
        Object remoteObject = registry.lookup("BulletinBoard");
        System.out.println("Object retrieved from registry: " + remoteObject.getClass().getName());

        // Cast naar BulletinBoardInterface
        BulletinBoardInterface bulletinBoard = (BulletinBoardInterface) remoteObject;
        System.out.println("Successfully casted to BulletinBoardInterface.");

        System.out.println("Client started");
        return bulletinBoard;
    }
    
    @SuppressWarnings("unchecked")
    public void login(JFrame frame) throws Exception {
        String filename = "ClientSide/jsonFiles/" + username + ".json";
        file = new File(filename);

        if (!file.exists()) {
            file.createNewFile();
            jsonHandler = new JsonHandler(filename);
            createNewUser();
            addAllSubscribers();

            // Maak een nieuw JFrame voor de chat GUI (in plaats van het inlogpaneel)
            this.setGUI(new GUI(username, this));
            frame.setTitle(username + "'s Chat");
            frame.setContentPane(chatGUI); // Verander de inhoud van het frame naar de chat GUI
            frame.revalidate(); // Herbouw het frame om de chat GUI weer te geven
            frame.setSize(panelWidth, panelHight);
            frame.setLocationRelativeTo(null);

            chatGUI.sendNotification("Welkom in onze service, " + username + "!");
        } else {
            jsonHandler = new JsonHandler(filename);
            lookForNewFriends();

            // Maak een nieuw JFrame voor de chat GUI (in plaats van het inlogpaneel)
            this.setGUI(new GUI(username, this));
            frame.setTitle(username + "'s Chat");
            frame.setContentPane(chatGUI); // Verander de inhoud van het frame naar de chat GUI
            frame.revalidate(); // Herbouw het frame om de chat GUI weer te geven
            frame.setSize(panelWidth, panelHight);
            frame.setLocationRelativeTo(null);

            chatGUI.sendNotification("Welkom terug, " + username + "!");
        }

        ArrayList<JSONObject> allUsersWeNeedAHandShake = jsonHandler.getList("waitingHandShake");
        for (JSONObject userObject : allUsersWeNeedAHandShake) {
            String userNameOtherSubscriber = (String) userObject.keySet().iterator().next();
            //String userNameOtherSubscriber = (String) userObject.get("username");

            JSONObject userInfo = (JSONObject) userObject.get(userNameOtherSubscriber);

            int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();
            
            String receiveTag = (String) userInfo.get("receiveTag");
            
            indexesToFetch.put(receiveIndex, receiveTag + ";" + userNameOtherSubscriber + ";waitingHandShake");
        }

        ArrayList<JSONObject> allNewPeople = jsonHandler.getList("newPeople");
        for (JSONObject userObject : allNewPeople) {
            String userNameOtherSubscriber = (String) userObject.keySet().iterator().next();
            //String userNameOtherSubscriber = (String) userObject.get("username");

            JSONObject userInfo = (JSONObject) userObject.get(userNameOtherSubscriber);

            int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();
            
            String receiveTag = (String) userInfo.get("receiveTag");
            
            indexesToFetch.put(receiveIndex, receiveTag + ";" + userNameOtherSubscriber + ";newPeople");
        }

        ArrayList<JSONObject> usersNotAnsweredRequestYet = jsonHandler.getList("inAfwachting");
        for (JSONObject userObject : usersNotAnsweredRequestYet) {
            String userNameOtherSubscriber = (String) userObject.keySet().iterator().next();
            //String userNameOtherSubscriber = (String) userObject.get("username");

            JSONObject userInfo = (JSONObject) userObject.get(userNameOtherSubscriber);

            int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();
            
            String receiveTag = (String) userInfo.get("receiveTag");
            
            indexesToFetch.put(receiveIndex, receiveTag + ";" + userNameOtherSubscriber + ";inAfwachting");
        }

        // checken op nieuwe berichten tijdens je weg was
        ArrayList<JSONObject> friends = jsonHandler.getList("friends");
        for (JSONObject userObject : friends) {
            String nameFriend = (String) userObject.keySet().iterator().next();

            JSONObject userInfo = (JSONObject) userObject.get(nameFriend);

            int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();
            
            if (bulletinBoard.isOccupied(receiveIndex)) {
                chatGUI.sendNotification("Je hebt een nieuw bericht van " + nameFriend + "!");
            } else if (bulletinBoard.isDeleted(receiveIndex)) {
                JSONObject userRemoveInfo = jsonHandler.removeUserFromList(nameFriend, "friends");

                int receiveIndexDup = ((Long) userRemoveInfo.get("receiveIndex")).intValue();
                int sendIndex = ((Long) userRemoveInfo.get("sendIndex")).intValue();
        
                bulletinBoard.clearSpot(receiveIndexDup);
                bulletinBoard.clearSpot(sendIndex);

                chatGUI.sendNotification(nameFriend + " heeft je verwijderd!");

                change = true;
            }   
        }
                
        
        scheduler = Executors.newScheduledThreadPool(1);  // Maak een threadpool
        scheduler.scheduleAtFixedRate(() -> {
            try {
                lookForNewFriends();
                fetchAndUpdateData();
                if (change) {
                    chatGUI.refreshAllPanels();
                    change = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    public void lookForNewFriends() throws Exception{
        JSONArray newFriends = bulletinBoard.fetchNewFriends(hashUserName(username));
        if (newFriends == null) {
            chatGUI.sendNotification("Er zijn geen nieuwe subscribers.");
            return;
        }
        for (Object obj : newFriends) {
            JSONObject newFriend = (JSONObject) obj;

            // Haal de geëncodeerde symmetrische sleutel en boodschap op
            String encryptedSymmetricKeyBase64Send = (String) newFriend.get("encryptedSymmetricKeySend");
            String encryptedSymmetricKeyBase64Receive = (String) newFriend.get("encryptedSymmetricKeyReceive");
            String encryptedMessageBase64 = (String) newFriend.get("encryptedMessage");
            String publicKeyBase64 = (String) newFriend.get("publicKey");

            // Decodeer de Base64 geëncodeerde waarden naar byte arrays
            byte[] encryptedSymmetricKeySend = Base64.getDecoder().decode(encryptedSymmetricKeyBase64Send);
            byte[] encryptedSymmetricKeyReceive = Base64.getDecoder().decode(encryptedSymmetricKeyBase64Receive);
            byte[] encryptedMessage = Base64.getDecoder().decode(encryptedMessageBase64);

            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            JSONObject jsonObject = jsonHandler.readJsonFile();
            String privateKeyBase64 = (String) jsonObject.get("privateKey");
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            // Receiver (B) decrypts the symmetric key (K) with his private key (SK_B)
            SecretKey decryptedSymmetricKeySend = decryptSymmetricKeyWithRSA(encryptedSymmetricKeySend, privateKey); // SK_B
            SecretKey decryptedSymmetricKeyReceive = decryptSymmetricKeyWithRSA(encryptedSymmetricKeyReceive, privateKey); // SK_B
            
            // Receiver (B) decrypts message with decrypted symmetric key (K)
            String[] decryptedParts = decryptMessageWithAES(encryptedMessage, decryptedSymmetricKeyReceive);
            String decryptedMessage = decryptedParts[0];  // The original message
            byte[] receivedSignature = Base64.getDecoder().decode(decryptedParts[1]);  // The signature

            // Receiver (B) verifies the signature with sender's public key (PK_A)
            boolean isVerified = verifySignature(decryptedMessage, receivedSignature, publicKey); // PK_A

            // extract alle info uit de message
            String[] parts = decryptedMessage.split(";");

            String usernameNewPerson = parts[0];

            if (!isVerified) {
                System.out.println("De handtekening is niet geldig.");
                chatGUI.sendNotification("De handtekening van user " + username + " is niet geldig in functie lookForNewFriends().");
                return;
            }

            JSONObject newPersonSubscribedInfo = new JSONObject();
            int receiveIndex = Integer.parseInt(parts[1]);
            String receiveTag = parts[2];
            int sendIndex = Integer.parseInt(parts[3]);
            String sendTag = parts[4];

            newPersonSubscribedInfo.put("receiveIndex", receiveIndex);
            newPersonSubscribedInfo.put("receiveTag", receiveTag);
            newPersonSubscribedInfo.put("sendIndex", sendIndex);
            newPersonSubscribedInfo.put("sendTag", sendTag);

            // Output the results
            String encodedKeySend = Base64.getEncoder().encodeToString(decryptedSymmetricKeySend.getEncoded());
            String encodedKeyReceive = Base64.getEncoder().encodeToString(decryptedSymmetricKeyReceive.getEncoded());
            
            newPersonSubscribedInfo.put("symmetricKeySend", encodedKeySend);
            newPersonSubscribedInfo.put("symmetricKeyReceive", encodedKeyReceive);
            jsonHandler.addUserToList(usernameNewPerson, newPersonSubscribedInfo, "newPeople");
            indexesToFetch.put(receiveIndex, receiveTag + ";" + usernameNewPerson + ";newPeople");

            if (jsonHandler.containsUserName(usernameNewPerson)){
                jsonHandler.removeUserFromList(usernameNewPerson, "friends");
            } else {
                jsonHandler.addNewUserName(usernameNewPerson);
            }

            answerToNewUser(usernameNewPerson, newPersonSubscribedInfo);

            change = true;
        }
    }

    @SuppressWarnings("unchecked")
    private void createNewUser() throws IOException, NoSuchAlgorithmException{
        JSONObject jsonObject = new JSONObject();

        KeyPair userKeyPair = generateRSAKeyPair();
        PublicKey publicKey = userKeyPair.getPublic();
        PrivateKey privateKey = userKeyPair.getPrivate();

        // Zet de sleutels om naar Base64-strings
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        jsonObject.put("publicKey", publicKeyBase64);
        jsonObject.put("privateKey", privateKeyBase64);
        jsonObject.put("waitingHandShake", new JSONArray()); // ONZICHTBAAR: Nieuwe gebruiker stuurt naar iedereen info
        jsonObject.put("newPeople", new JSONArray());  // andere gebruikers ontvangen info van nieuwe gebruiker
        jsonObject.put("friendRequests", new JSONArray()); // de andere persoon heeft je geAdd is geklikt en je hebt een vriendschapsverzoek ontvangen
        jsonObject.put("friends", new JSONArray()); // Vrienden
        jsonObject.put("inAfwachting", new JSONArray()); // Friend request is verstuurd, maar nog niet geaccepteerd
        jsonObject.put("allUserNames", new JSONArray()); // username, hash


        jsonHandler.writeJsonFile(jsonObject);
        
        // toevoegen van de gebruiker aan de server
        byte[] userHashBytes = digestSHA256.digest(username.getBytes());
        String userHashBase64 = java.util.Base64.getEncoder().encodeToString(userHashBytes);
        boolean gelukt = bulletinBoard.newSubscriber(userHashBase64, publicKeyBase64);

        if (!gelukt) {
            assert false : "Er is iets fout gegaan bij het toevoegen van de gebruiker aan de server.";
            return;
        }
    }

    @SuppressWarnings("unchecked")
    private void addAllSubscribers() throws Exception {
        // lees alle gebruikers in
        HashMap<String, String> subscribers = bulletinBoard.getSubscribers();

        // genereer een random Integer
        for (String userNameOtherSubscriber : subscribers.keySet()) {
            if (userNameOtherSubscriber.equals(hashUserName(username))) {
                continue;
            }

            // genereer een random tag
            String sendTag = generateRandomTag(30);
            String receiveTag = generateRandomTag(30);
            // genereer een random Integer = index, checken of deze niet in gebruik is
            int sendIndex;
            while (true) {
                sendIndex = (int) (Math.random() * 999999);
                if (bulletinBoard.reserveSpot(sendIndex)) {
                    break;
                }
            }
            System.out.println(username + " een index gereserveerd voor communicatie met " + userNameOtherSubscriber + ". Index = " + sendIndex);
            int receiveIndex;
            while (true) {
                receiveIndex = (int) (Math.random() * 999999);
                if (bulletinBoard.reserveSpot(receiveIndex)) {
                    break;
                }
            }
            System.out.println(username + " een index gereserveerd voor communicatie met " + userNameOtherSubscriber + ". Index = " + receiveIndex);
            // bericht opstellen
            String originalMessage = username + ";" + sendIndex + ";" + sendTag + ";" + receiveIndex + ";" + receiveTag;

            // opslaan voor jezelf
            JSONObject newFriend = new JSONObject();
            newFriend.put("sendIndex", sendIndex);
            newFriend.put("sendTag", sendTag);
            newFriend.put("receiveIndex", receiveIndex);
            newFriend.put("receiveTag", receiveTag);

            JSONObject jsonObject = jsonHandler.readJsonFile();
            // Verkrijg de waarde van de private key
            String privateKeyBase64 = (String) jsonObject.get("privateKey");
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            byte[] signedMessage = signMessage(originalMessage, privateKey); // SK_A

            // encrypts signed message with symmetric key
            SecretKey symmetricKeySend = generateAESKey();
            SecretKey symmetricKeyReceive = generateAESKey();
            byte[] encryptedMessage = encryptMessageAndSignatureWithAES(originalMessage, signedMessage, symmetricKeySend);

            String encodedKeySend = Base64.getEncoder().encodeToString(symmetricKeySend.getEncoded());
            String encodedKeyReceive = Base64.getEncoder().encodeToString(symmetricKeyReceive.getEncoded());
            newFriend.put("symmetricKeySend", encodedKeySend);
            newFriend.put("symmetricKeyReceive", encodedKeyReceive);

            jsonHandler.addUserToList(userNameOtherSubscriber, newFriend, "waitingHandShake");
            indexesToFetch.put(receiveIndex, receiveTag + ";" + userNameOtherSubscriber + ";waitingHandShake");

            // encrypts symmetric key with receiver's public key 
            String publicKeyOtherUserBase64 = subscribers.get(userNameOtherSubscriber);
            byte[] publicKeyOtherUserBytes = Base64.getDecoder().decode(publicKeyOtherUserBase64);
            PublicKey publicKeyOther = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyOtherUserBytes));
            byte[] encryptedSymmetricKeySend = encryptSymmetricKeyWithRSA(symmetricKeySend, publicKeyOther);
            byte[] encryptedSymmetricKeyReceive = encryptSymmetricKeyWithRSA(symmetricKeyReceive, publicKeyOther);
        
            // send key to user
            String publicKeyBase64 = (String) jsonObject.get("publicKey");
            bulletinBoard.addNewFriendTo(userNameOtherSubscriber, Base64.getEncoder().encodeToString(encryptedSymmetricKeyReceive), Base64.getEncoder().encodeToString(encryptedSymmetricKeySend), Base64.getEncoder().encodeToString(encryptedMessage), publicKeyBase64);
        }
            
    }

    ArrayList<Integer> indexesToDelete = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private void fetchAndUpdateData() throws Exception {
        for (int deleteIndex : indexesToDelete) {
            indexesToFetch.remove(deleteIndex);
        }
        for (int index : indexesToFetch.keySet()) {
            String[] infoAboutFetch = indexesToFetch.get(index).split(";");
            String tag = infoAboutFetch[0];
            String userNameSender = infoAboutFetch[1];
            String listName = infoAboutFetch[2];

            // checken of je niet verwijderd bent
            if (bulletinBoard.isDeleted(index)) {
                JSONObject userRemoveInfo = jsonHandler.removeUserFromList(userNameSender, "friends");

                int receiveIndexDup = ((Long) userRemoveInfo.get("receiveIndex")).intValue();
                int sendIndex = ((Long) userRemoveInfo.get("sendIndex")).intValue();
        
                bulletinBoard.clearSpot(receiveIndexDup);
                bulletinBoard.clearSpot(sendIndex);

                chatGUI.sendNotification(userNameSender + " heeft je verwijderd!");

                if (ChatWindow.getInstance(username, userNameSender, this, 0, "", "").isVisible()) {
                    ChatWindow.getInstance(username, userNameSender, this, 0, "", "").close();
                }

                indexesToFetch.remove(index);

                change = true;

                return;
            }

            if (bulletinBoard.isEmpty(index)) {
                recoverCorruptCommunication(userNameSender, listName, "index");
                indexesToDelete.add(index);
                break;
            }

            if (!bulletinBoard.hasMessage(index)) {
                continue;
            }
            
            String message = bulletinBoard.getMessage(index, tag);
            if (message != null) {
                String symKeyBase64 = jsonHandler.getSymmetricKeyReceiveFromList(userNameSender, listName);
                String decryptedMessage = decryptMessageWithAES(message, symKeyBase64, userNameSender, listName);
                if (decryptedMessage.equals("Corrupt")){
                    recoverCorruptCommunication(userNameSender, listName, "Symmetric key");
                    indexesToDelete.add(index);
                    break;
                }
                String[] parts = decryptedMessage.split(";");
                    
                if (parts[0].equals("ID")) {
                    String usernameSender = parts[1];  // username
                    String hashedUsername = parts[2];  // hashedUsername
                    int nextReceiveIndex = Integer.parseInt(parts[3]);  // nextSendIndex
                    String nextReceiveTag = parts[4];  // nextSendTag

                    if (!hashedUsername.equals(userNameSender)) {
                        System.out.println("In de if(!hashedUsername.equals(usernameHashed)) statement met de assert false in fetchAndUpdateData");
                        assert false : "ERROR: hashedUsername does not match the username in fetchHandshakes";
                    }
                    JSONObject userInfo = jsonHandler.removeUserFromList(hashedUsername, "waitingHandShake");


                    if (userInfo == null) {
                        System.out.println("In de if(userInfo == null) statement met de assert false in fetchHandshakes");
                        assert false : "ERROR: userInfo is null in addPeriodicFetchOnIndexForID";
                    }
                    jsonHandler.addUserToList(usernameSender, userInfo, "newPeople");
                    indexesToFetch.put(nextReceiveIndex, nextReceiveTag + ";" + usernameSender + ";newPeople");

                    String derivedSymKey = deriveSymKey(tag, symKeyBase64);

                    jsonHandler.updateReceiveInfo(usernameSender, nextReceiveIndex, nextReceiveTag, derivedSymKey ,"newPeople");

                    if (!jsonHandler.containsUserName(usernameSender)){
                        jsonHandler.addNewUserName(usernameSender);
                    }

                    change = true;

                    indexesToFetch.remove(index);
                } else if(parts[0].equals("REQUEST")) {
                    String usernameSender = parts[1];  // username
                    int nextReceiveIndex = Integer.parseInt(parts[2]);  // nextSendIndex
                    String nextReceiveTag = parts[3];  // nextSendTag

                    JSONObject userInfo = jsonHandler.removeUserFromList(usernameSender, "newPeople");


                    if (userInfo == null) {
                        System.out.println("In de if(userInfo == null) statement met de assert false in fetchAndUpdateData");
                        assert false : "ERROR: userInfo is null in addPeriodicFetchOnIndexForID";
                    }

                    jsonHandler.addUserToList(usernameSender, userInfo, "friendRequests");

                    // Volgens mij hoef je niet te checken of hij nog antwoord, want jij moet gwn nog kiezen of je bevriend wilt zijn
                    // indexesToFetch.put(nextReceiveIndex, nextReceiveTag + ";" + username + ";newPeople");

                    String derivedSymKey = deriveSymKey(tag, symKeyBase64);

                    jsonHandler.updateReceiveInfo(usernameSender, nextReceiveIndex, nextReceiveTag, derivedSymKey ,"friendRequests");

                    change = true;

                    indexesToFetch.remove(index);

                } else if (parts[0].equals("REMOVE")) {
                    JSONObject userRemoveInfo = jsonHandler.removeUserFromList(userNameSender, "newPeople");

                    int receiveIndex = ((Long) userRemoveInfo.get("receiveIndex")).intValue();
                    int sendIndex = ((Long) userRemoveInfo.get("sendIndex")).intValue();
            
                    bulletinBoard.clearSpot(receiveIndex);
                    bulletinBoard.clearSpot(sendIndex);

                    change = true;

                    indexesToFetch.remove(index);
                } else if (parts[0].equals("ACCEPT")) {
                    String usernameSender = parts[1];  // username
                    int nextReceiveIndex = Integer.parseInt(parts[2]);  // nextSendIndex
                    String nextReceiveTag = parts[3];  // nextSendTag

                    JSONObject userInfo = jsonHandler.removeUserFromList(userNameSender, "inAfwachting");

                    userInfo.put("chat", new JSONArray());

                    jsonHandler.addUserToList(usernameSender, userInfo, "friends");

                    String derivedSymKey = deriveSymKey(tag, symKeyBase64);

                    jsonHandler.updateReceiveInfo(usernameSender, nextReceiveIndex, nextReceiveTag, derivedSymKey ,"friends");

                    change = true;

                    indexesToFetch.remove(index);
                } else if (parts[0].equals("DECLINE")) {
                    JSONObject userRemoveInfo = jsonHandler.removeUserFromList(userNameSender, "inAfwachting");

                    int receiveIndex = ((Long) userRemoveInfo.get("receiveIndex")).intValue();
                    int sendIndex = ((Long) userRemoveInfo.get("sendIndex")).intValue();
            
                    bulletinBoard.clearSpot(receiveIndex);
                    bulletinBoard.clearSpot(sendIndex);

                    change = true;

                    indexesToFetch.remove(index);
                } else if (parts[0].equals("MESSAGE")) { 
                    String usernameSender = parts[1];  // username
                    int nextReceiveIndex = Integer.parseInt(parts[2]);  // nextSendIndex
                    String nextReceiveTag = parts[3];  // nextSendTag
                    String messageReceived = parts[4];

                    String derivedSymKey = deriveSymKey(tag, symKeyBase64);

                    // get time received to store in json
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter sortableFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                    String sortableDate = now.format(sortableFormat);

                    jsonHandler.updateReceiveInfoChat(usernameSender, nextReceiveIndex, nextReceiveTag, derivedSymKey, messageReceived, sortableDate);

                    indexesToFetch.remove(index);
                    indexesToFetch.put(nextReceiveIndex, nextReceiveTag + ";" + usernameSender + ";friends");

                    openChatWindows.get(usernameSender).receiveMessage(messageReceived);

                } else {
                    System.out.println("In de if(!parts[0].equals(\"ID\")) statement met de assert false in fetchHandshakes");
                    assert false : "ERROR: message got in addPeriodicFetchOnIndexForID is not an ID message";
                }
            } else {
                chatGUI.sendNotification("De tag is corrupt van ." + userNameSender);
                recoverCorruptCommunication(userNameSender, listName, "tag");
                indexesToDelete.add(index);
                break;
            }
        }
                
    }

    private void recoverCorruptCommunication(String usernameCorrupt, String listName, String type) throws Exception {
        if (ChatWindow.getInstance(username, usernameCorrupt, this, 0, "", "").isVisible()) {
            ChatWindow.getInstance(username, usernameCorrupt, this, 0, "", "").close();
        }

        int response = JOptionPane.showConfirmDialog(
                null, 
                "De communicatie met " + usernameCorrupt + " is corrupt (fout: " + type + "). Wil je deze herinitialiseren? Deze persoon komt in New People te staan als hij/zij online is.", 
                "Corrupt communication", 
                JOptionPane.YES_NO_OPTION
        );

        // Controleer de gebruiker zijn keuze
        if (response == JOptionPane.YES_OPTION) {
            // verwijder eerst de gebruiker uit de lijst
            jsonHandler.removeUserFromList(usernameCorrupt, listName);

            // haal dit op voor de public key van de andere persoon te weten. Door alles op te halen extra veilig?
            HashMap<String, String> subscribers = bulletinBoard.getSubscribers();

            // genereer een random tag
            String sendTag = generateRandomTag(30);
            String receiveTag = generateRandomTag(30);
            // genereer een random Integer = index, checken of deze niet in gebruik is
            int sendIndex;
            while (true) {
                sendIndex = (int) (Math.random() * 999999);
                if (bulletinBoard.reserveSpot(sendIndex)) {
                    break;
                }
            }
            System.out.println(username + " een index gereserveerd voor communicatie met " + usernameCorrupt + ". Index = " + sendIndex);
            int receiveIndex;
            while (true) {
                receiveIndex = (int) (Math.random() * 999999);
                if (bulletinBoard.reserveSpot(receiveIndex)) {
                    break;
                }
            }
            System.out.println(username + " een index gereserveerd voor communicatie met " + usernameCorrupt + ". Index = " + receiveIndex);

            // bericht opstellen
            String originalMessage = username + ";" + sendIndex + ";" + sendTag + ";" + receiveIndex + ";" + receiveTag;

            // opslaan voor jezelf
            JSONObject newFriend = new JSONObject();
            newFriend.put("sendIndex", sendIndex);
            newFriend.put("sendTag", sendTag);
            newFriend.put("receiveIndex", receiveIndex);
            newFriend.put("receiveTag", receiveTag);

            JSONObject jsonObject = jsonHandler.readJsonFile();
            // Verkrijg de waarde van de private key
            String privateKeyBase64 = (String) jsonObject.get("privateKey");
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            byte[] signedMessage = signMessage(originalMessage, privateKey); // SK_A

            // encrypts signed message with symmetric key
            SecretKey symmetricKeySend = generateAESKey();
            SecretKey symmetricKeyReceive = generateAESKey();
            byte[] encryptedMessage = encryptMessageAndSignatureWithAES(originalMessage, signedMessage, symmetricKeySend);

            String encodedKeySend = Base64.getEncoder().encodeToString(symmetricKeySend.getEncoded());
            String encodedKeyReceive = Base64.getEncoder().encodeToString(symmetricKeyReceive.getEncoded());
            newFriend.put("symmetricKeySend", encodedKeySend);
            newFriend.put("symmetricKeyReceive", encodedKeyReceive);

            // indien username nog nier gehashed, eerst doen
            if (!isHashedUsername(usernameCorrupt)) {
                usernameCorrupt = hashUserName(usernameCorrupt);
            } 

            jsonHandler.addUserToList(usernameCorrupt, newFriend, "waitingHandShake");
            indexesToFetch.put(receiveIndex, receiveTag + ";" + usernameCorrupt + ";waitingHandShake");

            // encrypts symmetric key with receiver's public key 
            String publicKeyOtherUserBase64 = subscribers.get(usernameCorrupt);
            byte[] publicKeyOtherUserBytes = Base64.getDecoder().decode(publicKeyOtherUserBase64);
            PublicKey publicKeyOther = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyOtherUserBytes));
            byte[] encryptedSymmetricKeySend = encryptSymmetricKeyWithRSA(symmetricKeySend, publicKeyOther);
            byte[] encryptedSymmetricKeyReceive = encryptSymmetricKeyWithRSA(symmetricKeyReceive, publicKeyOther);
        
            // send key to user
            String publicKeyBase64 = (String) jsonObject.get("publicKey");
            bulletinBoard.addNewFriendTo(usernameCorrupt, Base64.getEncoder().encodeToString(encryptedSymmetricKeyReceive), Base64.getEncoder().encodeToString(encryptedSymmetricKeySend), Base64.getEncoder().encodeToString(encryptedMessage), publicKeyBase64);
            
            change = true;
            
            return;
        } else {
            //Doe niets als de gebruiker "No" kiest
            chatGUI.sendNotification("Communicatie met " + usernameCorrupt + " blijft corrupt.");
        }
    }
    
    private static String generateRandomTag(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"; // Mogelijke karakters
        StringBuilder tag = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());  // Kies een willekeurig index
            tag.append(characters.charAt(index));  // Voeg het karakter toe aan de tag
        }

        return tag.toString();  // Retourneer de gegenereerde tag
    }

    private void answerToNewUser(String usernameNewPerson, JSONObject newUser) throws Exception {
        int sendIndex = (int) newUser.get("sendIndex");
        String sendTag = (String) newUser.get("sendTag");
        String encodedKeySend = (String) newUser.get("symmetricKeySend");

        int nextSendIndex;
        while(true) {
            nextSendIndex = (int) (Math.random() * 999999);
            if (bulletinBoard.reserveSpot(nextSendIndex)) {
                break;
            }
        }
        System.out.println(username + " een index gereserveerd voor communicatie met " + usernameNewPerson + ". Index = " + nextSendIndex);

        String nextSendTag = generateRandomTag(30);

        String message = "ID;" + username + ";" + hashUserName(username) + ";" + nextSendIndex + ";" + nextSendTag;
        String encryptedMessage = encryptMessageWithAES(message, encodedKeySend);

        String derivedSymKey = deriveSymKey(sendTag, encodedKeySend);

        jsonHandler.updateSendInfo(usernameNewPerson, nextSendIndex, nextSendTag, derivedSymKey ,"newPeople");
        
        System.out.println(username + " heeft een bericht gestuurd naar " + usernameNewPerson + " op de index " + sendIndex);
        bulletinBoard.addMessage(sendIndex, encryptedMessage, sendTag);
    }

    public void sendFriendRequest(String userNameReceiver) throws Exception {
        JSONObject userInfo = jsonHandler.getPersonOfList(userNameReceiver, "newPeople");

        int sendIndex = ((Long) userInfo.get("sendIndex")).intValue();
        String sendTag = (String) userInfo.get("sendTag");
        String encodedKeySend = (String) userInfo.get("symmetricKeySend");

        int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();
        String receiveTag = (String) userInfo.get("receiveTag");

        int nextSendIndex;
        while(true) {
            nextSendIndex = (int) (Math.random() * 999999);
            if (bulletinBoard.reserveSpot(nextSendIndex)) {
                break;
            }
        }
        System.out.println(username + " een index gereserveerd voor communicatie met " + userNameReceiver + ". Index = " + nextSendIndex);

        String nextSendTag = generateRandomTag(30);

        String message = "REQUEST;" + username + ";" + nextSendIndex + ";" + nextSendTag;
        String encryptedMessage = encryptMessageWithAES(message, encodedKeySend);

        String derivedSymKey = deriveSymKey(sendTag, encodedKeySend);


        JSONObject receiverInfo = jsonHandler.removeUserFromList(userNameReceiver, "newPeople");

        jsonHandler.addUserToList(userNameReceiver, receiverInfo, "inAfwachting");

        indexesToFetch.put(receiveIndex, receiveTag + ";" + userNameReceiver + ";inAfwachting");

        jsonHandler.updateSendInfo(userNameReceiver, nextSendIndex, nextSendTag, derivedSymKey ,"inAfwachting");

        System.out.println(username + " heeft een bericht gestuurd naar " + userNameReceiver + " op de index " + sendIndex);
        bulletinBoard.addMessage(sendIndex, encryptedMessage, sendTag);

        change = true;
    }

    public void removePersonOutNewPeople(String userNameReceiver) throws Exception {
        JSONObject userInfo = jsonHandler.removeUserFromList(userNameReceiver, "newPeople");

        int sendIndex = ((Long) userInfo.get("sendIndex")).intValue();
        String sendTag = (String) userInfo.get("sendTag");
        String encodedKeySend = (String) userInfo.get("symmetricKeySend");

        int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();

        String message = "REMOVE;" + ";" + username + ";" + 0;
        String encryptedMessage = encryptMessageWithAES(message, encodedKeySend);

        System.out.println(username + " heeft een bericht gestuurd naar " + userNameReceiver + " op de index " + sendIndex);
        bulletinBoard.addMessage(sendIndex, encryptedMessage, sendTag);

        indexesToFetch.remove(sendIndex);
        indexesToFetch.remove(receiveIndex);

        change = true;
    }

    @SuppressWarnings("unchecked")
    public void acceptFriendRequest(String userNameReceiver) throws Exception {
        JSONObject userInfo = jsonHandler.getPersonOfList(userNameReceiver, "friendRequests");

        int sendIndex = ((Long) userInfo.get("sendIndex")).intValue();
        String sendTag = (String) userInfo.get("sendTag");
        String encodedKeySend = (String) userInfo.get("symmetricKeySend");

        int nextSendIndex;
        while(true) {
            nextSendIndex = (int) (Math.random() * 999999);
            if (bulletinBoard.reserveSpot(nextSendIndex)) {
                break;
            }
        }
        System.out.println(username + " een index gereserveerd voor communicatie met " + userNameReceiver + ". Index = " + nextSendIndex);

        String nextSendTag = generateRandomTag(30);

        String message = "ACCEPT;" + username + ";" + nextSendIndex + ";" + nextSendTag;
        String encryptedMessage = encryptMessageWithAES(message, encodedKeySend);

        String derivedSymKey = deriveSymKey(sendTag, encodedKeySend);

        JSONObject receiverInfo = jsonHandler.removeUserFromList(userNameReceiver, "friendRequests");

        receiverInfo.put("chat", new JSONArray());

        jsonHandler.addUserToList(userNameReceiver, receiverInfo, "friends");

        jsonHandler.updateSendInfo(userNameReceiver, nextSendIndex, nextSendTag, derivedSymKey ,"friends");

        System.out.println(username + " heeft een bericht gestuurd naar " + userNameReceiver + " op de index " + sendIndex);
        bulletinBoard.addMessage(sendIndex, encryptedMessage, sendTag);

        change = true;
    }
    
    public void declineFriendRequest(String userNameReceiver) throws Exception {
        JSONObject userInfo = jsonHandler.removeUserFromList(userNameReceiver, "friendRequests");

        int sendIndex = ((Long) userInfo.get("sendIndex")).intValue();
        String sendTag = (String) userInfo.get("sendTag");
        String encodedKeySend = (String) userInfo.get("symmetricKeySend");

        int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();

        String message = "DECLINE;" + ";" + username + ";" + 0;
        String encryptedMessage = encryptMessageWithAES(message, encodedKeySend);
        
        System.out.println(username + " heeft een bericht gestuurd naar " + userNameReceiver + " op de index " + sendIndex);
        bulletinBoard.addMessage(sendIndex, encryptedMessage, sendTag);

        indexesToFetch.remove(sendIndex);
        indexesToFetch.remove(receiveIndex);

        change = true;
    }

    // indexes to check of open chats
    HashMap<String, ChatWindow> openChatWindows = new HashMap<>();

    @SuppressWarnings("unchecked")
    public void startChat(String userNameReceiver) throws RemoteException {    
        JSONObject userInfo = jsonHandler.getPersonOfList(userNameReceiver, "friends");

        // info to receive messages
        int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();
        String receiveTag = (String) userInfo.get("receiveTag");
            
        if (bulletinBoard.isDeleted(receiveIndex)) {
            JSONObject userRemoveInfo = jsonHandler.removeUserFromList(userNameReceiver, "friends");

            int receiveIndexDup = ((Long) userRemoveInfo.get("receiveIndex")).intValue();
            int sendIndex = ((Long) userRemoveInfo.get("sendIndex")).intValue();
    
            bulletinBoard.clearSpot(receiveIndexDup);
            bulletinBoard.clearSpot(sendIndex);

            chatGUI.sendNotification(userNameReceiver + " heeft je verwijderd!");

            change = true;

            return;
        }
        

        indexesToFetch.put(receiveIndex, receiveTag + ";" + userNameReceiver + ";friends");      

        // info to send messages
        int sendIndex = ((Long) userInfo.get("sendIndex")).intValue();
        String sendTag = (String) userInfo.get("sendTag");
        String encodedKeySend = (String) userInfo.get("symmetricKeySend");

        // ChatWindow chatWindow = new ChatWindow.getInstance(username, userNameReceiver, this);
        ChatWindow chatWindow = ChatWindow.getInstance(username, userNameReceiver, this, sendIndex, sendTag, encodedKeySend);

        // inladen van de chatgeschiedenis
        JSONArray chatHistory = jsonHandler.getChatHistory(userNameReceiver);
        Map<String, String> unsortedMap = new HashMap<>();
        for (Object obj : chatHistory) {
            JSONObject chatMessage = (JSONObject) obj;
            String message = (String) chatMessage.get("message");
            String time = (String) chatMessage.get("time");
            unsortedMap.put(time, message);
        }

        Map<String, String> sortedMap = new TreeMap<>(unsortedMap);


        int oversized = sortedMap.size() - chatHistorySize;

        if (oversized > 0) {
            for (int i = 0; i < oversized; i++) {
                sortedMap.remove(sortedMap.keySet().iterator().next());
            }
            // hashmap terug omzetten naar JSONArray
            JSONArray chatHistoryNew = new JSONArray();
            for (String time : sortedMap.keySet()) {
                JSONObject chatMessage = new JSONObject();
                chatMessage.put("time", time);
                chatMessage.put("message", sortedMap.get(time));
                chatHistoryNew.add(chatMessage);
            }

            jsonHandler.updateChatHistory(userNameReceiver, chatHistoryNew);
        }

        for (String time : sortedMap.keySet()) {
            LocalDateTime dateTime = LocalDateTime.parse(time);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH'u'mm");
            String formattedDate = dateTime.format(formatter);

            String message = sortedMap.get(time);
            String[] parts = message.split(";");

            String sender = null;
            if (parts[0].equals("SEND")) {
                sender = username;
            } else if (parts[0].equals("RECEIVE")) {
                sender = userNameReceiver;
            } else {
                System.out.println("ERROR: message does not start with SEND or RECEIVE in startChat");
                assert false;
            }
            

            String bericht = formattedDate + " - " + sender + ": " + parts[1];
            chatWindow.addMessage(bericht);
        }
        chatWindow.addMessage("Momenteel------------------------");


        // chatwindow toevoegen aan de hashmap
        openChatWindows.put(userNameReceiver, chatWindow);
    }

    public int sendMessage(String userNameReceiver, String messageToSend, int sendIndex, String sendTag, String encodedKeySend, ChatWindow currentChat) throws Exception {
        int nextSendIndex;
        while(true) {
            nextSendIndex = (int) (Math.random() * 999999);
            if (bulletinBoard.reserveSpot(nextSendIndex)) {
                break;
            }
        }
        System.out.println(username + " een index gereserveerd voor communicatie met " + userNameReceiver + ". Index = " + nextSendIndex);

        String nextSendTag = generateRandomTag(30);

        String message = "MESSAGE;" + username + ";" + nextSendIndex + ";" + nextSendTag + ";" + messageToSend;
        String encryptedMessage = encryptMessageWithAES(message, encodedKeySend);

        String derivedSymKey = deriveSymKey(sendTag, encodedKeySend);

        // get time received to store in json
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter sortableFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String sortableDate = now.format(sortableFormat);

        // updaten in de json (gwn als backup)
        jsonHandler.updateSendInfoChat(userNameReceiver, nextSendIndex, nextSendTag, derivedSymKey, messageToSend, sortableDate);

        // updaten in de chatGUI
        currentChat.setSendTag(nextSendTag);  // index geven we terug als returnwaarde -> garandeerd dat je nog geen bericht kan versturen tot het aangepast is
        currentChat.setEncodedKeySend(derivedSymKey);
        
        System.out.println(username + " heeft een bericht gestuurd naar " + userNameReceiver + " op de index " + sendIndex);
        bulletinBoard.addMessage(sendIndex, encryptedMessage, sendTag);

        return nextSendIndex;
    }

    public void closeChatWith(String userNameReceiver) {            
        // to receive messages
        JSONObject userInfo = jsonHandler.getPersonOfList(userNameReceiver, "friends");

        int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();
                    
        String controle = indexesToFetch.remove(receiveIndex);
        String controleUserName = controle.split(";")[1];
        if(!controleUserName.equals(userNameReceiver)) {
            System.out.println("In de if(!controleUserName.equals(userNameReceiver)) statement met de assert false in closeChatWith");
            assert false : "ERROR: controleUserName does not match the userNameReceiver in closeChatWith";
        }

        openChatWindows.remove(userNameReceiver);
    }



    public void removeFriend(String friendName) throws Exception {
        // Maak een bevestigingsvenster
        int response = JOptionPane.showConfirmDialog(
                null, 
                "Wil je zeker " + friendName + " verwijderen als vriend??", 
                "But are you sure?", 
                JOptionPane.YES_NO_OPTION
        );

        // Controleer de gebruiker zijn keuze
        if (response == JOptionPane.YES_OPTION) {
            // Extra logica als de gebruiker "Yes" kiest
            if (ChatWindow.getInstance(username, friendName, this, 0, "", "").isVisible()) {
                ChatWindow.getInstance(username, friendName, this, 0, "", "").close();
            }

            JSONObject userInfo = jsonHandler.removeUserFromList(friendName, "friends");

            int sendIndex = ((Long) userInfo.get("sendIndex")).intValue();
            int receiveIndex = ((Long) userInfo.get("receiveIndex")).intValue();

            if (indexesToFetch.containsKey(receiveIndex)) {
                indexesToFetch.remove(receiveIndex);
            } else if (indexesToFetch.containsKey(sendIndex)) {
                indexesToFetch.remove(sendIndex);
            }

            bulletinBoard.setDeleted(sendIndex);

            change = true;

            chatGUI.sendNotification("Je hebt " + friendName + " verwijderd als vriend.");
        } else {
            //Doe niets als de gebruiker "No" kiest
            chatGUI.sendNotification("Verwijderen is geannuleerd.");
        }
    }

    private void setBulletinBoard(BulletinBoardInterface bulletinBoard) {
        this.bulletinBoard = bulletinBoard;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setGUI(GUI chatGUI) {
        this.chatGUI = chatGUI;
    }

    private static String hashUserName(String userName) {
        byte[] userHashBytes = digestSHA256.digest(userName.getBytes());
        String userHashBase64 = java.util.Base64.getEncoder().encodeToString(userHashBytes);

        return userHashBase64;
    }

    // Generate RSA Key Pair
    private static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);  // Key size 2048 bits
        return keyGen.generateKeyPair();
    }

    // Generate AES Key (Symmetric Key)
    private static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);  // AES-256
        return keyGen.generateKey();
    }

    private String deriveSymKey(String tagUsed, String base64encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] KeyBytes = Base64.getDecoder().decode(base64encodedKey);

        // Salt (gedeeld tussen zender en ontvanger)
        byte[] sharedSalt = tagUsed.getBytes();

        // Combineer de sleutelbytes met contextinformatie
        String inputKeyMaterial = Base64.getEncoder().encodeToString(KeyBytes);

        // Afgeleide sleutel maken met PBKDF2
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(inputKeyMaterial.toCharArray(), sharedSalt, 100000, 256);
        byte[] derivedKeyBytes = factory.generateSecret(spec).getEncoded();

        // Print de afgeleide sleutel
        return Base64.getEncoder().encodeToString(derivedKeyBytes);
    }

    // Sign the message with your private key
    private static byte[] signMessage(String message, PrivateKey privateKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(message.getBytes(StandardCharsets.UTF_8));

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(hash);

        return signature.sign();
    }

    // Encrypt the message + signature with AES (Symmetric key)
    private static byte[] encryptMessageAndSignatureWithAES(String message, byte[] signature, SecretKey symmetricKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
        
        String combined = message + "||" + Base64.getEncoder().encodeToString(signature);
        return cipher.doFinal(combined.getBytes(StandardCharsets.UTF_8));
    }

    public static String decryptMessageWithAES(String encryptedMessage, String base64encodedKey, String userNameSender, String listName) throws Exception {
        try {
            byte[] decodedKeyBytes = Base64.getDecoder().decode(base64encodedKey);
            SecretKey symmetricKey = new SecretKeySpec(decodedKeyBytes, 0, decodedKeyBytes.length, "AES");

            // Initialiseer een AES Cipher in DECRYPT_MODE
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
        
            // Decodeer de Base64-gecodeerde string naar een byte[]
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedMessage);
        
            // Ontsleutel de bytes
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        
            // Zet de gedecrypteerde bytes om naar een string
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Corrupt";
        }
    }

    public static String encryptMessageWithAES(String message, String base64encodedKey) throws Exception {
        byte[] decodedKeyBytes = Base64.getDecoder().decode(base64encodedKey);
        SecretKey symmetricKey = new SecretKeySpec(decodedKeyBytes, 0, decodedKeyBytes.length, "AES");

        // Initialiseer een AES Cipher in ENCRYPT_MODE
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
    
        // Versleutel de boodschap en geef het resultaat terug
        byte[] encryptedMessage = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        String encryptedMessageString = Base64.getEncoder().encodeToString(encryptedMessage);
        return encryptedMessageString;
    }

    // Encrypt symmetric key (K) with receiver's public key (PK_B)
    private static byte[] encryptSymmetricKeyWithRSA(SecretKey symmetricKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(symmetricKey.getEncoded());
    }

    // Decrypt symmetric key (K) with receiver's private key (SK_B)
    private static SecretKey decryptSymmetricKeyWithRSA(byte[] encryptedSymmetricKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKey = cipher.doFinal(encryptedSymmetricKey);
        return new SecretKeySpec(decryptedKey, "AES");
    }

    // Decrypt the message with AES (Symmetric key)
    private static String[] decryptMessageWithAES(byte[] encryptedMessage, SecretKey symmetricKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, symmetricKey);
        String decryptedText = new String(cipher.doFinal(encryptedMessage), StandardCharsets.UTF_8);
        
        // Split the decrypted text into message and signature (based on the separator "||")
        String[] parts = decryptedText.split("\\|\\|");  // Splits the message and signature
        return parts;
    }

    // Verify the signature with sender's public key (PK_A)
    private static boolean verifySignature(String message, byte[] signature, PublicKey publicKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(message.getBytes(StandardCharsets.UTF_8));

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(hash);
        return sig.verify(signature);
    }

    public static boolean isBase64Encoded(String str) {
        try {
            // Probeer de string te decoderen. Als het geen geldige Base64 is, zal dit een exception veroorzaken.
            java.util.Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public static boolean isHashedUsername(String userNameOrHash) {
        // Controleer of de lengte overeenkomt met een Base64-gecodeerde SHA-256 hash (44 tekens)
        if (userNameOrHash.length() == 44 && isBase64Encoded(userNameOrHash)) {
            return true;  // Waarschijnlijk een gehashte gebruikersnaam
        }
        return false;  // Waarschijnlijk een normale gebruikersnaam
    }
}
