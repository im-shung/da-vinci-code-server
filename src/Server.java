//JavaObjServer.java ObjectStream 기반 채팅 Server

import java.awt.*;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;

public class Server extends JFrame {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	JTextArea textArea;
	private JTextField txtPortNumber;

	private ServerSocket socket; // 서버소켓
	private Socket client_socket; // accept() 에서 생성된 client 소켓
	private Vector allUser = new Vector(); // 연결된 사용자를 저장할 벡터
	private Vector waitUser = new Vector(); // 대기중인 사용자를 저장할 벡터
	private RoomManager roomManager = new RoomManager(); // 방들을 관리할 클래스
	private static final int BUF_LEN = 128; // Windows 처럼 BUF_LEN 을 정의
	private static final String WHITE = "w";
	private static final String BLACK = "b";

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Server frame = new Server();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Server() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 640, 440);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 10, 600, 298);
		contentPane.add(scrollPane);

		textArea = new JTextArea();
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);

		JLabel lblNewLabel = new JLabel("Port Number");
		lblNewLabel.setBounds(200, 318, 87, 26);
		contentPane.add(lblNewLabel);

		txtPortNumber = new JTextField();
		txtPortNumber.setHorizontalAlignment(SwingConstants.CENTER);
		txtPortNumber.setText("30000");
		txtPortNumber.setBounds(280, 318, 199, 26);
		contentPane.add(txtPortNumber);
		txtPortNumber.setColumns(10);

		JButton btnServerStart = new JButton("Server Start");
		btnServerStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					socket = new ServerSocket(Integer.parseInt(txtPortNumber.getText()));
				} catch (NumberFormatException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				AppendText("Chat Server Running..");
				btnServerStart.setText("Chat Server Running..");
				btnServerStart.setEnabled(false); // 서버를 더이상 실행시키지 못 하게 막는다
				txtPortNumber.setEnabled(false); // 더이상 포트번호 수정못 하게 막는다
				AcceptServer accept_server = new AcceptServer();
				accept_server.start();
			}
		});
		btnServerStart.setBounds(12, 356, 600, 35);
		contentPane.add(btnServerStart);
	}

	// 새로운 참가자 accept() 하고 user thread를 새로 생성한다.
	class AcceptServer extends Thread {
		@SuppressWarnings("unchecked")
		public void run() {
			while (true) { // 사용자 접속을 계속해서 받기 위해 while문
				try {
					AppendText("Waiting new clients ...");
					client_socket = socket.accept(); // accept가 일어나기 전까지는 무한 대기중
					AppendText("새로운 참가자 from " + client_socket);
					// User 당 하나씩 Thread 생성
					UserService new_user = new UserService(client_socket);
					allUser.add(new_user); // 새로운 참가자 배열에 추가
					new_user.start(); // 만든 객체의 스레드 실행
					AppendText("현재 참가자 수 " + allUser.size());
				} catch (IOException e) {
					AppendText("accept() error");
					//System.exit(0);
				}
			}
		}
	}

	public void AppendText(String str) {
		// textArea.append("사용자로부터 들어온 메세지 : " + str+"\n");
		textArea.append(str + "\n");
		textArea.setCaretPosition(textArea.getText().length());
	}

	public void AppendObject(ChatMsg msg) {
		// textArea.append("사용자로부터 들어온 object : " + str+"\n");
		String s = String.format("From 【%s】 code = %s ┃ data = %s\n", msg.UserName, msg.code, msg.data);
		textArea.append(s);
		/*textArea.append("code = " + msg.code + "\n");
		textArea.append("id = " + msg.UserName + "\n");
		textArea.append("data = " + msg.data + "\n");*/
		textArea.setCaretPosition(textArea.getText().length());
	}

	// User 당 생성되는 Thread
	// Read One 에서 대기 -> Write All
	class UserService extends Thread {
		private InputStream is;
		private OutputStream os;
		private DataInputStream dis;
		private DataOutputStream dos;

		private ObjectInputStream ois;
		private ObjectOutputStream oos;

		private Socket client_socket;
		private Vector user_vc;
		public String UserName = "";
		public String UserStatus;

		public UserService(Socket client_socket) {
			// TODO Auto-generated constructor stub
			// 매개변수로 넘어온 자료 저장
			this.client_socket = client_socket;
			this.user_vc = allUser;
			try {
				oos = new ObjectOutputStream(client_socket.getOutputStream());
				oos.flush();
				ois = new ObjectInputStream(client_socket.getInputStream());
			} catch (Exception e) {
				AppendText("userService error");
			}
		}

		public void Login() {
			AppendText("새로운 참가자 [" + UserName + "] 입장.");
			WriteOne("Welcome to Java chat server\n");
			WriteOne(UserName + "님 환영합니다.\n"); // 연결된 사용자에게 정상접속을 알림
			String msg = "[" + UserName + "]님이 입장 하였습니다.\n";
			WriteOthers(msg); // 아직 user_vc에 새로 입장한 user는 포함되지 않았다.
		}

		public void Logout() {
			String msg = "[" + UserName + "]님이 퇴장 하였습니다.\n";
			allUser.removeElement(this); // Logout한 현재 객체를 벡터에서 지운다
			WriteAll(msg); // 나를 제외한 다른 User들에게 전송
			this.client_socket = null;
			AppendText("사용자 " + "[" + UserName + "] 퇴장. 현재 참가자 수 " + allUser.size());
		}

		// 모든 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteAll(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.WriteOne(str);
			}
		}
		// 모든 User들에게 Object를 방송. 채팅 message와 image object를 보낼 수 있다
		public void WriteAllObject(ChatMsg obj) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.WriteChatMsg(obj);
			}
		}

		// 나를 제외한 User들에게 방송. 각각의 UserService Thread의 WriteONe() 을 호출한다.
		public void WriteOthers(String str) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user != this && user.UserStatus == "O")
					user.WriteOne(str);
			}
		}

		// 모든 User들에게 List를 방송.
		public void WriteAllList(ChatMsg obj) {
			for (int i = 0; i < user_vc.size(); i++) {
				UserService user = (UserService) user_vc.elementAt(i);
				if (user.UserStatus == "O")
					user.WriteChatList(obj);
			}
		}
		// 방 안의 User들에게 List를 방송.
		public void WriteRoomUserList(ChatMsg obj, Room room) {
			for (int i = 0; i < room.roomUser.size(); i++) {
				String userName = room.roomUser.elementAt(i);
				UserService userService = (UserService) user_vc.elementAt(i);
				if (userService.UserName == userName)
					userService.WriteChatMsg(obj);
			}
		}
		// 클라이언트에게 목록 형식으로 보낼 때 사용
		public void WriteChatList (ChatMsg obj) {
			try {
				oos.writeObject(obj.code);
				oos.writeObject(obj.UserName);
				oos.writeObject(obj.data);
				oos.writeObject(obj.list);
				oos.reset();
			}
			catch (IOException e) {
				AppendText("oos.writeObject(ob) error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
//            Logout();
			}
		}
		// Windows 처럼 message 제외한 나머지 부분은 NULL 로 만들기 위한 함수
		public byte[] MakePacket(String msg) {
			byte[] packet = new byte[BUF_LEN];
			byte[] bb = null;
			int i;
			for (i = 0; i < BUF_LEN; i++)
				packet[i] = 0;
			try {
				bb = msg.getBytes("euc-kr");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (i = 0; i < bb.length; i++)
				packet[i] = bb[i];
			return packet;
		}

		// UserService Thread가 담당하는 Client 에게 1:1 전송
		public void WriteOne(String msg) {
			ChatMsg obcm = new ChatMsg("SERVER", "BROADCAST", msg);
			WriteChatMsg(obcm);
		}

		// 귓속말 전송
		public void WritePrivate(String msg) {
			ChatMsg obcm = new ChatMsg("귓속말", "BROADCAST", msg);
			WriteChatMsg(obcm);
		}
		//
		public void WriteChatMsg(ChatMsg obj) {
			try {
				oos.writeObject(obj.code);
				oos.writeObject(obj.UserName);
				oos.writeObject(obj.data);
				oos.reset();

//             if (obj.code.equals("300")) { // 방 목록 요청
//                oos.writeObject(obj.roomsList);
//             }
			}
			catch (IOException e) {
				AppendText("oos.writeObject(ob) error");
				try {
					ois.close();
					oos.close();
					client_socket.close();
					client_socket = null;
					ois = null;
					oos = null;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Logout();

			}
		}

		public ChatMsg ReadChatMsg() {
			Object obj = null;
			String msg = null;
			ChatMsg cm = new ChatMsg("", "", "");
			// Android와 호환성을 위해 각각의 Field를 따로따로 읽는다.
			try {
				obj = ois.readObject();
				cm.code = (String) obj;
				obj = ois.readObject();
				cm.UserName = (String) obj;
				obj = ois.readObject();
				cm.data = (String) obj;
//            if (cm.code.equals("300")) {
//               obj = ois.readObject();
//               cm.imgbytes = (byte[]) obj;
////               obj = ois.readObject();
////               cm.bimg = (BufferedImage) obj;
//            }
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				Logout();
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Logout();
				return null;
			}
			return cm;
		}

		// run
		public void run() {
			while (true) { // 사용자 접속을 계속해서 받기 위해 while문
				ChatMsg cm = null;
				if (client_socket == null)
					break;
				cm = ReadChatMsg();
				if (cm==null)
					break;
				if (cm.code.length()==0)
					break;
				AppendObject(cm);
				// 로그인
				if (cm.code.matches("LOGIN")) {
					UserName = cm.UserName;
					UserStatus = "O"; // Online 상태
					Login();
				}
				// 로그 아웃
				if (cm.code.matches("LOGOUT")) {
					Logout();
					break;
				}
				// 방 목록 요청
				if (cm.code.matches("ROOMLIST")) {
					ArrayList<String> roomsListInfo = roomManager.getRoomsListInfo();
					ChatMsg obcm = new ChatMsg(UserName,"ROOMLIST","RoomList");
					obcm.setList(roomsListInfo);
					AppendText(UserName+" 에게 방 목록 전송");
					WriteChatList(obcm);
				}
				// 방 생성 요청 : data = roomName maxCount passWd
				if (cm.code.matches("ROOMCREATE")) {
					String[] args = cm.data.split("//"); // 단어들을 분리한다.
					String roomName = args[0];
					int maxCount = Integer.parseInt(args[1]);
					String passWd = args[2];
					if (maxCount < 2) { // 인원 수 미달
						AppendText(UserName + "의 방 생성 요청을 인원 수 미달로 거절합니다.");
						ChatMsg obcm = new ChatMsg(UserName,"ROOMCREATE","MaxCountDown");
						WriteChatMsg(obcm);
					}
					else if (maxCount > 4) { // 인원 수 초과
						AppendText(UserName + "의 방 생성 요청을 인원 수 초과로 거절합니다.");
						ChatMsg obcm = new ChatMsg(UserName,"ROOMCREATE","MaxCountUp");
						WriteChatMsg(obcm);
					}
					else { // 인원 수 적절 -> 방을 생성
						Room room = new Room(roomName, maxCount, passWd);
						roomManager.addRoom(room);
						AppendText("현재 방 수 " + roomManager.roomVec.size());
						ChatMsg obcm = new ChatMsg(UserName,"ROOMCREATE",roomManager.getRoomInfo(room));
						WriteAllObject(obcm); // 모든 유저들에게 방 정보를 알린다.
					}
				}
				// 방 참가 요청 : data = roomId  passWd
				if (cm.code.matches("ROOMIN")) {
					String[] args = cm.data.split("//"); // 단어들을 분리한다.
					String requestedId = args[0]; // 방 ID
					String requestedPassWd = args[1]; // 방 패스워드
					Room room = roomManager.findRoomByPwd(requestedId,requestedPassWd);
					if(room != null) { // 해당 방이 있으면
						if(room.currentCount < room.maxCount ) { // 방 인원 수가 최대 인원 수 보다 적을 때
							room.addUser(this);
							AppendText("방 ["+room.roomName+"] "+"에 "+UserName+" 참가");
							String roomInfo = roomManager.getRoomInfo(room);
							ChatMsg obcm = new ChatMsg(UserName,"ROOMIN",roomInfo);
							WriteAllObject(obcm); // 모든 유저들에게 방 정보 변경을 알린다.
						}
						else { // 방 인원 수가 다 찼을 때
							AppendText("["+room.roomName+"] 의 인원이 다 찼습니다.");
							ChatMsg obcm = new ChatMsg(UserName,"ROOMIN","MaxCount");
							WriteOne("Room count is max");
						}
					}
					else { // 방이 없으면
						AppendText("["+requestedId+"]"+"은 존재하지 않습니다.");
						ChatMsg obcm = new ChatMsg(UserName,"ROOMIN","NotExist"); // 요청 보낸 유저에게 참가 거절 메세지를 보낸다.
						WriteChatMsg(obcm);
					}
				}
				// 방 안의 유저 목록
				if (cm.code.matches("ROOMUSERLIST")) {
					String roomUID = cm.data; // 단어들을 분리한다.
					String users = roomManager.findUsersInRoom(roomUID);
					Room room = roomManager.findRoomByUID(roomUID);
					if (users != null){
						AppendText("방 ["+room.roomName+"] 정보 유저들에게 전송");
						ChatMsg obcm = new ChatMsg(UserName,"ROOMUSERLIST",users);
						WriteRoomUserList(obcm,room); // 방 안의 모든 유저들에게 전송
					}
				}
				// 게임 시작, 코인 배팅
				if (cm.code.matches("600")) {

				}
				// 카드 뽑기
				if (cm.code.matches("700")) {

				}
				// 조커 위치 지정8
				if (cm.code.matches("800")) {

				}
				// 카드 맞추기
				if (cm.code.matches("900")) {

				}
				// 패스
				if (cm.code.matches("1000")) {

				}
				// 카드 공개
				if (cm.code.matches("1100")) {

				}
				// 랭킹보기 요청
				if (cm.code.matches("1200")) {

				}
			} // while
		} // run
	}
	// Room 관리 클래스
	class RoomManager {
		private Vector<Room> roomVec = new Vector<Room>(); // 현재 서버에 존재하는 Room 목록을 저장하는 벡터

		public Vector<Room> getRoomVec() {
			return roomVec;
		}
		public void addRoom(Room r){
			roomVec.add(r);
		}
		public void removeRoom(Room r){
			roomVec.remove(r);
		}

		// 방 정보
		public String getRoomInfo(Room r) {
			return r.roomName+"//"+r.roomUID+"//"+r.maxCount+"//"+r.currentCount;
		}
		// 방 목록
		public ArrayList<String> getRoomsListInfo() {
			ArrayList<String> roomList = new ArrayList<String>();
			for (Room room : roomVec) {
				String info = getRoomInfo(room);
				roomList.add(info);
			}
			return roomList;
		}
		// UID로 방 찾기
		public Room findRoomByUID (String requestedId) {
			for (Room room : roomVec) {
				if(requestedId.equals(room.getRoomUID())){ // getRoomUID()
					return room;
				}
			}
			return null;
		}
		// UID와 패스워드로 방 찾기
		public Room findRoomByPwd (String requestedId, String requestedPasswd) {
			for (Room room : roomVec) {
				if(requestedId.equals(room.getRoomUID())){ // getRoomUID()
					if(requestedPasswd.equals(room.passWd)) {
						return room;
					}
				}
			}
			return null;
		}
		// UID로 방 이용자 모두 찾기
		public String findUsersInRoom (String UID) {
			for (Room room : roomVec) {
				if (UID.equals(room.getRoomUID())){ // getRoomUID()
					return room.getRoomUserList();
				}
			}
			return null;
		}
		public void requestRoomIn(String requestedName) {

		}
	}

	class Room {
		// Room에 들어와 있는 사용자 벡터
		private Vector<String> roomUser;
		private int currentCount = 0;
		private int maxCount;

		private String roomName;
		private UUID roomUID;
		private String passWd;

		private Vector<Card> cards;

		public Room(String roomName, int maxCount, String passWd) {
			roomUser = new Vector<String>();
			this.maxCount = maxCount;
			this.roomName = roomName;
			this.passWd = passWd;
			this.roomUID = UUID.randomUUID(); // Room UID 생성

			String roomInfo = String.format("방 이름: %s / 제한 인원: %d / 비밀번호: %s", roomName, maxCount, passWd);
			AppendText("새로운 방 생성 [" + roomInfo + "]");

			// 카드 초기화
			cards = new Vector<Card>();
			for (int i = 0; i < 12; i++) {
				cards.add(new Card(WHITE,i)); // 'w'hite color 카드
				cards.add(new Card(BLACK,i)); // 'b'lack color 카드
				if (i == 0) {
					cards.add(new Card(WHITE,true));
					cards.add(new Card(BLACK,true));
				}
			}
		}
		// Room UID
		public String getRoomUID() {
			return roomUID.toString();
		}
		// Room에 사용자 추가
		public void addUser (UserService user) {
			roomUser.add(user.UserName);
			user.WriteOne("Welcome to Room");
			currentCount ++;
		}
		public String getRoomUserList() {
			String users = "";
			for (String userName : roomUser) {
				users += userName+"//";
			}
			System.out.println("getRoomUserList="+users);
			return users;
		}
		// 게임 시작
		public void startGame() {
			// 카드 나눠주기 // [2-3인] : 4개, [4인] : 3개


		}



	}
	// 카드 클래스
	class Card {
		private String owner;
		private int cardNum; // 0~11
		private String cardColor;
		private Boolean isJocker;

		public Card(String color, int num) {
			owner = "RoomId";
			cardColor = color;
			cardNum = num;
			isJocker = false;
		}
		public Card(String color, Boolean isJocker) {
			owner = "RoomId";
			cardColor = color;
			cardNum = -1;
			this.isJocker = isJocker;
		}
		public void setOwner(String owner) {
			this.owner = owner;
		}
	}

}