/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import Controladores.Conexion;
import Modelo.Producto;
import Modelo.Proveedor;
import java.sql.Statement;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import javax.swing.JOptionPane;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

/**
 *
 * @author Juanm
 */
public class Interfaz_AProducto extends javax.swing.JFrame {

    Conexion con = new Conexion();
    Connection conexion = (Connection) con.conectar();
    PreparedStatement ps;
    ResultSet res;
    Interfaz_Almacen alm = new Interfaz_Almacen();
    
    public Interfaz_AProducto() {
        initComponents();
        
        AutoCompleteDecorator.decorate(boxProveedor);
        
        // Llenar JComboBox con proveedores existentes
        cargarProveedores();
    }
    
    
    
    private void cargarProveedores() {
        boxProveedor.removeAllItems(); // Limpiar el comboBox antes de llenarlo
    try {
        String query = "SELECT ID, Proveedor FROM proveedor";  // Obtener ID y Nombre
        PreparedStatement ps = conexion.prepareStatement(query);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String id = rs.getString("ID");
            String nombre = rs.getString("Proveedor");

            // Guardar ambos valores en el JComboBox con un formato personalizado
            boxProveedor.addItem(nombre);
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error al cargar proveedores: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
    
    private void LimpiarCampos(){
        txtPCodigo.setText("");
        txtCantidadProducto.setText("");
        txtDescripcionP.setText("");
        boxProveedor.setSelectedItem(null);
        txtPrecioP.setText("");
        txtIdProveedor.setText("");
    }

   

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        txtPCodigo = new javax.swing.JTextField();
        txtDescripcionP = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtCantidadProducto = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtPrecioP = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        btnSalirProducto = new javax.swing.JButton();
        btnAgregarProducto1 = new javax.swing.JButton();
        boxProveedor = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        txtIdProveedor = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(50, 101, 255));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel2.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("REF/CODIGO");
        jPanel2.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 80, -1, -1));

        txtPCodigo.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        txtPCodigo.setBorder(null);
        txtPCodigo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtPCodigoKeyPressed(evt);
            }
        });
        jPanel2.add(txtPCodigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 110, 130, 30));

        txtDescripcionP.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        txtDescripcionP.setBorder(null);
        jPanel2.add(txtDescripcionP, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 210, 460, 30));

        jLabel3.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("DESCRIPCION");
        jPanel2.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 180, -1, -1));

        txtCantidadProducto.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        txtCantidadProducto.setBorder(null);
        jPanel2.add(txtCantidadProducto, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 110, 130, 30));

        jLabel4.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("CANTIDAD");
        jPanel2.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 80, -1, -1));

        txtPrecioP.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        txtPrecioP.setBorder(null);
        jPanel2.add(txtPrecioP, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 310, 130, 30));

        jLabel5.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("PRECIO");
        jPanel2.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 280, -1, -1));

        jLabel6.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("PROVEEDOR");
        jPanel2.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 280, -1, -1));

        btnSalirProducto.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        btnSalirProducto.setText("SALIR");
        btnSalirProducto.setBorder(null);
        btnSalirProducto.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirProductoActionPerformed(evt);
            }
        });
        jPanel2.add(btnSalirProducto, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 380, 110, 40));

        btnAgregarProducto1.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        btnAgregarProducto1.setText("AGREGAR");
        btnAgregarProducto1.setBorder(null);
        btnAgregarProducto1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarProducto1ActionPerformed(evt);
            }
        });
        jPanel2.add(btnAgregarProducto1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 380, 110, 40));

        boxProveedor.setEditable(true);
        boxProveedor.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        boxProveedor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        boxProveedor.setBorder(null);
        boxProveedor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxProveedorActionPerformed(evt);
            }
        });
        jPanel2.add(boxProveedor, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 310, 240, 30));

        jLabel1.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("NUEVO PRODUCTO");
        jPanel2.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 10, -1, -1));

        txtIdProveedor.setEditable(false);
        txtIdProveedor.setBackground(new java.awt.Color(255, 255, 255));
        txtIdProveedor.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        txtIdProveedor.setBorder(null);
        jPanel2.add(txtIdProveedor, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 350, 60, 20));

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 540, 460));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 485, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAgregarProducto1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarProducto1ActionPerformed
String cod = txtPCodigo.getText().trim();
String can = txtCantidadProducto.getText();
String desc = txtDescripcionP.getText();
String provee = (String) boxProveedor.getSelectedItem(); // Nombre del proveedor
String prec = txtPrecioP.getText();

if (cod.isEmpty() || desc.isEmpty() || prec.isEmpty() || can.isEmpty() || provee.isEmpty()) {
    JOptionPane.showMessageDialog(null, "POR FAVOR INGRESE TODOS LOS CAMPOS NECESARIOS", "ERROR", JOptionPane.ERROR_MESSAGE);
    return;
}

try {
    int canInt = Integer.parseInt(can);
    double precDouble = Double.parseDouble(prec);
    int idProveedor = -1; // ID del proveedor

    // Verificar si el proveedor existe
    String queryProveedor = "SELECT ID FROM proveedor WHERE Proveedor = ?";
    PreparedStatement psProveedor = conexion.prepareStatement(queryProveedor);
    psProveedor.setString(1, provee);
    ResultSet rsProveedor = psProveedor.executeQuery();

    if (rsProveedor.next()) { 
        // Si el proveedor existe, obtenemos su ID
        idProveedor = rsProveedor.getInt("ID");
    } else { 
        // Si no existe, lo insertamos
        String insertProveedor = "INSERT INTO proveedor(Proveedor) VALUES (?)";
        PreparedStatement psInsertProveedor = conexion.prepareStatement(insertProveedor, Statement.RETURN_GENERATED_KEYS);
        psInsertProveedor.setString(1, provee);
        psInsertProveedor.executeUpdate();
        
        cargarProveedores();

        // Obtener el ID generado
        ResultSet rsGenKeys = psInsertProveedor.getGeneratedKeys();
        if (rsGenKeys.next()) {
            idProveedor = rsGenKeys.getInt(1);
        }
    } 
   

    // Mostrar el ID en el txtidProveedor
    txtIdProveedor.setText(String.valueOf(idProveedor));

    // Insertar el producto con el proveedor correcto
    String queryProducto = "INSERT INTO producto(Codigo, Descripcion, ID_Proveedor, Precio, Stock) VALUES (?, ?, ?, ?, ?)";
    PreparedStatement psProducto = conexion.prepareStatement(queryProducto);
    psProducto.setString(1, cod);
    psProducto.setString(2, desc);
    psProducto.setInt(3, idProveedor); // Aquí usamos el ID correcto
    psProducto.setDouble(4, precDouble);
    psProducto.setInt(5, canInt);
    int res = psProducto.executeUpdate();

    if (res > 0) {
        JOptionPane.showMessageDialog(null, "PRODUCTO GUARDADO");
        LimpiarCampos();
    } else {
        JOptionPane.showMessageDialog(null, "ERROR AL GUARDAR PRODUCTO", "ERROR", JOptionPane.ERROR_MESSAGE);
    }

} catch (SQLException e) {
    JOptionPane.showMessageDialog(null, "Error de base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
} catch (NumberFormatException e) {
    JOptionPane.showMessageDialog(null, "Por favor ingrese valores numéricos válidos para precio y cantidad.", "Error", JOptionPane.ERROR_MESSAGE);
}
 


    }//GEN-LAST:event_btnAgregarProducto1ActionPerformed

    private void boxProveedorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxProveedorActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_boxProveedorActionPerformed

    private void btnSalirProductoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirProductoActionPerformed
        this.dispose();
        alm.CargarInventario();
    }//GEN-LAST:event_btnSalirProductoActionPerformed

    private void txtPCodigoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPCodigoKeyPressed
    if (evt.getKeyCode() == KeyEvent.VK_ENTER) { // Detecta Enter
    }
    }//GEN-LAST:event_txtPCodigoKeyPressed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Interfaz_AProducto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Interfaz_AProducto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Interfaz_AProducto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Interfaz_AProducto.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Interfaz_AProducto().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> boxProveedor;
    private javax.swing.JButton btnAgregarProducto1;
    private javax.swing.JButton btnSalirProducto;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField txtCantidadProducto;
    private javax.swing.JTextField txtDescripcionP;
    private javax.swing.JTextField txtIdProveedor;
    private javax.swing.JTextField txtPCodigo;
    private javax.swing.JTextField txtPrecioP;
    // End of variables declaration//GEN-END:variables
}
