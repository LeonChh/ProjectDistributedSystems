package ServerSide;

public class BulletinBoardElement {
    private byte[] tag;
    private String message;
    private boolean isEmpty;

    public BulletinBoardElement(byte[] tag, String message) {
        this.tag = tag;
        this.message = message;
        this.isEmpty = true;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public byte[] getTag() {
        return tag;
    }

    public String getMessage() {
        return message;
    }

    public void setTag(byte[] tag) {
        this.tag = tag;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setEmpty(boolean empty) {
        isEmpty = empty;
    }
    
}
