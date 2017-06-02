package core;

import java.awt.EventQueue;
import javafx.application.Application;

/**
 *
 * @author Miloslav Zezulka, 2017
 */
public class Main {
    /**
     * @param args
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> Application.launch(GuiEntryPoint.class, args));
    }
}