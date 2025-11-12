package org.example.educonnect1.client.utils;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class SocketManager {
    private static SocketManager instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final ReentrantLock lock = new ReentrantLock(true); // Fair lock

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 2005;
    private static final int MAX_RETRY = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private volatile boolean isConnected = false;

    private SocketManager() {
    }

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    public void connect() throws IOException {
        lock.lock();
        try {
            if (isConnected && socket != null && !socket.isClosed()) {
                return;
            }

            int retries = 0;
            IOException lastException = null;

            while (retries < MAX_RETRY) {
                try {
                    System.out.println("🔌 Connecting to server... Attempt " + (retries + 1));
                    socket = new Socket(SERVER_HOST, SERVER_PORT);
                    out = new ObjectOutputStream(socket.getOutputStream());
                    out.flush();
                    in = new ObjectInputStream(socket.getInputStream());
                    isConnected = true;
                    System.out.println("✅ Connected to server successfully!");
                    return;

                } catch (IOException e) {
                    lastException = e;
                    retries++;

                    if (retries < MAX_RETRY) {
                        System.err.println("⚠️ Connection failed. Retrying in " + RETRY_DELAY_MS + "ms...");
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Connection interrupted", ie);
                        }
                    }
                }
            }
            throw new IOException("❌ Failed to connect after " + MAX_RETRY + " attempts", lastException);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gửi request và đọc 2 responses (SUCCESS/FAILURE + data) trong 1 lock
     * Đảm bảo không thread nào khác can thiệp vào giữa
     */
    public Object[] sendRequestAndRead2Responses(String action, Object... params)
            throws IOException, ClassNotFoundException {
        ensureConnected();
        lock.lock();
        try {
            String threadName = Thread.currentThread().getName();
            System.out.println("📤 [" + threadName + "] Sending: " + action);

            // Gửi request
            out.writeObject(action);
            for (Object param : params) {
                out.writeObject(param);
            }
            out.flush();

            // Đọc response 1 (SUCCESS/FAILURE)
            Object response1 = in.readObject();

            // Đọc response 2 (data)
            Object response2 = in.readObject();

            String response2Info;
            if (response2 instanceof List) {
                response2Info = "List[" + ((List<?>) response2).size() + " items]";
            } else {
                response2Info = response2.getClass().getSimpleName();
            }

            System.out.println("📥 [" + threadName + "] Received: " + response1 + " + " + response2Info);

            return new Object[]{response1, response2};

        } catch (SocketException e) {
            isConnected = false;
            System.err.println("⚠️ Connection lost during request: " + action);
            throw new IOException("Connection lost", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gửi request đơn giản (không cần đọc response ngay)
     * CHỈ DÙNG CHO CÁC REQUEST ĐƠN GIẢN như SEND_MESSAGE, MARK_AS_READ
     */
    public void sendRequest(String action, Object... params) throws IOException {
        ensureConnected();
        lock.lock();
        try {
            System.out.println("📤 [" + Thread.currentThread().getName() + "] Sending: " + action);
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
     * Đọc 1 response
     * CHỈ DÙNG SAU KHI sendRequest()
     */
    public Object readResponse() throws IOException, ClassNotFoundException {
        ensureConnected();
        lock.lock();
        try {
            Object response = in.readObject();
            System.out.println("📥 [" + Thread.currentThread().getName() + "] Received: " +
                    (response instanceof String ? response : response.getClass().getSimpleName()));
            return response;
        } finally {
            lock.unlock();
        }
    }

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
                    out.writeObject("DISCONNECT");
                    out.flush();
                } catch (IOException e) {
                    // Ignore
                }
                closeQuietly(out);
                closeQuietly(in);
                closeQuietly(socket);
                isConnected = false;
                System.out.println("👋 Disconnected from server");
            }
        } finally {
            lock.unlock();
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.disconnect();
            instance = null;
        }
    }

    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }
}