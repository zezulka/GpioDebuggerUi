package core.util;
import core.net.AgentConnectionValueObject;
import java.net.InetAddress;
import layouts.controllers.ControllerUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Miloslav Zezulka
 */
public class MessageParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageParser.class);

    /**
     * Utility method for parsing incoming messages from agent. The main focus
     * of this method is to parse agent response using AgentResponseFactory and then
     * call react() method should the message be valid. Possible response types: 
     * 
     * INIT:[BOARD_TYPE]
     * GPIO:[PIN_NUMBER]:[HIGH|LOW]
     * SPI:[BYTE_IN_HEX]* 
     * SPI:[WRITE_REQUEST_OK]
     * I2C:[BYTE_IN_HEX]*
     * [INTR_STOPPED | INTR_STARTED | INTR_GENERATED]:[PIN_NAME]:[INTERRUPT_TYPE]:[TIME]
     * [ILLEGAL_REQUEST]
     *
     * @param connection
     * @param agentMessage
     * @throws IllegalArgumentException {@code agentMessage} is null
     */
    public static void parseAgentMessage(AgentConnectionValueObject connection, String agentMessage) {
        if (agentMessage == null) {
            throw new IllegalArgumentException("Agent message cannot be null.");
        }
        try {
            AgentResponseFactory.of(connection, agentMessage).react();
        } catch (IllegalResponseException ex) {
            LOGGER.error(String.format("Agent response '%s' could not be recognized by the message parser, error generated: %s", agentMessage, ex.getMessage()));
            ControllerUtils.showInformationDialogMessage(String.format("Unrecognized message has been received: %s", agentMessage));
        }
    }
}
