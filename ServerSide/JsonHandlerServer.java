package ServerSide;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;

import org.json.simple.parser.ParseException;

public class JsonHandlerServer {

    String filePath;
    // Attributen voor reader, writer en parser
    private FileReader reader;
    private FileWriter writer;
    private JSONParser parser;

    // Constructor die de parser en writer initialiseert
    public JsonHandlerServer(String filePath) {
        this.parser = new JSONParser();
        this.filePath = filePath;
    }

    // Methode om een JSON-bestand te lezen
    public JSONObject readJsonFile() {
        try {
            reader = new FileReader(filePath);
            return (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Methode om een JSON-bestand te schrijven
    public void writeJsonFile(JSONObject jsonObject) {
        try {
            writer = new FileWriter(filePath);
            writer.write(jsonObject.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addToNewSubscriber(String userName, String publicKeyBase64) {
        JSONObject subscribers = readJsonFile(); // Leest het huidige JSON-bestand
        if (subscribers == null) {
            subscribers = new JSONObject(); // Maak een nieuw JSON-object als het bestand leeg is
        }
        
        JSONObject newSubcriber = new JSONObject();
        newSubcriber.put("publicKey", publicKeyBase64);
        newSubcriber.put("newFriends", new JSONArray());
        subscribers.put(userName, newSubcriber); // Voeg de nieuwe sleutel/waarde toe
        
        writeJsonFile(subscribers); // ScSendhrijf het bijgewerkte JSON-object terug naar het bestand
    }

    // Methode om een element toe te voegen aan het JSON-bestand
    public void addNewFriendTo(String userName, String encryptedSymmetricKeyBase64Send, String encryptedSymmetricKeyBase64Receive, String encryptedMessageBase64, String publicKeyBase64) {
        JSONObject subscribers = readJsonFile(); // Lees het huidige JSON-bestand
        if (subscribers == null) {
            subscribers = new JSONObject(); // Maak een nieuw JSON-object als het bestand leeg is
        }
        
        // Haal het JSON-object voor de subscriber (userName) op
        JSONObject jsonUser = (JSONObject) subscribers.get(userName);
        if (jsonUser == null) {
            assert false : "ERROR: jsonUser is null in addNewFriendTo";
        }

        JSONArray newFriends = (JSONArray) jsonUser.get("newFriends");
        
        JSONObject newFriend = new JSONObject();
        newFriend.put("encryptedSymmetricKeySend", encryptedSymmetricKeyBase64Send);
        newFriend.put("encryptedSymmetricKeyReceive", encryptedSymmetricKeyBase64Receive);
        newFriend.put("encryptedMessage", encryptedMessageBase64);
        newFriend.put("publicKey", publicKeyBase64);

        newFriends.add(newFriend);

        writeJsonFile(subscribers);
    }

    public JSONArray fetchNewFriends(String usernameHash) {
        JSONObject subscribers = readJsonFile(); // Lees het huidige JSON-bestand
        if (subscribers == null) {
            subscribers = new JSONObject(); // Maak een nieuw JSON-object als het bestand leeg is
        }
        
        // Haal het JSON-object voor de subscriber (userName) op
        JSONObject jsonUser = (JSONObject) subscribers.get(usernameHash);
        if (jsonUser == null) {
            assert false : "ERROR: jsonUser is null in fetchNewFriends";
        }

        JSONArray newFriends = (JSONArray) jsonUser.get("newFriends");

        // Maak de lijst leeg
        jsonUser.put("newFriends", new JSONArray());

        // Schrijf het JSON-bestand bij om de wijziging op te slaan
        writeJsonFile(subscribers);

        return newFriends;
    }

    public void clearNewFriends(String usernameHash) {
        JSONObject subscribers = readJsonFile(); // Lees het huidige JSON-bestand
        if (subscribers == null) {
            subscribers = new JSONObject(); // Maak een nieuw JSON-object als het bestand leeg is
        }
        
        // Haal het JSON-object voor de subscriber (userName) op
        JSONObject jsonUser = (JSONObject) subscribers.get(usernameHash);
        if (jsonUser == null) {
            assert false : "ERROR: jsonUser is null in clearNewFriends";
        }

        // Haal de lijst met 'newFriends' op
        JSONArray newFriends = (JSONArray) jsonUser.get("newFriends");
        jsonUser.put("newFriends", new JSONArray());

        writeJsonFile(subscribers);
    }

    // Methode om JSON-string te parseren
    public JSONObject parseJsonString(String jsonString) {
        return (JSONObject) JSONValue.parse(jsonString);
    }
}
