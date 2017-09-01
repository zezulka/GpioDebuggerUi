package protocol.response;

import java.net.InetAddress;
import protocol.InterruptValueObject;

public abstract class InterruptAgentResponse implements AgentResponse {

    private final InterruptValueObject response;
    private final InetAddress address;

    public InterruptAgentResponse(InterruptValueObject response,
            InetAddress address) {
        this.response = response;
        this.address = address;
    }

    protected final InterruptValueObject getResponse() {
        return response;
    }

    protected final InetAddress getAddress() {
        return address;
    }
}
