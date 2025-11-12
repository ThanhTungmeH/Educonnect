package org.example.educonnect1.Server;

import org.example.educonnect1.Server.Commands.*;
import org.example.educonnect1.Server.dao.UserDAO;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class TCPServer {
    private static final int PORT = 2005;
    private static final int MAX_THREADS = 50; // Tối đa 50 clients đồng thời
    private static final int SOCKET_TIMEOUT = 5 * 60 * 1000; // 5 phút timeout

    private static UserDAO userDAO = new UserDAO();
    private static Map<String, Command> commandMap = new HashMap<>();
    private static ExecutorService threadPool;
    private static ServerSocket serverSocket;
    private static volatile boolean isRunning = true;

    // Track active connections
    private static final ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Khởi tạo command map
        initializeCommands();

        // Tạo thread pool
        threadPool = Executors.newFixedThreadPool(MAX_THREADS);

        // Thêm shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(TCPServer::shutdown));

        try {
            serverSocket = new ServerSocket(PORT);
            serverSocket.setSoTimeout(0); // No timeout for accept()
            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║   EduConnect Server Started!           ║");
            System.out.println("║   Port: " + PORT + "                            ║");
            System.out.println("║   Status: READY                        ║");
            System.out.println("╚════════════════════════════════════════╝");

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(SOCKET_TIMEOUT);
                    String clientId = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
                    ClientHandler handler = new ClientHandler(clientSocket, clientId);
                    activeClients.put(clientId, handler);
                    threadPool.submit(handler);
                } catch (SocketTimeoutException e) {
                    // Normal timeout, continue
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeCommands() {
        commandMap.put("LOGIN", new LoginCommand(userDAO));
        commandMap.put("SIGNUP", new SignupCommand(userDAO));
        commandMap.put("VERIFY", new VerifyCommand(userDAO));
        // Thêm các commands khác ở đây
        // commandMap.put("SEARCH_FRIEND", new SearchFriendCommand(userDAO));
        // commandMap.put("UPDATE_PROFILE", new UpdateProfileCommand(userDAO));
    }

    /**
     * ClientHandler - Xử lý từng client trong persistent connection
     */
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final String clientId;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        public ClientHandler(Socket socket, String clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                // Khởi tạo streams
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                System.out.println("→ Client handler started for: " + clientId);
                // Vòng lặp xử lý requests từ client
                while (!socket.isClosed() && isRunning) {
                    try {
                        // Đọc action từ client
                        String action = (String) in.readObject();
                        if ("PING".equals(action)) {
                            out.writeObject("PONG");
                            out.flush();
                            // Không log để tránh spam console
                            continue;
                        }
                        // Xử lý disconnect
                        if ("DISCONNECT".equals(action)) {
                            System.out.println("← Client requested disconnect: " + clientId);
                            break;
                        }
                        System.out.println("Processing: " + action + " from " + clientId);
                        // Tìm và thực thi command
                        Command cmd = commandMap.get(action);
                        if (cmd != null) {
                            cmd.execute(in, out);
                            out.flush();
                        } else {
                            out.writeObject("UNKNOWN_ACTION");
                            out.flush();
                            System.err.println("Unknown action: " + action);
                        }
                    } catch (EOFException e) {
                        System.out.println("← Client disconnected (EOF): " + clientId);
                        break;
                    } catch (SocketException e) {
                        System.out.println("← Client connection lost: " + clientId);
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Invalid object received from " + clientId);
                        e.printStackTrace();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client " + clientId + ": " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();

                activeClients.remove(clientId);
                System.out.println("✗ Client disconnected: " + clientId + " (Total: " + activeClients.size() + ")");
            } catch (IOException e) {
                System.err.println("Error closing resources for " + clientId);
            }
        }
    }

    /**
     * Graceful shutdown
     */
    private static void shutdown() {
        System.out.println("\n⚠ Shutting down server...");
        isRunning = false;
        // Đóng tất cả client connections
        for (ClientHandler handler : activeClients.values()) {
            try {
                if (handler.socket != null && !handler.socket.isClosed()) {
                    handler.socket.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        // Shutdown thread pool
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        // Đóng server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        }

        System.out.println("✓ Server shutdown complete");
    }

    /**
     * Lấy số lượng clients đang kết nối
     */
    public static int getActiveClientCount() {
        return activeClients.size();
    }
}