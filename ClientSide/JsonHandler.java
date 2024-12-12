package ClientSide;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.json.simple.parser.ParseException;

public class JsonHandler {

    String filePath;
    // Attributen voor reader, writer en parser
    private FileReader reader;
    private FileWriter writer;
    private JSONParser parser;

    // synchronizeren
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


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

    // // Methode om een JSON-bestand te lezen
    // public JSONObject readJsonFile() {
    //     lock.readLock().lock(); // Verkrijg de read-lock
    //     try (FileReader reader = new FileReader(filePath)) {
    //         return (JSONObject) parser.parse(reader);
    //     } catch (IOException | ParseException e) {
    //         e.printStackTrace();
    //         return null;
    //     } finally {
    //         lock.readLock().unlock(); // Vrijgeven van de read-lock
    //     }
    // }

    // // Methode om een JSON-bestand te schrijven
    // public void writeJsonFile(JSONObject jsonObject) {
    //     lock.writeLock().lock(); // Verkrijg de write-lock
    //     try (FileWriter writer = new FileWriter(filePath)) {
    //         writer.write(jsonObject.toJSONString());
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     } finally {
    //         lock.writeLock().unlock(); // Vrijgeven van de write-lock
    //     }
    // }

    // Methode om een element toe te voegen aan het JSON-bestand
    public void addToJsonFile(String key, Object value) {
        JSONObject jsonObject = readJsonFile(); // Leest het huidige JSON-bestand
        if (jsonObject == null) {
            jsonObject = new JSONObject(); // Maak een nieuw JSON-object als het bestand leeg is
        }
        
        jsonObject.put(key, value); // Voeg de nieuwe sleutel/waarde toe
        
        writeJsonFile(jsonObject); // Schrijf het bijgewerkte JSON-object terug naar het bestand
    }

    public void addNewUserName(String userName) {
        JSONObject jsonObject = readJsonFile(); // Leest het huidige JSON-bestand
        if (jsonObject == null) {
            jsonObject = new JSONObject(); // Maak een nieuw JSON-object als het bestand leeg is
        }
        
        JSONArray userNames = (JSONArray) jsonObject.get("allUserNames");
        userNames.add(userName);
        
        writeJsonFile(jsonObject); // Schrijf het bijgewerkte JSON-object terug naar het bestand
    }

    public JSONObject removeUserFromList(String userName, String listName) {
        JSONObject jsonObject = readJsonFile();
        JSONArray listGiven = (JSONArray) jsonObject.get(listName);

        System.out.println("removeUserFromList: user to remove: " + userName);

        for (int i = 0; i < listGiven.size(); i++) {
            JSONObject listElement = (JSONObject) listGiven.get(i);
            for (Object key : listElement.keySet()) {
                String userNameKey = (String) key;
                if (userNameKey.equals(userName)) {
                    JSONObject friendInfo = (JSONObject) listElement.get(userName);
                    listGiven.remove(i);
                    System.out.println("removeUserFromList: removed" + userName);
                    writeJsonFile(jsonObject);
                    return friendInfo;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public void addUserToList(String userName, Object info, String listName) {
        JSONObject jsonObject = readJsonFile();
        JSONArray listGiven = (JSONArray) jsonObject.get(listName);

        JSONObject newPersonSubscribed = new JSONObject();
        newPersonSubscribed.put(userName,(JSONObject) info);

        listGiven.add(newPersonSubscribed);

        writeJsonFile(jsonObject);
    }

    public String getSymmetricKeyReceiveFromList(String userName, String listName) {
        JSONObject jsonObject = readJsonFile();
        JSONArray selectedList = (JSONArray) jsonObject.get(listName);

        for (int i = 0; i < selectedList.size(); i++) {
            JSONObject listElement = (JSONObject) selectedList.get(i);
            for (Object key : listElement.keySet()) {
                String userNameKey = (String) key;
                if (userNameKey.equals(userName)) {
                    JSONObject friendInfo = (JSONObject) listElement.get(userName);
                    return (String) friendInfo.get("symmetricKeyReceive");
                }
            }
        }
        assert false : "ERROR: getSymmetricKeyReceive: " + userName + " not found";
        return null;
    }

    public void updateSendInfo(String usernameGiven, int nextSendIndex, String nextSendTag, String derivedSymKey, String listName) {
        JSONObject jsonObject = readJsonFile();
        JSONArray listGiven = (JSONArray) jsonObject.get(listName);

        for (int i = 0; i < listGiven.size(); i++) {
            JSONObject friend = (JSONObject) listGiven.get(i);
            for (Object key : friend.keySet()) {
                String userName = (String) key;
                System.out.println("in updateSendInfo username: " + userName + ", usernameSearch: " + usernameGiven);
                if (userName.equals(usernameGiven)) {
                    JSONObject friendInfo = (JSONObject) friend.get(userName);
                    friendInfo.put("sendIndex", nextSendIndex);
                    friendInfo.put("sendTag", nextSendTag);
                    friendInfo.put("symmetricKeySend", derivedSymKey);

                    writeJsonFile(jsonObject);

                    return;
                }
            }
        }
       
    }

    @SuppressWarnings("unchecked")
    public void updateSendInfoChat(String usernameGiven, int nextSendIndex, String nextSendTag, String derivedSymKey, String messageSend, String Date) {
        JSONObject jsonObject = readJsonFile();
        JSONArray listGiven = (JSONArray) jsonObject.get("friends");

        for (int i = 0; i < listGiven.size(); i++) {
            JSONObject friend = (JSONObject) listGiven.get(i);
            for (Object key : friend.keySet()) {
                String userName = (String) key;
                System.out.println("in updateSendInfoChat username: " + userName + ", usernameSearch: " + usernameGiven);
                if (userName.equals(usernameGiven)) {
                    JSONObject timeMessage = new JSONObject();
                    timeMessage.put("time", Date);
                    timeMessage.put("message", "SEND;" + messageSend);

                    JSONObject friendInfo = (JSONObject) friend.get(userName);
                    friendInfo.put("sendIndex", nextSendIndex);
                    friendInfo.put("sendTag", nextSendTag);
                    friendInfo.put("symmetricKeySend", derivedSymKey);
                    ((JSONArray) friendInfo.get("chat")).add(timeMessage);

                    writeJsonFile(jsonObject);

                    return;
                }
            }
        }

        
    }

    @SuppressWarnings("unchecked")
    public void updateReceiveInfo(String username, int nextSendIndex, String nextSendTag, String derivedSymKey, String listName) {
        JSONObject jsonObject = readJsonFile();
        JSONArray listGiven = (JSONArray) jsonObject.get(listName);

        for (int i = 0; i < listGiven.size(); i++) {
            JSONObject friend = (JSONObject) listGiven.get(i);
            for (Object key : friend.keySet()) {
                String userName = (String) key;
                if (userName.equals(username)) {
                    JSONObject friendInfo = (JSONObject) friend.get(userName);
                    friendInfo.put("receiveIndex", nextSendIndex);
                    friendInfo.put("receiveTag", nextSendTag);
                    friendInfo.put("symmetricKeyReceive", derivedSymKey);

                    writeJsonFile(jsonObject);

                    return;
                }
            }
        }

        
    }

    @SuppressWarnings("unchecked")
    public void updateReceiveInfoChat(String username, int nextSendIndex, String nextSendTag, String derivedSymKey, String messageReceived, String Date) {
        JSONObject jsonObject = readJsonFile();
        JSONArray listGiven = (JSONArray) jsonObject.get("friends");

        for (int i = 0; i < listGiven.size(); i++) {
            JSONObject friend = (JSONObject) listGiven.get(i);
            for (Object key : friend.keySet()) {
                String userName = (String) key;
                if (userName.equals(username)) {
                    JSONObject timeMessage = new JSONObject();
                    timeMessage.put("time", Date);
                    timeMessage.put("message", "RECEIVE;" + messageReceived);

                    JSONObject friendInfo = (JSONObject) friend.get(userName);
                    friendInfo.put("receiveIndex", nextSendIndex);
                    friendInfo.put("receiveTag", nextSendTag);
                    friendInfo.put("symmetricKeyReceive", derivedSymKey);
                    ((JSONArray) friendInfo.get("chat")).add(timeMessage);

                    writeJsonFile(jsonObject);

                    return;
                }
            }
        }

        
    }

    public JSONArray getChatHistory(String username) {
        JSONObject jsonObject = readJsonFile();
        JSONArray listGiven = (JSONArray) jsonObject.get("friends");

        for (int i = 0; i < listGiven.size(); i++) {
            JSONObject friend = (JSONObject) listGiven.get(i);
            for (Object key : friend.keySet()) {
                String userName = (String) key;
                if (userName.equals(username)) {
                    JSONObject friendInfo = (JSONObject) friend.get(userName);
                    return (JSONArray) friendInfo.get("chat");
                }
            }
        }
        assert false : "ERROR: getChatHistory: " + username + " not found";
        return null;
    }

    @SuppressWarnings("unchecked")
    public void updateChatHistory(String username, JSONArray chatHistory) {
        JSONObject jsonObject = readJsonFile();
        JSONArray listGiven = (JSONArray) jsonObject.get("friends");

        for (int i = 0; i < listGiven.size(); i++) {
            JSONObject friend = (JSONObject) listGiven.get(i);
            for (Object key : friend.keySet()) {
                String userName = (String) key;
                if (userName.equals(username)) {
                    JSONObject friendInfo = (JSONObject) friend.get(userName);
                    friendInfo.put("chat", chatHistory);

                    writeJsonFile(jsonObject);

                    return;
                }
            }
        }
        assert false : "ERROR: setChatHistory: " + username + " not found";
    }

    public ArrayList<String> getUserNamesOfList(String listName){
        ArrayList<String> newAskedList = new ArrayList<>();

        JSONObject jsonObject = readJsonFile();
        JSONArray newAskedListJson = (JSONArray) jsonObject.get(listName);

        for (int i = 0; i < newAskedListJson.size(); i++) {
            JSONObject newListElement = (JSONObject) newAskedListJson.get(i);
            for (Object key : newListElement.keySet()) {
                String userName = (String) key;
                newAskedList.add(userName);
            }
        }

        return newAskedList;
    }

    public ArrayList<JSONObject> getList(String listName){
        ArrayList<JSONObject> newAskedList = new ArrayList<>();

        JSONObject jsonObject = readJsonFile();
        JSONArray newAskedListJson = (JSONArray) jsonObject.get(listName);

        for (int i = 0; i < newAskedListJson.size(); i++) {
            newAskedList.add((JSONObject) newAskedListJson.get(i));
            
        }

        System.out.println("JsonHandler: getList " + listName + " : " + newAskedList);

        return newAskedList;
    }

    public JSONObject getPersonOfList(String userName, String listName){ 
        JSONObject jsonObject = readJsonFile();
        JSONArray newPeopleJson = (JSONArray) jsonObject.get(listName);

        System.out.println("Person searching for in list " + listName + " : " + userName);
        System.out.println("getPersonOfList: " + newPeopleJson);

        for (int i = 0; i < newPeopleJson.size(); i++) {
            JSONObject newPerson = (JSONObject) newPeopleJson.get(i);
            for (Object key : newPerson.keySet()) {
                String userNameKey = (String) key;
                if (userNameKey.equals(userName)) {
                    return (JSONObject) newPerson.get(userName);
                }
            }
        }
        assert false : "ERROR: getNewPerson: " + userName + " not found";
        return null;
    }

    // Methode om JSON-string te parseren
    public JSONObject parseJsonString(String jsonString) {
        return (JSONObject) JSONValue.parse(jsonString);
    }
}
