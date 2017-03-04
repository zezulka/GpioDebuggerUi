/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package layouts.xmlGenerics;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import layouts.ClientPin;
import layouts.PinLayoutFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import protocol.BoardType;

/**
 *
 * @author miloslav
 */
public abstract class AbstractDeviceXmlGenerator implements DeviceXmlGenerator {

    private final int height;
    private final int width;
    private final BoardType type;
    private final String docName;

    protected AbstractDeviceXmlGenerator(int height, int width, BoardType type, String docName) {
        this.height = height;
        this.width = width;
        this.type = type;
        this.docName = docName;
    }
    
    private static void addImports(Document doc) {
        Node[] imports = {   doc.createProcessingInstruction("import", "javafx.scene.control.Button"),
                             doc.createProcessingInstruction("import", "javafx.scene.layout.AnchorPane"),
                             doc.createProcessingInstruction("import", "javafx.scene.layout.ColumnConstraints"),
                             doc.createProcessingInstruction("import", "javafx.scene.layout.GridPane"),
                             doc.createProcessingInstruction("import", "javafx.scene.layout.RowConstraints")
                          };
        for(Node node : imports) {
            doc.appendChild(node);
        }
    }

    @Override
    public void createXml() throws IOException {
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            
            addImports(doc);
            //root element
            Element rootElement = doc.createElement("AnchorPane");
            rootElement.setAttribute("id", "AnchorPane");
            rootElement.setAttribute("xmlns", "http://javafx.com/javafx/8.0.111");
            rootElement.setAttribute("xmlns:fx", "http://javafx.com/fxml/1");
            rootElement.setAttribute("fx:controller", "layouts.controllers.Raspi");
            doc.appendChild(rootElement);

            Element children = doc.createElement("children");
            rootElement.appendChild(children);
            //grid pane
            Element gridPane = doc.createElement("GridPane");
            gridPane.setAttribute("layoutX", "0.0");
            gridPane.setAttribute("layoutY", "0.0");
            children.appendChild(gridPane);

            //column constraints
            Element columnConstraints = doc.createElement("columnConstraints");

            for (int i = 0; i < width; i++) {
                Element columnConstraint = doc.createElement("ColumnConstraints");
                columnConstraint.setAttribute("hgrow", "SOMETIMES");
                columnConstraint.setAttribute("minWidth", "10.0");
                columnConstraint.setAttribute("prefWidth", "100.0");
                columnConstraints.appendChild(columnConstraint);
            }
            gridPane.appendChild(columnConstraints);

            //row constraints
            Element rowConstraints = doc.createElement("rowConstraints");

            for (int i = 0; i < height; i++) {
                Element rowConstraint = doc.createElement("RowConstraints");
                rowConstraint.setAttribute("minHeight", "10.0");
                rowConstraint.setAttribute("prefHeight", "30.0");
                rowConstraint.setAttribute("vgrow", "SOMETIMES");
                rowConstraints.appendChild(rowConstraint);
            }

            gridPane.appendChild(rowConstraints);
            
            //buttons
            Element buttons = doc.createElement("children");
            for (int col = 0; col < width; col++) {
                for (int row = 0; row < height; row++) {
                    Element button = doc.createElement("Button");
                    ClientPin currentPin = PinLayoutFactory.getInstance(type).
                            getPinFromIndex(row * 2 + 1 + col);
                    button.setAttribute("mnemonicParsing", "false");
                    button.setAttribute("disable", Boolean.toString(currentPin.isGpio()));
                    button.setAttribute("text", currentPin.getName());
                    button.setAttribute("GridPane.columnIndex", Integer.toString(col));
                    button.setAttribute("GridPane.rowIndex", Integer.toString(row));
                    buttons.appendChild(button);
                }
            }

            gridPane.appendChild(buttons);

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            doc.normalizeDocument();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("./src/main/resources/fxml/"+docName));
            
            //transformer formatting magic... taken from StackOverflow
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,"yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);

        } catch (ParserConfigurationException pce) {
            Logger.getAnonymousLogger().log(Level.SEVERE, pce.getMessage());
        } catch (TransformerException tfe) {
            Logger.getAnonymousLogger().log(Level.SEVERE, tfe.getMessage());
        }
    }
}
