package ServerSide;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {
    private static final int PORT = 12345;
    private static final int BULLETIN_BOARD_SIZE = 99999;

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);

        try {
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind("BulletinBoard", new BulletinBoardImpl(BULLETIN_BOARD_SIZE));
            System.out.println("BulletinBoard is ready");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
