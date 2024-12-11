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
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Random;

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
    
    public static void main(String[] args) {
        try {
            // connect to the server
            ClientMain client = new ClientMain();
            BulletinBoardInterface bulletinBoard = client.connectToServer();
            client.setBulletinBoard(bulletinBoard);

            // Maak een nieuw JFrame voor de applicatie
            JFrame frame = new JFrame("Chat System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 500);

            // Start met het inlogpaneel en geef ClientMain door aan het LoginPanel
            LoginPanel loginPanel = new LoginPanel(client, frame);
            frame.setContentPane(loginPanel);
            frame.setVisible(true);
            

            // boolean online = true;
            // while (online){
            //     System.out.println("1. Bericht plaatsen");
            //     System.out.println("2. Bericht ophalen");
            //     System.out.println("3. Kijken of er nieuwe vrienden zijn");
            //     System.out.println("4. Afmelden");
                
            //     String keuze = System.console().readLine();
            //     switch (keuze) {
            //         case "1":
            //             System.out.println("Bericht plaatsen");
            //             break;
            //         case "2":
            //             System.out.println("Bericht ophalen");
            //             break;
            //         case "3":
            //             client.lookForNewFriends(bulletinBoard);
            //             break;
            //         case "4":
            //             System.out.println("Afmelden");
            //             online = false;
            //             break;
            //         default:
            //             System.out.println("Ongeldige keuze");
            //             break;
            //     }
            // }
            

        } catch (Exception e) {
            System.err.println("Client exception: " + e.getMessage());
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

    public void login(JFrame frame) throws Exception {
        // while (true) {
        //     System.out.print("Geef een gebruikersnaam in: ");
        //     username = System.console().readLine();
        //     if (username.length() > 0) {
        //         System.out.print("Is " + username + " je username? (ja/nee): ");
        //         if (System.console().readLine().equals("ja")) {
        //             break;
        //         }
        //     }
        // }

        // Maak een nieuw JFrame voor de chat GUI (in plaats van het inlogpaneel)
        GUI chatGUI = new GUI(username);
        frame.setTitle(username + "'s Chat");
        frame.setContentPane(chatGUI); // Verander de inhoud van het frame naar de chat GUI
        frame.revalidate(); // Herbouw het frame om de chat GUI weer te geven

        String filename = "ClientSide/jsonFiles/" + username + ".json";
        file = new File(filename);

        if (!file.exists()) {
            System.out.println("Welkom in onze service, " + username + "!");
            file.createNewFile();
            jsonHandler = new JsonHandler(filename);
            createNewUser(bulletinBoard);
            addAllSubscribers(bulletinBoard);
            System.out.println("1");
        } else {
            System.out.println("Welkom terug, " + username + "!");
            jsonHandler = new JsonHandler(filename);
            lookForNewFriends(bulletinBoard);
        }
    }

    private void lookForNewFriends(BulletinBoardInterface bulletinBoard) throws Exception{
        JSONArray newFriends = bulletinBoard.fetchNewFriends(username);
        if (newFriends == null) {
            System.out.println("Er zijn geen nieuwe vrienden.");
            return;
        }
        for (Object obj : newFriends) {
            JSONObject newFriend = (JSONObject) obj;

            // Haal de geëncodeerde symmetrische sleutel en boodschap op
            String encryptedSymmetricKeyBase64 = (String) newFriend.get("encryptedSymmetricKey");
            String encryptedMessageBase64 = (String) newFriend.get("encryptedMessage");
            String publicKeyBase64 = (String) newFriend.get("publicKey");

            // Decodeer de Base64 geëncodeerde waarden naar byte arrays
            byte[] encryptedSymmetricKey = Base64.getDecoder().decode(encryptedSymmetricKeyBase64);
            byte[] encryptedMessage = Base64.getDecoder().decode(encryptedMessageBase64);

            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            JSONObject jsonObject = jsonHandler.readJsonFile();
            String privateKeyBase64 = (String) jsonObject.get("privateKey");
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            // Receiver (B) decrypts the symmetric key (K) with his private key (SK_B)
            SecretKey decryptedSymmetricKey = decryptSymmetricKeyWithRSA(encryptedSymmetricKey, privateKey); // SK_B
            
            // Receiver (B) decrypts message with decrypted symmetric key (K)
            String[] decryptedParts = decryptMessageWithAES(encryptedMessage, decryptedSymmetricKey);
            String decryptedMessage = decryptedParts[0];  // The original message
            byte[] receivedSignature = Base64.getDecoder().decode(decryptedParts[1]);  // The signature

            // Receiver (B) verifies the signature with sender's public key (PK_A)
            boolean isVerified = verifySignature(decryptedMessage, receivedSignature, publicKey); // PK_A

            // Output the results
            String encodedKey = Base64.getEncoder().encodeToString(decryptedSymmetricKey.getEncoded());
            System.out.println("userName: " + decryptedMessage.split(";")[0]);
            System.out.println("Decrypted message: " + decryptedMessage);
            System.out.println("Symmetric key: " + encodedKey);
            System.out.println("userName: " + decryptedMessage.split(";")[0]);
            System.out.println("Signature verification: " + (isVerified ? "VALID" : "INVALID"));
        }
    }

    private void createNewUser(BulletinBoardInterface bulletinBoard) throws IOException, NoSuchAlgorithmException{
        JSONObject jsonObject = new JSONObject();

        KeyPair userKeyPair = generateRSAKeyPair();
        PublicKey publicKey = userKeyPair.getPublic();
        PrivateKey privateKey = userKeyPair.getPrivate();

        // Zet de sleutels om naar Base64-strings
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        jsonObject.put("publicKey", publicKeyBase64);
        jsonObject.put("privateKey", privateKeyBase64);

        jsonHandler.writeJsonFile(jsonObject);
        System.out.println("Keys successfully saved to the file.");
        
        // toevoegen van de gebruiker aan de server
        boolean gelukt = bulletinBoard.newSubscriber(username, publicKeyBase64);

        if (!gelukt) {
            assert false : "Er is iets fout gegaan bij het toevoegen van de gebruiker aan de server.";
            return;
        }
    }

    private void addAllSubscribers(BulletinBoardInterface bulletinBoard) throws Exception {
        // lees alle gebruikers in
        HashMap<String, String> subscribers = bulletinBoard.getSubscribers();

        // genereer een random Integer
        for (String userNameOtherSubscriber : subscribers.keySet()) {
            if (userNameOtherSubscriber.equals(username)) {
                continue;
            }

            System.out.println("Bezig met toevoegen van " + userNameOtherSubscriber + "...");
            // genereer een random tag
            String tagMe = generateRandomTag(30);
            String tagOther = generateRandomTag(30);
            // genereer een random Integer = index, checken of deze niet in gebruik is
            int indexMe;
            while (true) {
                indexMe = (int) (Math.random() * 999999);
                if (bulletinBoard.checkIfIndexIsEmpty(indexMe)) {
                    break;
                }
                // System.out.println("Random integer: " + randomIndex);
            }
            int indexOther;
            while (true) {
                indexOther = (int) (Math.random() * 999999);
                if (bulletinBoard.checkIfIndexIsEmpty(indexOther)) {
                    break;
                }
                // System.out.println("Random integer: " + randomIndex);
            }
            // bericht opstellen
            String originalMessage = username + ";" + indexMe + ";" + tagMe + ";" + indexOther + ";" + tagOther;

            // opslaan voor jezelf
            JSONObject newFriend = new JSONObject();
            newFriend.put("myIndex", indexMe);
            newFriend.put("myTag", tagMe);
            newFriend.put("otherIndex", indexOther);
            newFriend.put("otherTag", tagOther);

            JSONObject jsonObject = jsonHandler.readJsonFile();
            // Verkrijg de waarde van de private key
            String privateKeyBase64 = (String) jsonObject.get("privateKey");
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            byte[] signedMessage = signMessage(originalMessage, privateKey); // SK_A

            // encrypts signed message with symmetric key
            SecretKey symmetricKeySend = generateAESKey();
            SecretKey symmetricKeyReceive = generateAESKey();
            byte[] encryptedMessage = encryptMessageWithAES(originalMessage, signedMessage, symmetricKeySend);

            String encodedKeySend = Base64.getEncoder().encodeToString(symmetricKeySend.getEncoded());
            String encodedKeyReceive = Base64.getEncoder().encodeToString(symmetricKeyReceive.getEncoded());
            newFriend.put("symmetricKeySend", encodedKeySend);
            newFriend.put("symmetricKeyReceive", encodedKeyReceive);

            jsonHandler.addNewFriend(userNameOtherSubscriber, newFriend);

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

    private void setBulletinBoard(BulletinBoardInterface bulletinBoard) {
        this.bulletinBoard = bulletinBoard;
    }

    public void setUsername(String username) {
        this.username = username;
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
    private static byte[] encryptMessageWithAES(String message, byte[] signature, SecretKey symmetricKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
        
        String combined = message + "||" + Base64.getEncoder().encodeToString(signature);
        return cipher.doFinal(combined.getBytes(StandardCharsets.UTF_8));
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
}
