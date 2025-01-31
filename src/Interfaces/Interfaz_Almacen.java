/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import Controladores.Conexion;
import Interfaces.Interfaz_AProducto;
import java.awt.event.KeyEvent;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author Juanm
 */
public class Interfaz_Almacen extends javax.swing.JFrame {

    Conexion con = new Conexion();
    Connection conexion = (Connection) con.conectar();
    PreparedStatement ps;
    ResultSet res;
    private TableRowSorter<DefaultTableModel> sorter;
    private Timer timer;  // Temporizador para esperar antes de ejecutar la consulta
    
    public Interfaz_Almacen() {
        initComponents();
        //CargarInventario();
    }
    
    private void LimpiarDespuesAgregarProducto(){
        txtrefcodigo.setText("");
        txtCantidad.setText("");
        txtStockP.setText("");
        txtDescripcionP.setText("");
        txtPrecioP.setText("");
    }
    
    public void CargarInventario(){
    DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return true; // Permitir edición en todas las celdas
        }
    };

    modelo.setColumnIdentifiers(new String[]{"Código", "Descripción", "Precio", "Stock", "Proveedor"});
    TablaInventario.setModel(modelo);
    
    // Inicializar sorter antes de usarlo en el DocumentListener
    sorter = new TableRowSorter<>(modelo);
    TablaInventario.setRowSorter(sorter); // Asignarlo a la tabla

    try {
        String query = "SELECT p.Codigo, p.Descripcion, p.Precio, p.Stock, pr.Proveedor " +
                       "FROM producto p INNER JOIN proveedor pr ON p.ID_Proveedor = pr.ID";
        PreparedStatement ps = conexion.prepareStatement(query);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String codigo = rs.getString("Codigo");
            String descripcion = rs.getString("Descripcion");
            double precio = rs.getDouble("Precio");
            int stock = rs.getInt("Stock");
            String proveedor = rs.getString("Proveedor");

            // Agregar fila al modelo
            modelo.addRow(new Object[]{codigo, descripcion, precio, stock, proveedor});
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error al cargar inventario: " + e.getMessage());
    }
    
     modelo.addTableModelListener(new TableModelListener() {
        @Override
        public void tableChanged(TableModelEvent e) {
            if (e.getType() == TableModelEvent.UPDATE) {
                int fila = e.getFirstRow(); // Fila editada
                int columna = e.getColumn(); // Columna editada

                // Obtener valores de la fila editada
                String codigo = modelo.getValueAt(fila, 0).toString();
                String descripcion = modelo.getValueAt(fila, 1).toString();
                double precio = Double.parseDouble(modelo.getValueAt(fila, 2).toString());
                int stock = Integer.parseInt(modelo.getValueAt(fila, 3).toString());
                String proveedor = modelo.getValueAt(fila, 4).toString();

                // Actualizar la base de datos
                actualizarProducto(codigo, descripcion, precio, stock, proveedor);
            }
        }
    });
     
         // Filtrar en tiempo real mientras el usuario escribe en txtBuscar
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
        public void insertUpdate(DocumentEvent e) { filtrar(); }
        public void removeUpdate(DocumentEvent e) { filtrar(); }
        public void changedUpdate(DocumentEvent e) { filtrar(); }


        private void filtrar() {
            String texto = txtBuscar.getText().trim();
            if (texto.isEmpty()) {
                sorter.setRowFilter(null); // Mostrar todos si está vacío
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 0, 1)); // Filtrar por Código o Descripción
            }
        }
    });
    }
    
    private void cargarDatosProducto() {
    String codigo = txtrefcodigo.getText();
    
    if (!codigo.isEmpty()) {
        try {
            String sql = "SELECT Descripcion, Precio, Stock FROM producto WHERE Codigo = ?";
            PreparedStatement pst = conexion.prepareStatement(sql);
            pst.setString(1, codigo);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                txtDescripcionP.setText(rs.getString("Descripcion"));
                txtPrecioP.setText(rs.getString("Precio"));
                txtStockP.setText(rs.getString("Stock"));
            } else {
                JOptionPane.showMessageDialog(null, "Producto no encontrado");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
    
    private void validarCantidad() {
    int stock = Integer.parseInt(txtStockP.getText());
    int cantidad = Integer.parseInt(txtCantidad.getText());

    if (cantidad > stock) {
        JOptionPane.showMessageDialog(null, "Cantidad excede el stock disponible");
        txtCantidad.setText("");
    }else if(cantidad < stock){
        JOptionPane.showMessageDialog(null, "Cantidad negativa el stock disponible");
        txtCantidad.setText("");
    }
}
    
private void agregarProductoATabla() {
    DefaultTableModel model = (DefaultTableModel) TablaVenta.getModel();
    String codigo = txtrefcodigo.getText();

    // Validar que los campos no estén vacíos
    if (codigo.isEmpty() || txtDescripcionP.getText().isEmpty() ||
        txtCantidad.getText().isEmpty() || txtPrecioP.getText().isEmpty()) {
        JOptionPane.showMessageDialog(null, "Por favor, complete todos los campos antes de agregar.");
        return;
    }

    try {
        int cantidadIngresada = Integer.parseInt(txtCantidad.getText());
        double precio = Double.parseDouble(txtPrecioP.getText());
        int stockDisponible = Integer.parseInt(txtStockP.getText());

        // Buscar si el producto ya está en la tabla
        boolean existe = false;
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).toString().equals(codigo)) { // Comparar códigos
                int cantidadActual = (int) model.getValueAt(i, 2); // Cantidad en la tabla
                int nuevaCantidad = cantidadActual + cantidadIngresada;

                // Verificar si la nueva cantidad supera el stock disponible
                if (nuevaCantidad > stockDisponible) {
                    JOptionPane.showMessageDialog(null, "No hay suficiente stock disponible.");
                    return;
                }

                // Actualizar la cantidad y el subtotal
                model.setValueAt(nuevaCantidad, i, 2);
                model.setValueAt(nuevaCantidad * precio, i, 4);
                existe = true;
                break;
            }
        }

        // Si el producto no existe, agregar una nueva fila
        if (!existe) {
            if (cantidadIngresada > stockDisponible) {
                JOptionPane.showMessageDialog(null, "Cantidad ingresada supera el stock disponible.");
                return;
            }

            model.addRow(new Object[]{
                codigo,
                txtDescripcionP.getText(),
                cantidadIngresada,
                precio,
                cantidadIngresada * precio
            });
        }

        calcularTotal(); // Actualiza el total después de agregar el producto
        LimpiarDespuesAgregarProducto();

    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(null, "Ingrese valores numéricos válidos en cantidad y precio.");
    }
}
    
private void calcularTotal() {
    double total = 0;
    DefaultTableModel model = (DefaultTableModel) TablaVenta.getModel();

    for (int i = 0; i < model.getRowCount(); i++) {
        total += (double) model.getValueAt(i, 4); // Sumar la columna "Subtotal"
    }

    txtTotalV.setText(String.valueOf(total));
}

    private void aplicarDescuento() {
    double total = Double.parseDouble(txtTotalV.getText());
    if (!txtDescuento.getText().isEmpty()) {
        double descuento = Double.parseDouble(txtDescuento.getText());
        total -= total * (descuento / 100);
    }
    txtTotalV.setText(String.format("%.2f", total));
}

    private void asignarCliente() {
    try {
        // Consulta SQL para obtener los clientes registrados
        String sql = "SELECT ID, Nombre FROM cliente";
        PreparedStatement pst = conexion.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();

        // Limpiar el JComboBox antes de agregar elementos
        boxCliente.removeAllItems();

        // Agregar opción por defecto
        boxCliente.addItem("Consumidor Final");

        // Agregar los clientes desde la base de datos
        while (rs.next()) {
            String cliente = rs.getInt("ID_Cliente") + " - " + rs.getString("Nombre");
            boxCliente.addItem(cliente);
        }

        // Establecer la opción por defecto
        boxCliente.setSelectedIndex(0);

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error al cargar clientes: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    private void actualizarStock() {
    DefaultTableModel model = (DefaultTableModel) TablaVenta.getModel();
    for (int i = 0; i < model.getRowCount(); i++) {
        String codigo = model.getValueAt(i, 0).toString();
        int cantidadVendida = Integer.parseInt(model.getValueAt(i, 2).toString());

        try {
            String sql = "UPDATE producto SET Stock = Stock - ? WHERE Codigo = ?";
            PreparedStatement pst = conexion.prepareStatement(sql);
            pst.setInt(1, cantidadVendida);
            pst.setString(2, codigo);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
    
    private void registrarVenta() {
        int idcliente = boxCliente.setSelectedItem(ABORT)
    try {
        String sql = "INSERT INTO venta (ID_Cliente, Fecha_Venta, ID_Pago, Total) VALUES (?, NOW(), ?, ?)";
        PreparedStatement pst = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, obtenerIDCliente());
        pst.setInt(2, obtenerIDMetodoPago());
        pst.setDouble(3, Double.parseDouble(txtTotalV.getText()));
        pst.executeUpdate();

        // Obtener el ID de la venta recién insertada
        ResultSet rs = pst.getGeneratedKeys();
        int idVenta = -1;
        if (rs.next()) {
            idVenta = rs.getInt(1);
        }

        // Registrar detalles de la factura
        registrarFactura(idVenta);
        
        // Actualizar el stock
        actualizarStock();

        JOptionPane.showMessageDialog(null, "Venta registrada con éxito.");

    } catch (SQLException e) {
        e.printStackTrace();
    }
}



    
    private void actualizarProducto(String codigo, String descripcion, double precio, int stock, String proveedor) {
    try {
        // Obtener ID del proveedor
        String queryProveedor = "SELECT ID FROM proveedor WHERE Proveedor = ?";
        PreparedStatement psProveedor = conexion.prepareStatement(queryProveedor);
        psProveedor.setString(1, proveedor);
        ResultSet rsProveedor = psProveedor.executeQuery();

        int idProveedor = -1;
        if (rsProveedor.next()) {
            idProveedor = rsProveedor.getInt("ID");
        } else {
            // Si el proveedor no existe, insertarlo
            String insertProveedor = "INSERT INTO proveedor(Proveedor) VALUES (?)";
            PreparedStatement psInsertProveedor = conexion.prepareStatement(insertProveedor, Statement.RETURN_GENERATED_KEYS);
            psInsertProveedor.setString(1, proveedor);
            psInsertProveedor.executeUpdate();

            ResultSet rsKeys = psInsertProveedor.getGeneratedKeys();
            if (rsKeys.next()) {
                idProveedor = rsKeys.getInt(1);
            }
        }

        // Actualizar el producto en la base de datos
        String query = "UPDATE producto SET Descripcion=?, Precio=?, Stock=?, ID_Proveedor=? WHERE Codigo=?";
        PreparedStatement ps = conexion.prepareStatement(query);
        ps.setString(1, descripcion);
        ps.setDouble(2, precio);
        ps.setInt(3, stock);
        ps.setInt(4, idProveedor);
        ps.setString(5, codigo);

        int resultado = ps.executeUpdate();
        if (resultado > 0) {
            JOptionPane.showMessageDialog(null, "Producto actualizado correctamente");
        } else {
            JOptionPane.showMessageDialog(null, "Error al actualizar el producto", "Error", JOptionPane.ERROR_MESSAGE);
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error en la base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
    
    private void configurarTablaVenta() {
    DefaultTableModel model = new DefaultTableModel();
    model.setColumnIdentifiers(new String[]{"Código", "Descripción", "Cantidad", "Precio", "Subtotal"}); 
    TablaVenta.setModel(model); // Asignar el modelo configurado a la tabla
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
        jPanel4 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jLabel32 = new javax.swing.JLabel();
        jLabel34 = new javax.swing.JLabel();
        jLabel35 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtEfectivo = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtEfectivo1 = new javax.swing.JTextField();
        txtEfectivo2 = new javax.swing.JTextField();
        txtEfectivo3 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        txtEfectivo4 = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        txtEfectivo5 = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        TablaInventario = new javax.swing.JTable();
        txtBuscar = new javax.swing.JTextField();
        txtExportar = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        jLabel36 = new javax.swing.JLabel();
        txtIngresoTotal = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        txtrefcodigo = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        TablaVenta = new javax.swing.JTable();
        jLabel13 = new javax.swing.JLabel();
        txtTotalV = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        txtDescripcionP = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        txtCantidad = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        txtPrecioP = new javax.swing.JTextField();
        btnRegistrar = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        boxCliente = new javax.swing.JComboBox<>();
        jLabel29 = new javax.swing.JLabel();
        txtStockP = new javax.swing.JTextField();
        jLabel30 = new javax.swing.JLabel();
        jButton11 = new javax.swing.JButton();
        jLabel24 = new javax.swing.JLabel();
        txtDescuento = new javax.swing.JTextField();
        jPanel7 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        TablaCliente = new javax.swing.JTable();
        txtBuscarCliente = new javax.swing.JTextField();
        txtNombre = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        txtTelefono = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        txtApellido = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        txtDireccion = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        txtCedula = new javax.swing.JTextField();
        btnAgregar = new javax.swing.JButton();
        btnAgregar1 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jPanel9 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel33 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel4.setBackground(new java.awt.Color(50, 101, 255));

        jButton1.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/cajero-automatico.png"))); // NOI18N
        jButton1.setText("REPORTE CAJA");
        jButton1.setBorder(null);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/anadir-al-carrito.png"))); // NOI18N
        jButton2.setText("COMPRA");
        jButton2.setBorder(null);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/ventas.png"))); // NOI18N
        jButton3.setText("VENTA");
        jButton3.setBorder(null);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/inventario.png"))); // NOI18N
        jButton4.setText("INVENTARIO");
        jButton4.setBorder(null);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/beneficio-financiero.png"))); // NOI18N
        jButton5.setText("HISTORIAL VENTA");
        jButton5.setBorder(null);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/nueva-cuenta.png"))); // NOI18N
        jButton6.setText("CLIENTE");
        jButton6.setBorder(null);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jLabel32.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/Tecnimotos.png"))); // NOI18N

        jLabel34.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jLabel34.setText("v1.0");

        jLabel35.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jLabel35.setText("Desarrollado Por Juan Olave");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel32, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel34))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel35)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel32, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 111, Short.MAX_VALUE)
                .addComponent(jLabel34)
                .addGap(4, 4, 4)
                .addComponent(jLabel35)
                .addContainerGap())
        );

        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 200, 740));

        jPanel11.setBackground(new java.awt.Color(255, 255, 255));

        jLabel2.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel2.setText("SISTEMA DE FACTURACION E INVENTARIO TECNIMOTOSJM");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(92, 92, 92)
                .addComponent(jLabel2)
                .addContainerGap(168, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addContainerGap(52, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(29, 29, 29))
        );

        jPanel1.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, -30, 930, 110));

        jPanel10.setBackground(new java.awt.Color(50, 101, 255));
        jPanel10.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel4.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("REPORTE DE CAJA");
        jPanel10.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 30, 310, -1));

        jLabel5.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 51, 51));
        jLabel5.setText("TOTAL");
        jPanel10.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 430, -1, -1));

        txtEfectivo.setEditable(false);
        txtEfectivo.setBackground(new java.awt.Color(50, 101, 255));
        txtEfectivo.setBorder(null);
        jPanel10.add(txtEfectivo, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 430, 110, 30));

        jLabel6.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("EFECTIVO");
        jPanel10.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 130, -1, -1));

        jLabel7.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("TARJETA");
        jPanel10.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 190, -1, -1));

        jLabel8.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("TRANSFERENCIA");
        jPanel10.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 250, -1, -1));

        txtEfectivo1.setEditable(false);
        txtEfectivo1.setBackground(new java.awt.Color(50, 101, 255));
        txtEfectivo1.setBorder(null);
        jPanel10.add(txtEfectivo1, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 130, 110, 30));

        txtEfectivo2.setEditable(false);
        txtEfectivo2.setBackground(new java.awt.Color(50, 101, 255));
        txtEfectivo2.setBorder(null);
        jPanel10.add(txtEfectivo2, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 190, 110, 30));

        txtEfectivo3.setEditable(false);
        txtEfectivo3.setBackground(new java.awt.Color(50, 101, 255));
        txtEfectivo3.setBorder(null);
        jPanel10.add(txtEfectivo3, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 250, 110, 30));

        jLabel9.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("CUENTA CORRIENTE");
        jPanel10.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 310, -1, -1));

        txtEfectivo4.setEditable(false);
        txtEfectivo4.setBackground(new java.awt.Color(50, 101, 255));
        txtEfectivo4.setBorder(null);
        jPanel10.add(txtEfectivo4, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 310, 110, 30));

        jLabel10.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(51, 255, 51));
        jLabel10.setText("DEVOLUCIONES");
        jPanel10.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 370, -1, -1));

        txtEfectivo5.setEditable(false);
        txtEfectivo5.setBackground(new java.awt.Color(50, 101, 255));
        txtEfectivo5.setBorder(null);
        jPanel10.add(txtEfectivo5, new org.netbeans.lib.awtextra.AbsoluteConstraints(510, 370, 110, 30));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("RC", jPanel3);

        jPanel5.setBackground(new java.awt.Color(50, 101, 255));
        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("INVENTARIO");
        jPanel5.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 20, -1, -1));

        jLabel3.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("BUSCAR:");
        jPanel5.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 100, -1, 30));

        TablaInventario.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(TablaInventario);

        jPanel5.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 160, 890, 420));

        txtBuscar.setBorder(null);
        txtBuscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBuscarActionPerformed(evt);
            }
        });
        jPanel5.add(txtBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 100, 310, 30));

        txtExportar.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        txtExportar.setText("EXPORTAR");
        txtExportar.setBorder(null);
        txtExportar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtExportarActionPerformed(evt);
            }
        });
        jPanel5.add(txtExportar, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 90, 110, 40));

        jButton9.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton9.setText("AGREGAR");
        jButton9.setBorder(null);
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton9, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 90, 110, 40));

        jButton12.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton12.setText("ELIMINAR");
        jButton12.setBorder(null);
        jPanel5.add(jButton12, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 90, 110, 40));

        jLabel36.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel36.setForeground(new java.awt.Color(255, 255, 255));
        jLabel36.setText("TOTAL DE MERCANCIA:");
        jPanel5.add(jLabel36, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, 30));

        txtIngresoTotal.setBorder(null);
        txtIngresoTotal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtIngresoTotalActionPerformed(evt);
            }
        });
        jPanel5.add(txtIngresoTotal, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 600, 310, 30));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 920, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("IT", jPanel2);

        jPanel12.setBackground(new java.awt.Color(50, 101, 255));
        jPanel12.setForeground(new java.awt.Color(255, 255, 0));
        jPanel12.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel11.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 0));
        jLabel11.setText("TOTAL:");
        jPanel12.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 560, -1, -1));

        jLabel12.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("VENTA");
        jPanel12.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 20, -1, -1));

        txtrefcodigo.setBorder(null);
        txtrefcodigo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtrefcodigoActionPerformed(evt);
            }
        });
        txtrefcodigo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtrefcodigoKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtrefcodigoKeyReleased(evt);
            }
        });
        jPanel12.add(txtrefcodigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 110, 150, 20));

        TablaVenta.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(TablaVenta);

        jPanel12.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 250, 900, 280));

        jLabel13.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("REF/CODIGO");
        jPanel12.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 80, -1, -1));

        txtTotalV.setEditable(false);
        txtTotalV.setBackground(new java.awt.Color(50, 101, 255));
        txtTotalV.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        txtTotalV.setForeground(new java.awt.Color(255, 255, 0));
        txtTotalV.setBorder(null);
        txtTotalV.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtTotalVKeyReleased(evt);
            }
        });
        jPanel12.add(txtTotalV, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 560, 260, 30));

        jLabel14.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 255, 255));
        jLabel14.setText("DESCRIPCION");
        jPanel12.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 160, -1, -1));

        txtDescripcionP.setEditable(false);
        txtDescripcionP.setBorder(null);
        txtDescripcionP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDescripcionPActionPerformed(evt);
            }
        });
        jPanel12.add(txtDescripcionP, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, 360, 20));

        jLabel15.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("CANTIDAD");
        jPanel12.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 80, -1, -1));

        txtCantidad.setBorder(null);
        txtCantidad.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtCantidadKeyReleased(evt);
            }
        });
        jPanel12.add(txtCantidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 110, 70, 20));

        jLabel16.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(255, 255, 255));
        jLabel16.setText("PRECIO");
        jPanel12.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 160, -1, -1));

        txtPrecioP.setEditable(false);
        txtPrecioP.setBorder(null);
        txtPrecioP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPrecioPActionPerformed(evt);
            }
        });
        jPanel12.add(txtPrecioP, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 190, 120, 20));

        btnRegistrar.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        btnRegistrar.setText("CONFIRMAR");
        btnRegistrar.setBorder(null);
        jPanel12.add(btnRegistrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(810, 550, 90, 40));

        jButton10.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton10.setText("Agregar");
        jButton10.setBorder(null);
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jPanel12.add(jButton10, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 90, 90, 40));

        boxCliente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jPanel12.add(boxCliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 110, 120, 20));

        jLabel29.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel29.setForeground(new java.awt.Color(255, 255, 255));
        jLabel29.setText("CLIENTE");
        jPanel12.add(jLabel29, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 80, -1, -1));

        txtStockP.setEditable(false);
        txtStockP.setBorder(null);
        jPanel12.add(txtStockP, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 110, 60, 20));

        jLabel30.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel30.setForeground(new java.awt.Color(255, 255, 255));
        jLabel30.setText("STOCK");
        jPanel12.add(jLabel30, new org.netbeans.lib.awtextra.AbsoluteConstraints(570, 80, -1, -1));

        jButton11.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton11.setText("Eliminar");
        jButton11.setBorder(null);
        jPanel12.add(jButton11, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 150, 90, 40));

        jLabel24.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(255, 255, 255));
        jLabel24.setText("DESCUENTO:");
        jPanel12.add(jLabel24, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 560, -1, -1));

        txtDescuento.setBackground(new java.awt.Color(255, 255, 255));
        txtDescuento.setBorder(null);
        txtDescuento.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtDescuentoKeyReleased(evt);
            }
        });
        jPanel12.add(txtDescuento, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 560, 140, 30));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel12, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Venta", jPanel6);

        jPanel13.setBackground(new java.awt.Color(50, 101, 255));
        jPanel13.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel17.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(255, 255, 255));
        jLabel17.setText("CLIENTES");
        jPanel13.add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 20, -1, -1));

        jLabel18.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(255, 255, 255));
        jLabel18.setText("BUSCAR:");
        jPanel13.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 110, -1, -1));

        TablaCliente.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(TablaCliente);

        jPanel13.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 150, -1, -1));

        txtBuscarCliente.setBorder(null);
        jPanel13.add(txtBuscarCliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 110, 340, 30));

        txtNombre.setBorder(null);
        jPanel13.add(txtNombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, 250, 20));

        jLabel19.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(255, 255, 255));
        jLabel19.setText("NOMBRES*");
        jPanel13.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, -1, -1));

        txtTelefono.setBorder(null);
        jPanel13.add(txtTelefono, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 360, 250, 20));

        jLabel20.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(255, 255, 255));
        jLabel20.setText("TELEFONO*");
        jPanel13.add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 320, -1, -1));

        txtApellido.setBorder(null);
        jPanel13.add(txtApellido, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 280, 250, 20));

        jLabel21.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(255, 255, 255));
        jLabel21.setText("APELLIDOS*");
        jPanel13.add(jLabel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 240, -1, -1));

        txtDireccion.setBorder(null);
        jPanel13.add(txtDireccion, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 440, 250, 20));

        jLabel22.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(255, 255, 255));
        jLabel22.setText("DIRECCION");
        jPanel13.add(jLabel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 400, -1, -1));

        jLabel23.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(255, 255, 255));
        jLabel23.setText("NIT/CEDULA");
        jPanel13.add(jLabel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 70, -1, -1));

        txtCedula.setBorder(null);
        jPanel13.add(txtCedula, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 110, 250, 20));

        btnAgregar.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        btnAgregar.setText("ELIMINAR");
        btnAgregar.setBorder(null);
        jPanel13.add(btnAgregar, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 490, 90, 40));

        btnAgregar1.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        btnAgregar1.setText("AGREGAR");
        btnAgregar1.setBorder(null);
        jPanel13.add(btnAgregar1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 490, 90, 40));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Comp", jPanel7);

        jPanel14.setBackground(new java.awt.Color(50, 101, 255));
        jPanel14.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel25.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel25.setForeground(new java.awt.Color(255, 255, 255));
        jLabel25.setText("INGRESOS DE MERCANCIA");
        jPanel14.add(jLabel25, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 30, -1, -1));

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane5.setViewportView(jTable2);

        jPanel14.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 100, -1, -1));

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Clie", jPanel8);

        jPanel15.setBackground(new java.awt.Color(50, 101, 255));
        jPanel15.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel31.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel31.setForeground(new java.awt.Color(255, 255, 255));
        jLabel31.setText("FECHA");
        jPanel15.add(jLabel31, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 130, -1, -1));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane4.setViewportView(jTable1);

        jPanel15.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 190, 880, -1));

        jLabel33.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel33.setForeground(new java.awt.Color(255, 255, 255));
        jLabel33.setText("HISTORIAL DE VENTAS");
        jPanel15.add(jLabel33, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 30, -1, -1));

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("HV", jPanel9);

        jPanel1.add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 40, 920, 680));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        jTabbedPane1.setSelectedIndex(0);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        jTabbedPane1.setSelectedIndex(4);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        jTabbedPane1.setSelectedIndex(2);
        asignarCliente();
        configurarTablaVenta();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        jTabbedPane1.setSelectedIndex(1);
        CargarInventario();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        jTabbedPane1.setSelectedIndex(5);
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        jTabbedPane1.setSelectedIndex(3);
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        Interfaz_AProducto open = new Interfaz_AProducto();
        open.setVisible(true);
        open.setLocationRelativeTo(null);
    }//GEN-LAST:event_jButton9ActionPerformed

    private void txtBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBuscarActionPerformed
        
    }//GEN-LAST:event_txtBuscarActionPerformed

    private void txtrefcodigoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtrefcodigoKeyReleased
 
    }//GEN-LAST:event_txtrefcodigoKeyReleased

    private void txtCantidadKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtCantidadKeyReleased
        validarCantidad();
    }//GEN-LAST:event_txtCantidadKeyReleased

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        agregarProductoATabla();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void txtTotalVKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTotalVKeyReleased
        calcularTotal();
    }//GEN-LAST:event_txtTotalVKeyReleased

    private void txtExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtExportarActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtExportarActionPerformed

    private void txtIngresoTotalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtIngresoTotalActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtIngresoTotalActionPerformed

    private void txtrefcodigoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtrefcodigoKeyPressed
    if (evt.getKeyCode() == KeyEvent.VK_ENTER) { // Detecta Enter
        cargarDatosProducto(); // Llama al método que busca el producto
    }
    }//GEN-LAST:event_txtrefcodigoKeyPressed

    private void txtDescripcionPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDescripcionPActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDescripcionPActionPerformed

    private void txtPrecioPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPrecioPActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtPrecioPActionPerformed

    private void txtDescuentoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtDescuentoKeyReleased
        aplicarDescuento();
    }//GEN-LAST:event_txtDescuentoKeyReleased

    private void txtrefcodigoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtrefcodigoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtrefcodigoActionPerformed

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
            java.util.logging.Logger.getLogger(Interfaz_Almacen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Interfaz_Almacen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Interfaz_Almacen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Interfaz_Almacen.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Interfaz_Almacen().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable TablaCliente;
    private javax.swing.JTable TablaInventario;
    private javax.swing.JTable TablaVenta;
    private javax.swing.JComboBox<String> boxCliente;
    private javax.swing.JButton btnAgregar;
    private javax.swing.JButton btnAgregar1;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton9;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtBuscarCliente;
    private javax.swing.JTextField txtCantidad;
    private javax.swing.JTextField txtCedula;
    private javax.swing.JTextField txtDescripcionP;
    private javax.swing.JTextField txtDescuento;
    private javax.swing.JTextField txtDireccion;
    private javax.swing.JTextField txtEfectivo;
    private javax.swing.JTextField txtEfectivo1;
    private javax.swing.JTextField txtEfectivo2;
    private javax.swing.JTextField txtEfectivo3;
    private javax.swing.JTextField txtEfectivo4;
    private javax.swing.JTextField txtEfectivo5;
    private javax.swing.JButton txtExportar;
    private javax.swing.JTextField txtIngresoTotal;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtPrecioP;
    private javax.swing.JTextField txtStockP;
    private javax.swing.JTextField txtTelefono;
    private javax.swing.JTextField txtTotalV;
    private javax.swing.JTextField txtrefcodigo;
    // End of variables declaration//GEN-END:variables
}
