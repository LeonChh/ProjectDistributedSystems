package ServerSide;

public class BulletinBoardElement {
    private byte[] tag;
    private String message;
    //private boolean isEmpty;
    private State state;

    public BulletinBoardElement(byte[] tag, String message) {
        this.tag = tag;
        this.message = message;
        state = State.EMPTY;    
    }

    public boolean isEmpty() {
        return state == State.EMPTY;
    }

    public boolean isOccupied() {
        return state == State.OCCUPIED;
    }

    public boolean isReserved() {
        return state == State.RESERVED;
    }

    public void setEmpty() {
        state = State.EMPTY;
    }

    public void setOccupied() {
        state = State.OCCUPIED;
    }

    public void setReserved() {
        state = State.RESERVED;
    }

    public State getState() {
        return state;
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

    // public void setEmpty(boolean empty) {
    //     isEmpty = empty;
    // }
    
}
