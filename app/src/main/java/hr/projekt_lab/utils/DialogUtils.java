package hr.projekt_lab.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.Node;

public class DialogUtils {

    private DialogUtils(){}

    public static void applyDialogTheme(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        String stylesheet = DialogUtils.class.getResource("/hr/projekt_lab/style.css").toExternalForm();

        if (!dialogPane.getStylesheets().contains(stylesheet)) {
            dialogPane.getStylesheets().add(stylesheet);
        }

        if (!dialogPane.getStyleClass().contains("app-dialog")) {
            dialogPane.getStyleClass().add("app-dialog");
        }

        for (ButtonType buttonType : dialogPane.getButtonTypes()) {
            Node buttonNode = dialogPane.lookupButton(buttonType);
            if (!(buttonNode instanceof Button button)) {
                continue;
            }

            button.getStyleClass().remove("dialog-ok-button");
            button.getStyleClass().remove("dialog-cancel-button");

            ButtonBar.ButtonData buttonData = buttonType.getButtonData();
            if (buttonData == ButtonBar.ButtonData.CANCEL_CLOSE
                    || buttonData == ButtonBar.ButtonData.BACK_PREVIOUS
                    || buttonData == ButtonBar.ButtonData.NO) {
                button.getStyleClass().add("dialog-cancel-button");
            } else {
                button.getStyleClass().add("dialog-ok-button");
            }
        }
    }

    public static void showDisplayScreenErrorDialog(){
        LocalizationManager loc = LocalizationManager.getInstance();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(loc.getString("dialog.screen_error_title"));
        alert.setHeaderText(loc.getString("dialog.screen_error_header"));
        alert.setContentText(loc.getString("dialog.screen_error_content"));
        applyDialogTheme(alert);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogTheme(alert);
        alert.showAndWait();
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogTheme(alert);
        alert.showAndWait();
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyDialogTheme(alert);
        alert.showAndWait(); }


}
