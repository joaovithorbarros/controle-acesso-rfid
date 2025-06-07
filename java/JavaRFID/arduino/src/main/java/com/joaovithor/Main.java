package com.joaovithor;
import com.fazecast.jSerialComm.SerialPort;
import java.sql.*;
import java.time.LocalDateTime;

public class Main {
    enum BUTTON_MODE{CADASTRAR, VALIDAR, EXCLUIR}
    static BUTTON_MODE currentMode = BUTTON_MODE.CADASTRAR;
    public static void main(String[] args) {
        Card card;
        SerialPort port = SerialPort.getCommPort("COM4"); 
        port.setBaudRate(9600);

        if (!port.openPort()) {
            System.out.println("Erro ao abrir porta.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/controle_acesso", "postgres", "1234")) {

            StringBuilder linha = new StringBuilder();

            while (true) {
                if(port.bytesAvailable() > 0){
                    byte[] buffer = new byte[port.bytesAvailable()];
                    port.readBytes(buffer, buffer.length);
                    String serialInput = new String(buffer).trim(); 

                    if(serialInput.equals("MODO:CADASTRAR")){
                        currentMode = BUTTON_MODE.CADASTRAR;
                        System.out.println("-CADASTRAR-");
                    } else if(serialInput.equals("MODO:VALIDAR")){
                        currentMode = BUTTON_MODE.VALIDAR;
                        System.out.println("-VALIDAR-");
                    } else if(serialInput.equals("MODO:DELETAR")){
                        currentMode = BUTTON_MODE.EXCLUIR;
                        System.out.println("-DELETAR-");
                    } else if(serialInput.startsWith("UID: ")){
                        String uid = serialInput.replace("UID: ", "").trim();
                        card = new Card(uid, LocalDateTime.now().toString());
                        if(currentMode == BUTTON_MODE.CADASTRAR){
                            insertUID(conn, card);
                        } else if(currentMode == BUTTON_MODE.VALIDAR){
                            if(checkUID(conn, card)){
                                System.out.println("Acesso liberado para UID: " + card.getUID());
                                port.writeBytes("BIP:PERMITIDO\n".getBytes(), "BIP:PERMITIDO\n".length());
                            } else {
                                System.out.println("Acesso negado para UID: " + card.getUID());
                                port.writeBytes("BIP:NEGADO\n".getBytes(), "BIP:NEGADO\n".length());
                            }
                        } else if(currentMode == BUTTON_MODE.EXCLUIR){
                            deleteUID(conn, card);
                        }
                    }
                }
            }

        } catch (Exception ERROR) {
            ERROR.printStackTrace();
        } finally {
            port.closePort();
        }
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
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM tags_rfid WHERE uid = ? ");
        stmt.setString(1, card.getUID());
        stmt.executeUpdate();
        stmt.close();
    }
}
