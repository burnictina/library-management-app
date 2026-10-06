package hr.projekt_lab.controlleri;

import hr.projekt_lab.entities.Magazine;
import hr.projekt_lab.libraryutils.ValidationUtils;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.MagazineXmlUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.time.Year;

public class MagazineUnosController {
    @FXML
    private Text titleText;

    @FXML
    private TextField titleField;

    @FXML
    private TextField publisherField;

    @FXML
    private TextField issueNumberField;

    @FXML
    private TextField publishingYearField;

    @FXML
    private TextField publishingMonthField;
    
    @FXML
    private TextField categoryField;

    @FXML
    private Button saveButton;

    @FXML
    void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();
        
        titleText.setText(loc.getString("magazine.unos_title"));
        titleField.setPromptText(loc.getString("magazine.unos_prompt_title"));
        publisherField.setPromptText(loc.getString("magazine.unos_prompt_publisher"));
        issueNumberField.setPromptText(loc.getString("magazine.unos_prompt_issue_number"));
        publishingYearField.setPromptText(loc.getString("magazine.unos_prompt_issue_year"));
        publishingMonthField.setPromptText(loc.getString("magazine.unos_prompt_issue_month"));
        categoryField.setPromptText(loc.getString("magazine.unos_prompt_category"));
        saveButton.setText(loc.getString("magazine.save_button"));
    }

    @FXML
    private void saveMagazineButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String title = titleField.getText().trim();
        String publisher = publisherField.getText().trim();
        String issueNumberText = issueNumberField.getText().trim();
        String yearText = publishingYearField.getText().trim();
        String month = publishingMonthField.getText().trim();
        String category = categoryField.getText().trim();

        if(title.isEmpty() || publisher.isEmpty() || issueNumberText.isEmpty() || yearText.isEmpty() || month.isEmpty() || category.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("error.empty_field"));
            return;
        }

        int issueNumber;
        try {
            issueNumber = ValidationUtils.validateIssueNumber(issueNumberText).orElseThrow();
        } catch (IllegalArgumentException e) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("magazine.issue_number_numeric"));
            return;
        }

        Year publishingYear;
        try {
            publishingYear = ValidationUtils.validateYear(yearText).orElseThrow();
        } catch (IllegalArgumentException e) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("magazine.issue_year_numeric"));
            return;
        }

        Magazine newMagazine = new Magazine.MagazineBuilder(title, publisher, issueNumber, publishingYear, month, category).build();

        MagazineXmlUtil.dodajCasopis(newMagazine);

        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("magazine.added_success"));

        clearFields();
    }
    private void clearFields() {
        titleField.clear();
        publisherField.clear();
        issueNumberField.clear();
        publishingYearField.clear();
        publishingMonthField.clear();
        categoryField.clear();
    }

}
