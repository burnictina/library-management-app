package hr.projekt_lab.controlleri;

import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.ScreenUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;


public class MenuController {
    @FXML
    private AnchorPane sidebarRoot;

    @FXML
    private Button booksNavButton;

    @FXML
    private Button authorsNavButton;

    @FXML
    private Button magazinesNavButton;

    @FXML
    private Button loansNavButton;

    @FXML
    private Button membersNavButton;

    @FXML
    private Button reservationsNavButton;

    @FXML
    private Button settingsNavButton;

    @FXML
    private void initialize() {
        Platform.runLater(this::applyActiveSidebarItem);
    }

    private void applyActiveSidebarItem() {
        if (sidebarRoot == null || sidebarRoot.getScene() == null) {
            return;
        }

        Parent sceneRoot = sidebarRoot.getScene().getRoot();
        Object userData = sceneRoot.getUserData();
        String section = userData == null ? "books" : userData.toString();

        clearActiveClass(booksNavButton);
        clearActiveClass(authorsNavButton);
        clearActiveClass(magazinesNavButton);
        clearActiveClass(loansNavButton);
        clearActiveClass(membersNavButton);
        clearActiveClass(reservationsNavButton);
        clearActiveClass(settingsNavButton);

        switch (section) {
            case "authors" -> addActiveClass(authorsNavButton);
            case "magazines" -> addActiveClass(magazinesNavButton);
            case "loans" -> addActiveClass(loansNavButton);
            case "members" -> addActiveClass(membersNavButton);
            case "reservations" -> addActiveClass(reservationsNavButton);
            case "settings" -> addActiveClass(settingsNavButton);
            default -> addActiveClass(booksNavButton);
        }
    }

    private void clearActiveClass(Button button) {
        if (button != null) {
            button.getStyleClass().remove("sidebar-item-active");
        }
    }

    private void addActiveClass(Button button) {
        if (button != null && !button.getStyleClass().contains("sidebar-item-active")) {
            button.getStyleClass().add("sidebar-item-active");
        }
    }

    public void showBooksScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/book-view.fxml", "window.title.books");
    }
    public void showAuthorScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/autori-view.fxml", "window.title.authors");
    }

    public void showMagazineScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/magazine-view.fxml", "window.title.magazines");
    }

    public void showLoansScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/loan-view.fxml", "window.title.loans");
    }

    public void showMembersScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/member-view.fxml", "window.title.members");
    }

    public void showReservationsScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/reservation-view.fxml", "window.title.reservations");
    }

    public void showAuthorInputScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/autori-unos.fxml", "window.title.author_input");
    }

    public void showBookInputScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/book-unos.fxml", "window.title.book_input");
    }

    public void showMagazineInputScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/magazine-unos.fxml", "window.title.magazine_input");
    }

    public void showLoansInputScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/loan-unos.fxml", "window.title.loan_input");
    }

    public void showMemberInputScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/member-unos.fxml", "window.title.member_input");
    }

    public void showSettingsScreen() {
        ScreenUtils.showScreenByTitleKey("/hr/projekt_lab/settings-view.fxml", "window.title.settings");
    }

    public void switchLanguageToCroatian() {
        LocalizationManager.getInstance().switchLanguage("hr");
        ScreenUtils.reloadCurrentScreen();
    }

    public void switchLanguageToEnglish() {
        LocalizationManager.getInstance().switchLanguage("en");
        ScreenUtils.reloadCurrentScreen();
    }
}
