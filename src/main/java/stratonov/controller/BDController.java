package stratonov.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.Callback;
import stratonov.bdclient.ClientPostgreSQL;
import stratonov.bdclient.JDBCClient;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by strat on 25.03.15.
 */
public class BDController implements Initializable {
    public SplitMenuButton smbTableBase;
    public TableView tableView;
    public Label lblLogin;
    private JDBCClient jdbcClient;
    private String selectedTable = "";
    private List<String> columnNames;

    BDController() {
    }

    BDController(String selectedTable) {
        this.selectedTable = selectedTable;
    }

    private void updateTable() {
        cliningTable();
        fillingTable();
        fillingColumnsTable();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jdbcClient = ClientPostgreSQL.getInstance();
        lblLogin.setText(jdbcClient.getLogin());
        List<String> nameTablesSet = jdbcClient.getTableNames();
        if (!selectedTable.isEmpty()) {
            updateTable();
        }
        if (nameTablesSet != null) {
            MenuItem menuItem;
            for (String iter : nameTablesSet) {
                menuItem = new MenuItem(iter);
                menuItem.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        selectedTable = ((MenuItem) event.getSource()).getText();
                        updateTable();
                    }
                });
                smbTableBase.getItems().addAll(menuItem);
            }
        } else {
            new Alert(Alert.AlertType.WARNING, "Таблицы не найдены!").showAndWait();
        }
    }

    private void cliningTable() {
        tableView.getColumns().clear();
        tableView.getItems().clear();
    }

    private void fillingTable() {
        ResultSet resultSet = jdbcClient.getTable(selectedTable);
        try {
            if (resultSet != null) {
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                columnNames = new ArrayList<>();
                for (int i = 0; i < resultSetMetaData.getColumnCount(); ++i)
                    columnNames.add(resultSetMetaData.getColumnName(i + 1));
                ObservableList<List<String>> data = FXCollections.observableArrayList();
                while (resultSet.next()) {
                    List<String> row = new ArrayList<>();
                    for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                        row.add(resultSet.getString(i));
                    }
                    data.add(row);
                }
                tableView.setItems(data);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fillingColumnsTable() {
        for (int i = 0; i < columnNames.size(); ++i) {
            TableColumn column = new TableColumn(columnNames.get(i));
            final int finalI = i;
            column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<List<String>, String>, ObservableValue<String>>() {
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<List<String>, String> data) {
                    return new ReadOnlyStringWrapper(data.getValue().get(finalI));
                }
            });
            column.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
                public void handle(TableColumn.CellEditEvent event) {
                    TablePosition tablePosition = event.getTablePosition();
                    String columnSearch = ((List) tableView.getItems().get(tablePosition.getRow())).get(0).toString();
                    String columnSearchName = ((TableColumn) tableView.getColumns().get(0)).getText();
                    if (!jdbcClient.updateTable(selectedTable, event.getTableColumn().getText(), event.getNewValue().toString(), columnSearchName, columnSearch)) {
                        new Alert(Alert.AlertType.WARNING, "Данные не изменены.").showAndWait();
                    }
                }
            });
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            tableView.getColumns().add(column);

        }
    }

    public void onActionDelete(ActionEvent actionEvent) {
        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
        if (selectedIndex != -1) {
            String columnSearch = ((List) tableView.getItems().get(selectedIndex)).get(0).toString();
            String columnSearchName = ((TableColumn) tableView.getColumns().get(0)).getText();
            tableView.getItems().remove(selectedIndex);
            jdbcClient.deleteRowTable(selectedTable, columnSearchName, columnSearch);
        }
    }

    public void onActionAdd(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/DialogAdd.fxml"));
            DialogAddController dialogAddController = new DialogAddController(columnNames, selectedTable);
            loader.setController(dialogAddController);
            Stage stage = new Stage();
            stage.setTitle("Новые данные");
            stage.setScene(new Scene(loader.load()));
            stage.show();
            Stage stageClose = (Stage) tableView.getScene().getWindow();
            stageClose.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
