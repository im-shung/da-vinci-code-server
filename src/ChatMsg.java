
// ChatMsg.java 채팅 메시지 ObjectStream 용.
import java.awt.event.MouseEvent;
import java.io.Serializable;
import javax.swing.ImageIcon;

class ChatMsg implements Serializable {
	private static final long serialVersionUID = 1L;
	public String code; // 100:로그인, 400:로그아웃, 200:채팅메시지, 300:Image, 500: Mouse Event
	public String userName;
	public String data;
	public ImageIcon img;
	public MouseEvent mouse_e;
	public int pen_size; // pen size

	public ChatMsg(String userName, String code, String msg) {
		this.code = code;
		this.userName = userName;
		this.data = msg;
	}
}