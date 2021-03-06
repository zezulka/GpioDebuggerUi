package net;

import gui.controllers.Utils;
import gui.controllers.MasterWindow;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.AppPreferencesExtractor;
import protocol.InterruptManager;
import protocol.MessageParser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public final class ConnectionThread implements Runnable {

    private static final Logger LOGGER
            = LoggerFactory.getLogger(ConnectionThread.class);
    private static final long SELECTOR_WAKEUP_TIMEOUT = 5000L;
    private static final NetworkManager MANAGER = NetworkManager.getInstance();
    private final ConnectionValueObject connection;

    public ConnectionThread(ConnectionValueObject connection) {
        this.connection = connection;
    }

    /**
     * Main loop of the application logic. An attempt is made to initialize this
     * manager (singleton); if manager succeeded in connecting to the supplied
     * IP address, it then tries to receive the "handshake" message containing
     * name of the device. If the handshake message is valid (=ok parse), it
     * then invokes appropriate method to switch to the given scene which
     * enables user to control the device the client connected to.
     *
     * While the agent is alive, this thread iterates through selection keys and
     * deals with them (this includes reading messages from input stream or
     * writing client messages to output stream).
     */
    @Override
    public void run() {
        try {
            iterateThroughRegisteredKeys();
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        } finally {
            cleanUpResources();
        }
    }

    private void iterateThroughRegisteredKeys() throws IOException {
        while (true) {
            connection.getSelector().select(SELECTOR_WAKEUP_TIMEOUT);
            if (!connection.getSelector().isOpen()) {
                break;
            }
            Iterator<SelectionKey> keys = connection.getSelector()
                    .selectedKeys().iterator();
            processSelectionKeys(keys);
            if (!isAlive()) {
                break;
            }
        }
    }

    /**
     * Checks whether device on the supplied IP address is alive.
     *
     * @return true if alive, false otherwise
     */
    private boolean isAlive() {
        return connection.getSelector().isOpen()
                && connection.getChannel().isOpen()
                && connection.getChannel().isConnected();
    }

    private void processSelectionKeys(Iterator<SelectionKey> keys)
            throws IOException {
        if (keys == null) {
            return;
        }
        while (keys.hasNext()) {
            SelectionKey key = keys.next();
            keys.remove();
            if (!key.isValid()) {
                LOGGER.error(String.format("Invalid key registered: %s", key));
                continue;
            }
            if (key.isConnectable() && !connect(key)) {
                break;
            }
            if (key.isWritable() && connection.getMessageToSend() != null) {
                write(key);
            }
            if (key.isReadable()) {
                processAgentMessage();
            }
        }
    }

    private void processAgentMessage() {
        String agentMessage = read();
        if (agentMessage != null) {
            MessageParser.parseAgentMessage(connection, agentMessage);
        } else {
            LOGGER.debug("disconnecting from agent...");
            disconnect();
            if (connection.getDevice().isDirty()) {
                if (MasterWindow.getTabManager().findTabByAddress(connection
                        .getDevice().getAddress()) != null) {
                    Platform.runLater(() -> MasterWindow.getTabManager()
                            .removeTab(connection.getDevice().getAddress()));
                }
                Utils.showInfoDialog(
                        String.format("Disconnected from address %s, device %s",
                                connection.getDevice().getAddress(),
                                connection.getDevice().getBoardType())
                );
            }
        }
    }

    private String read() {
        try {
            ByteBuffer readBuffer
                    = ByteBuffer.allocate(NetworkManager.BUFFER_SIZE);
            readBuffer.clear();
            int length;
            length = connection.getChannel().read(readBuffer);
            if (length == NetworkManager.END_OF_STREAM) {
                LOGGER.debug("reached end of the input stream");
                return null;
            }
            readBuffer.flip();
            byte[] buff = new byte[NetworkManager.BUFFER_SIZE];
            readBuffer.get(buff, 0, length);
            return (new String(buff).replaceAll("\0", ""));
        } catch (IOException ex) {
            Utils.showErrorDialog(
                    "There has been an error reading message from agent."
                            + "Either agent is not running on the IP "
                            + "supplied or "
                            + "network connection failure has occurred.\n"
            );
            LOGGER.error(ex.getLocalizedMessage());
            disconnect();
            return null;
        }
    }

    /**
     * Causes manager to close all resources binded to the existing connection.
     * If connection has not been established, invocation of this method is a
     * no-op (regarding network resources).
     */
    public void disconnect() {
        cleanUpResources();
        try {
            connection.getChannel().close();
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        } finally {
            connection.getDevice().disconnectedProperty().set(true);
            InterruptManager.clearAllListeners(connection
                    .getDevice().getAddress());
            NetworkManager.removeMapping(connection
                    .getDevice().getAddress());
        }
    }

    private void write(SelectionKey key) throws IOException {
        if (connection.getMessageToSend() == null) {
            throw new IllegalStateException("cannot send null message");
        }
        connection.getChannel().write(ByteBuffer.wrap(connection
                .getMessageToSend()
                .getBytes()));
        key.interestOps(SelectionKey.OP_READ);
        connection.setMessageToSend(null);
    }

    private boolean connect(SelectionKey key) {
        try {
            SocketChannel channel = connection.getChannel();
            channel.connect(
                    new InetSocketAddress(connection.getDevice().getAddress(),
                            AppPreferencesExtractor.defaultSocketPort())
            );
            if (channel.isConnectionPending() && channel.finishConnect()) {
                LOGGER.info("done connecting to server");
                MANAGER.addNew(connection.getDevice().getAddress(), this);
            } else {
                return false;
            }
            channel.configureBlocking(false);
            key.interestOps(SelectionKey.OP_READ);
            connection.getDevice().disconnectedProperty().set(false);
            return true;
        } catch (IOException ex) {
            LOGGER.info('<' + ex.getClass().toString()
                    + "> could not connect to server.");
            LOGGER.error(null, ex);
            Utils
                    .showErrorDialog("Could not connect to server.");
            return false;
        }
    }

    /**
     * @throws IllegalArgumentException if no connection to {@code ipAddress}
     *                                  exists.
     */
    private void cleanUpResources() {
        if (connection == null) {
            throw new IllegalArgumentException("connection does not exist");
        }
        SocketChannel channel = connection.getChannel();
        Selector selector = connection.getSelector();
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
        } catch (IOException ex) {
            LOGGER.error(null, ex);
        }
    }

    /**
     * Given message is stored in the appropriate variable and registers this
     * message in SocketChannel to be sent via output stream to agent.
     */
    public void registerMessage(String message) {
        connection.setMessageToSend(message);
        if (message != null) {
            try {
                connection.getChannel().register(connection.getSelector(),
                        SelectionKey.OP_WRITE);
                connection.getSelector().wakeup();
            } catch (ClosedChannelException ex) {
                LOGGER.error("There has been an attempt to "
                        + "register write operation on channel "
                        + "which has been closed.", ex);
                throw new IllegalStateException();
            }
        }
    }

}
