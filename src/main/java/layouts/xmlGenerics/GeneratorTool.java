/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package layouts.xmlGenerics;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tool used for generating GUI (more specifically, this tool creates fxml files representing
 * user interface).
 * @author Miloslav Zezulka, 2017
 */
public class GeneratorTool {
    public static void main(String[] args) {
        try {
            RaspiXmlGenerator.getInstance().createXml();
            //BeagleBoneBlackXmlGenerator.getInstance().createXml();
            //CubieBoardXmlGenerator.getInstance().createXml();
        } catch (IOException ex) {
            Logger.getLogger(GeneratorTool.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
