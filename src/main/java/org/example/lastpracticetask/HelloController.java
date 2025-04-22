package org.example.lastpracticetask;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HelloController
{
    private DataBase db;
    private Connection connection;

    @FXML private ComboBox<String> trainerComboBox;
    @FXML private ComboBox<String> clientComboBox;
    @FXML private ComboBox<String> serviceComboBox;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeComboBox;
    
    @FXML private TableView<ScheduleItem> scheduleTable;
    @FXML private TableColumn<ScheduleItem, String> trainerColumn;
    @FXML private TableColumn<ScheduleItem, String> clientColumn;
    @FXML private TableColumn<ScheduleItem, String> dateColumn;
    @FXML private TableColumn<ScheduleItem, String> timeColumn;
    @FXML private TableColumn<ScheduleItem, String> serviceColumn;
    @FXML private TableColumn<ScheduleItem, String> statusColumn;

    @FXML private TableView<Trainer> trainersTable;
    @FXML private TableColumn<Trainer, String> trainerNameColumn;
    @FXML private TableColumn<Trainer, String> trainerSpecColumn;
    @FXML private TableColumn<Trainer, String> trainerPhoneColumn;
    @FXML private TableColumn<Trainer, String> trainerEmailColumn;

    @FXML private TableView<Client> clientsTable;
    @FXML private TableColumn<Client, String> clientNameColumn;
    @FXML private TableColumn<Client, String> clientPhoneColumn;
    @FXML private TableColumn<Client, String> clientEmailColumn;

    @FXML private TableView<Service> servicesTable;
    @FXML private TableColumn<Service, String> serviceNameColumn;
    @FXML private TableColumn<Service, String> serviceDescColumn;
    @FXML private TableColumn<Service, Double> servicePriceColumn;
    @FXML private TableColumn<Service, String> serviceTypeColumn;

    @FXML private TableView<SubscriptionItem> subscriptionsTable;
    @FXML private TableColumn<SubscriptionItem, String> subscriptionClientColumn;
    @FXML private TableColumn<SubscriptionItem, String> subscriptionServiceColumn;
    @FXML private TableColumn<SubscriptionItem, String> subscriptionStartColumn;
    @FXML private TableColumn<SubscriptionItem, String> subscriptionEndColumn;
    @FXML private TableColumn<SubscriptionItem, String> subscriptionStatusColumn;
    @FXML private TableColumn<SubscriptionItem, String> subscriptionReasonColumn;

    @FXML private ComboBox<String> subscriptionClientComboBox;
    @FXML private ComboBox<String> subscriptionServiceComboBox;

    @FXML private DatePicker subscriptionStartDatePicker;

    @FXML
    public void initialize() {
        try {
            db = new DataBase();
            connection = db.getDBConnection();
            db.createTables();
            
            setupComboBoxes();
            setupTables();
            loadData();
            setupScheduleContextMenu();
        } catch (SQLException | ClassNotFoundException e) {
            showError("Ошибка инициализации", e.getMessage());
        }
    }

    private void setupComboBoxes() {
        timeComboBox.setItems(FXCollections.observableArrayList(
            "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00",
            "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00"
        ));
        
        datePicker.setValue(LocalDate.now());
        
        subscriptionClientComboBox.setItems(FXCollections.observableArrayList());
        subscriptionServiceComboBox.setItems(FXCollections.observableArrayList());
        
        subscriptionStartDatePicker.setValue(LocalDate.now());
    }

    private void setupTables() {
        trainerColumn.setCellValueFactory(new PropertyValueFactory<>("trainerName"));
        clientColumn.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        serviceColumn.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        trainerNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        trainerSpecColumn.setCellValueFactory(new PropertyValueFactory<>("specialization"));
        trainerPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        trainerEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        clientNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        clientPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        clientEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        serviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        serviceDescColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        servicePriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        serviceTypeColumn.setCellValueFactory(cellData -> {
            Service service = cellData.getValue();
            String type = "";
            if (service.isSubscription()) {
                type = "Абонемент";
            } else if (service.isGroup()) {
                type = "Групповая";
            } else {
                type = "Персональная";
            }
            return new SimpleStringProperty(type);
        });

        subscriptionClientColumn.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        subscriptionServiceColumn.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        subscriptionStartColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        subscriptionEndColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        subscriptionStatusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String displayStatus;
            switch (status) {
                case "active" -> displayStatus = "Активен";
                case "terminated" -> displayStatus = "Завершен";
                default -> displayStatus = status;
            }
            return new SimpleStringProperty(displayStatus);
        });
        subscriptionReasonColumn.setCellValueFactory(new PropertyValueFactory<>("terminationReason"));
    }

    private void loadData() {
        loadTrainers();
        loadClients();
        loadServices();
        loadSchedule();
        loadSubscriptions();
        loadSubscriptionComboBoxes();
    }

    private void loadTrainers() {
        try {
            String query = "SELECT * FROM trainers";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            List<Trainer> trainers = new ArrayList<>();
            List<String> trainerNames = new ArrayList<>();
            
            while (rs.next()) {
                Trainer trainer = new Trainer(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("specialization"),
                    rs.getString("phone"),
                    rs.getString("email")
                );
                trainers.add(trainer);
                trainerNames.add(trainer.getName());
            }
            
            trainersTable.setItems(FXCollections.observableArrayList(trainers));
            trainerComboBox.setItems(FXCollections.observableArrayList(trainerNames));
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка загрузки тренеров", e.getMessage());
        }
    }

    private void loadClients() {
        try {
            String query = "SELECT * FROM clients";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            List<Client> clients = new ArrayList<>();
            List<String> clientNames = new ArrayList<>();
            
            while (rs.next()) {
                Client client = new Client(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("email")
                );
                clients.add(client);
                clientNames.add(client.getName());
            }
            
            clientsTable.setItems(FXCollections.observableArrayList(clients));
            clientComboBox.setItems(FXCollections.observableArrayList(clientNames));
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка загрузки клиентов", e.getMessage());
        }
    }

    private void loadServices() {
        try {
            String query = "SELECT * FROM services WHERE is_active = true";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            List<Service> services = new ArrayList<>();
            List<String> serviceNames = new ArrayList<>();
            
            while (rs.next()) {
                Service service = new Service(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getDouble("price"),
                    rs.getBoolean("is_group"),
                    rs.getBoolean("is_active"),
                    rs.getBoolean("is_subscription")
                );
                services.add(service);
                if (!service.isSubscription()) {
                    serviceNames.add(service.getName());
                }
            }
            
            servicesTable.setItems(FXCollections.observableArrayList(services));
            serviceComboBox.setItems(FXCollections.observableArrayList(serviceNames));
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка загрузки услуг", e.getMessage());
        }
    }

    private void loadSchedule() {
        try {
            String query = """
                SELECT s.*, t.name as trainer_name, c.name as client_name, sv.name as service_name,
                CASE 
                    WHEN s.status = 'scheduled' THEN 'Запланирована'
                    WHEN s.status = 'completed' THEN 'Завершена'
                    WHEN s.status = 'cancelled' THEN 'Отменена'
                    ELSE s.status
                END as status_ru
                FROM schedule s
                JOIN trainers t ON s.trainer_id = t.id
                JOIN clients c ON s.client_id = c.id
                JOIN services sv ON s.service_id = sv.id
                ORDER BY s.training_date, s.training_time
            """;
            
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            List<ScheduleItem> scheduleItems = new ArrayList<>();
            
            while (rs.next()) {
                ScheduleItem item = new ScheduleItem(
                    rs.getString("trainer_name"),
                    rs.getString("client_name"),
                    rs.getDate("training_date").toString(),
                    rs.getTime("training_time").toString(),
                    rs.getString("service_name"),
                    rs.getString("status_ru")
                );
                scheduleItems.add(item);
            }
            
            scheduleTable.setItems(FXCollections.observableArrayList(scheduleItems));
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка загрузки расписания", e.getMessage());
        }
    }

    private void loadSubscriptionComboBoxes() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM clients ORDER BY name");
            List<String> clients = new ArrayList<>();
            while (rs.next()) {
                clients.add(rs.getString("name"));
            }
            subscriptionClientComboBox.setItems(FXCollections.observableArrayList(clients));
            
            rs = stmt.executeQuery("SELECT name FROM services WHERE is_subscription = true AND is_active = true ORDER BY name");
            List<String> services = new ArrayList<>();
            while (rs.next()) {
                services.add(rs.getString("name"));
            }
            subscriptionServiceComboBox.setItems(FXCollections.observableArrayList(services));
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка загрузки данных", e.getMessage());
        }
    }

    private void loadSubscriptions() {
        try {
            String query = """
                SELECT c.name as client_name, s.name as service_name,
                       sch.subscription_start_date, sch.subscription_end_date,
                       sch.status, sch.termination_reason
                FROM schedule sch
                JOIN clients c ON sch.client_id = c.id
                JOIN services s ON sch.service_id = s.id
                WHERE s.is_subscription = true
                ORDER BY sch.subscription_start_date DESC
            """;
            
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            List<SubscriptionItem> subscriptions = new ArrayList<>();
            
            while (rs.next()) {
                SubscriptionItem item = new SubscriptionItem(
                    rs.getString("client_name"),
                    rs.getString("service_name"),
                    rs.getDate("subscription_start_date").toString(),
                    rs.getDate("subscription_end_date").toString(),
                    rs.getString("status"),
                    rs.getString("termination_reason")
                );
                subscriptions.add(item);
            }
            
            subscriptionsTable.setItems(FXCollections.observableArrayList(subscriptions));
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка загрузки абонементов", e.getMessage());
        }
    }

    private void setupScheduleContextMenu() {
        scheduleTable.setRowFactory(tv -> {
            TableRow<ScheduleItem> row = new TableRow<>();
            ContextMenu contextMenu = new ContextMenu();
            
            MenuItem completeItem = new MenuItem("Отметить как завершенную");
            completeItem.setOnAction(event -> updateTrainingStatus(row.getItem(), "completed"));
            
            MenuItem cancelItem = new MenuItem("Отменить тренировку");
            cancelItem.setOnAction(event -> updateTrainingStatus(row.getItem(), "cancelled"));
            
            contextMenu.getItems().addAll(completeItem, cancelItem);
            
            row.contextMenuProperty().bind(
                Bindings.when(row.emptyProperty())
                        .then((ContextMenu) null)
                        .otherwise(contextMenu)
            );
            
            return row;
        });
    }

    private void updateTrainingStatus(ScheduleItem item, String newStatus) {
        if (item == null) return;
        
        try {
            String query = """
                UPDATE schedule 
                SET status = ? 
                WHERE trainer_id = (SELECT id FROM trainers WHERE name = ?)
                AND client_id = (SELECT id FROM clients WHERE name = ?)
                AND training_date = ?
                AND training_time = ?
            """;
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, newStatus);
            stmt.setString(2, item.getTrainerName());
            stmt.setString(3, item.getClientName());
            stmt.setDate(4, Date.valueOf(item.getDate()));
            stmt.setTime(5, Time.valueOf(item.getTime()));
            
            stmt.executeUpdate();
            loadSchedule();
            
            String statusText = newStatus.equals("completed") ? "завершенной" : "отмененной";
            showInfo("Успех", "Тренировка отмечена как " + statusText);
            
            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка", "Не удалось обновить статус тренировки: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteScheduleItem() {
        ScheduleItem selectedItem = scheduleTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showError("Ошибка", "Пожалуйста, выберите тренировку для удаления");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Подтверждение удаления");
        confirmDialog.setHeaderText("Удаление тренировки");
        confirmDialog.setContentText("Вы уверены, что хотите удалить тренировку?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String query = """
                        DELETE FROM schedule 
                        WHERE trainer_id = (SELECT id FROM trainers WHERE name = ?)
                        AND client_id = (SELECT id FROM clients WHERE name = ?)
                        AND training_date = ?
                        AND training_time = ?
                    """;
                    
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setString(1, selectedItem.getTrainerName());
                    stmt.setString(2, selectedItem.getClientName());
                    stmt.setDate(3, Date.valueOf(selectedItem.getDate()));
                    stmt.setTime(4, Time.valueOf(selectedItem.getTime()));
                    
                    stmt.executeUpdate();
                    loadSchedule();
                    showInfo("Успех", "Тренировка успешно удалена");
                    
                    stmt.close();
                } catch (SQLException e) {
                    showError("Ошибка", "Не удалось удалить тренировку: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onScheduleTraining() {
        try {
            String trainerName = trainerComboBox.getValue();
            String clientName = clientComboBox.getValue();
            String serviceName = serviceComboBox.getValue();
            LocalDate date = datePicker.getValue();
            String time = timeComboBox.getValue();

            if (clientName == null || serviceName == null || date == null || time == null) {
                showError("Ошибка", "Пожалуйста, заполните все поля");
                return;
            }

            String serviceCheckQuery = "SELECT is_group, is_subscription FROM services WHERE name = ? AND is_active = true";
            PreparedStatement serviceCheckStmt = connection.prepareStatement(serviceCheckQuery);
            serviceCheckStmt.setString(1, serviceName);
            ResultSet serviceRs = serviceCheckStmt.executeQuery();

            if (!serviceRs.next()) {
                showError("Ошибка", "Услуга не найдена или неактивна");
                return;
            }

            boolean isGroupTraining = serviceRs.getBoolean("is_group");
            boolean isSubscription = serviceRs.getBoolean("is_subscription");

            if (!isSubscription && trainerName == null) {
                showError("Ошибка", "Для обычной тренировки необходимо выбрать тренера");
                return;
            }

            if (trainerName != null) {
                String checkQuery = """
                    SELECT COUNT(*) as count
                    FROM schedule s
                    JOIN services sv ON s.service_id = sv.id
                    WHERE s.trainer_id = (SELECT id FROM trainers WHERE name = ?)
                    AND s.training_date = ?
                    AND s.training_time = ?
                    AND s.status = 'scheduled'
                """;

                PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
                checkStmt.setString(1, trainerName);
                checkStmt.setDate(2, Date.valueOf(date));
                checkStmt.setTime(3, Time.valueOf(time + ":00"));

                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt("count");
                    if (count > 0) {
                        if (!isGroupTraining || count >= 10) {
                            showError("Ошибка", isGroupTraining ? 
                                "В группе уже максимальное количество участников (10 человек)" : 
                                "Это время уже занято");
                            return;
                        }
                    }
                }
            }

            String duplicateCheckQuery = """
                SELECT COUNT(*) as count
                FROM schedule s
                WHERE s.client_id = (SELECT id FROM clients WHERE name = ?)
                AND s.training_date = ?
                AND s.training_time = ?
                AND s.status = 'scheduled'
            """;

            PreparedStatement duplicateCheckStmt = connection.prepareStatement(duplicateCheckQuery);
            duplicateCheckStmt.setString(1, clientName);
            duplicateCheckStmt.setDate(2, Date.valueOf(date));
            duplicateCheckStmt.setTime(3, Time.valueOf(time + ":00"));

            ResultSet duplicateRs = duplicateCheckStmt.executeQuery();
            if (duplicateRs.next() && duplicateRs.getInt("count") > 0) {
                showError("Ошибка", "Этот клиент уже записан на тренировку в это время");
                return;
            }

            String insertQuery;
            if (isSubscription) {
                insertQuery = """
                    INSERT INTO schedule (client_id, service_id, training_date, training_time, status, 
                                       subscription_start_date, subscription_end_date)
                    SELECT c.id, s.id, ?, ?, 'scheduled', ?, ?
                    FROM clients c, services s
                    WHERE c.name = ? AND s.name = ?
                """;
            } else {
                insertQuery = """
                    INSERT INTO schedule (trainer_id, client_id, service_id, training_date, training_time, status)
                    SELECT t.id, c.id, s.id, ?, ?, 'scheduled'
                    FROM trainers t, clients c, services s
                    WHERE t.name = ? AND c.name = ? AND s.name = ?
                """;
            }

            PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
            if (isSubscription) {
                insertStmt.setDate(1, Date.valueOf(date));
                insertStmt.setTime(2, Time.valueOf(time + ":00"));
                insertStmt.setDate(3, Date.valueOf(date));
                insertStmt.setDate(4, Date.valueOf(date.plusDays(30)));
                insertStmt.setString(5, clientName);
                insertStmt.setString(6, serviceName);
            } else {
                insertStmt.setDate(1, Date.valueOf(date));
                insertStmt.setTime(2, Time.valueOf(time + ":00"));
                insertStmt.setString(3, trainerName);
                insertStmt.setString(4, clientName);
                insertStmt.setString(5, serviceName);
            }

            insertStmt.executeUpdate();
            loadSchedule();
            showInfo("Успех", isSubscription ? 
                "Абонемент успешно оформлен" : 
                "Тренировка успешно запланирована");

        } catch (SQLException e) {
            showError("Ошибка записи на тренировку", e.getMessage());
        }
    }

    @FXML
    private void onAddTrainer() {
        Dialog<Trainer> dialog = new Dialog<>();
        dialog.setTitle("Добавить тренера");
        dialog.setHeaderText("Введите данные тренера");

        TextField nameField = new TextField();
        TextField specField = new TextField();
        TextField phoneField = new TextField();
        TextField emailField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Имя:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Специализация:"), 0, 1);
        grid.add(specField, 1, 1);
        grid.add(new Label("Телефон:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new Trainer(0, nameField.getText(), specField.getText(), 
                                 phoneField.getText(), emailField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(trainer -> {
            try {
                String query = """
                    INSERT INTO trainers (name, specialization, phone, email)
                    VALUES (?, ?, ?, ?)
                """;
                
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, trainer.getName());
                stmt.setString(2, trainer.getSpecialization());
                stmt.setString(3, trainer.getPhone());
                stmt.setString(4, trainer.getEmail());
                
                stmt.executeUpdate();
                loadTrainers();
                showInfo("Успех", "Тренер успешно добавлен");
                
                stmt.close();
            } catch (SQLException e) {
                showError("Ошибка", "Не удалось добавить тренера: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onAddClient() {
        Dialog<Client> dialog = new Dialog<>();
        dialog.setTitle("Добавить клиента");
        dialog.setHeaderText("Введите данные клиента");

        TextField nameField = new TextField();
        TextField phoneField = new TextField();
        TextField emailField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Имя:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Телефон:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new Client(0, nameField.getText(), phoneField.getText(), emailField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(client -> {
            try {
                String query = """
                    INSERT INTO clients (name, phone, email)
                    VALUES (?, ?, ?)
                """;
                
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, client.getName());
                stmt.setString(2, client.getPhone());
                stmt.setString(3, client.getEmail());
                
                stmt.executeUpdate();
                loadClients();
                showInfo("Успех", "Клиент успешно добавлен");
                
                stmt.close();
            } catch (SQLException e) {
                showError("Ошибка", "Не удалось добавить клиента: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteTrainer() {
        Trainer selectedTrainer = trainersTable.getSelectionModel().getSelectedItem();
        if (selectedTrainer == null) {
            showError("Ошибка", "Пожалуйста, выберите тренера для удаления");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Подтверждение удаления");
        confirmDialog.setHeaderText("Удаление тренера");
        confirmDialog.setContentText("Вы уверены, что хотите удалить тренера " + selectedTrainer.getName() + "?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String query = "DELETE FROM trainers WHERE id = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, selectedTrainer.getId());
                    stmt.executeUpdate();
                    loadTrainers();
                    showInfo("Успех", "Тренер успешно удален");
                    stmt.close();
                } catch (SQLException e) {
                    showError("Ошибка", "Не удалось удалить тренера: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onDeleteClient() {
        Client selectedClient = clientsTable.getSelectionModel().getSelectedItem();
        if (selectedClient == null) {
            showError("Ошибка", "Пожалуйста, выберите клиента для удаления");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Подтверждение удаления");
        confirmDialog.setHeaderText("Удаление клиента");
        confirmDialog.setContentText("Вы уверены, что хотите удалить клиента " + selectedClient.getName() + "?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String query = "DELETE FROM clients WHERE id = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, selectedClient.getId());
                    stmt.executeUpdate();
                    loadClients();
                    showInfo("Успех", "Клиент успешно удален");
                    stmt.close();
                } catch (SQLException e) {
                    showError("Ошибка", "Не удалось удалить клиента: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onAddService() {
        Dialog<Service> dialog = new Dialog<>();
        dialog.setTitle("Добавить услугу");
        dialog.setHeaderText("Введите данные услуги");

        TextField nameField = new TextField();
        TextArea descField = new TextArea();
        TextField priceField = new TextField();
        CheckBox isGroupCheckBox = new CheckBox("Групповая тренировка");
        CheckBox isSubscriptionCheckBox = new CheckBox("Абонемент на 30 дней");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Описание:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Цена:"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Тип:"), 0, 3);
        grid.add(isGroupCheckBox, 1, 3);
        grid.add(isSubscriptionCheckBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    return new Service(0, nameField.getText(), descField.getText(), price, 
                                    isGroupCheckBox.isSelected(), true, isSubscriptionCheckBox.isSelected());
                } catch (NumberFormatException e) {
                    showError("Ошибка", "Цена должна быть числом");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(service -> {
            try {
                String query = """
                    INSERT INTO services (name, description, price, is_group, is_active, is_subscription)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;
                
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, service.getName());
                stmt.setString(2, service.getDescription());
                stmt.setDouble(3, service.getPrice());
                stmt.setBoolean(4, service.isGroup());
                stmt.setBoolean(5, service.isActive());
                stmt.setBoolean(6, service.isSubscription());
                
                stmt.executeUpdate();
                loadServices();
                showInfo("Успех", "Услуга успешно добавлена");
                
                stmt.close();
            } catch (SQLException e) {
                showError("Ошибка", "Не удалось добавить услугу: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteService() {
        Service selectedService = servicesTable.getSelectionModel().getSelectedItem();
        if (selectedService == null) {
            showError("Ошибка", "Пожалуйста, выберите услугу для удаления");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Подтверждение удаления");
        confirmDialog.setHeaderText("Удаление услуги");
        confirmDialog.setContentText("Вы уверены, что хотите удалить услугу " + selectedService.getName() + "?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String query = "UPDATE services SET is_active = false WHERE id = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, selectedService.getId());
                    stmt.executeUpdate();
                    loadServices();
                    showInfo("Успех", "Услуга успешно удалена");
                    stmt.close();
                } catch (SQLException e) {
                    showError("Ошибка", "Не удалось удалить услугу: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onCompleteTraining() {
        ScheduleItem selectedItem = scheduleTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showError("Ошибка", "Пожалуйста, выберите тренировку");
            return;
        }
        
        try {
            String query = """
                UPDATE schedule 
                SET status = 'completed'
                WHERE trainer_id = (SELECT id FROM trainers WHERE name = ?)
                AND client_id = (SELECT id FROM clients WHERE name = ?)
                AND training_date = ?
                AND training_time = ?
            """;
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, selectedItem.getTrainerName());
            stmt.setString(2, selectedItem.getClientName());
            stmt.setDate(3, Date.valueOf(selectedItem.getDate()));
            stmt.setTime(4, Time.valueOf(selectedItem.getTime()));
            
            stmt.executeUpdate();
            loadSchedule();
            showInfo("Успех", "Тренировка отмечена как завершенная");
            
            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка", "Не удалось обновить статус тренировки: " + e.getMessage());
        }
    }

    @FXML
    private void onCancelTraining() {
        ScheduleItem selectedItem = scheduleTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showError("Ошибка", "Пожалуйста, выберите тренировку");
            return;
        }
        
        try {
            String query = """
                UPDATE schedule 
                SET status = 'cancelled'
                WHERE trainer_id = (SELECT id FROM trainers WHERE name = ?)
                AND client_id = (SELECT id FROM clients WHERE name = ?)
                AND training_date = ?
                AND training_time = ?
            """;
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, selectedItem.getTrainerName());
            stmt.setString(2, selectedItem.getClientName());
            stmt.setDate(3, Date.valueOf(selectedItem.getDate()));
            stmt.setTime(4, Time.valueOf(selectedItem.getTime()));
            
            stmt.executeUpdate();
            loadSchedule();
            showInfo("Успех", "Тренировка отменена");
            
            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка", "Не удалось отменить тренировку: " + e.getMessage());
        }
    }

    @FXML
    private void onAddSubscription() {
        String clientName = subscriptionClientComboBox.getValue();
        String serviceName = subscriptionServiceComboBox.getValue();
        LocalDate startDate = subscriptionStartDatePicker.getValue();

        if (clientName == null || serviceName == null || startDate == null) {
            showError("Ошибка", "Пожалуйста, заполните все поля");
            return;
        }

        try {
            String checkQuery = """
                SELECT s.name as service_name, sch.subscription_end_date
                FROM schedule sch
                JOIN services s ON sch.service_id = s.id
                JOIN clients c ON sch.client_id = c.id
                WHERE c.name = ? AND s.is_subscription = true 
                AND sch.status = 'active'
                AND sch.subscription_end_date >= CURRENT_DATE
            """;

            PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
            checkStmt.setString(1, clientName);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String existingService = rs.getString("service_name");
                LocalDate endDate = rs.getDate("subscription_end_date").toLocalDate();
                showError("Ошибка", 
                    String.format("У клиента уже есть активный абонемент '%s' до %s", 
                    existingService, endDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
                return;
            }

            LocalDate endDate = startDate.plusDays(30);

            String query = """
                INSERT INTO schedule (client_id, service_id, training_date, training_time, status,
                                   subscription_start_date, subscription_end_date)
                SELECT c.id, s.id, ?, ?, 'active', ?, ?
                FROM clients c, services s
                WHERE c.name = ? AND s.name = ?
            """;

            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setTime(2, Time.valueOf(LocalTime.of(0, 0))); // Устанавливаем полночь как время
            stmt.setDate(3, Date.valueOf(startDate));
            stmt.setDate(4, Date.valueOf(endDate));
            stmt.setString(5, clientName);
            stmt.setString(6, serviceName);

            stmt.executeUpdate();
            loadSubscriptions();
            showInfo("Успех", "Абонемент успешно добавлен");

            stmt.close();
        } catch (SQLException e) {
            showError("Ошибка добавления абонемента", e.getMessage());
        }
    }

    @FXML
    private void onTerminateSubscription() {
        SubscriptionItem selectedItem = subscriptionsTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showError("Ошибка", "Выберите абонемент для завершения");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Завершение абонемента");
        dialog.setHeaderText("Укажите причину досрочного завершения абонемента");
        dialog.setContentText("Причина:");

        dialog.showAndWait().ifPresent(reason -> {
            try {
                String updateQuery = """
                    UPDATE schedule 
                    SET status = 'terminated', 
                        subscription_end_date = ?, 
                        termination_reason = ?
                    WHERE client_id = (SELECT id FROM clients WHERE name = ?)
                    AND service_id = (SELECT id FROM services WHERE name = ?)
                    AND subscription_start_date = ?
                """;

                PreparedStatement stmt = connection.prepareStatement(updateQuery);
                stmt.setDate(1, Date.valueOf(LocalDate.now()));
                stmt.setString(2, reason);
                stmt.setString(3, selectedItem.getClientName());
                stmt.setString(4, selectedItem.getServiceName());
                stmt.setDate(5, Date.valueOf(selectedItem.getStartDate()));

                stmt.executeUpdate();
                loadSubscriptions();
                showInfo("Успех", "Абонемент успешно завершен");

                stmt.close();
            } catch (SQLException e) {
                showError("Ошибка завершения абонемента", e.getMessage());
            }
        });
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class Trainer {
        private final int id;
        private final String name;
        private final String specialization;
        private final String phone;
        private final String email;

        public Trainer(int id, String name, String specialization, String phone, String email) {
            this.id = id;
            this.name = name;
            this.specialization = specialization;
            this.phone = phone;
            this.email = email;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getSpecialization() { return specialization; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
    }

    public static class Client {
        private final int id;
        private final String name;
        private final String phone;
        private final String email;

        public Client(int id, String name, String phone, String email) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.email = email;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
    }

    public static class ScheduleItem {
        private final String trainerName;
        private final String clientName;
        private final String date;
        private final String time;
        private final String serviceName;
        private final String status;

        public ScheduleItem(String trainerName, String clientName, String date, String time, String serviceName, String status) {
            this.trainerName = trainerName;
            this.clientName = clientName;
            this.date = date;
            this.time = time;
            this.serviceName = serviceName;
            this.status = status;
        }

        public String getTrainerName() { return trainerName; }
        public String getClientName() { return clientName; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getServiceName() { return serviceName; }
        public String getStatus() { return status; }
    }

    public static class Service {
        private final int id;
        private final String name;
        private final String description;
        private final double price;
        private final boolean isGroup;
        private final boolean isActive;
        private final boolean isSubscription;

        public Service(int id, String name, String description, double price, boolean isGroup, boolean isActive, boolean isSubscription) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.isGroup = isGroup;
            this.isActive = isActive;
            this.isSubscription = isSubscription;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getPrice() { return price; }
        public boolean isGroup() { return isGroup; }
        public boolean isActive() { return isActive; }
        public boolean isSubscription() { return isSubscription; }
    }

    public static class SubscriptionItem {
        private final String clientName;
        private final String serviceName;
        private final String startDate;
        private final String endDate;
        private final String status;
        private final String terminationReason;

        public SubscriptionItem(String clientName, String serviceName, String startDate, 
                              String endDate, String status, String terminationReason) {
            this.clientName = clientName;
            this.serviceName = serviceName;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.terminationReason = terminationReason;
        }

        public String getClientName() { return clientName; }
        public String getServiceName() { return serviceName; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getStatus() { return status; }
        public String getTerminationReason() { return terminationReason; }
    }
}