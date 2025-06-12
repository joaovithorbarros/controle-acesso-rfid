package com.joaovithor.view;
import com.fazecast.jSerialComm.SerialPort;
import com.joaovithor.exception.DataConflictException;
import com.joaovithor.model.Card;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDateTime;

import javafx.scene.control.TableColumn;

public class Main extends Application {

    private Label actualMode = new Label("Carregando...");
    private TextArea logArea = new TextArea();
    private TableView<Card> table = new TableView<>();
    private Card card;

    enum BUTTON_MODE{CADASTRAR, VALIDAR, EXCLUIR}
    static BUTTON_MODE currentMode = BUTTON_MODE.CADASTRAR;

    @SuppressWarnings("unchecked")
    @Override
    public void start(Stage primaryStage){
        logArea.setEditable(false);
        VBox root = new VBox(10, actualMode, logArea, table);
        Scene scene = new Scene(root, 500, 500);

        TableColumn<Card, String> uidColumn = new TableColumn<>("UID");
        uidColumn.setCellValueFactory(new PropertyValueFactory<>("UID"));

        TableColumn<Card, String> dateColumn = new TableColumn<>("Data");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        table.getColumns().addAll(uidColumn, dateColumn);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(200);
        root.setAlignment(Pos.TOP_CENTER);
        actualMode.setStyle("-fx-font-size: 20px; -fx-text-fill: black; -fx-font-weight: bold;");
        logArea.setStyle("-fx-font-size: 14px;");

        primaryStage.setTitle("Controle de Acesso - Interface");
        primaryStage.setScene(scene);
        primaryStage.show();

        startSerialCommunication();

    }
    private void startSerialCommunication() {
        SerialPort port = SerialPort.getCommPort("COM3"); 
        port.setBaudRate(9600);

        if (!port.openPort()) {
            appendLog("Erro ao abrir porta serial.");
            return;
        }

        Thread serialThread = new Thread(() ->{
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/controle_acesso", "postgres", "1234")) {

                while (true) {
                    if(port.bytesAvailable() > 0){
                        byte[] buffer = new byte[port.bytesAvailable()];
                        port.readBytes(buffer, buffer.length);
                        String serialInput = new String(buffer).trim(); 

                        Platform.runLater(() -> processInput(serialInput));
                        try {
                            table.setItems(loadDataBase(conn));
                        } catch (SQLException ERROR) {
                            appendLog("Erro ao atualizar tabela: " + ERROR.getMessage());
                        }                   
                        try{
                            loadTableData(conn);
                        } catch(SQLException ERROR){
                            ERROR.printStackTrace();
                        }
                        
                        if(serialInput.startsWith("UID: ")){
                            String uid = serialInput.replace("UID: ", "");
                            card = new Card(uid, LocalDateTime.now().toString());
                            if(currentMode == BUTTON_MODE.CADASTRAR){
                                try{
                                    insertUID(conn, card);
                                    appendLog("Cadastro bem sucedido!");
                                    updateTable();
                                } catch(DataConflictException ERROR){
                                    appendLog(ERROR.getMessage());
                                    port.writeBytes("BIP:NEGADO\n".getBytes(), "BIP:NEGADO\n".length());
                                }
                            } else if(currentMode == BUTTON_MODE.VALIDAR){
                                if(checkUID(conn, card)){
                                    appendLog("Acesso liberado para UID: " + card.getUID());
                                    updateTable();
                                    port.writeBytes("BIP:PERMITIDO\n".getBytes(), "BIP:PERMITIDO\n".length());
                                } else {
                                    appendLog("Acesso negado para UID: " + card.getUID());
                                    port.writeBytes("BIP:NEGADO\n".getBytes(), "BIP:NEGADO\n".length());
                                }
                            } else if(currentMode == BUTTON_MODE.EXCLUIR){
                                try {
                                    deleteUID(conn, card);
                                    updateTable();
                                    appendLog("Cartao deletado.");
                                    port.writeBytes("BIP:PERMITIDO\n".getBytes(), "BIP:PERMITIDO\n".length());
                                } catch (DataConflictException ERROR) {
                                    appendLog(ERROR.getMessage());
                                    port.writeBytes("BIP:DELETADO\n".getBytes(), "BIP:DELETADO\n".length());
                                }
                            }
                        }
                    }
                    try{ Thread.sleep(100);} catch(InterruptedException ignored){}
                }

            } catch (Exception ERROR) {
                ERROR.printStackTrace();
            } finally {
                port.closePort();
            }   
        });
        serialThread.setDaemon(true);
        serialThread.start();
    }

    private static void insertUID(Connection conn, Card card) throws SQLException {
        if(checkUID(conn, card)){throw new DataConflictException("Cartao ja cadastrado.");}
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO tags_rfid (uid) VALUES (?)");
        stmt.setString(1, card.getUID());
        stmt.executeUpdate();
        stmt.close();
    }

    private static Boolean checkUID(Connection conn, Card card) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM tags_rfid WHERE uid = ?");
        stmt.setString(1, card.getUID());
        ResultSet rs = stmt.executeQuery();
        boolean exists = rs.next() && rs.getInt(1) > 0;
        stmt.close();
        return exists;
    }

    private static void deleteUID(Connection conn, Card card) throws SQLException{
        if(!checkUID(conn, card)){throw new DataConflictException("Cartao nao existe no banco de dados.");}
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM tags_rfid WHERE uid = ? ");
        stmt.setString(1, card.getUID());
        stmt.executeUpdate();
        stmt.close();
    }

    private void processInput(String serialInput ){
        switch (serialInput) {
            case "MODO:CADASTRAR":
                currentMode = BUTTON_MODE.CADASTRAR;
                actualMode.setText("Modo atual: CADASTRAR");
                actualMode.setStyle("-fx-text-fill: green; -fx-font-size: 16px; -fx-font-weight: bold;");
                break;
            case "MODO:VALIDAR":
                currentMode = BUTTON_MODE.VALIDAR;
                actualMode.setText("Modo atual: VALIDAR");
                actualMode.setStyle("-fx-text-fill: blue; -fx-font-size: 16px; -fx-font-weight: bold;");
                break;
            case "MODO:DELETAR":
                currentMode = BUTTON_MODE.EXCLUIR;
                actualMode.setText("Modo atual: EXCLUIR");
                actualMode.setStyle("-fx-text-fill: red; -fx-font-size: 16px; -fx-font-weight: bold;");
                break;  
        }
    }

    private void appendLog(String message) {
        String currentText = logArea.getText();
        String[] lines = currentText.split("\n");

        if(lines.length >= 2){
            logArea.clear();
        }
        logArea.appendText(message + "\n");

    }

    private void loadTableData(Connection conn) throws SQLException{
        ObservableList<Card> data = FXCollections.observableArrayList();
        PreparedStatement stmt = conn.prepareStatement("SELECT uid, data_hora FROM tags_rfid");
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            data.add(new Card(rs.getString("uid"), rs.getString("data_hora")));
        }

        table.setItems(data);
        stmt.close();
    }

    private ObservableList<Card> loadDataBase(Connection conn) throws SQLException{
        ObservableList<Card> dados = FXCollections.observableArrayList();
        PreparedStatement stmt = conn.prepareStatement("SELECT uid, data_hora FROM tags_rfid ORDER BY id DESC");
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            dados.add(new Card(rs.getString("uid"), rs.getString("data_hora")));
        }
        rs.close();
        stmt.close();
        return dados;
    }

    private void updateTable() {
        new Thread(() -> {
            ObservableList<Card> data = FXCollections.observableArrayList();
            try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/controle_acesso", "postgres", "1234");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM tags_rfid ORDER BY id DESC")) {

                while (rs.next()) {
                    String uid = rs.getString("uid");
                    Timestamp timestamp = rs.getTimestamp("data_hora");
                    String date = timestamp != null ? timestamp.toString() : "N/A";
                    data.add(new Card(uid, date));
                }

                Platform.runLater(() -> table.setItems(data));
            } catch (SQLException e) {
                Platform.runLater(() -> appendLog("Erro ao atualizar tabela: " + e.getMessage()));
            }
        }).start();
    }


    public static void main(String args[]){
        launch(args);
    }
}
