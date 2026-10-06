package hr.projekt_lab.controlleri;

import hr.projekt_lab.entities.Member;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.MemberJsonUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MemberUnosController {

    @FXML
    private Text titleText;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private DatePicker memberDatePicker;

    @FXML
    private Button saveButton;

    @FXML
    void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();
        titleText.setText(loc.getString("member.unos_title"));
        firstNameField.setPromptText(loc.getString("member.unos_prompt_first_name"));
        lastNameField.setPromptText(loc.getString("member.unos_prompt_last_name"));
        saveButton.setText(loc.getString("member.save_button"));
        memberDatePicker.setValue(java.time.LocalDate.now());
    }

    @FXML
    void saveMemberButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        LocalDate memberDate = memberDatePicker.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || memberDate == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("error.empty_field"));
            return;
        }

        List<Member> members = new ArrayList<Member>(MemberJsonUtils.ucitajClanove().orElse(List.of()));

        String newId = generateNextId(members);

        Member newMember = new Member(firstName, lastName, memberDate);
        newMember.setId(newId);
        newMember.setNumberOfLoans(0);

        members.add(newMember);
        MemberJsonUtils.spremiClanove(members);

        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("member.added_success") + " " + newId);

        clearFields();
    }
    /**
     * Generira ID u formatu MBRXXX
     */
    private String generateNextId(List<Member> members) {
        int max = members.stream()
                .map(Member::getId)
                .filter(id -> id.startsWith("MBR"))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.substring(3));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);

        int next = max + 1;
        return String.format("MBR%03d", next);
    }

    private void clearFields() {
        firstNameField.clear();
        lastNameField.clear();
        memberDatePicker.setValue(null);
    }
}
