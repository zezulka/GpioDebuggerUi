package gui.layouts.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public abstract class AbstractInterfaceFormController implements Initializable {

    protected static final String HEXA_PREFIX = "0x";
    protected static final String HEX_BYTE_REGEX = "^([0-9A-Fa-f])+$";
    protected static final String HEX_BYTE_REGEX_BINDING
            = "^([0-9A-Fa-f]{2})+$";

    protected final void addAllModes(ComboBox<Operation> operationList) {
        operationList.setItems(Operation.observableValues());
    }

    protected final void enforceNumericValuesOnly(TextField textfield) {
        textfield.textProperty()
                .addListener((ObservableValue<? extends String> obs, String old,
                        String newValue) -> {
                    if (!(newValue.matches("\\d*"))) {
                        textfield.setText(newValue.replaceAll("[^\\d]", ""));
                    }
                });
    }

    protected List<String> getBytesFromUser(String str) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < str.length() - 1; i += 2) {
            String subStr = str.substring(i, i + 2);
            result.add(subStr + "\n(" + Short.parseShort(subStr, 16) + ')');
        }
        return result;
    }

    protected final BooleanBinding hexValuesOnly(TextField textfield) {
        BooleanBinding binding = Bindings.createBooleanBinding(()
                -> Pattern.compile(HEX_BYTE_REGEX_BINDING)
                        .matcher(textfield.getText()).matches(),
                textfield.textProperty());
        return Bindings.when(binding).then(true).otherwise(false);
    }

    protected final void enforceHexValuesOnly(TextField textfield) {
        textfield.textProperty()
                .addListener((ObservableValue<? extends String> obs, String old,
                        String newValue) -> {
                    if (newValue.equals("")) {
                        textfield.setText("");
                        return;
                    }
                    if (!(newValue.matches(HEX_BYTE_REGEX))) {
                        textfield.setText(old);
                    }
                });
    }

    protected abstract StringBuilder getMessagePrefix();
}
