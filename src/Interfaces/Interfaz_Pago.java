/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import Controladores.Conexion;
import Controladores.Ticket;
import java.util.List;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Juanm
 */
public class Interfaz_Pago extends javax.swing.JFrame {

    Conexion con = new Conexion();
    Connection conexion = (Connection) con.conectar();
    PreparedStatement ps;
    ResultSet res;
    private double total;
    private int idCliente;
    private String nombreCliente;
    private List<String> productos;

    public Interfaz_Pago(double total, int idCliente, String nombreCliente, List<String> productos) {
        initComponents();
        this.total = total;
        this.idCliente = idCliente;
        this.nombreCliente = nombreCliente;
        this.productos = productos;

        // Mostrar los datos en la interfaz
        txtTotalPagar.setText(String.format("$%.2f", total));

        // Mostrar los productos en un JTextArea o JTable
        for (String producto : productos) {
            System.out.println("Producto: " + producto);
        }

        cargarMetodosPago();
        txtTotalPagar.setText(String.format("Total: $%.2f", total));
        setDefaultCloseOperation(Interfaz_Pago.DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    public Interfaz_Pago() {
    }

    private void calcularCambio() {
        String textoPaga = txtPaga.getText().trim(); // Eliminar espacios en blanco
        String metodoPago = boxMetodoPago.getSelectedItem().toString(); // Obtener método de pago seleccionado

        // Si el método de pago NO es efectivo, no calcular el cambio
        if (!metodoPago.equalsIgnoreCase("Efectivo")) {
            txtCambio.setText("No aplica");
            return;
        }

        if (textoPaga.isEmpty() || !textoPaga.matches("\\d+")) {
            txtCambio.setText("Monto insuficiente");
            return;
        }

        try {
            double paga = Double.parseDouble(textoPaga);

            // Reemplazar "Total: $" y convertir formato de coma a punto (para decimales)
            String totalText = txtTotalPagar.getText().replace("Total: $", "").replace(",", ".").trim();
            double totalVenta = Double.parseDouble(totalText);

            double cambio = paga - totalVenta;

            if (cambio < 0) {
                txtCambio.setText("Monto insuficiente");
            } else {
                txtCambio.setText(String.format("$%.2f", cambio));
            }
        } catch (NumberFormatException e) {
            txtCambio.setText("Monto insuficiente");
        }
    }

    private HashMap<String, Integer> metodosPagoMap = new HashMap<>();

    private void cargarMetodosPago() {
        try {
            String sql = "SELECT ID, Pago FROM metodo_pago";
            PreparedStatement pst = conexion.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            boxMetodoPago.removeAllItems();
            metodosPagoMap.clear();

            while (rs.next()) {
                int idPago = rs.getInt("ID");
                String nombre = rs.getString("Pago");
                boxMetodoPago.addItem(nombre);
                metodosPagoMap.put(nombre, idPago); // Guardar la relación Nombre -> ID
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void finalizarCompra() {
        // Obtener el método de pago seleccionado
        String metodoPago = boxMetodoPago.getSelectedItem().toString();
        int idPago = metodosPagoMap.get(metodoPago);

        // Obtener el total de la venta
        double totalVenta = Double.parseDouble(txtTotalPagar.getText().replace("Total: $", "").replace(",", ".").trim());

        // Obtener el cambio (si aplica)
        double cambio = 0.0;
        if (metodoPago.equalsIgnoreCase("Efectivo")) {
            cambio = Double.parseDouble(txtCambio.getText().replace("$", "").replace(",", ".").trim());
        }

        // Registrar la venta en la tabla Venta
        int idVenta = registrarVenta(idCliente, idPago, totalVenta, cambio);

        if (idVenta > 0) {
            // Registrar los productos vendidos en la tabla Factura
            for (String producto : productos) {
                String[] partes = producto.split(" - ");
                String codigoProducto = partes[0];
                int cantidad = Integer.parseInt(partes[2].replace("Cant: ", ""));
                double precio = Double.parseDouble(partes[3].replace("$", ""));

                registrarFactura(idVenta, codigoProducto, precio, cantidad);
            }

            // Actualizar el stock de los productos (si es necesario)
            actualizarStock(productos);

            // Generar el recibo de la venta
            Ticket ticket = new Ticket();
            ticket.generarTicketPDF(idVenta);

            // Mostrar mensaje de éxito
            JOptionPane.showMessageDialog(null, "Venta registrada exitosamente. Recibo generado.");
            this.dispose(); // Cerrar la ventana de pago
        } else {
            JOptionPane.showMessageDialog(null, "Error al registrar la venta.");
        }
    }

    private int registrarVenta(int idCliente, int idPago, double total, double cambio) {
        int idVenta = -1;
        try {
            String sql = "INSERT INTO Venta (ID_Cliente, ID_Pago, Total, Cambio) VALUES (?, ?, ?, ?)";
            PreparedStatement pst = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pst.setInt(1, idCliente);
            pst.setInt(2, idPago);
            pst.setDouble(3, total);
            pst.setDouble(4, cambio);
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                idVenta = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idVenta;
    }

    private void registrarFactura(int idVenta, String codigoProducto, double precio, int cantidad) {
        try {
            String sql = "INSERT INTO Factura (ID_Venta, ID_Producto, Precio, Cantidad) VALUES (?, ?, ?, ?)";
            PreparedStatement pst = conexion.prepareStatement(sql);
            pst.setInt(1, idVenta);
            pst.setString(2, codigoProducto);
            pst.setDouble(3, precio);
            pst.setInt(4, cantidad);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void actualizarStock(List<String> productos) {
        try {
            for (String producto : productos) {
                String[] partes = producto.split(" - ");
                String codigoProducto = partes[0];
                int cantidad = Integer.parseInt(partes[2].replace("Cant: ", ""));

                String sql = "UPDATE Producto SET Stock = Stock - ? WHERE Codigo = ?";
                PreparedStatement pst = conexion.prepareStatement(sql);
                pst.setInt(1, cantidad);
                pst.setString(2, codigoProducto);
                pst.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int obtenerIdCliente(String nombreCliente) {
        int idCliente = -1;
        try {
            String sql = "SELECT ID FROM Cliente WHERE Nombre = ?";
            PreparedStatement pst = conexion.prepareStatement(sql);
            pst.setString(1, nombreCliente);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                idCliente = rs.getInt("ID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return idCliente;
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
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        boxMetodoPago = new javax.swing.JComboBox<>();
        txtCambio = new javax.swing.JTextField();
        txtTotalPagar = new javax.swing.JTextField();
        txtPaga = new javax.swing.JTextField();
        btnFinalizar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(204, 204, 204));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel1.setText("METODO DE PAGO");
        jPanel2.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 180, -1, -1));

        jLabel2.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel2.setText("PAGO");
        jPanel2.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 10, -1, -1));

        jLabel3.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel3.setText("TOTAL A PAGAR:");
        jPanel2.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 60, -1, -1));

        jLabel4.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel4.setText("CUANTO PAGA:");
        jPanel2.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, -1, -1));

        jLabel5.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel5.setText("CAMBIO:");
        jPanel2.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 140, -1, -1));

        boxMetodoPago.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        boxMetodoPago.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        boxMetodoPago.setBorder(null);
        jPanel2.add(boxMetodoPago, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 210, 130, -1));

        txtCambio.setEditable(false);
        txtCambio.setBackground(new java.awt.Color(204, 204, 204));
        txtCambio.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        txtCambio.setBorder(null);
        jPanel2.add(txtCambio, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 140, 140, 30));

        txtTotalPagar.setEditable(false);
        txtTotalPagar.setBackground(new java.awt.Color(204, 204, 204));
        txtTotalPagar.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        txtTotalPagar.setBorder(null);
        txtTotalPagar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTotalPagarActionPerformed(evt);
            }
        });
        jPanel2.add(txtTotalPagar, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 62, 140, 30));

        txtPaga.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        txtPaga.setBorder(null);
        txtPaga.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtPagaKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtPagaKeyReleased(evt);
            }
        });
        jPanel2.add(txtPaga, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 100, 140, 30));

        btnFinalizar.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        btnFinalizar.setText("FINALIZAR");
        btnFinalizar.setBorder(null);
        btnFinalizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFinalizarActionPerformed(evt);
            }
        });
        jPanel2.add(btnFinalizar, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 230, 100, 40));

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 380, 280));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtTotalPagarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTotalPagarActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTotalPagarActionPerformed

    private void txtPagaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPagaKeyReleased
        SwingUtilities.invokeLater(() -> calcularCambio());
    }//GEN-LAST:event_txtPagaKeyReleased

    private void txtPagaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPagaKeyPressed

    }//GEN-LAST:event_txtPagaKeyPressed

    private void btnFinalizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFinalizarActionPerformed
        finalizarCompra();
    }//GEN-LAST:event_btnFinalizarActionPerformed

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
            java.util.logging.Logger.getLogger(Interfaz_Pago.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Interfaz_Pago.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Interfaz_Pago.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Interfaz_Pago.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Interfaz_Pago().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> boxMetodoPago;
    private javax.swing.JButton btnFinalizar;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField txtCambio;
    private javax.swing.JTextField txtPaga;
    private javax.swing.JTextField txtTotalPagar;
    // End of variables declaration//GEN-END:variables
}
