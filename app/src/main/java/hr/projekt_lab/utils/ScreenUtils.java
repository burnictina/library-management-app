package hr.projekt_lab.utils;

import hr.projekt_lab.application.BookApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.IOException;

public class ScreenUtils {

    private static String currentFxmlPath;
    private static String currentTitleKey;
    private static String currentStaticTitle;

    private ScreenUtils() {}

    public static void showScreen(String fxmlPath, String title){
        try{
            var stage = BookApplication.getMainStage();
            LocalizationManager loc = LocalizationManager.getInstance();
            FXMLLoader fxmlLoader = new FXMLLoader(
                ScreenUtils.class.getResource(fxmlPath),
                loc.getBundle()
            );

            double width = stage.getScene() != null
                ? stage.getScene().getWidth()
                : stage.getWidth();
            double height = stage.getScene() != null
                ? stage.getScene().getHeight()
                : stage.getHeight();
            Scene scene = new Scene(fxmlLoader.load(), width, height);
            scene.getStylesheets().add(
                ScreenUtils.class.getResource("/hr/projekt_lab/style.css").toExternalForm()
            );
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();

            currentFxmlPath = fxmlPath;
            currentTitleKey = null;
            currentStaticTitle = title;
        } catch(IOException e) {
            DialogUtils.showDisplayScreenErrorDialog();
        }
    }

    public static void showScreenByTitleKey(String fxmlPath, String titleKey) {
        LocalizationManager loc = LocalizationManager.getInstance();
        String title = loc.getString(titleKey);
        showScreen(fxmlPath, title);
        currentFxmlPath = fxmlPath;
        currentTitleKey = titleKey;
    }

    public static void reloadCurrentScreen() {
        if (currentFxmlPath == null || BookApplication.getMainStage() == null) {
            return;
        }

        if (currentTitleKey != null) {
            showScreenByTitleKey(currentFxmlPath, currentTitleKey);
            return;
        }

        String titleToUse = currentStaticTitle != null
                ? currentStaticTitle
                : BookApplication.getMainStage().getTitle();
        showScreen(currentFxmlPath, titleToUse);
    }
}
