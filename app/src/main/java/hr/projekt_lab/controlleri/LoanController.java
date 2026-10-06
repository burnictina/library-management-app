package hr.projekt_lab.controlleri;


import hr.projekt_lab.databaseUtil.LoanDao;
import hr.projekt_lab.entities.Loan;
import hr.projekt_lab.poslovnaLogika.FineCalculator;
import hr.projekt_lab.utils.DataPreloadService;
import hr.projekt_lab.utils.LoanBinaryUtil;
import hr.projekt_lab.utils.LocalizationManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import hr.projekt_lab.utils.DialogUtils;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class LoanController {
    @FXML
    private Button searchButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button returnButton;

    @FXML
    private Button backupButton;

    @FXML
    private Button restoreButton;

    @FXML
    private TableView<Loan> loansTable;

    @FXML
    private TableColumn<Loan, String> memberColumn;

    @FXML
    private TableColumn<Loan, String> publicationColumn;

    @FXML
    private TableColumn<Loan, LocalDate> loanDateColumn;

    @FXML
    private TableColumn<Loan, LocalDate> returnDateColumn;

    @FXML
    private TableColumn<Loan, String> daysUntilExpirationColumn;

    @FXML
    private TableColumn<Loan, String> fineColumn;

    @FXML
    private ComboBox<String> criteriaComboBox;

    @FXML
    private TextField searchField;

    private final ObservableList<Loan> loans = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();
        memberColumn.setCellValueFactory(cellData -> {
            var m = cellData.getValue().getMember();
            return new SimpleStringProperty(m.getFirstName()+" "+m.getLastName());
        });
        memberColumn.setText(loc.getString("loan.member"));

        publicationColumn.setCellValueFactory(cellData -> {
            Loan loan = cellData.getValue();
            String title = loan.getBook()
                    .map(b -> b.getTitle())
                    .or(() -> loan.getMagazine().map(m -> m.getTitle()))
                    .orElse("");
            return new SimpleStringProperty(title);
        });
        publicationColumn.setText(loc.getString("loan.publication"));

        loanDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("loanDate")
        );
        loanDateColumn.setText(loc.getString("loan.loan_date"));

        returnDateColumn.setCellValueFactory(
                new PropertyValueFactory<>("returnDate")
        );
        returnDateColumn.setText(loc.getString("loan.return_date"));

        daysUntilExpirationColumn.setCellValueFactory(cellData -> {
            Loan loan = cellData.getValue();
            if ("VRACENA".equals(loan.getStatus())) {
                return new SimpleStringProperty("Vraćeno");
            }
            LocalDate returnDate = cellData.getValue().getReturnDate();
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), returnDate);
            if (daysRemaining < 0) {
                return new SimpleStringProperty("Isteklo");
            }
            return new SimpleStringProperty(String.valueOf(daysRemaining));
        });
        daysUntilExpirationColumn.setText(loc.getString("loan.days_until_expiration"));

        FineCalculator fineCalculator = new FineCalculator(2.0, 0);
        fineColumn.setCellValueFactory(cellData -> {
            Loan loan = cellData.getValue();
            if ("VRACENA".equals(loan.getStatus())) {
                LocalDate actualReturn = loan.getActualReturnDate() != null 
                    ? loan.getActualReturnDate() 
                    : LocalDate.now();
                
                double fine = fineCalculator.calculateFine(loan.getReturnDate(), actualReturn);
                if (fine > 0) {
                    return new SimpleStringProperty(String.format("%.2f €", fine));
                }
                return new SimpleStringProperty("");
            }
            if (loan.getReturnDate().isBefore(LocalDate.now())) {
                double fine = fineCalculator.calculateFine(loan.getReturnDate(), LocalDate.now());
                if (fine > 0) {
                    return new SimpleStringProperty(String.format("%.2f €", fine));
                }
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty("");
        });
        fineColumn.setText(loc.getString("loan.fine"));

        criteriaComboBox.getItems().addAll(loc.getString("loan.member"), loc.getString("loan.publication"));
        criteriaComboBox.getSelectionModel().selectFirst();
        criteriaComboBox.setPromptText(loc.getString("loan.search_criteria"));

        searchButton.setText(loc.getString("loan.search"));
        editButton.setText(loc.getString("loan.edit"));
        deleteButton.setText(loc.getString("loan.delete"));
        returnButton.setText(loc.getString("loan.return"));
        backupButton.setText(loc.getString("loan.backup"));
        restoreButton.setText(loc.getString("loan.restore"));

            loansTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            memberColumn.prefWidthProperty().bind(loansTable.widthProperty().multiply(0.20));
            publicationColumn.prefWidthProperty().bind(loansTable.widthProperty().multiply(0.25));
            loanDateColumn.prefWidthProperty().bind(loansTable.widthProperty().multiply(0.13));
            returnDateColumn.prefWidthProperty().bind(loansTable.widthProperty().multiply(0.13));
            daysUntilExpirationColumn.prefWidthProperty().bind(loansTable.widthProperty().multiply(0.14));
            fineColumn.prefWidthProperty().bind(loansTable.widthProperty().multiply(0.15));

        loansTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Loan loan, boolean empty) {
                super.updateItem(loan, empty);
                getStyleClass().remove("expired-row");
                getStyleClass().remove("returned-row");

                if (!empty && loan != null && "VRACENA".equals(loan.getStatus())) {
                    getStyleClass().add("returned-row");
                } else if (!empty && loan != null && loan.getReturnDate().isBefore(LocalDate.now())) {
                    getStyleClass().add("expired-row");
                }
            }
        });

        loansTable.setItems(loans);

        loadAllLoansFromDatabase();
    }

    @FXML
    void searchButtonAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String query = searchField.getText().trim().toLowerCase();
        String criteria = criteriaComboBox.getValue();


        if (criteria == null || criteria.isBlank()) {
            DialogUtils.showWarning(
                    loc.getString("dialog.warning"),
                    loc.getString("dialog.criteria_empty")
            );
            return;
        }
        if (query.isBlank()) {
            DialogUtils.showWarning(
                    loc.getString("dialog.warning"),
                    loc.getString("dialog.search_empty")
            );
            return;
        }
        List<Loan> filtered = loans.stream()
                .filter(loan -> {
                    if (loc.getString("loan.member").equals(criteria)) {
                        String fullName = loan.getMember().getFirstName() + " " + loan.getMember().getLastName();
                        return fullName.toLowerCase().contains(query);
                    } else if (loc.getString("loan.publication").equals(criteria)) {
                        return loan.getBook()
                                .map(b -> b.getTitle().toLowerCase().contains(query))
                                .or(() -> loan.getMagazine()
                                        .map(m -> m.getTitle().toLowerCase().contains(query)))
                                .orElse(false);
                    }
                    return false;
                }).collect(Collectors.toList());


        if (filtered.isEmpty()) {
            DialogUtils.showInfo(
                    loc.getString("dialog.info"),
                    loc.getString("dialog.no_results")
            );
            return;
        }

        loans.setAll(filtered);
    }

    @FXML
    void editButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }

        DatePicker loanDatePicker = new DatePicker(selected.getLoanDate());
        DatePicker returnDatePicker = new DatePicker(selected.getReturnDate());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label(loc.getString("loan.loan_date")), 0, 0);
        form.add(loanDatePicker, 1, 0);
        form.add(new Label(loc.getString("loan.return_date")), 0, 1);
        form.add(returnDatePicker, 1, 1);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(loc.getString("loan.edit_title"));
        dialog.setHeaderText(loc.getString("loan.edit_header"));
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogUtils.applyDialogTheme(dialog);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }

            LocalDate updatedLoanDate = loanDatePicker.getValue();
            LocalDate updatedReturnDate = returnDatePicker.getValue();

            if (updatedLoanDate == null || updatedReturnDate == null) {
                DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("loan.select_dates"));
                return;
            }

            if (updatedReturnDate.isBefore(updatedLoanDate)) {
                DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("loan.invalid_date_range"));
                return;
            }

            selected.setLoanDate(updatedLoanDate);
            selected.setReturnDate(updatedReturnDate);
            LoanDao.updateLoan(selected);
            reloadLoans("success.loan_updated");
        });
    }

    @FXML
    void deleteButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }
        if (selected.getLoanId() == null) {
            DialogUtils.showWarning(loc.getString("dialog.error"), loc.getString("loan.no_loan_id"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(loc.getString("confirm.delete_loan"));
        confirm.setHeaderText(null);
        confirm.setContentText(loc.getString("confirm.delete_loan") + "?");
        DialogUtils.applyDialogTheme(confirm);

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                LoanDao.deleteLoan(selected.getLoanId());
                reloadLoans("success.loan_deleted");
            }
        });
    }

    @FXML
    void backupButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        if (loans.isEmpty()) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("loan.no_loans_for_backup"));
            return;
        }

        try {
            LoanBinaryUtil.zapiši(loans.stream().toList());
            DialogUtils.showInfo(loc.getString("dialog.info"), 
                    loc.getString("loan.backup_success"));
        } catch (Exception e) {
            DialogUtils.showError(loc.getString("dialog.error"), 
                    loc.getString("loan.backup_error") + e.getMessage());
        }
    }

    @FXML
    void returnButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Loan selected = loansTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("dialog.select_item"));
            return;
        }
        if (selected.getLoanId() == null) {
            DialogUtils.showWarning(loc.getString("dialog.error"), loc.getString("loan.no_loan_id"));
            return;
        }
        if ("VRACENA".equals(selected.getStatus())) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("loan.already_returned"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(loc.getString("confirm.return_loan"));
        confirm.setHeaderText(null);
        confirm.setContentText(loc.getString("confirm.return_loan") + "?");
        DialogUtils.applyDialogTheme(confirm);

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                LoanDao.markAsReturned(selected.getLoanId());
                reloadLoans("success.loan_returned");
            }
        });
    }

    @FXML
    void restoreButtonOnAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        try {
            var restored = LoanBinaryUtil.učitaj();
            if (restored.isEmpty()) {
                DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("loan.backup_empty"));
                return;
            }

            loans.setAll(restored);
                DataPreloadService.getInstance().replaceCachedLoans(restored);
            DialogUtils.showInfo(loc.getString("dialog.info"),
                    loc.getString("loan.restore_success"));
        } catch (Exception e) {
            DialogUtils.showError(loc.getString("dialog.error"), 
                    loc.getString("loan.restore_error") + e.getMessage());
        }
    }

    private void loadAllLoansFromDatabase() {
        DataPreloadService cache = DataPreloadService.getInstance();
        if (cache.hasCachedLoans()) {
            loans.setAll(cache.getCachedLoans());
            return;
        }
        LocalizationManager loc = LocalizationManager.getInstance();
        try {
            List<Loan> loadedLoans = LoanDao.getAllLoans();
            loans.setAll(loadedLoans);
            cache.replaceCachedLoans(loadedLoans);
        } catch (Exception e) {
            DialogUtils.showError(loc.getString("dialog.error"), loc.getString("loan.fetch_error"));
            e.printStackTrace();
        }
    }

    private void reloadLoans(String successMessageKey) {
        LocalizationManager loc = LocalizationManager.getInstance();
        DataPreloadService cache = DataPreloadService.getInstance();
        cache.invalidateLoansCache();

        cache.runDbTask(
                LoanDao::getAllLoans,
                newLoans -> {
                    cache.replaceCachedLoans(newLoans);
                    loans.setAll(newLoans);
                    if (successMessageKey != null && !successMessageKey.isBlank()) {
                        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString(successMessageKey));
                    }
                },
                e -> DialogUtils.showError(loc.getString("dialog.error"), loc.getString("loan.fetch_error"))
        );
    }
}
