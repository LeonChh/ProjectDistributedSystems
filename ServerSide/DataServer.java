package ServerSide;

import java.util.ArrayList;

public class DataServer {

    private int serverID;
    private int bulletinBoardSize;
    private ArrayList<BulletinBoardElement> bulletinBoard;

    public DataServer(int serverID, int bulletinBoardSize) {
        System.out.println("DataServer created with serverID: " + serverID + " and bulletinBoardSize: " + bulletinBoardSize);
        this.serverID = serverID;
        this.bulletinBoardSize = bulletinBoardSize;
        bulletinBoard = new ArrayList<>();
        for (int i = 0; i < bulletinBoardSize; i++) {
            bulletinBoard.add(new BulletinBoardElement(null, null));
        }
    }

    public int getServerID() {
        return serverID;
    }

    public int getBulletinBoardSize() {
        return bulletinBoardSize;
    }

    public ArrayList<BulletinBoardElement> getBulletinBoard() {
        return bulletinBoard;
    }
    
}
