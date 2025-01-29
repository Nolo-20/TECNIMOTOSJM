/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author Juanm
 */
public class Producto {
    private String Codigo;
    private String Descripcion;
    private double Precio;
    private int Stock;

    public Producto(String Codigo, String Descripcion, double Precio, int Stock) {
        this.Codigo = Codigo;
        this.Descripcion = Descripcion;
        this.Precio = Precio;
        this.Stock = Stock;
    }
    
    public Producto(){
        
    }

    public String getCodigo() {
        return Codigo;
    }

    public void setCodigo(String Codigo) {
        this.Codigo = Codigo;
    }

    public String getDescripcion() {
        return Descripcion;
    }

    public void setDescripcion(String Descripcion) {
        this.Descripcion = Descripcion;
    }

    public double getPrecio() {
        return Precio;
    }

    public void setPrecio(double Precio) {
        this.Precio = Precio;
    }

    public int getStock() {
        return Stock;
    }

    public void setStock(int Stock) {
        this.Stock = Stock;
    }
    
    
}
