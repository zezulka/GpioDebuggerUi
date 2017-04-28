/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import layouts.controllers.DeviceControllerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protocol.BoardType;
import protocol.ProtocolMessages;
import layouts.controllers.DeviceController;

/**
 *
 * @author Miloslav Zezulka, 2017
 */
public final class GuiEntryPoint extends Application {

    private static Stage stage;
    private static DeviceController currentController;

    private static final Logger GUI_LOGGER = LoggerFactory.getLogger(GuiEntryPoint.class);
    private static final GuiEntryPoint INSTANCE = new GuiEntryPoint();
    private static final File PATH_TO_FXML_DIR = new File(System.getProperty("user.dir")
            + File.separator + "src"
            + File.separator + "main"
            + File.separator + "resources"
            + File.separator + "fxml");
    private static URL ipPrompt;
    private static URL popupFeedback;
    private static URL raspiController;
    private static URL beagleBoneBlackController;
    private static URL cubieBoardController;

    public GuiEntryPoint() {
        try {
            //TODO: refactor this ugly code, possibly create new FileLoader utility class?...
            File pathToFxml = new File(PATH_TO_FXML_DIR + File.separator + "IpPrompt" + ".fxml");
            ipPrompt = pathToFxml.toURI().toURL();
            pathToFxml = new File(PATH_TO_FXML_DIR + File.separator + "Raspi" + ".fxml");
            raspiController = pathToFxml.toURI().toURL();
            pathToFxml = new File(PATH_TO_FXML_DIR + File.separator + "PopupFeedback" + ".fxml");
            popupFeedback = pathToFxml.toURI().toURL();
        } catch (MalformedURLException ex) {
            GUI_LOGGER.error("Malformed URL:", ex);
        }
    }

    public static void provideFeedback(String msg) {
        ((TextArea) stage.getScene().lookup("#feedbackArea")).setText(msg);
    }

    static void providePopupFeedback(String receivedMessage) {
        Stage stageLoc = new Stage();
        Parent root;
        try {
            root = FXMLLoader.load(popupFeedback);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(GuiEntryPoint.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        stageLoc.setScene(new Scene(root));
        stageLoc.initModality(Modality.APPLICATION_MODAL);
        //stage.initOwner(null);
        ((TextArea) stageLoc.getScene().lookup("#popupFeedback")).setText(receivedMessage);
        stageLoc.showAndWait();
    }

    @Override
    public void stop() {
        Platform.exit();
        System.exit(0);
    }

    public static GuiEntryPoint getInstance() {
        return INSTANCE;
    }

    //this should be in separate class! single responsibility broken
    public static void writeErrorToLoggerWithClass(Class<?> controllerClass, Throwable cause) {
        GUI_LOGGER.error(controllerClass.getName(), cause);
    }

    public static void writeErrorToLoggerWithMessage(String msg, Throwable cause) {
        GUI_LOGGER.error(msg, cause);
    }

    public static void writeInfoToLogger(String msg) {
        GUI_LOGGER.info(msg);
    }

    public static void writeErrorToLoggerWithoutCause(String msg) {
        GUI_LOGGER.error(msg);
    }
    
    private void switchScene(URL fxml) throws IOException {
        GuiEntryPoint.writeInfoToLogger("attempting to load " + fxml.toString() + " ...");
        Parent newParent = (Parent) FXMLLoader.load(fxml);
        GuiEntryPoint.writeInfoToLogger("load successful!");
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(newParent, 1000, 800);
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(newParent);
        }
    }
    
    private void switchToRaspi() throws IOException {
        switchScene(raspiController);
    }

    private void switchToBBB() throws IOException {
        switchScene(beagleBoneBlackController);
    }

    private void switchToCubieBoard() throws IOException {
        switchScene(cubieBoardController);
    }

    public void switchToCurrentDevice() throws IOException {
        if (ClientConnectionManager.getInstance() == null) {
            GUI_LOGGER.debug("manager not ready!");
            return;
        }
        BoardType board = ClientConnectionManager.getInstance().getBoardType();

        if (board == null) {
            GUI_LOGGER.debug("cannot view device controller, no device available");
            return;
        }
        currentController = DeviceControllerFactory.getController(board);
        switch (board) {
            case BEAGLEBONEBLACK: {
                switchToBBB();
                break;
            }
            case CUBIEBOARD: {
                switchToCubieBoard();
                break;
            }
            case RASPBERRY_PI: {
                switchToRaspi();
                break;
            }
            default:
                throw new IllegalStateException(
                        ProtocolMessages.C_ERR_NO_BOARD.toString());
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            stage = primaryStage;
            switchScene(ipPrompt);
            primaryStage.show();
        } catch (IOException ex) {
            GuiEntryPoint.writeErrorToLoggerWithMessage(ProtocolMessages.C_ERR_GUI_FXML.toString(), ex);
            Platform.exit();
        }
    }

}
