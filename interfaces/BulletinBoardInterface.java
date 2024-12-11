package interfaces;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;


public interface BulletinBoardInterface extends Remote {
    boolean reserveSpot(int index) throws RemoteException;
    boolean addMessage(int index, String message, String tag) throws RemoteException;
    String getMessage(int index, String tag) throws RemoteException;
    boolean newSubscriber(String userName, String dataToInitializeConversation) throws RemoteException;
    HashMap<String, String> getSubscribers() throws RemoteException;
    void addNewFriendTo(String userName, String encryptedSymmetricKeyBase64Send, String encryptedSymmetricKeyBase64Receive, String encryptedMessageBase64, String publicKeyBase64) throws RemoteException;
    JSONArray fetchNewFriends(String userName) throws RemoteException;
    void clearSpot(int index) throws RemoteException;
}