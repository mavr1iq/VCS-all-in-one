package vcs.repository.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vcs.repository.VcsManager;

import java.io.IOException;

public class App extends Application {

    private static VcsManager vcsManager;

    @Override
    public void start(Stage stage) throws IOException {
        vcsManager = new VcsManager();
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        stage.setTitle("VCS All-In-One");
        stage.setScene(scene);
        stage.show();
    }

    public static VcsManager getVcsManager() {
        return vcsManager;
    }

    public static void main(String[] args) {
        launch();
    }
}