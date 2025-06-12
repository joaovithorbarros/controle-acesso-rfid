package com.joaovithor.view;
import com.fazecast.jSerialComm.SerialPort;
import com.joaovithor.exception.DataConflictException;
import com.joaovithor.model.Card;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDateTime;

public class Main extends Application {

    private Label actualMode = new Label("Modo atual: ---");
    private TextArea logArea = new TextArea();
    private Card card;

    enum BUTTON_MODE{CADASTRAR, VALIDAR, EXCLUIR}
    static BUTTON_MODE currentMode = BUTTON_MODE.CADASTRAR;

    @Override
    public void start(Stage primaryStage){
        logArea.setEditable(false);
        VBox root = new VBox(10, actualMode, logArea);
        Scene scene = new Scene(root, 500, 500);

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
                        
                        if(serialInput.startsWith("UID: ")){
                            String uid = serialInput.replace("UID: ", "");
                            card = new Card(uid, LocalDateTime.now().toString());
                            if(currentMode == BUTTON_MODE.CADASTRAR){
                                insertUID(conn, card);
                            } else if(currentMode == BUTTON_MODE.VALIDAR){
                                if(checkUID(conn, card)){
                                    appendLog("Acesso liberado para UID: " + card.getUID());
                                    port.writeBytes("BIP:PERMITIDO\n".getBytes(), "BIP:PERMITIDO\n".length());
                                } else {
                                    appendLog("Acesso negado para UID: " + card.getUID());
                                    port.writeBytes("BIP:NEGADO\n".getBytes(), "BIP:NEGADO\n".length());
                                }
                            } else if(currentMode == BUTTON_MODE.EXCLUIR){
                                try {
                                    deleteUID(conn, card);
                                    appendLog("Cartao deletado.");
                                    port.writeBytes("BIP:PERMITIDO\n".getBytes(), "BIP:PERMITIDO\n".length());
                                } catch (DataConflictException ERROR) {
                                    appendLog("ERROR MESSAGE: " + ERROR.getMessage());
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
        if(checkUID(conn, card)){
            System.out.println("UID já cadastrado: " + card.getUID());
            return;
        }
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO tags_rfid (uid) VALUES (?)");
        stmt.setString(1, card.getUID());
        stmt.executeUpdate();
        stmt.close();
        System.out.println("UID inserido: " + card.getUID());
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
        if(!checkUID(conn, card)){throw new DataConflictException("Cartao nao existe no banco de dados");}
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
                break;
            case "MODO:VALIDAR":
                currentMode = BUTTON_MODE.VALIDAR;
                actualMode.setText("Modo atual: VALIDAR");
                break;
            case "MODO:DELETAR":
                currentMode = BUTTON_MODE.EXCLUIR;
                actualMode.setText("Modo atual: EXCLUIR");
                break;  
        }
    }

    private void appendLog(String message) {
    logArea.appendText(message + "\n");
    }

    public static void main(String args[]){
        launch(args);
    }
}
