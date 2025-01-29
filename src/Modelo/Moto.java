/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;


public class Moto {
    private String Placa;
    private String Marca;
    private String Linea;
    private String Modelo;
    private String Kilometraje;

    public Moto(String Placa, String Marca, String Linea, String Modelo, String Kilometraje) {
        this.Placa = Placa;
        this.Marca = Marca;
        this.Linea = Linea;
        this.Modelo = Modelo;
        this.Kilometraje = Kilometraje;
    }

   public Moto(){
       
   }

    public String getPlaca() {
        return Placa;
    }

    public void setPlaca(String Placa) {
        this.Placa = Placa;
    }

    public String getMarca() {
        return Marca;
    }

    public void setMarca(String Marca) {
        this.Marca = Marca;
    }

    public String getLinea() {
        return Linea;
    }

    public void setLinea(String Linea) {
        this.Linea = Linea;
    }

    public String getModelo() {
        return Modelo;
    }

    public void setModelo(String Modelo) {
        this.Modelo = Modelo;
    }

    public String getKilometraje() {
        return Kilometraje;
    }

    public void setKilometraje(String Kilometraje) {
        this.Kilometraje = Kilometraje;
    }
   
   
    
    
}
