/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import Interfaces.Interfaz_Almacen;
import Interfaces.Interfaz_Main;


public class Main {
    
    public static void main(String [] args){
        Interfaz_Almacen open = new Interfaz_Almacen();
        open.setVisible(true);
        open.setLocationRelativeTo(null);
    } 
}
