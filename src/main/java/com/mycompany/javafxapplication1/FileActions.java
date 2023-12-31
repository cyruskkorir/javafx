package com.mycompany.javafxapplication1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import javafx.event.ActionEvent;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class FileActions{
    static File file = new File("folder");
    static String selectedFileName = "";
    static String selectedUser = "";
    static String errorMessage = "";
    DB db = new DB();

    public  void start(Stage stage, String userName) throws SQLException {
        Connection connection = DriverManager.getConnection(db.getFileName());
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT name FROM files");
        VBox vBox = new VBox();
        vBox.setSpacing(8);
        vBox.setPadding(new Insets(20, 10, 20, 10));
        Button createButton = new Button("CREATE FILE");
        Button updateButton = new Button("UPDATE FILE");
        Button deleteButton = new Button("DELETE FILE");
        Button readButton = new Button("READ FILE");
        Button grantButton = new Button("GRANT PERMISSION");
        TextField textField = new TextField();
        MenuButton menuButton = new MenuButton("Choose File");
        MenuButton chooseUser = new MenuButton("Choose User");

        while (rs.next()) {
            String name = rs.getString("name");
            MenuItem menuItem = new MenuItem(name);
            menuButton.getItems().add(menuItem);
            menuItem.setOnAction(e->selectedFileName=name);
        }
        rs.close();
        connection.close();
        statement.close();


        Connection conn = db.connection;
        Statement stmt = conn.createStatement();
        ResultSet resultSet = stmt.executeQuery("SELECT name FROM users");
        while (resultSet.next()) {
            String name = resultSet.getString("name");
            MenuItem menuItem = new MenuItem(name);
            chooseUser.getItems().add(menuItem);
            menuItem.setOnAction(e->selectedUser=name);
        }
        resultSet.close();
        stmt.close();
        conn.close();

        createButton.setOnAction(e->createFile(stage, userName, textField.getText()));
        updateButton.setOnAction((ActionEvent e)->{
            try {
                updateFile(stage, textField.getText(), userName);
            } catch (Exception e1) {
            }
        });
        deleteButton.setOnAction(e->deleteFile(stage, selectedFileName));
        readButton.setOnAction(e->{
            try {
                readFile(stage, selectedFileName);
            } catch (Exception e1) {
            }
        });
        grantButton.setOnAction(e->{

            if(selectedFileName.isEmpty() || selectedUser.isEmpty()){
                errorMessage  = "Select user and file";
            }else{
                try {
                    Connection con = DriverManager.getConnection(db.getFileName());
                    Statement st = con.createStatement();
                    boolean succeed = st.execute(String.format("INSERT INTO fileOwners(userName, name) VALUES ('%s','%s')", selectedUser, selectedFileName));
                    if(succeed){
                        errorMessage = "Permission Granted";
                    }
                    st.close();
                    con.close();

                } catch (SQLException sqlerr) {
                    sqlerr.printStackTrace();
                }
                displayMesage(errorMessage);
                
            }
        });
        vBox.getChildren().addAll(
            textField, 
        createButton, menuButton, 
        updateButton, deleteButton, 
        readButton, chooseUser,
        grantButton);
        statement.close();
        resultSet.close();
        connection.close();
        Scene scene = new Scene(vBox, 400, 600);
        stage.setScene(scene);
        stage.show();
    }

    private void readFile(Stage stage, String name){
        if(name.isEmpty()){return;}
        try {
            SecretKey secretKey = FileEncryption.generateKey();
        byte[] bytes = new byte[16];
        IvParameterSpec ivParameterSpec;
        StringBuilder input;
            try (Connection conn = DriverManager.getConnection(db.getFileName()); Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(String.format("SELECT a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p FROM files WHERE name='%s'", name));
                int i=0;
                while (i<16) {
                    bytes[i]=(byte) rs.getInt(i+1);
                    i++;
                }       ivParameterSpec = new IvParameterSpec(bytes);
                BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(file, selectedFileName)));
                input = new StringBuilder();
                String line;
                while ((line=bufferedReader.readLine())!=null) {
                    input.append(line);
                }       rs.close();
            }
        String output = FileEncryption.decrypt(input.toString(), secretKey, ivParameterSpec);
        displayMesage(output);
        } catch (Exception e) {
            e.printStackTrace();
            displayMesage(e.getMessage());
        }

    }

    private void deleteFile(Stage stage, String fileName) {
        if(selectedFileName.isEmpty()){
            errorMessage = "No File Selected";
        }else{
            String sql = "DELETE FROM files WHERE name=?";
        try (Connection conn = DriverManager.getConnection(db.getFileName());
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fileName);
                pstmt.executeUpdate();
                pstmt.close();
                if (new File(file, selectedFileName).delete()) {
                errorMessage = "Success";
                }
        } catch (SQLException e) {
            e.printStackTrace();
           System.out.println(e.getMessage());
        }
            
        }
        displayMesage(errorMessage);
    }


    private void updateFile(Stage stage, String input, String user) throws Exception {
        if (selectedFileName.isEmpty()) {
            displayMesage("No File selected");
        }
        try {
            Connection connection = DriverManager.getConnection(db.getFileName());
            Statement statement = connection.createStatement();
            String sql = String.format("SELECT EXISTS(SELECT userName FROM fileOwners WHERE userName='%s')", user);
            ResultSet resultSet = statement.executeQuery(sql);
            if(resultSet.getInt(1)==0){displayMesage("No Permissions"); return;}
            File out = new File(file, selectedFileName);
            SecretKey secretKey = FileEncryption.generateKey();
            byte[] bytes = new byte[16];
            String query = String.format("SELECT a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p FROM files WHERE name='%s'", selectedFileName);
            resultSet.close();
            statement.close();
            connection.close();
            Connection conn = DriverManager.getConnection(db.getFileName());
            Statement stmt = conn.createStatement();
            ResultSet rSet = stmt.executeQuery(query);
            for(int i=0; i<16; i++){
                bytes[i]=(byte) rSet.getInt(i+1);
            }
            
            IvParameterSpec ivParameterSpec = new IvParameterSpec(bytes);
            String encryptedString = FileEncryption.encrypt(input, secretKey, ivParameterSpec);
        if(out.exists()){
            copy(encryptedString, out);

        }
        errorMessage="Success";
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage=e.getMessage();
            
        }
        displayMesage(errorMessage);

    }

    private void createFile(Stage stage, String user, String input) {
        String fileName = String.valueOf(Instant.now().toEpochMilli());
        File newFile = new File(file, fileName);
        try {
                if (newFile.createNewFile()) {
                    SecretKey secretKey = FileEncryption.generateKey();
                    IvParameterSpec ivParameterSpec = FileEncryption.generateIv();
                    insertFile(fileName, ivParameterSpec.getIV(), user);
                    String cipherText = FileEncryption.encrypt(input, secretKey, ivParameterSpec);
                    copy(cipherText, newFile);
                    displayMesage("Success");    
                }
        } catch (Exception e) {
            displayMesage(e.getMessage());
        }
    }

    private static void copy(String string, File file) {
            try {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(string+"\n");
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void insertFile(String name, byte[] bytes, String userName) {
        String sql = "INSERT INTO files (name,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String sq = "INSERT INTO fileOwners (userName, name) VALUES (?,?)";
        try (Connection conn = DriverManager.getConnection(db.getFileName());
                PreparedStatement pstmt = conn.prepareStatement(sql);
                PreparedStatement ps = conn.prepareStatement(sq);
                ) {
                pstmt.setString(1, name);
                ps.setString(1, userName);
                ps.setString(2, name);
                int i = 2;
                int j = 0;
                while (i<18) {
                    pstmt.setInt(i, bytes[j]);
                    i++;
                    j++;
                }
                pstmt.executeUpdate();
                ps.executeUpdate();
                conn.close();
                pstmt.close();
                ps.close();
        } catch (SQLException e) {
           e.printStackTrace();
        }
    }

    private static void displayMesage(String output) {
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UTILITY);
        dialog.centerOnScreen();
        Scene scene = new Scene(new Group(new Text(100, 50, output)));
        dialog.setScene(scene);
        dialog.show();
    }
    
}
