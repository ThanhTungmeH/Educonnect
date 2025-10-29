package org.example.educonnect1.Server;

import org.example.educonnect1.Server.Commands.LoginCommand;
import org.example.educonnect1.Server.Commands.SignupCommand;
import org.example.educonnect1.Server.Commands.VerifyCommand;
import org.example.educonnect1.Server.dao.UserDAO;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class TCPServer {
    private static final int PORT = 2005;
    private static UserDAO userDAO = new UserDAO();
    private static Map<String, Command> commandMap = new HashMap<>();

    public static void main(String[] args) {
        commandMap.put("LOGIN", new LoginCommand(userDAO));
        commandMap.put("SIGNUP", new SignupCommand(userDAO));
        commandMap.put("VERIFY", new VerifyCommand(userDAO));
//        commandMap.put("SEARCH_FRIEND", new SearchFriendCommand(userDAO));
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            String action = (String) in.readObject();
            Command cmd = commandMap.get(action);
            if (cmd != null) {
                cmd.execute(in, out);
            } else {
                out.writeObject("UNKNOWN_ACTION");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

