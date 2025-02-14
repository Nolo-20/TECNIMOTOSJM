/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author Juanm
 */
public class Proveedor {

    private int id;
    private String nombre;

    public Proveedor(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
    
    public Proveedor(){
        
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;  // Esto hace que en el JComboBox solo se vea el nombre
    }
}
