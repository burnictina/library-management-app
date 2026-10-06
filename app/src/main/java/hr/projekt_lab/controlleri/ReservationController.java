package hr.projekt_lab.controlleri;

import hr.projekt_lab.application.BookApplication;
import hr.projekt_lab.databaseUtil.ReservationDao;
import hr.projekt_lab.entities.Reservation;
import hr.projekt_lab.utils.DataPreloadService;
import hr.projekt_lab.utils.DialogUtils;
import hr.projekt_lab.utils.LocalizationManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;

public class ReservationController {

    private static volatile WeakReference<ReservationController> activeController = new WeakReference<>(null);

    @FXML
    private Button refreshButton;

    @FXML
    private TableView<Reservation> reservationsTable;

    @FXML
    private TableColumn<Reservation, String> memberColumn;

    @FXML
    private TableColumn<Reservation, String> bookColumn;

    @FXML
    private TableColumn<Reservation, LocalDate> reservationDateColumn;

    @FXML
    private TableColumn<Reservation, String> statusColumn;

    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        activeController = new WeakReference<>(this);
        ReservationDao.expireStaleNotifiedReservationsAndPromoteNext();
        LocalizationManager loc = LocalizationManager.getInstance();

        memberColumn.setCellValueFactory(cellData -> {
            var member = cellData.getValue().getMember();
            return new SimpleStringProperty(member.getFirstName() + " " + member.getLastName());
        });
        memberColumn.setText(loc.getString("reservation.member"));

        bookColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getBook().getTitle())
        );
        bookColumn.setText(loc.getString("reservation.book"));

        reservationDateColumn.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getReservationDate())
        );
        reservationDateColumn.setText(loc.getString("reservation.date"));

        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatus())
        );
        statusColumn.setText(loc.getString("reservation.status"));

        refreshButton.setText(loc.getString("reservation.refresh"));

        reservationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        memberColumn.prefWidthProperty().bind(reservationsTable.widthProperty().multiply(0.28));
        bookColumn.prefWidthProperty().bind(reservationsTable.widthProperty().multiply(0.34));
        reservationDateColumn.prefWidthProperty().bind(reservationsTable.widthProperty().multiply(0.20));
        statusColumn.prefWidthProperty().bind(reservationsTable.widthProperty().multiply(0.18));

        reservationsTable.setItems(reservations);
        loadAllReservationsFromDatabase();
    }

    public static void refreshIfCurrentlyDisplayed() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(ReservationController::refreshIfCurrentlyDisplayed);
            return;
        }

        ReservationController controller = activeController.get();
        if (controller == null || !controller.isCurrentlyDisplayed()) {
            return;
        }

        controller.refreshReservationsFromCache();
    }

    @FXML
    void refreshButtonAction(ActionEvent event) {
        ReservationDao.expireStaleNotifiedReservationsAndPromoteNext();
        reloadReservations(null);
    }

    private void loadAllReservationsFromDatabase() {
        DataPreloadService cache = DataPreloadService.getInstance();
        if (cache.hasCachedReservations()) {
            reservations.setAll(sortReservations(cache.getCachedReservations()));
            return;
        }

        LocalizationManager loc = LocalizationManager.getInstance();
        try {
            List<Reservation> loadedReservations = sortReservations(ReservationDao.getAllReservations());
            reservations.setAll(loadedReservations);
            cache.replaceCachedReservations(loadedReservations);
        } catch (Exception e) {
            DialogUtils.showError(loc.getString("dialog.error"), loc.getString("reservation.fetch_error"));
        }
    }

    private void reloadReservations(String successMessageKey) {
        LocalizationManager loc = LocalizationManager.getInstance();
        DataPreloadService cache = DataPreloadService.getInstance();
        cache.invalidateReservationsCache();

        cache.runDbTask(
                () -> sortReservations(ReservationDao.getAllReservations()),
                newReservations -> {
                    cache.replaceCachedReservations(newReservations);
                    reservations.setAll(newReservations);
                    if (successMessageKey != null && !successMessageKey.isBlank()) {
                        DialogUtils.showInfo(loc.getString("dialog.info"), loc.getString(successMessageKey));
                    }
                },
                e -> DialogUtils.showError(loc.getString("dialog.error"), loc.getString("reservation.fetch_error"))
        );
    }

    private void refreshReservationsFromCache() {
        DataPreloadService cache = DataPreloadService.getInstance();
        reservations.setAll(sortReservations(cache.getCachedReservations()));
    }

    private boolean isCurrentlyDisplayed() {
        if (reservationsTable == null || reservationsTable.getScene() == null || BookApplication.getMainStage() == null) {
            return false;
        }

        return reservationsTable.getScene() == BookApplication.getMainStage().getScene();
    }

    private List<Reservation> sortReservations(List<Reservation> source) {
        return source.stream()
                .sorted(
                        Comparator.comparingInt((Reservation r) -> statusPriority(r.getStatus()))
                                .thenComparing(Reservation::getReservationDate)
                                .thenComparing(Reservation::getId, Comparator.nullsLast(Integer::compareTo))
                )
                .toList();
    }

    private int statusPriority(String status) {
        if ("OBAVIJESTEN".equals(status)) {
            return 0;
        }
        if ("CEKA".equals(status)) {
            return 1;
        }
        if ("ISTEKLO".equals(status)) {
            return 2;
        }
        return 3;
    }
}
