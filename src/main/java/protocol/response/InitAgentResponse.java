package protocol.response;

import gui.controllers.Utils;
import gui.controllers.MasterWindow;
import gui.misc.Feature;
import gui.tab.loader.TabAddressPair;
import gui.userdata.DeviceValueObject;
import javafx.scene.control.Tab;
import protocol.BoardType;

import java.time.LocalDateTime;
import java.util.List;

public final class InitAgentResponse implements AgentResponse {

    private final DeviceValueObject device;
    private final BoardType boardType;
    private final List<Feature> features;

    public InitAgentResponse(DeviceValueObject device,
            BoardType boardType, List<Feature> features) {
        this.device = device;
        this.boardType = boardType;
        this.features = features;
    }

    @Override
    public void react() {
        device.setTimeConnected(LocalDateTime.now());
        device.setBoardType(boardType);

        Tab loadedTab = Utils.getLoader().loadNewTab(
                device.getAddress(),
                device.getBoardType(),
                features
        );
        MasterWindow.getTabManager()
                .addTab(new TabAddressPair(loadedTab, device.getAddress()));
    }

}
