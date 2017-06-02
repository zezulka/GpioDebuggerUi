package core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import protocol.BoardType;
import protocol.ProtocolMessages;

/**
 *
 * @author Miloslav Zezulka, 2017
 */
public class ClientConnectionManager implements Runnable {

    private String receivedMessage = null;
    private String messageToSend = null;
    private Selector selector;
    private static String ipAddress;
    private BoardType boardType;
    private SocketChannel channel;

    private static final Logger MAIN_LOGGER = LoggerFactory.getLogger(Main.class);
    public static final int DEFAULT_SOCK_PORT = 8088;
    private static final int TIMEOUT = 5 * 1000;

    private static final ClientConnectionManager CM = new ClientConnectionManager();

    public static ClientConnectionManager getInstance() {
        return CM;
    }

    private ClientConnectionManager() {
    }

    public void connectToDevice(String ipAddress) {
        setIpAddress(ipAddress);
        new Thread(this).start();
    }

    private void resetResources() {
        try {
            if (channel != null) {
                channel.close();
            }
            if (selector != null) {
                selector.close();
            }
        } catch (IOException ex) {
            MAIN_LOGGER.error(null, ex);
        }
        selector = null;
        ipAddress = null;
        channel = null;
        boardType = null;
    }

    public boolean isAlive() {
        return channel != null && channel.isConnected();
    }

    public boolean initManager() {
        if (selector != null || channel != null) {
            resetResources();
            return false;
        }
        if (ipAddress == null) {
            throw new IllegalStateException("Cannot initialize server,"
                    + " ip address has not been set yet!");
        }
        setIpAddress(ipAddress);
        try {
            selector = Selector.open();
            channel = SocketChannel.open();
            channel.configureBlocking(true);
            String[] octets = ipAddress.split("\\.", 4);
            byte[] ipAddr = new byte[4];
            for (int i = 0; i < 4; i++) {
                ipAddr[i] = (byte) Integer.parseInt(octets[i]);
            }
            InetAddress inetAddress = InetAddress.getByAddress(ipAddr);
            if (!inetAddress.isReachable(TIMEOUT)) {
                Platform.runLater(() -> {
                    GuiEntryPoint.provideFeedback(String.format("Host %s could not be reached.", ipAddress));
                });
                resetResources();
                return false;
            }
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_CONNECT);
            channel.connect(new InetSocketAddress(ipAddress, DEFAULT_SOCK_PORT));
        } catch (IOException ex) {
            MAIN_LOGGER.error("Init I/O error", ex);
            resetResources();
            return false;
        }
        return true;
    }

    public void setIpAddress(String ipAddress) {
        if (ipAddress == null) {
            throw new IllegalArgumentException("ip address cannot be null!");
        }
        ClientConnectionManager.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setMessageToSend(String message) {
        this.messageToSend = message;
    }

    public void setReceivedMessage(String message) {
        this.receivedMessage = message;
    }

    private void setBoardType(BoardType type) {
        this.boardType = type;
    }

    public BoardType getBoardType() {
        return this.boardType;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!initManager()) {
                    GuiEntryPoint.getInstance().switchToIpPrompt();
                    resetResources();
                    Platform.runLater(() -> {
                        GuiEntryPoint.provideFeedback("Cannot connect to manager. \nPlease make sure that agent instance is running on the specified address.");
                    });
                    return;
                }
                while (!Thread.interrupted()) {
                    selector.select(TIMEOUT);
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        if (!key.isValid()) {
                            keys.remove();
                            continue;
                        }
                        if (key.isConnectable()) {
                            keys.remove();
                            if (!connect(key)) {
                                break;
                            }
                        }
                        if (this.messageToSend != null && key.isWritable()) {
                            keys.remove();
                            write(key);
                        }
                        if (key.isReadable() && this.boardType == null) {
                            keys.remove();
                            readInitMessage(key);
                            try {
                                GuiEntryPoint.getInstance().switchToCurrentDevice();
                            } catch (IOException ex) {
                                MAIN_LOGGER.error(null, ex);
                            }
                            continue;
                        }
                        if (key.isReadable()) {
                            read(key);
                            if (this.receivedMessage == null) {
                                continue;
                            }
                            keys.remove();
                            Platform.runLater(() -> GuiEntryPoint.provideFeedback(receivedMessage));
                        }
                    }

                    if (!isAlive()) {
                        break;
                    }
                }
            } catch (IOException ex) {
                MAIN_LOGGER.error(null, ex);
            } finally {
                close();
            }
        }
    }

    private void close() {
        if (selector == null) {
            return;
        }
        try {
            selector.close();
        } catch (IOException ex) {
            MAIN_LOGGER.error("I/O error", ex);
        }
    }

    private void read(SelectionKey key) throws IOException {
        ByteBuffer readBuffer = ByteBuffer.allocate(1000);
        readBuffer.clear();
        int length;
        length = channel.read(readBuffer);
        if (length == -1) {
            MAIN_LOGGER.error("Nothing was read from server");
            channel.close();
            this.setReceivedMessage(null);
            key.cancel();
            return;
        }
        readBuffer.flip();
        byte[] buff = new byte[1024];
        readBuffer.get(buff, 0, length);
        this.setReceivedMessage(new String(buff).replaceAll("\0", ""));
        channel.register(selector, SelectionKey.OP_WRITE);
    }

    private void readInitMessage(SelectionKey key) throws IOException {
        read(key);
        this.setBoardType(BoardType.parse(this.receivedMessage));
    }

    private void write(SelectionKey key) throws IOException {
        channel.write(ByteBuffer.wrap(this.messageToSend.getBytes()));
        key.interestOps(SelectionKey.OP_READ);
        this.setMessageToSend(null);
    }

    private boolean connect(SelectionKey key) {
        try {
            if (channel.isConnectionPending() && channel.finishConnect()) {
                MAIN_LOGGER.info("done connecting to server");
            }
            MAIN_LOGGER.info(ProtocolMessages.C_CONNECTION_OK.toString());
            channel.configureBlocking(false);
            key.interestOps(SelectionKey.OP_READ);

        } catch (IOException ex) {
            MAIN_LOGGER.error("Could not connect to server", ex);
            return false;
        }
        return true;
    }
}