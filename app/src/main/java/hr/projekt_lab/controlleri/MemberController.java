package hr.projekt_lab.controlleri;

import hr.projekt_lab.databaseUtil.LoanDao;
import hr.projekt_lab.entities.Loan;
import hr.projekt_lab.entities.Member;
import hr.projekt_lab.utils.AppSettings;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import hr.projekt_lab.utils.MemberLoansPdfReportUtil;
import hr.projekt_lab.utils.MemberJsonUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MemberController {
    @FXML
    private TableView <Member> membersTable;

    @FXML
    private TableColumn<Member,String> idColumn;

    @FXML
    private TableColumn<Member,String> firstNameColumn;

    @FXML
    private TableColumn<Member,String> lastNameColumn;

    @FXML
    private TableColumn<Member,String> joinDateColumn;

    @FXML
    private TableColumn<Member,Integer> loansCountColumn;

    @FXML
    private Button showAllButton;

    @FXML
    private Button searchButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button exportReportButton;

    @FXML
    private ComboBox<String> criteriaComboBox;

    @FXML
    private TextField searchField;

    private final ObservableList<Member> members = FXCollections.observableArrayList();

    @FXML
    void initialize()
    {
        LocalizationManager loc = LocalizationManager.getInstance();
        idColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getId())
        );
        idColumn.setText(loc.getString("member.id"));

        firstNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getFirstName()));
        firstNameColumn.setText(loc.getString("member.first_name"));

        lastNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getLastName()));
        lastNameColumn.setText(loc.getString("member.last_name"));

        joinDateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getMemberDate().toString()));
        joinDateColumn.setText(loc.getString("member.join_date"));
        
        loansCountColumn.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getNumberOfLoans()).asObject());
        loansCountColumn.setText(loc.getString("member.loans_count"));

        criteriaComboBox.getItems().addAll(loc.getString("member.search_criteria_name"), loc.getString("member.search_criteria_loans"));
        criteriaComboBox.getSelectionModel().selectFirst();
        criteriaComboBox.setPromptText(loc.getString("member.search_criteria"));

        showAllButton.setText(loc.getString("member.show_all"));
        searchButton.setText(loc.getString("member.search"));
        editButton.setText(loc.getString("member.edit"));
        deleteButton.setText(loc.getString("member.delete"));
        exportReportButton.setText(loc.getString("member.report.export"));

        membersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        idColumn.prefWidthProperty().bind(membersTable.widthProperty().multiply(0.15));
        firstNameColumn.prefWidthProperty().bind(membersTable.widthProperty().multiply(0.20));
        lastNameColumn.prefWidthProperty().bind(membersTable.widthProperty().multiply(0.25));
        joinDateColumn.prefWidthProperty().bind(membersTable.widthProperty().multiply(0.25));
        loansCountColumn.prefWidthProperty().bind(membersTable.widthProperty().multiply(0.15));

        membersTable.setItems(members);

        loadMembersWithComputedLoanCounts();
    }

    @FXML
    void showAllButtonAction(ActionEvent event)
    {
        loadMembersWithComputedLoanCounts();
    }

    @FXML
    void searchButtonAction(ActionEvent event)
    {
        LocalizationManager loc = LocalizationManager.getInstance();
        String query = searchField.getText().trim().toLowerCase();
        String criteria = criteriaComboBox.getValue();

        if (query.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.search_empty"));
            return;
        }

        List<Member> sourceMembers = MemberJsonUtils.ucitajClanove().orElse(List.of());
        sourceMembers.forEach(this::applyComputedLoanCount);

        List<Member> filtered = sourceMembers.stream()
                .filter(member -> {
                    String criteriaName = loc.getString("member.search_criteria_name");
                    String criteriaLoans = loc.getString("member.search_criteria_loans");
                    
                    if (criteriaName.equals(criteria)) {
                        String firstName = Optional.ofNullable(member.getFirstName()).orElse("").toLowerCase();
                        String lastName = Optional.ofNullable(member.getLastName()).orElse("").toLowerCase();
                        String[] queryParts = query.split("\\s+");
                        return Arrays.stream(queryParts)
                                .allMatch(part -> firstName.contains(part) || lastName.contains(part));
                    } else if (criteriaLoans.equals(criteria)) {
                        try {
                            int number = Integer.parseInt(query);
                            return member.getNumberOfLoans() == number;
                        } catch (NumberFormatException e) {
                            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("member.search_error_loans"));
                            return false;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("dialog.no_results"));
        }

        members.setAll(filtered);
    }

    @FXML
    void editButtonAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Member selected = membersTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(loc.getString("member.edit_title"));
        dialog.setHeaderText(loc.getString("member.edit_header"));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogUtils.applyDialogTheme(dialog);

        TextField firstNameField = new TextField(selected.getFirstName());
        TextField lastNameField = new TextField(selected.getLastName());
        DatePicker datePicker = new DatePicker(selected.getMemberDate());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label(loc.getString("member.first_name")), 0, 0);
        form.add(firstNameField, 1, 0);
        form.add(new Label(loc.getString("member.last_name")), 0, 1);
        form.add(lastNameField, 1, 1);
        form.add(new Label(loc.getString("member.join_date")), 0, 2);
        form.add(datePicker, 1, 2);
        dialog.getDialogPane().setContent(form);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        String newFirst = firstNameField.getText().trim();
        String newLast = lastNameField.getText().trim();
        LocalDate newDate = datePicker.getValue();

        if (newFirst.isEmpty() || newLast.isEmpty() || newDate == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("error.empty_field"));
            return;
        }

        List<Member> allMembers = new ArrayList<>(MemberJsonUtils.ucitajClanove().orElse(List.of()));
        allMembers.stream()
                .filter(m -> m.getId().equals(selected.getId()))
                .findFirst()
                .ifPresent(m -> {
                    m.setFirstName(newFirst);
                    m.setLastName(newLast);
                    m.setMemberDate(newDate);
                });
        MemberJsonUtils.spremiClanove(allMembers);
        loadMembersWithComputedLoanCounts();
        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("member.edit_success"));
    }

    @FXML
    void deleteButtonAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Member selected = membersTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }

        boolean hasActiveLoans = LoanDao.getAllLoans().stream()
                .filter(loan -> loan.getMember() != null)
                .anyMatch(loan -> selected.getId().equals(loan.getMember().getId())
                        && loan.getReturnDate() == null);

        if (hasActiveLoans) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("member.delete_error"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(loc.getString("member.delete_confirm"));
        confirm.setHeaderText(null);
        confirm.setContentText(loc.getString("member.delete_confirm") + "\n"
                + selected.getFirstName() + " " + selected.getLastName() + " (" + selected.getId() + ")");
        DialogUtils.applyDialogTheme(confirm);

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                List<Member> allMembers = new ArrayList<>(MemberJsonUtils.ucitajClanove().orElse(List.of()));
                allMembers.removeIf(m -> m.getId().equals(selected.getId()));
                MemberJsonUtils.spremiClanove(allMembers);
                loadMembersWithComputedLoanCounts();
                DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("member.delete_success"));
            }
        });
    }

    @FXML
    void exportReportButtonAction(ActionEvent event)
    {
        LocalizationManager loc = LocalizationManager.getInstance();
        Member selectedMember = membersTable.getSelectionModel().getSelectedItem();

        if (selectedMember == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }

        List<Loan> memberLoans = LoanDao.getAllLoans().stream()
                .filter(loan -> loan.getMember() != null)
                .filter(loan -> selectedMember.getId().equals(loan.getMember().getId()))
                .toList();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(loc.getString("member.report.save_title"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("posudbe_po_clanu_" + selectedMember.getId() + ".pdf");

        File initialDir = new File(AppSettings.getLastExportDir());
        if (initialDir.exists() && initialDir.isDirectory()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        Window window = exportReportButton.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(window);

        if (selectedFile == null) {
            return;
        }

        try {
            Path outputPath = selectedFile.toPath();
            MemberLoansPdfReportUtil.generateReport(selectedMember, memberLoans, outputPath);
            AppSettings.setLastExportDir(selectedFile.getParent());
            DialogUtils.showInfo(loc.getString("dialog.info"),
                    String.format(loc.getString("member.report.success"), outputPath));
        } catch (IOException e) {
            DialogUtils.showError(loc.getString("dialog.error"),
                    String.format(loc.getString("member.report.error"), e.getMessage()));
        }
    }

    private void loadMembersWithComputedLoanCounts() {
        List<Member> loadedMembers = MemberJsonUtils.ucitajClanove().orElse(List.of());
        loadedMembers.forEach(this::applyComputedLoanCount);
        members.setAll(loadedMembers);
    }

    private void applyComputedLoanCount(Member member) {
        if (member == null) {
            return;
        }
        member.setNumberOfLoans(LoanDao.countLoansByMemberId(member.getId()));
    }
}
