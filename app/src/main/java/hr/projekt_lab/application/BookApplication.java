package hr.projekt_lab.application;

import hr.projekt_lab.databaseUtil.LibrarianDao;
import hr.projekt_lab.databaseUtil.BookDao;
import hr.projekt_lab.databaseUtil.ReservationDao;
import hr.projekt_lab.utils.BackgroundSchedulerService;
import hr.projekt_lab.utils.AppSettings;
import hr.projekt_lab.utils.DataPreloadService;
import hr.projekt_lab.utils.LocalizationManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class BookApplication extends Application {

    private static final double MIN_WINDOW_WIDTH = 1120.0;
    private static final double MIN_WINDOW_HEIGHT = 680.0;
    private static final double MAX_START_WINDOW_WIDTH = 1260.0;
    private static final double MAX_START_WINDOW_HEIGHT = 780.0;

    private static Stage mainStage;
    @Override
    public void start(Stage stage) throws IOException {
        mainStage = stage;
        LibrarianDao.ensureDefaultAdminIfEmpty();
        BookDao.reconcileAvailableCopiesWithActiveLoans();
        ReservationDao.ensureReservationTableExists();
        ReservationDao.expireStaleNotifiedReservationsAndPromoteNext();
        BackgroundSchedulerService.getInstance().start();
        LocalizationManager loc = LocalizationManager.getInstance();
        FXMLLoader fxmlLoader = new FXMLLoader(
            getClass().getResource("/hr/projekt_lab/login-view.fxml"),
            loc.getBundle()
        );
        double savedWidth = AppSettings.getWindowWidth();
        double width = Math.max(Math.min(savedWidth, MAX_START_WINDOW_WIDTH), MIN_WINDOW_WIDTH);
        double savedHeight = AppSettings.getWindowHeight();
        double height = Math.max(Math.min(savedHeight, MAX_START_WINDOW_HEIGHT), MIN_WINDOW_HEIGHT);

        Scene scene = new Scene(fxmlLoader.load(), width, height);
        scene.getStylesheets().add(
            getClass().getResource("/hr/projekt_lab/style.css").toExternalForm()
        );

        stage.setOnCloseRequest(event ->
            AppSettings.setWindowSize(stage.getWidth(), stage.getHeight())
        );
        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(MIN_WINDOW_HEIGHT);

        stage.setTitle(loc.getString("window.title.login"));
        stage.setScene(scene);
        stage.show();
    }

    public static Stage getMainStage() {
        return mainStage;
    }

    @Override
    public void stop() {
        BackgroundSchedulerService.getInstance().shutdown();
        DataPreloadService.shutdown();
    }
}
