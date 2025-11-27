package vcs.repository.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import vcs.repository.VcsManager;
import vcs.repository.classes.Branch;
import vcs.repository.classes.Commit;
import vcs.repository.classes.Repository;
import vcs.repository.VcsType;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class MainController {

    @FXML private ListView<Repository> repoListView;
    @FXML private ListView<String> fileListView;
    @FXML private TextArea commitMessageArea;
    @FXML private TableView<Commit> historyTable;
    @FXML private TableColumn<Commit, Integer> colCommitId;
    @FXML private TableColumn<Commit, String> colMessage;
    @FXML private TableColumn<Commit, Integer> colAuthor; // Поки ID автора
    @FXML private Label statusLabel;
    @FXML private ListView<Branch> branchListView;
    @FXML private TextField branchNameField;
    @FXML private TextField tagNameField;
    @FXML private TextField tagMessageField;
    @FXML private TextArea graphTextArea;

    private VcsManager vcsManager;
    private Repository currentRepo;
    private final int CURRENT_USER_ID = 1;

    @FXML
    public void initialize() {
        // 1. Отримуємо менеджер
        this.vcsManager = App.getVcsManager();

        // 2. Налаштовуємо таблицю історії
        colCommitId.setCellValueFactory(new PropertyValueFactory<>("commit_Id"));
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author_id"));

        // 3. Завантажуємо список репозиторіїв
        loadRepositories();

        // 4. Слухач вибору репозиторію
        repoListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentRepo = newVal;
                statusLabel.setText("Selected: " + currentRepo.getName() + " (" + currentRepo.getType() + ")");
                onRefreshClicked(); // Оновити список файлів
                loadBranches();
            }
        });

        branchListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Branch item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        onRefreshGraphClicked();
    }

    private void loadBranches() {
        if (currentRepo == null) return;
        try {
            List<Branch> branches = vcsManager.getBranches(currentRepo.getRepo_id());
            branchListView.setItems(FXCollections.observableArrayList(branches));
        } catch (Exception e) {
            System.out.println("Could not load branches: " + e.getMessage());
        }
    }

    private void loadRepositories() {
        List<Repository> repos = vcsManager.getAllRepositories();
        repoListView.setItems(FXCollections.observableArrayList(repos));
        repoListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Repository item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " [" + item.getType() + "]");
                }
            }
        });
    }

    @FXML
    public void onRefreshClicked() {
        if (currentRepo == null) return;
        try {
            List<String> files = vcsManager.listFiles(currentRepo.getRepo_id(), null);
            fileListView.setItems(FXCollections.observableArrayList(files));
            statusLabel.setText("Files refreshed.");
        } catch (Exception e) {
            statusLabel.setText("Error listing files: " + e.getMessage());
        }
    }

    @FXML
    public void onCommitClicked() {
        if (currentRepo == null) {
            new Alert(Alert.AlertType.WARNING, "Select a repository first").show();
            return;
        }
        String msg = commitMessageArea.getText();
        if (msg.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Enter commit message").show();
            return;
        }

        try {
            vcsManager.commit(currentRepo.getRepo_id(), CURRENT_USER_ID, msg);

            commitMessageArea.clear();
            onRefreshClicked();
            statusLabel.setText("Committed successfully!");

            onLoadHistoryClicked();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Commit failed: " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @FXML
    public void onLoadHistoryClicked() {
        if (currentRepo == null) return;
        try {
            List<Commit> history = vcsManager.getLog(currentRepo.getRepo_id());
            historyTable.setItems(FXCollections.observableArrayList(history));
        } catch (Exception e) {
            statusLabel.setText("Error loading history");
        }
    }

    @FXML
    public void onCreateBranchClicked() {
        if (currentRepo == null) {
            showAlert(Alert.AlertType.WARNING, "Select a repository first");
            return;
        }
        String branchName = branchNameField.getText().trim();
        if (branchName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Enter branch name");
            return;
        }

        try {
            vcsManager.createBranch(currentRepo.getRepo_id(), branchName, CURRENT_USER_ID);

            showAlert(Alert.AlertType.INFORMATION, "Branch '" + branchName + "' created!");
            branchNameField.clear();
            loadBranches();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error creating branch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onCheckoutClicked() {
        Branch selectedBranch = branchListView.getSelectionModel().getSelectedItem();
        if (selectedBranch == null) {
            showAlert(Alert.AlertType.WARNING, "Select a branch from the list");
            return;
        }

        try {
            vcsManager.checkout(currentRepo.getRepo_id(), selectedBranch.getName());

            showAlert(Alert.AlertType.INFORMATION, "Switched to branch: " + selectedBranch.getName());
            onRefreshClicked();
            onLoadHistoryClicked();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Checkout failed: " + e.getMessage());
        }
    }

    @FXML
    public void onCreateTagClicked() {
        if (currentRepo == null) {
            showAlert(Alert.AlertType.WARNING, "Select a repository first");
            return;
        }
        String tagName = tagNameField.getText().trim();
        String message = tagMessageField.getText().trim();

        if (tagName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Enter tag name");
            return;
        }

        try {
            vcsManager.createTag(currentRepo.getRepo_id(), tagName, message);

            showAlert(Alert.AlertType.INFORMATION, "Tag '" + tagName + "' created successfully!");
            tagNameField.clear();
            tagMessageField.clear();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error creating tag: " + e.getMessage());
        }
    }

    @FXML
    public void onCreateRepoClicked() {
        // 1. Створюємо кастомний діалог
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Create New Repository");
        dialog.setHeaderText("Configure your new repository");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // 2. Створюємо поля для вводу
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Project Name");

        TextField pathField = new TextField();
        pathField.setPromptText("C:\\Path\\To\\Repo");
        pathField.setEditable(false); // Тільки через кнопку Browse, щоб уникнути помилок

        Button browseButton = new Button("Browse...");

        ComboBox<VcsType> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().setAll(VcsType.values()); // GIT, SVN, MERCURIAL
        typeComboBox.setValue(VcsType.GIT); // Значення за замовчуванням

        TextArea descArea = new TextArea();
        descArea.setPromptText("Description...");
        descArea.setPrefRowCount(2);

        // 3. Розміщуємо елементи на сітці
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);

        grid.add(new Label("Path:"), 0, 1);
        grid.add(pathField, 1, 1);
        grid.add(browseButton, 2, 1); // Кнопка Browse збоку

        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeComboBox, 1, 2);

        grid.add(new Label("Description:"), 0, 3);
        grid.add(descArea, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // 4. Логіка кнопки Browse (DirectoryChooser)
        browseButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Parent Directory");
            File selectedDirectory = chooser.showDialog(dialog.getOwner());
            if (selectedDirectory != null) {
                String basePath = selectedDirectory.getAbsolutePath();
                String projectName = nameField.getText().trim();

                if (!projectName.isEmpty()) {
                    pathField.setText(basePath + File.separator + projectName);
                } else {
                    pathField.setText(basePath);
                }
            }
        });

        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            String currentPath = pathField.getText();
            if (!currentPath.isEmpty() && !oldValue.isEmpty() && currentPath.endsWith(oldValue)) {
                pathField.setText(currentPath.substring(0, currentPath.lastIndexOf(oldValue)) + newValue);
            }
        });

        javafx.scene.Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            createButton.setDisable(newValue.trim().isEmpty() || pathField.getText().isEmpty());
        });
        pathField.textProperty().addListener((observable, oldValue, newValue) -> {
            createButton.setDisable(newValue.trim().isEmpty() || nameField.getText().isEmpty());
        });

        // 5. Обробка результату (Натискання Create)
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try {
                    vcsManager.createRepository(
                            nameField.getText(),
                            pathField.getText(),
                            typeComboBox.getValue(),
                            CURRENT_USER_ID,
                            descArea.getText()
                    );
                    return true;
                } catch (Exception e) {
                    // Показуємо помилку, якщо щось пішло не так (наприклад, папка існує)
                    showAlert(Alert.AlertType.ERROR, "Creation Failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return false;
        });

        // 6. Показуємо діалог
        Optional<Boolean> result = dialog.showAndWait();

        // Якщо успішно створили - оновлюємо список
        result.ifPresent(success -> {
            if (success) {
                loadRepositories();
                showAlert(Alert.AlertType.INFORMATION, "Repository created successfully!");
            }
        });
    }

    @FXML
    public void onPullClicked() {
        if (currentRepo == null) {
            showAlert(Alert.AlertType.WARNING, "Select a repository first.");
            return;
        }

        // 1. Для SVN шлях не потрібен (він знає свій сервер), але для Git/Hg - критично.
        // Ми відкриваємо діалог вибору папки для універсальності P2P.
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Repository to Pull FROM");
        File remoteDir = chooser.showDialog(null);

        if (remoteDir != null) {
            try {
                // Блокуємо UI або показуємо прогрес
                statusLabel.setText("Pulling changes...");

                // 2. Викликаємо Pull
                boolean success = vcsManager.pull(currentRepo.getRepo_id(), remoteDir.getAbsolutePath());

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Pull successful!");

                    // 3. Обов'язково оновлюємо UI, бо прийшли нові файли та історія
                    onRefreshClicked();     // Оновити список файлів
                    onLoadHistoryClicked(); // Оновити історію комітів
                    statusLabel.setText("Pull complete.");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Pull finished, but verify output.");
                }

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Pull Failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onPushClicked() {
        if (currentRepo == null) {
            showAlert(Alert.AlertType.WARNING, "Select a repository first.");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Repository to Push TO");
        File remoteDir = chooser.showDialog(null);

        if (remoteDir != null) {
            try {
                statusLabel.setText("Pushing changes...");

                boolean success = vcsManager.push(currentRepo.getRepo_id(), remoteDir.getAbsolutePath());

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Push successful!");
                    statusLabel.setText("Push complete.");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Push finished with warnings.");
                }

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Push Failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onMergeClicked() {
        if (currentRepo == null) return;

        // 1. Отримуємо гілку, яку хочемо влити (SOURCE)
        Branch sourceBranch = branchListView.getSelectionModel().getSelectedItem();

        if (sourceBranch == null) {
            showAlert(Alert.AlertType.WARNING, "Select a branch from the list to merge (SOURCE).");
            return;
        }

        // 2. Запитуємо назву поточної гілки (TARGET)
        TextInputDialog dialog = new TextInputDialog("master"); // або "trunk" / "default"
        dialog.setTitle("Merge Operation");
        dialog.setHeaderText("Merging: " + sourceBranch.getName() + " -> TARGET");
        dialog.setContentText("Enter TARGET branch name (current branch):");

        java.util.Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String targetBranchName = result.get().trim();
            if (targetBranchName.isEmpty()) return;

            // Блокуємо UI на час операції, бо мердж може бути довгим
            Alert processingAlert = new Alert(Alert.AlertType.INFORMATION, "Merging in progress...");
            processingAlert.show();

            try {
                // 3. Викликаємо менеджер
                vcs.repository.classes.MergeResults results = vcsManager.merge(
                        currentRepo.getRepo_id(),
                        sourceBranch.getName(),
                        targetBranchName
                );

                processingAlert.close();

                if (results.isSuccess()) {
                    showAlert(Alert.AlertType.INFORMATION, "Merge Successful!");

                    onRefreshClicked();
                    onLoadHistoryClicked();
                } else {
                    // 4. Обробка конфліктів
                    StringBuilder sb = new StringBuilder("Merge finished with conflicts:\n");
                    for (vcs.repository.classes.FileConflict conflict : results.getConflicts()) {
                        sb.append("- ").append(conflict.getFilePath()).append("\n");
                    }
                    sb.append("\nPlease resolve conflicts manually in the working directory.");

                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Merge Conflicts");
                    errorAlert.setHeaderText("Automatic merge failed");
                    errorAlert.setContentText(sb.toString());
                    errorAlert.showAndWait();
                }

            } catch (Exception e) {
                processingAlert.close();
                showAlert(Alert.AlertType.ERROR, "Merge Failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onRefreshGraphClicked() {
        if (currentRepo == null) return;

        try {
            String graphOutput = vcsManager.getRepoGraph(currentRepo.getRepo_id());
            graphTextArea.setText(graphOutput);

            graphTextArea.setScrollTop(0);

        } catch (Exception e) {
            graphTextArea.setText("Error: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String content) {
        Alert alert = new Alert(type, content);
        alert.show();
    }
}