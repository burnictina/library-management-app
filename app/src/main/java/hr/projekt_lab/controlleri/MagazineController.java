package hr.projekt_lab.controlleri;
import hr.projekt_lab.libraryutils.ValidationUtils;
import hr.projekt_lab.entities.Magazine;
import hr.projekt_lab.utils.ControllerUtils;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.MagazineXmlUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.Year;
import java.util.List;
import java.util.Optional;

public class MagazineController {

    @FXML
    private Button ispisiButton;

    @FXML
    private TableView<Magazine> magazineTable;

    @FXML
    private TableColumn<Magazine, String> titleColumn;

    @FXML
    private TableColumn<Magazine, String> publisherColumn;

    @FXML
    private TableColumn<Magazine, String> issueNumberColumn;

    @FXML
    private TableColumn<Magazine, String> issueMonthColumn;

    @FXML
    private TableColumn<Magazine, String> issueYearColumn;

    @FXML
    private TableColumn<Magazine, String> categoryColumn;

        @FXML
        private Button editButton;

        @FXML
        private Button deleteButton;

    private final ObservableList<Magazine> magazines = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();
        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle())
        );
        titleColumn.setText(loc.getString("magazine.title"));
        
        publisherColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPublisher())
        );
        publisherColumn.setText(loc.getString("magazine.publisher"));
        
        issueNumberColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIssueNumber().toString())
        );
        issueNumberColumn.setText(loc.getString("magazine.issue_number"));
        
        issueMonthColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPublishingMonth())
        );
        issueMonthColumn.setText(loc.getString("magazine.issue_month"));
        
        issueYearColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPublishingYear().toString())
        );
        issueYearColumn.setText(loc.getString("magazine.issue_year"));
        
        categoryColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getCategory())
        );
        categoryColumn.setText(loc.getString("magazine.category"));
        
        ispisiButton.setText(loc.getString("magazine.show_all"));
        editButton.setText(loc.getString("magazine.edit"));
        deleteButton.setText(loc.getString("magazine.delete"));
        
        magazineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        titleColumn.prefWidthProperty().bind(magazineTable.widthProperty().multiply(0.22));
        publisherColumn.prefWidthProperty().bind(magazineTable.widthProperty().multiply(0.18));
        issueNumberColumn.prefWidthProperty().bind(magazineTable.widthProperty().multiply(0.12));
        issueYearColumn.prefWidthProperty().bind(magazineTable.widthProperty().multiply(0.12));
        issueMonthColumn.prefWidthProperty().bind(magazineTable.widthProperty().multiply(0.18));
        categoryColumn.prefWidthProperty().bind(magazineTable.widthProperty().multiply(0.18));
        
        magazineTable.setItems(magazines);
        
        // Load all magazines on initialize
        ControllerUtils.loadData(magazines, MagazineXmlUtil::ucitajCasopise, null);

    }

    @FXML
    void showAllButtonOnAction(ActionEvent event) {
                ControllerUtils.loadData(magazines, MagazineXmlUtil::ucitajCasopise, null);
        }

        @FXML
        void editButtonOnAction(ActionEvent event) {
                LocalizationManager loc = LocalizationManager.getInstance();
                Magazine selected = magazineTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                        DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
                        return;
                }

                Optional<Magazine> updatedMagazineOptional = showMagazineEditDialog(selected, loc);
                if (updatedMagazineOptional.isEmpty()) {
                        return;
                }

                MagazineXmlUtil.izmijeniCasopis(selected.getTitle(), updatedMagazineOptional.get());
                magazines.setAll(MagazineXmlUtil.ucitajCasopise().orElse(List.of()));
                DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("success.magazine_updated"));
        }

        private Optional<Magazine> showMagazineEditDialog(Magazine selected, LocalizationManager loc) {
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle(loc.getString("magazine.edit_title"));
                dialog.setHeaderText(loc.getString("magazine.edit_header"));
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
                DialogUtils.applyDialogTheme(dialog);

                TextField titleField = new TextField(selected.getTitle());
                TextField publisherField = new TextField(selected.getPublisher());
                TextField issueNumberField = new TextField(String.valueOf(selected.getIssueNumber()));
                TextField issueYearField = new TextField(String.valueOf(selected.getPublishingYear().getValue()));
                TextField issueMonthField = new TextField(selected.getPublishingMonth());
                TextField categoryField = new TextField(selected.getCategory());

                GridPane form = new GridPane();
                form.setHgap(10);
                form.setVgap(10);
                form.add(new Label(loc.getString("magazine.title")), 0, 0);
                form.add(titleField, 1, 0);
                form.add(new Label(loc.getString("magazine.publisher")), 0, 1);
                form.add(publisherField, 1, 1);
                form.add(new Label(loc.getString("magazine.issue_number")), 0, 2);
                form.add(issueNumberField, 1, 2);
                form.add(new Label(loc.getString("magazine.issue_year")), 0, 3);
                form.add(issueYearField, 1, 3);
                form.add(new Label(loc.getString("magazine.issue_month")), 0, 4);
                form.add(issueMonthField, 1, 4);
                form.add(new Label(loc.getString("magazine.category")), 0, 5);
                form.add(categoryField, 1, 5);

                dialog.getDialogPane().setContent(form);

                Optional<ButtonType> result = dialog.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                        return Optional.empty();
                }

                String title = titleField.getText().trim();
                String publisher = publisherField.getText().trim();
                String issueNumberText = issueNumberField.getText().trim();
                String issueYearText = issueYearField.getText().trim();
                String issueMonth = issueMonthField.getText().trim();
                String category = categoryField.getText().trim();

                if (title.isEmpty() || publisher.isEmpty() || issueNumberText.isEmpty() || issueYearText.isEmpty() || issueMonth.isEmpty() || category.isEmpty()) {
                        DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("error.empty_field"));
                        return Optional.empty();
                }

                Integer issueNumber;
                try {
                        issueNumber = ValidationUtils.validateIssueNumber(issueNumberText).orElseThrow();
                } catch (IllegalArgumentException ex) {
                        DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("magazine.issue_number_numeric"));
                        return Optional.empty();
                }

                Year issueYear;
                try {
                        issueYear = ValidationUtils.validateYear(issueYearText).orElseThrow();
                } catch (IllegalArgumentException ex) {
                        DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("magazine.issue_year_numeric"));
                        return Optional.empty();
                }

                Magazine updated = new Magazine.MagazineBuilder(
                        title,
                        publisher,
                        issueNumber,
                        issueYear,
                        issueMonth,
                        category
                ).borrowed(!selected.isAvailable()).build();

                return Optional.of(updated);
        }

        @FXML
        void deleteButtonOnAction(ActionEvent event) {
                LocalizationManager loc = LocalizationManager.getInstance();
                Magazine selected = magazineTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                        DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
                        return;
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle(loc.getString("confirm.delete_magazine"));
                confirm.setHeaderText(null);
                confirm.setContentText(loc.getString("confirm.delete_magazine") + ": " + selected.getTitle() + "?");
                DialogUtils.applyDialogTheme(confirm);

                confirm.showAndWait().ifPresent(result -> {
                        if (result == ButtonType.OK) {
                                MagazineXmlUtil.obrisiCasopis(selected.getTitle());
                                magazines.setAll(MagazineXmlUtil.ucitajCasopise().orElse(List.of()));
                                LocalizationManager loc2 = LocalizationManager.getInstance();
                                DialogUtils.showInfo(loc2.getString("dialog.info"), loc2.getString("success.magazine_deleted"));
                        }
                });
    }
}
