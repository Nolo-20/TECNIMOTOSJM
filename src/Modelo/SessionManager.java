/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author Juanm
 */
public class SessionManager {
    private static String nombreUsuarioActual;
    private static String rolUsuarioActual;

    // Guardar el nombre del usuario al iniciar sesión
    public static void setNombreUsuarioActual(String nombreUsuario) {
        nombreUsuarioActual = nombreUsuario;
    }

    // Obtener el nombre del usuario actual
    public static String getNombreUsuarioActual() {
        return nombreUsuarioActual;
    }

    // Guardar el rol del usuario al iniciar sesión
    public static void setRolUsuarioActual(String rolUsuario) {
        rolUsuarioActual = rolUsuario;
    }

    // Obtener el rol del usuario actual
    public static String getRolUsuarioActual() {
        return rolUsuarioActual;
    }
}


