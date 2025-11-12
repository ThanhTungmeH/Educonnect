package org.example.educonnect1.client.utils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.locks.ReentrantLock;

public class SocketManager {
    private static SocketManager instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final ReentrantLock lock = new ReentrantLock();
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 2005;
    private static final int MAX_RETRY = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private boolean isConnected = false;

    private SocketManager() {
    }

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    /**
     * Kết nối tới server (tự động gọi nếu chưa connect)
     */
    public void connect() throws IOException {
        lock.lock();
        try {
            if (isConnected && socket != null && !socket.isClosed()) {
                return; // Đã connected rồi
            }

            int retries = 0;
            IOException lastException = null;

            while (retries < MAX_RETRY) {
                try {
                    System.out.println("Connecting to server... Attempt " + (retries + 1));
                    socket = new Socket(SERVER_HOST, SERVER_PORT);
                    out = new ObjectOutputStream(socket.getOutputStream());
                    out.flush(); // Important: flush after creating
                    in = new ObjectInputStream(socket.getInputStream());
                    isConnected = true;
                    System.out.println("✓ Connected to server successfully!");
                    return;

                } catch (IOException e) {
                    lastException = e;
                    retries++;

                    if (retries < MAX_RETRY) {
                        System.err.println("Connection failed. Retrying in " + RETRY_DELAY_MS + "ms...");
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Connection interrupted", ie);
                        }
                    }
                }
            }
            // Nếu retry hết mà vẫn fail
            throw new IOException("Failed to connect after " + MAX_RETRY + " attempts", lastException);
        } finally {
            lock.unlock();
        }
    }
    /**
     * Gửi request tới server
     * @param action Loại action (LOGIN, SIGNUP, VERIFY, etc.)
     * @param params Các tham số kèm theo
     */
    public synchronized void sendRequest(String action, Object... params) throws IOException {
        ensureConnected();
        lock.lock();
        try {
            out.writeObject(action);
            for (Object param : params) {
                out.writeObject(param);
            }
            out.flush();

        } catch (SocketException e) {
            // Kết nối bị đứt, thử reconnect
            isConnected = false;
            System.err.println("Connection lost. Attempting to reconnect...");
            connect();

            // Retry gửi lại request
            out.writeObject(action);
            for (Object param : params) {
                out.writeObject(param);
            }
            out.flush();

        } finally {
            lock.unlock();
        }
    }
    /**
     * Đọc response từ server
     */
    public synchronized Object readResponse() throws IOException, ClassNotFoundException {
        ensureConnected();
        lock.lock();
        try {
            return in.readObject();
        } catch (SocketException e) {
            isConnected = false;
            throw new IOException("Connection lost while reading response", e);

        } finally {
            lock.unlock();
        }
    }
    /**
     * Đảm bảo đã kết nối trước khi thực hiện thao tác
     */
    private void ensureConnected() throws IOException {
        if (!isConnected || socket == null || socket.isClosed()) {
            connect();
        }
    }

    public void disconnect() {
        lock.lock();
        try {
            if (socket != null && !socket.isClosed()) {
                try {
                    // Gửi tín hiệu disconnect cho server (optional)
                    out.writeObject("DISCONNECT");
                    out.flush();
                } catch (IOException e) {
                    // Ignore error khi đóng
                }
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignore
                }
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
                isConnected = false;
                System.out.println("Disconnected from server");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reset singleton instance (dùng cho testing hoặc logout)
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.disconnect();
            instance = null;
        }
    }
}