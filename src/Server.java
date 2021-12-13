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
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.SwingConstants;

public class Server extends JFrame implements Serializable {

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
        textArea.setFont(new Font("D2Coding", Font.BOLD, 16));
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
                AppendText("[Davinci Code] Server Running~!");
                btnServerStart.setText("[Davinci Code] Server Running~!");
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
                    //AppendText("Waiting new clients ...");
                    client_socket = socket.accept(); // accept가 일어나기 전까지는 무한 대기중
                    System.out.println("새로운 참가자 from " + client_socket);
                    // User 당 하나씩 Thread 생성
                    UserService new_user = new UserService(client_socket);
                    allUser.add(new_user); // 새로운 참가자 배열에 추가
                    new_user.start(); // 만든 객체의 스레드 실행
                    System.out.println("현재 참가자 수 : " + allUser.size());
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
        System.out.print(s);
        //textArea.append(s);
		/*textArea.append("code = " + msg.code + "\n");
		textArea.append("id = " + msg.UserName + "\n");
		textArea.append("data = " + msg.data + "\n");*/
        textArea.setCaretPosition(textArea.getText().length());
    }

    // User 당 생성되는 Thread
    // Read One 에서 대기 -> Write All
    class UserService extends Thread implements Serializable {
        private static final long serialVersionUID = 1L;

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
        public String score;

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

        public String getScore() {
            return score;
        }

        public void setScore(String rank) {

        }

        public void Login() {
            AppendText("새로운 참가자 【" + UserName + "】 입장.");
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
            AppendText("【" + UserName + "】 퇴장. 현재 참가자 수 " + allUser.size());
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
        public void WriteRoomList(ChatMsg obj, Room room) {
            for (int i = 0; i < user_vc.size(); i++) { // 전체 유저 이름 중
                UserService userService = (UserService) user_vc.elementAt(i);

                for (int j = 0; j < room.usersList.size(); j++) { // 방 유저 이름이 같으면
                    String roomUserName = room.usersList.get(j);
                    if (userService.UserName == roomUserName) {
                        userService.WriteChatList(obj);
                        break;
                    }
                }
            }
        }

        // 방 안의 User들에게 방송.
        public void WriteRoomUsers(ChatMsg obj, Room room) {
            for (int i = 0; i < user_vc.size(); i++) { // 전체 유저 이름 중
                UserService userService = (UserService) user_vc.elementAt(i);

                for (int j = 0; j < room.usersList.size(); j++) { // 방 유저 이름이 같으면
                    String roomUserName = room.usersList.get(j);

                    if (userService.UserName == roomUserName) {
                        obj.UserName = roomUserName;
                        userService.WriteChatMsg(obj);
                        break;
                    }
                }
            }
        }

        // 방 안의 User들에게 카드 방송.
        public void WriteRoomCardInfo(ChatMsg obj, Room room) {
            for (int i = 0; i < user_vc.size(); i++) { // 전체 유저 이름 중
                UserService userService = (UserService) user_vc.elementAt(i);

                for (int j = 0; j < room.usersList.size(); j++) { // 방 유저 이름이 같으면
                    String roomUserName = room.usersList.get(j);
                    if (userService.UserName == roomUserName) {
                        userService.WriteChatMsg(obj);
                        break;
                    }
                }
            }
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

        public void WriteChatMsg(ChatMsg obj) {
            try {
                oos.writeObject(obj.code);
                oos.writeObject(obj.UserName);
                oos.writeObject(obj.data);
                oos.reset();
            } catch (IOException e) {
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

        // 클라이언트에게 목록 형식으로 보낼 때 사용
        public void WriteChatList(ChatMsg obj) {
            try {
                oos.writeObject(obj.code);
                oos.writeObject(obj.UserName);
                oos.writeObject(obj.data);
                oos.writeObject(obj.list);
                oos.reset();
            } catch (IOException e) {
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

        // run
        public void run() {
            while (true) { // 사용자 접속을 계속해서 받기 위해 while문
                ChatMsg cm = null;
                if (client_socket == null)
                    break;
                cm = ReadChatMsg();
                if (cm == null)
                    break;
                if (cm.code.length() == 0)
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
                    //AppendText(UserName + "에게 방 목록을 전송합니다.");
                    ChatMsg obcm = new ChatMsg(UserName, "ROOMLIST", "RoomList");
                    obcm.setList(roomsListInfo);
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
                        ChatMsg obcm = new ChatMsg(UserName, "ROOMCREATE", "MaxCountDown");
                        WriteChatMsg(obcm);
                    } else if (maxCount > 4) { // 인원 수 초과
                        AppendText(UserName + "의 방 생성 요청을 인원 수 초과로 거절합니다.");
                        ChatMsg obcm = new ChatMsg(UserName, "ROOMCREATE", "MaxCountUp");
                        WriteChatMsg(obcm);
                    } else { // 인원 수 적절 -> 방을 생성
                        AppendText(UserName + "이(가) 요청한 방을 생성합니다. (현재 방 " + roomManager.roomVec.size() + "개)");
                        Room room = new Room(roomName, maxCount, passWd);
                        roomManager.addRoom(room);
                        ChatMsg obcm = new ChatMsg(UserName, "ROOMCREATE", roomManager.getRoomInfo(room));
                        WriteAllObject(obcm); // 모든 유저들에게 방 정보를 알린다.
                    }
                }
                // 방 참가 요청 : data = roomId  passWd
                if (cm.code.matches("ROOMIN")) {
                    String[] args = cm.data.split("//"); // 단어들을 분리한다.
                    String requestedId = args[0]; // 방 ID
                    String requestedPassWd = args[1]; // 방 패스워드
                    Room room = roomManager.findRoomByPwd(requestedId, requestedPassWd);
                    if (room != null) { // 해당 방이 있으면
                        if (room.currentCount < room.maxCount) { // 방 인원 수가 최대 인원 수 보다 적을 때
                            roomManager.addUser(room, this);
                            AppendText("방 [" + room.roomName + "] " + UserName + " 참가.");
                            String roomInfo = roomManager.getRoomInfo(room);
                            ChatMsg obcm = new ChatMsg(UserName, "ROOMIN", roomInfo);
                            WriteAllObject(obcm); // 모든 유저들에게 방 정보 변경을 알린다.
                        } else { // 방 인원 수가 다 찼을 때
                            AppendText("[" + room.roomName + "] 인원이 다 찼습니다.");
                            ChatMsg obcm = new ChatMsg(UserName, "ROOMIN", "MaxCount");
                            WriteOne("Room count is max");
                        }
                    } else { // 방이 없으면
                        AppendText("[" + requestedId + "]" + "은(는) 존재하지 않습니다.");
                        ChatMsg obcm = new ChatMsg(UserName, "ROOMIN", "NotExist"); // 요청 보낸 유저에게 참가 거절 메세지를 보낸다.
                        WriteChatMsg(obcm);
                    }
                }
                // 방 안의 유저 목록
                if (cm.code.matches("ROOMUSERLIST")) {
                    String roomUID = cm.data; // 단어들을 분리한다.
                    ArrayList<String> users = roomManager.findUsersInRoom(roomUID);
                    Room room = roomManager.findRoomByUID(roomUID);
                    if (users != null) {
                        AppendText("방 [" + room.roomName + "] " + UserName + "에게 방참가자 목록 전송.");
                        ChatMsg obcm = new ChatMsg(UserName, "ROOMUSERLIST", "send room user list!");
                        obcm.setList(users);
                        WriteRoomList(obcm, room);
                        WriteRoomList(obcm, room); // 방 안의 모든 유저들에게 전송
                        WriteRoomList(obcm, room); // 방 안의 모든 유저들에게 전송
                    }
                }
                // 방 삭제
                if (cm.code.matches("ROOMREMOVE")) {
                    String roomUID = cm.data; // 삭제 할 RoomUID
                    Room room = roomManager.findRoomByUID(roomUID);
                    roomManager.removeRoom(room); // Room 삭제
                    AppendText("방 [" + room.roomName + "] 이 삭제되었습니다.");
                    ChatMsg obcm = new ChatMsg(UserName, "ROOMREMOVE", roomUID);
                    WriteAllObject(obcm);
                }
                // 게임 시작, 코인 배팅
                if (cm.code.matches("GAMESTART")) {
                    String roomUID = cm.data; // 단어들을 분리한다.
                    ArrayList<String> users = roomManager.findUsersInRoom(roomUID); // 방 안의 유저 목록
                    Room room = roomManager.findRoomByUID(roomUID);
                    if (users != null) {
                        AppendText("\t**********방 [" + room.roomName + "] 게임 시작**********");
                        ChatMsg obcm = new ChatMsg(UserName, "GAMESTART", roomUID);
                        WriteRoomUsers(obcm, room); // 방 안의 모든 유저들에게 전송
                    }
                    room.startGame();
                }
                // 카드 배정
                if (cm.code.matches("READY")) {
                    String roomUID = cm.data;
                    Room room = roomManager.findRoomByUID(roomUID);
                    CardManager cardManager = room.getCardManager();
                    ArrayList<String> users = roomManager.findUsersInRoom(roomUID); // 방 안의 유저 목록
                    AppendText("\t**********방 [" + room.roomName + "] 카드 배정 시작**********");
                    for (String userName : room.usersList) {
                        ChatMsg obcm = new ChatMsg(userName, "READY", userName + "의 카드 리스트 전송");
                        obcm.setList(cardManager.getObserverByName(userName).getCardsList()); // 방 유저 카드리스트 등록
                        WriteRoomList(obcm, room);
                    }
                }
                // 시작 턴
                if (cm.code.matches("START")) {
                    String roomUID = cm.data;
                    Room room = roomManager.findRoomByUID(roomUID);
                    String fitstUser = room.startTurn();
                    ChatMsg obcm = new ChatMsg(UserName, "TURN", fitstUser); // 다음 차례 유저 이름 방송
                    WriteChatMsg(obcm);
                }
                // 턴 
                if (cm.code.matches("TURN")) {
                    String roomUID = cm.data;
                    Room room = roomManager.findRoomByUID(roomUID);
                    String nextUser = room.nextTurn(UserName); // 현재 차례의 유저 이름을 보내면 다음 차례의 유저 이름을 리턴
                    AppendText(nextUser + "의 턴입니다.");
                    ChatMsg obcm = new ChatMsg(UserName, "TURN", nextUser); // 다음 차례 유저 이름 방송
                    WriteRoomUsers(obcm, room);
                }
                // 카드 뽑기
                if (cm.code.matches("TAKECARD")) {
                    String[] args = cm.data.split("//");
                    String roomUID = args[0];
                    String cardColor = args[1];
                    Room room = roomManager.findRoomByUID(roomUID);
                    CardManager cardManager = room.getCardManager();
                    String cardInfo = cardManager.takeCard(UserName, cardColor);
                    AppendText(UserName + "이(가) 카드를 뽑았습니다.");
                    AppendText("뽑은 카드: "+cardInfo + " (남은 카드 수 "+cardManager.getCurrentCardSize()+")");
                    ChatMsg obcm = new ChatMsg(UserName, "TAKECARD", cardInfo); // 랜덤 카드 정보 전송
                    WriteRoomCardInfo(obcm, room);
                }
                // 카드 맞추기
                if (cm.code.matches("MATCHCARD")) {
                    String[] args = cm.data.split("//");
                    String cardOwner = args[0];
                    String cardInfo = args[1];
                    String cardIndex = args[2];
                    String roomUID = args[3];
                    Room room = roomManager.findRoomByUID(roomUID);
                    CardManager cardManager = room.getCardManager();

                    AppendText(UserName + "이(가) " + cardOwner + "의 " + (Integer.parseInt(cardIndex) + 1) + "번째 카드를 지목했습니다.");
                    AppendText("예상 값: [" + cardInfo + "]");

                    String ownerCardIndex = cardManager.matchCard(cardOwner, cardInfo, Integer.parseInt(cardIndex));
                    if (ownerCardIndex != null) { // 카드 맞추기 성공 시
                        AppendText("SUCCESS!!!");
                        ChatMsg obcm = new ChatMsg(UserName, "SUCCESS", "match card success!");
                        WriteRoomCardInfo(obcm, room);
                        // 카드 주인의 카드 정보 방송
                        //ChatMsg obcm2 = new ChatMsg(cardOwner, "CARDOPEN", ownerCardIndex); // [index]
                        obcm = new ChatMsg(cardOwner, "CARDOPEN", ownerCardIndex); // [index]
                        //WriteRoomCardInfo(obcm2, room);
                        WriteRoomCardInfo(obcm, room);
                    } else { // 카드 맞추기 실패 시
                        AppendText("FAIL!!!");
                        ChatMsg obcm = new ChatMsg(UserName, "FAIL", "match card fail!");
                        WriteChatMsg(obcm);
                        String userCardIndex = cardManager.getObserverByName(UserName).newCardOpen();
                        // 유저의 카드 정보 방송
                        //ChatMsg obcm2 = new ChatMsg(UserName, "CARDOPEN", userCardIndex); // [index]
                        obcm = new ChatMsg(UserName, "CARDOPEN", userCardIndex); // [index]
                        //WriteRoomCardInfo(obcm2, room);
                        WriteRoomCardInfo(obcm, room);
                    }
                }
                // 패스
                if (cm.code.matches("PASS")) {
                    String roomUID = cm.data;
                    Room room = roomManager.findRoomByUID(roomUID);
                    String nextUser = room.nextTurn(UserName); // 현재 차례의 유저 이름을 보내면 다음 차례의 유저 이름을 리턴
                    AppendText(UserName + "이(가) PASS를 선택했습니다.");
                    AppendText(nextUser + "의 턴 입니다.");
                    ChatMsg obcm = new ChatMsg(UserName, "TURN", nextUser); // 다음 차례 유저 이름 방송
                    WriteRoomUsers(obcm, room);
                }
                // JOKER
                if (cm.code.matches("JOKER")) {
                    String[] args = cm.data.split("//");
                    String roomUID = args[0];
                    String cardColor = args[1];
                    String isOpened = args[2];
                    String from = args[3];
                    String to = args[4];
                    Room room = roomManager.findRoomByUID(roomUID);
                    CardManager cardManager = room.getCardManager();
                    AppendText(UserName+"이(가) 조커 위치를 이동.");
                    cardManager.joker(UserName,from,to);
                    String msg = roomUID+"//"+cardColor+"//"+isOpened+"//"+from+"//"+to+"//"+UserName;
                    ChatMsg obcm = new ChatMsg(UserName, "JOKER", msg);
                    WriteRoomUsers(obcm,room);
                }
                // 랭킹보기 요청
                if (cm.code.matches("RANK")) {
                    String roomUID = cm.data;
                }
            } // while
        } // run
    }

    // Room 관리 클래스
    class RoomManager {
        private Vector<Room> roomVec = new Vector<Room>(); // 현재 서버에 존재하는 Room 목록을 저장하는 벡터
        private HashMap<Object, String> allRoomUsers = new HashMap<Object, String>();

        public Vector<Room> getRoomVec() {
            return roomVec;
        }

        public void addRoom(Room r) {
            roomVec.add(r);
        }

        public void removeRoom(Room r) {
            roomVec.remove(r);
        }

        // 방 정보
        public String getRoomInfo(Room r) {
            return r.roomName + "//" + r.roomUID + "//" + r.maxCount + "//" + r.currentCount;
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
        public Room findRoomByUID(String requestedId) {
            for (Room room : roomVec) {
                if (requestedId.equals(room.getRoomUID())) { // getRoomUID()
                    return room;
                }
            }
            return null;
        }

        // UID와 패스워드로 방 찾기
        public Room findRoomByPwd(String requestedId, String requestedPasswd) {
            for (Room room : roomVec) {
                if (requestedId.equals(room.getRoomUID())) { // getRoomUID()
                    if (requestedPasswd.equals(room.passWd)) {
                        return room;
                    }
                }
            }
            return null;
        }

        // UID로 방 이용자 모두 찾기
        public ArrayList<String> findUsersInRoom(String UID) {
            for (Room room : roomVec) {
                if (UID.equals(room.getRoomUID())) { // getRoomUID()
                    return room.usersList;
                }
            }
            return null;
        }

        // 방 유저 입장
        public void addUser(Room room, UserService userService) {
            room.addUser(userService);
            allRoomUsers.put(userService, String.valueOf(room.roomUID));
        }

        // 방 유저 퇴장
        public void removeUser(UserService userService) {
            Room room = findRoomByUID(allRoomUsers.get(userService));
            allRoomUsers.remove(userService);
            room.removeUser(userService);

        }
    }

    // ROOM
    class Room {
        private ArrayList<String> usersList = new ArrayList<>(); // Room에 들어와 있는 사용자 리스트
        private int currentCount = 0;
        private int maxCount;

        private String roomName;
        private UUID roomUID;
        private String passWd;

        private CardManager cardManager;

        private int firstUserIndex = -1;
        private int startFlag = 0;

        public Room(String roomName, int maxCount, String passWd) {
            this.maxCount = maxCount;
            this.roomName = roomName;
            this.passWd = passWd;
            this.roomUID = UUID.randomUUID(); // Room UID 생성

            String roomInfo = String.format("방 이름: %s / 제한 인원: %d / 비밀번호: %s", roomName, maxCount, passWd);
            AppendText("[" + roomInfo + "]");
        }
        // Room UID
        public String getRoomUID() {
            return roomUID.toString();
        }
        // Room에 사용자 추가
        public void addUser(UserService user) {
            usersList.add(user.UserName);
            user.WriteOne("Welcome to Room");
            currentCount++;
        }
        // Room에 사용자 제거
        public void removeUser(UserService user) {
            usersList.remove(user.UserName);
            currentCount--;
        }
        // 게임 시작, user observer 생성
        public void startGame() {
            this.cardManager = new CardManager(currentCount);
            for (String s : usersList) {
                Observer o = new Observer(s);
                cardManager.attach(o); // 사용자 수만큼 observer(with user이름) 추가
            }
            cardManager.init(); // 카드 초기화
            cardManager.ready(); // 카드 나눠주기
        }
        public CardManager getCardManager() {
            return cardManager;
        }
        // 시작 턴
        public String startTurn() {
            if (startFlag == 0) {
                firstUserIndex = new Random().nextInt(usersList.size());
                System.out.println("firstIndex: " + firstUserIndex + "(" + usersList.get(firstUserIndex) + ")");
                startFlag = 1;
                return usersList.get(firstUserIndex);
            } else {
                AppendText(usersList.get(firstUserIndex) + "의 턴 입니다.");
                return usersList.get(firstUserIndex);
            }
        }
        // 다음 차례 유저 리턴
        public String nextTurn(String currnetUser) {
            for (int i = 0; i < usersList.size(); i++) {
                if (usersList.get(i).equals(currnetUser)) {
                    if (i == (usersList.size() - 1))
                        return usersList.get(0);
                    else
                        return usersList.get(i + 1);
                }
            }
            return currnetUser;
        }
    }

    // Card 관리 클래스
    class CardManager extends Subject {
        private int count;

        public CardManager(int count) {
            this.count = count;
        }

    }

    // 방 유저 observer 관리
    abstract class Subject {
        private List<Observer> observers = new ArrayList<Observer>();
        private Vector<Card> RoomCards; // 남아있는 카드 벡터
        private ArrayList<Integer> selectedNum ;

        public Observer getObserverByName(String UserName) {
            for (Observer o : observers) {
                if (o.owner.equals(UserName)) {
                    return o;
                }
            }
            return null;
        }
        public void attach(Observer observer) {
            observers.add(observer);
        }
        public void detach(Observer observer) {
            observers.remove(observer);
        }
        public void notifyObservers() {
            for (Observer o : observers) {
                o.update();
            }
        }
        // 카드 초기화
        public void init() {
            // 카드 초기화 (WHITE 0-11, BLACK 0-11, WHITE 조커, BLACK 조커)
            RoomCards = new Vector<Card>();
            for (int i = 0; i < 12; i++) {
                RoomCards.add(new Card(WHITE, i)); // 'w'hite color 카드
                RoomCards.add(new Card(BLACK, i)); // 'b'lack color 카드
            }
            RoomCards.add(new Card(WHITE, true));
            RoomCards.add(new Card(BLACK, true));
        }
        // 카드 나눠주기 // [2-3인] : 4개, [4인] : 3개
        public void ready() {
            int randomIndex;
            int flag = 0;
            int j;
            Random random = new Random();
            for (Observer o : observers) { // 방의 사용자 수만큼 돌기
                for (int i = 0; i < 3; i++) {
                    do { // 중복 없이 랜덤 숫자 뽑기
                        randomIndex = random.nextInt(RoomCards.size()); // 총 카드 수: 26

                        if (selectedNum.size() == 0) break;

                        for (j = 0; j < selectedNum.size(); j++) { // selectedNum: 뽑힌 카드 번호 리스트
                            if (selectedNum.get(j) == RoomCards.get(randomIndex).cardNum) {  // 뽑힌 카드 번호와 RoomCard의 랜덤 카드의 번호와 비교
                                break;
                            }
                        }
                        if (j == selectedNum.size()) flag = 0;
                        else flag = 1;

                    } while (flag == 1);

                    int selected = RoomCards.get(randomIndex).cardNum; // 뽑힌 카드 번호
                    selectedNum.add(selected);

                    Card c = RoomCards.get(randomIndex); // Room이 owner인 카드 벡터에서 랜덤 카드 꺼내기
                    c.setOwner(o.owner);
                    o.cards.add(c);
                    o.sort();
                    RoomCards.remove(randomIndex);// Room이 owner인 카드 벡터에서 랜덤 카드 제거
                }
                System.out.println(o.owner + "의 카드리스트: ");
                o.print();
                System.out.println("------------------------------------------");
            }
        }
        // 카드 뽑기
        public String takeCard(String owner, String color) {
            selectedNum = new ArrayList<>();
            Observer o = getObserverByName(owner);
            int randomIndex;
            Card c;
            Random random = new Random();
            System.out.println("원하는 컬러: " + color);

            if (RoomCards.size() == 0)
                return "EMPTY";

            while (true) {
                randomIndex = random.nextInt(RoomCards.size()); // 남아있는 카드 중 하나 뽑기
                c = RoomCards.get(randomIndex); // Room이 owner인 카드 벡터에서 랜덤 카드 꺼내기
                if (c.cardColor.equals(color)) break;
            }

            selectedNum.add(randomIndex); // 선택된 숫자를 selectedNum리스트에 저장

            c.setOwner(owner);
            System.out.println("뽑힌 컬러: " + c.cardColor);
            o.cards.add(c);
            o.newCardIndex = o.cards.indexOf(c);
            o.sort();
            RoomCards.remove(randomIndex);// Room이 owner인 카드 벡터에서 랜덤 카드 제거

            return c.cardColor + c.cardNum; // 카드 색깔+카드 번호 리턴
        }
        // 카드 맞추기
        public String matchCard(String UserName, String answer, int cardIndex) { // answer = "b11"
            System.out.println("answer: " + answer);
            System.out.println("index:" + cardIndex);
            Observer observer = getObserverByName(UserName); // 맞춰지는 카드 주인 observer
            String color = answer.substring(0, 1);
            int number = Integer.parseInt(answer.substring(1));

            return observer.matchCardInfo(color, number, cardIndex); // 정답이면 카드 정보를, 아니면 null를 리턴
        }
        // 조커 upate
        public void joker(String UserName, String from, String to) {
            Observer observer = getObserverByName(UserName); // 카드 주인 observer
            Card joker = observer.cards.get(Integer.parseInt(from));
            AppendText("조커의 원래 위치: "+from);
            observer.cards.remove(joker);
            observer.cards.add(joker);
            AppendText("조커의 바뀐 위치: "+to);
        }
        // 남은 카드 개수
        public String getCurrentCardSize() {
            return String.valueOf(RoomCards.size());
        }
    }

    class Observer {
        private String owner;
        private Vector<Card> cards;
        private int newCardIndex;
        private Comparator<Card> sortCard;

        public Observer(String owner) {
            this.owner = owner;
            cards = new Vector<>();
        }

        public void update() {
            //cards =
        }
        public void print() {
            for (Card c : cards) {
                System.out.println(c.cardColor + c.cardNum);
            }
        }
        public String matchCardInfo(String color, int num, int index) {
            Card card = cards.get(index);
            if (Objects.equals(card.cardColor, color) && card.cardNum == num) {
                AppendText("실제 값: [" + card.cardColor + card.cardNum + "]");
                return String.valueOf(index);
            }
            return null;
        }
        public ArrayList<String> getCardsList() {
            ArrayList<String> list = new ArrayList<>();
            for (Card c : cards) {
                list.add(c.cardColor + c.cardNum);
            }
            return list;
        }
        // new Card 오픈
        public String newCardOpen() {
            return String.valueOf(newCardIndex);
        }
        // 카드 정렬
        public void sort() {
            sortCard = new Comparator<Card>() {
                @Override
                public int compare(Card o1, Card o2) {
                    // o2 - o1 리턴하면 내림차순, -1 곱하면 오름차순

                    if (o2.cardNum - o1.cardNum == 0) { // 수가 같을 경우에
                        return compareByColor(o1.cardColor, o2.cardColor);
                    } else {
                        return (o2.cardNum - o1.cardNum) * (-1);
                    }

                }

                public int compareByColor(String o1Color, String o2Color) {
                    if (o1Color.equals(o2Color)) return 0;
                    if (o2Color.equals("w") && o1Color.equals("b")) return -1;
                    else return 1;
                }

            };
            cards.sort(sortCard); // 정렬
        }
    }

    class Card implements Serializable {
        private static final long serialVersionUID = 1L;

        private String owner;
        private int cardNum; // 0~11
        private String cardColor;
        private Boolean isJocker;
        private Boolean isOpened;
        private Boolean isNewOpened;

        public Card(String color, int num) {
            this.owner = "RoomId";
            this.cardColor = color;
            this.cardNum = num;
            this.isJocker = false;
            this.isOpened = false;
            this.isNewOpened = false;
        }

        public Card(String color, Boolean isJocker) {
            this.owner = "RoomId";
            this.cardColor = color;
            this.cardNum = -1;
            this.isJocker = isJocker;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }


    }
}