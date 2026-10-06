package hr.projekt_lab.controlleri;

import hr.projekt_lab.databaseUtil.LibrarianDao;
import hr.projekt_lab.entities.Librarian;
import hr.projekt_lab.utils.AppSettings;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.ScreenUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.util.Optional;

public class LoginController {

    @FXML
    private Text loginTitleText;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    public void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();
        loginTitleText.setText(loc.getString("login.title"));
        usernameField.setPromptText(loc.getString("login.username"));
        passwordField.setPromptText(loc.getString("login.password"));
        loginButton.setText(loc.getString("login.button"));

        usernameField.setText(AppSettings.getRememberedUsername());
    }

    @FXML
    void loginButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("login.empty_fields"));
            return;
        }

        Optional<Librarian> librarianOptional = LibrarianDao.findByUsername(username);
        boolean authenticated = librarianOptional
                .map(librarian -> LibrarianDao.verifyPassword(password, librarian))
                .orElse(false);

        if (!authenticated) {
            DialogUtils.showError(loc.getString("dialog.error"), loc.getString("login.failed"));
            return;
        }

        AppSettings.setRememberedUsername(username);
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/book-view.fxml", "window.title.books");
    }
}
