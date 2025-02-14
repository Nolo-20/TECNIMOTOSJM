/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 *
 * @author Juanm
 */
public class Conexion {

    Connection conec = null;
    private static final String url = "jdbc:mysql://127.0.0.1/tecnimotosjm";
    private static final String usuario = "root";
    private static final String clave = "";

    public Connection conectar(){
        try {
            conec = DriverManager.getConnection(url, usuario, clave);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        } 
        return conec;

    }
}
