package ClientSide;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.parser.ParseException;

public class JsonHandler {

    String filePath;
    // Attributen voor reader, writer en parser
    private FileReader reader;
    private FileWriter writer;
    private JSONParser parser;

    // Constructor die de parser en writer initialiseert
    public JsonHandler(String filePath) {
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

    // Methode om een element toe te voegen aan het JSON-bestand
    public void addToJsonFile(String key, Object value) {
        JSONObject jsonObject = readJsonFile(); // Leest het huidige JSON-bestand
        if (jsonObject == null) {
            jsonObject = new JSONObject(); // Maak een nieuw JSON-object als het bestand leeg is
        }
        
        jsonObject.put(key, value); // Voeg de nieuwe sleutel/waarde toe
        
        writeJsonFile(jsonObject); // Schrijf het bijgewerkte JSON-object terug naar het bestand
    }

    public void addNewFriend(String key, Object value) {
        JSONObject jsonObject = readJsonFile(); // Leest het huidige JSON-bestand
        if (jsonObject == null) {
            jsonObject = new JSONObject(); // Maak een nieuw JSON-object als het bestand leeg is
        }
        
        jsonObject.put(key, value); // Voeg de nieuwe sleutel/waarde toe
        
        writeJsonFile(jsonObject); // Schrijf het bijgewerkte JSON-object terug naar het bestand
    }


    // Methode om JSON-string te parseren
    public JSONObject parseJsonString(String jsonString) {
        return (JSONObject) JSONValue.parse(jsonString);
    }
}
