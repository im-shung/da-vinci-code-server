import java.util.List;

public class ChatList {
    public String code; // 100:로그인, 400:로그아웃, 200:채팅메시지, 300:Image, 500: Mouse Event
    public String UserName;
    public List<String> list;

    public ChatList(String UserName, String code, List<String> list){
        this.code = code;
        this.UserName = UserName;
        this.list = list;
    }

    public void setList (List<String> list) {
        this.list = list;
    }
}
