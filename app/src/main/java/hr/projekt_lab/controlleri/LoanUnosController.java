package hr.projekt_lab.controlleri;

import hr.projekt_lab.databaseUtil.BookDao;
import hr.projekt_lab.databaseUtil.LoanDao;
import hr.projekt_lab.databaseUtil.ReservationDao;
import hr.projekt_lab.entities.Book;
import hr.projekt_lab.entities.Loan;
import hr.projekt_lab.entities.Magazine;
import hr.projekt_lab.entities.Member;
import hr.projekt_lab.entities.Reservation;
import hr.projekt_lab.utils.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class LoanUnosController {

    @FXML
    private Text titleText;

    @FXML
    private Text publicationTypeLabel;

    @FXML
    private ToggleGroup typeToggleGroup;

    @FXML
    private RadioButton bookRadioButton;

    @FXML
    private RadioButton magazineRadioButton;

    @FXML
    private ComboBox<Member> memberComboBox;

    @FXML
    private ComboBox<Book> bookComboBox;

    @FXML
    private ComboBox<Magazine> magazineComboBox;

    @FXML
    private DatePicker loanDatePicker;

    @FXML
    private DatePicker returnDatePicker;

    @FXML
    private Button saveLoanButton;

    private final ObservableList<Member> members = FXCollections.observableArrayList();
    private final ObservableList<Book> books = FXCollections.observableArrayList();
    private final ObservableList<Magazine> magazines = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        LocalizationManager loc = LocalizationManager.getInstance();

        titleText.setText(loc.getString("loan.unos_title"));
        saveLoanButton.setText(loc.getString("loan.save_button"));
        publicationTypeLabel.setText(loc.getString("loan.publication_type"));
        bookRadioButton.setText(loc.getString("loan.type_book"));
        magazineRadioButton.setText(loc.getString("loan.type_magazine"));
        memberComboBox.setPromptText(loc.getString("loan.unos_prompt_member"));
        bookComboBox.setPromptText(loc.getString("loan.unos_prompt_book"));
        magazineComboBox.setPromptText(loc.getString("loan.unos_prompt_magazine"));

        members.addAll(MemberJsonUtils.ucitajClanove().orElse(List.of()));
        memberComboBox.setItems(members);

        books.addAll(BookDao.getAllBooks());
        bookComboBox.setItems(books);

        magazines.addAll(MagazineXmlUtil.ucitajCasopise().orElse(List.of()));
        magazineComboBox.setItems(magazines);

        typeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean bookSelected = (newVal == bookRadioButton);
            bookComboBox.setVisible(bookSelected);
            bookComboBox.setManaged(bookSelected);
            magazineComboBox.setVisible(!bookSelected);
            magazineComboBox.setManaged(!bookSelected);
            if (bookSelected) {
                magazineComboBox.getSelectionModel().clearSelection();
            } else {
                bookComboBox.getSelectionModel().clearSelection();
            }
        });
        bookRadioButton.setSelected(true);

        ControllerUtils.consumePendingLoanBookIsbn().ifPresent(isbn ->
            books.stream()
                .filter(book -> isbn.equals(book.getIsbn()))
                .findFirst()
                .ifPresent(book -> bookComboBox.getSelectionModel().select(book))
        );

        loanDatePicker.setValue(LocalDate.now());
        returnDatePicker.setValue(LocalDate.now().plusDays(14));
    }

    @FXML
    void saveLoanButtonAction(ActionEvent event) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Member selectedMember = memberComboBox.getValue();
        Book selectedBook = bookComboBox.getValue();
        Magazine selectedMagazine = magazineComboBox.getValue();
        LocalDate loanDate = loanDatePicker.getValue();
        LocalDate returnDate = returnDatePicker.getValue();

        if(selectedMember == null){
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("loan.select_member"));
            return;
        }

        if(selectedBook == null && selectedMagazine == null){
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("loan.select_publication"));
            return;
        }

        if (loanDate == null || returnDate == null) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("loan.select_dates"));
            return;
        }

        if (LoanDao.countLoansByMemberId(selectedMember.getId()) >= 3) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("loan.max_active_loans"));
            return;
        }

        List<Loan> loans = LoanDao.getAllLoans();

        boolean alreadyBorrowed = loans.stream().anyMatch(loan -> {

            if (!loan.isActive()) {
                return false;
            }
            if (!loan.getMember().getId().equals(selectedMember.getId())) {
                return false;
            }
            if (selectedBook != null && loan.getBook().isPresent()) {
                return loan.getBook().get().getIsbn().equals(selectedBook.getIsbn());
            }
            if (selectedMagazine != null && loan.getMagazine().isPresent()) {
                return loan.getMagazine().get().getTitle().equalsIgnoreCase(selectedMagazine.getTitle());
            }
            return  false;
        });
        if (alreadyBorrowed) {
            DialogUtils.showWarning(loc.getString("dialog.warning"), loc.getString("loan.duplicate_active"));
            return;
        }

        if (selectedBook != null) {
            String isbn = selectedBook.getIsbn();
            Optional<Integer> bookId = ReservationDao.findBookIdByIsbn(isbn);
            boolean borrowedSuccessfully = BookDao.tryBorrowBook(isbn);
            if (!borrowedSuccessfully) {
                if (bookId.isPresent() && ReservationDao.hasActiveReservation(selectedMember.getId(), bookId.get())) {
                    DialogUtils.showInfo(
                            loc.getString("dialog.info"),
                            loc.getString("loan.reservation_already_exists")
                    );
                    clearFields();
                    return;
                }

                Reservation reservation = new Reservation(
                        selectedMember,
                        selectedBook,
                        LocalDate.now(),
                        "CEKA"
                );

                boolean reservationCreated = ReservationDao.insertReservation(reservation);
                if (reservationCreated) {
                    DataPreloadService cache = DataPreloadService.getInstance();
                    cache.invalidateReservationsCache();
                    cache.refreshReservationsCache();

                    DialogUtils.showInfo(
                            loc.getString("dialog.info"),
                            "Knjiga trenutno nije dostupna - dodani ste na listu čekanja, obavijestit ćemo vas kad postane dostupna."
                    );
                } else {
                    DialogUtils.showError(
                            loc.getString("dialog.error"),
                            "Knjiga trenutno nije dostupna, a stvaranje rezervacije nije uspjelo."
                    );
                }
                clearFields();
                return;
            }

            boolean loanInserted = LoanDao.insertLoan(new Loan(
                selectedMember,
                Optional.of(selectedBook),
                Optional.empty(),
                loanDate,
                returnDate
            ));

            if (!loanInserted) {
                // Kompenzacija: ako upis posudbe ne uspije, vrati primjerak u zalihu.
                BookDao.returnBook(isbn);
                DialogUtils.showError(loc.getString("dialog.error"), loc.getString("loan.save_error"));
                return;
            }

            bookId.ifPresent(id ->
                ReservationDao.findActiveReservationId(selectedMember.getId(), id).ifPresent(reservationId -> {
                    boolean deleted = ReservationDao.deleteReservation(reservationId);
                    if (deleted) {
                        DataPreloadService cache = DataPreloadService.getInstance();
                        cache.invalidateReservationsCache();
                        cache.refreshReservationsCache();
                    }
                })
            );
        }

        if (selectedMagazine != null) {
            boolean loanInserted = LoanDao.insertLoan(new Loan(
                    selectedMember,
                    Optional.empty(),
                    Optional.of(selectedMagazine),
                    loanDate,
                    returnDate
            ));
            if (!loanInserted) {
                DialogUtils.showError(loc.getString("dialog.error"), loc.getString("loan.save_error"));
                return;
            }
        }

        DataPreloadService cache = DataPreloadService.getInstance();
        cache.invalidateLoansCache();
        cache.refreshLoansCache();

        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString("loan.added_success"));

        clearFields();
    }

    private void clearFields() {
        memberComboBox.getSelectionModel().clearSelection();
        bookComboBox.getSelectionModel().clearSelection();
        magazineComboBox.getSelectionModel().clearSelection();
        bookRadioButton.setSelected(true);
        loanDatePicker.setValue(LocalDate.now());
        returnDatePicker.setValue(LocalDate.now().plusDays(14));
    }
}
