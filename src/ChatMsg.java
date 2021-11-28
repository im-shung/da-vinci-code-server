import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

// ChatMsg.java 채팅 메시지 ObjectStream 용.
public class ChatMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    public String code; // 100:로그인, 400:로그아웃, 200:채팅메시지, 300:Image, 500: Mouse Event
    public String UserName;
    public String data;
    public ArrayList<String> list = new ArrayList<>();
    public ArrayList<Server.Card> cardList = new ArrayList<Server.Card>();

    public ChatMsg(String UserName, String code, String msg) {
        this.code = code;
        this.UserName = UserName;
        this.data = msg;
    }
    // 목록
    public void setList (ArrayList<String> list) {
        this.list = list;
    }
    // 카드 목록
    public void setCardList (ArrayList<String> list) {
        this.list = list;
    }

}