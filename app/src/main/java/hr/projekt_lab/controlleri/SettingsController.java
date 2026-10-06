package hr.projekt_lab.controlleri;

import hr.projekt_lab.utils.AppSettings;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.ScreenUtils;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.Map;

public class SettingsController {

    private static final String LABEL_HR = "Hrvatski";
    private static final String LABEL_EN = "English";
    private static final Map<String, String> CODE_TO_LABEL = Map.of("hr", LABEL_HR, "en", LABEL_EN);
    private static final Map<String, String> LABEL_TO_CODE = Map.of(LABEL_HR, "hr", LABEL_EN, "en");

    @FXML
    private ComboBox<String> languageComboBox;

    @FXML
    private TextField exportDirField;

    @FXML
    private TextField usernameField;

    @FXML
    void initialize() {
        languageComboBox.setItems(FXCollections.observableArrayList(LABEL_HR, LABEL_EN));
        String savedCode = AppSettings.getLanguage();
        languageComboBox.setValue(CODE_TO_LABEL.getOrDefault(savedCode, LABEL_HR));
        exportDirField.setText(AppSettings.getLastExportDir());
        usernameField.setText(AppSettings.getRememberedUsername());
    }

    @FXML
    void saveSettingsButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String selectedLabel = languageComboBox.getValue();
        String exportDir = exportDirField.getText().trim();
        String username = usernameField.getText().trim();

        if (selectedLabel == null || exportDir.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("settings.validation"));
            return;
        }

        String language = LABEL_TO_CODE.get(selectedLabel);

        loc.switchLanguage(language);
        AppSettings.setLastExportDir(exportDir);
        AppSettings.setRememberedUsername(username);

        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("settings.saved"));
        ScreenUtils.reloadCurrentScreen();
    }

    @FXML
    void manageLibrariansButtonOnAction(ActionEvent event) {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/librarians-view.fxml", "window.title.librarians");
    }
}
