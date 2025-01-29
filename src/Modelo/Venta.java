/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;

/**
 *
 * @author Juanm
 */
public class Venta {
    private String Codigo;
    private String Fecha;
    private double Total;

    public Venta(String Codigo, String Fecha, double Total) {
        this.Codigo = Codigo;
        this.Fecha = Fecha;
        this.Total = Total;
    }
    
    public Venta(){
        
    }

    public String getCodigo() {
        return Codigo;
    }

    public void setCodigo(String Codigo) {
        this.Codigo = Codigo;
    }

    public String getFecha() {
        return Fecha;
    }

    public void setFecha(String Fecha) {
        this.Fecha = Fecha;
    }

    public double getTotal() {
        return Total;
    }

    public void setTotal(double Total) {
        this.Total = Total;
    }
    
    
}
