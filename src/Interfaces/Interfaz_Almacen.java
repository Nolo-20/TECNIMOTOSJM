/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import Controladores.Conexion;
import java.awt.event.KeyEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

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
        setResizable(false);
        asignarCliente();
        obtenerUltimaFactura();
        cargarProveedores();
        DefaultTableModel modeloCompra = (DefaultTableModel) TablaCompra.getModel();
        txtCantidad.setEnabled(false);
        bloquearCampos();
        AutoCompleteDecorator.decorate(boxProveedorCompra);
        historialVenta();
    }

    public DefaultTableModel getTablaVentaModel() {
        return (DefaultTableModel) TablaVenta.getModel();
    }

    public JTable getTablaVenta() {
        return TablaVenta;
    }

    private void LimpiarDespuesAgregarProducto() {
        txtrefcodigo.setText("");
        txtCantidad.setText("");
        txtStockP.setText("");
        txtDescripcionP.setText("");
        txtPrecioP.setText("");
    }

    public void CargarInventario() {
        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // Permitir edición en todas las celdas
            }
        };

        modelo.setColumnIdentifiers(new String[]{"Código", "Descripción", "Precio Uni", "Precio Cost", "Stock", "Proveedor"});
        TablaInventario.setModel(modelo);

        // Inicializar sorter antes de usarlo en el DocumentListener
        sorter = new TableRowSorter<>(modelo);
        TablaInventario.setRowSorter(sorter); // Asignarlo a la tabla

        try {
            String query = "SELECT p.Codigo, p.Descripcion, p.Precio, p.Precio_Costo, p.Stock, pr.Proveedor "
                    + "FROM producto p INNER JOIN proveedor pr ON p.ID_Proveedor = pr.ID";
            PreparedStatement ps = conexion.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String codigo = rs.getString("Codigo");
                String descripcion = rs.getString("Descripcion");
                double precio = rs.getDouble("Precio");
                double precioV = rs.getDouble("Precio_Costo");
                int stock = rs.getInt("Stock");
                String proveedor = rs.getString("Proveedor");

                // Agregar fila al modelo
                modelo.addRow(new Object[]{codigo, descripcion, precio, precioV, stock, proveedor});
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
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }

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

    public void exportarAExcel() {
        // Obtener el modelo de la tabla
        DefaultTableModel modelo = (DefaultTableModel) TablaInventario.getModel();

        // Crear un nuevo libro de Excel
        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Inventario");

        // Crear la fila de encabezados
        Row filaEncabezados = hoja.createRow(0);
        for (int i = 0; i < modelo.getColumnCount(); i++) {
            Cell celda = filaEncabezados.createCell(i);
            celda.setCellValue(modelo.getColumnName(i));
        }

        // Llenar la hoja con los datos de la tabla
        for (int i = 0; i < modelo.getRowCount(); i++) {
            Row fila = hoja.createRow(i + 1); // +1 para evitar sobrescribir los encabezados
            for (int j = 0; j < modelo.getColumnCount(); j++) {
                Cell celda = fila.createCell(j);
                Object valor = modelo.getValueAt(i, j);
                if (valor != null) {
                    celda.setCellValue(valor.toString());
                } else {
                    celda.setCellValue(""); // Celda vacía si el valor es nulo
                }
            }
        }

        // Guardar el archivo Excel
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar archivo Excel");
        int seleccionUsuario = fileChooser.showSaveDialog(null);
        if (seleccionUsuario == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream archivo = new FileOutputStream(fileChooser.getSelectedFile() + ".xlsx")) {
                libro.write(archivo);
                JOptionPane.showMessageDialog(null, "Inventario exportado correctamente.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error al exportar el archivo: " + e.getMessage());
            } finally {
                try {
                    libro.close();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Error al cerrar el libro: " + e.getMessage());
                }
            }
        }
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
                    txtCantidad.setEnabled(true);
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
        String cantidad = txtCantidad.getText();
        int cantidadInt = Integer.parseInt(cantidad);

        if (cantidadInt > stock) {
            JOptionPane.showMessageDialog(null, "Cantidad excede el stock disponible");
            txtCantidad.setText("");
        } else if (cantidadInt == 0 || cantidad.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese una cantidad valida o existente");
            txtCantidad.setText("");
        }
    }

    private void agregarProductoATabla() {
        DefaultTableModel model = (DefaultTableModel) TablaVenta.getModel();
        String codigo = txtrefcodigo.getText();

        // Validar que los campos no estén vacíos
        if (codigo.isEmpty() || txtDescripcionP.getText().isEmpty()
                || txtCantidad.getText().isEmpty() || txtPrecioP.getText().isEmpty()) {
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
                    int cantidadActual = ((Number) model.getValueAt(i, 2)).intValue(); // Cantidad en la tabla
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
            txtTotalV.setText(String.format("%.2f", calcularTotal()));
            LimpiarDespuesAgregarProducto();
            txtCantidad.setEnabled(false);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }

    private double calcularTotal() {
        double total = 0.0;

        for (int i = 0; i < TablaVenta.getRowCount(); i++) {
            double precio = Double.parseDouble(TablaVenta.getValueAt(i, 3).toString()); // Columna 3 es el precio
            int cantidad = Integer.parseInt(TablaVenta.getValueAt(i, 2).toString()); // Columna 2 es la cantidad
            total += precio * cantidad;
        }

        return total;
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
            String sql = "SELECT ID, Nombre FROM cliente";
            PreparedStatement pst = conexion.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            boxCliente.removeAllItems(); // Limpiar JComboBox

            int count = 0; // Contador de clientes

            while (rs.next()) {
                int id = rs.getInt("ID");
                String nombre = rs.getString("Nombre");
                String cliente = id + " - " + nombre;

                System.out.println("Cliente cargado: " + cliente); // Verifica qué se está agregando
                boxCliente.addItem(cliente);
                count++;
            }

            System.out.println("Total de clientes cargados: " + count);

            if (count == 0) {
                JOptionPane.showMessageDialog(null, "No hay clientes en la base de datos.");
            }

            boxCliente.setSelectedIndex(0); // Seleccionar el primer cliente

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al cargar clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void AgregarCliente() {
        String Cedula = txtCedula.getText().trim();
        String Nombre = txtNombre.getText();
        String Apellido = txtApellido.getText();
        String Telefono = txtTelefono.getText().trim();
        String Direccion = txtDireccion.getText();

        if (Nombre.isEmpty() || Apellido.isEmpty() || Telefono.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Llenar los campos obligatorios", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            try {
                int CedulaInt = Integer.parseInt(Cedula);
                int TelefonoInt = Integer.parseInt(Telefono);
                ps = conexion.prepareStatement("INSERT INTO Cliente (Cedula, Nombre, Apellido, Telefono, Direccion) VALUES (?,?,?,?,?)");
                ps.setInt(1, CedulaInt);
                ps.setString(2, Nombre);
                ps.setString(3, Apellido);
                ps.setInt(4, TelefonoInt);
                ps.setString(5, Direccion);

                int res = ps.executeUpdate();
                if (res > 0) {
                    JOptionPane.showMessageDialog(null, "Cliente Agregado Correctamente", "Mensaje", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error en la base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void actualizarStock() {
        String codigo = txtRefCodigoCompra.getText().trim();
        int cantidad = Integer.parseInt(txtCantidadCompra.getText());

        try {
            String sql = "UPDATE producto SET Stock = Stock + ? WHERE Codigo = ?";
            PreparedStatement pst = conexion.prepareStatement(sql);
            pst.setInt(1, cantidad);
            pst.setString(2, codigo);

            int filas = pst.executeUpdate();
            if (filas > 0) {
                JOptionPane.showMessageDialog(null, "Stock actualizado correctamente.");

                // REFRESCAR STOCK EN EL TEXTFIELD
                int nuevoStock = Integer.parseInt(txtStockCompra.getText()) + cantidad;
                txtStockCompra.setText(String.valueOf(nuevoStock));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar el stock: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String obtenerClienteSeleccionado() {
        String clienteSeleccionado = boxCliente.getSelectedItem().toString();
        return clienteSeleccionado; // Devuelve algo como "1 - Consumidor Final"
    }

    private void abrirVentanaPago() {
        double total = calcularTotal();

        if (total <= 0) {
            JOptionPane.showMessageDialog(null, "No hay productos en la venta.");
            return;
        }

        // Obtener el cliente seleccionado
        String clienteSeleccionado = obtenerClienteSeleccionado();
        String[] partesCliente = clienteSeleccionado.split(" - ");
        int idCliente = Integer.parseInt(partesCliente[0]); // Extraer el ID del cliente
        String nombreCliente = partesCliente[1]; // Extraer el nombre del cliente

        // Obtener los productos de la tabla
        List<String> productos = new ArrayList<>();
        DefaultTableModel modelo = (DefaultTableModel) TablaVenta.getModel();

        for (int i = 0; i < modelo.getRowCount(); i++) {
            Object codigo = modelo.getValueAt(i, 0);
            Object descripcion = modelo.getValueAt(i, 1);
            Object cantidad = modelo.getValueAt(i, 2);
            Object precio = modelo.getValueAt(i, 3);

            if (codigo != null && descripcion != null && cantidad != null && precio != null) {
                String producto = codigo.toString() + " - " + descripcion.toString() + " - Cant: "
                        + cantidad.toString() + " - $" + precio.toString();
                productos.add(producto);
            }
        }

        // Abrir la ventana de pago con total, ID del cliente, nombre del cliente y productos
        Interfaz_Pago ventanaPago = new Interfaz_Pago(total, idCliente, nombreCliente, productos);
        ventanaPago.setVisible(true);
        ventanaPago.setLocationRelativeTo(null);
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
        model.setRowCount(0);
        TablaVenta.setModel(model); // Asignar el modelo configurado a la tabla
    }

    //ventana Compra
    private void obtenerUltimaFactura() {
        try {
            Statement st = conexion.createStatement();
            ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(ID), 0) + 1 FROM Compra"); // Manejo de NULL

            if (rs.next()) {
                txtNFactura.setText(String.valueOf(rs.getInt(1))); // Convertir a String y asignar al TextField
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al obtener el número de factura: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            txtNFactura.setText("1"); // Si hay error, asignar "1"
        }
    }

    private void CargarProductosCompra(String codigo) {
        if (codigo.isEmpty()) {
            return;
        }

        try {
            String sql = "SELECT Descripcion, Precio, Stock FROM producto WHERE Codigo = ?";
            PreparedStatement pst = conexion.prepareStatement(sql);
            pst.setString(1, codigo);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                // Si el producto existe, carga los datos
                txtDescripcionCompra.setText(rs.getString("Descripcion"));
                txtPrecioVentaCompra.setText(rs.getString("Precio"));
                txtStockCompra.setText(rs.getString("Stock"));
                txtCantidadCompra.setEnabled(true);
                txtPrecioCostoCompra.setEnabled(true);
                boxProveedorCompra.setEnabled(false); // Bloquea proveedor si ya existe
            } else {
                // Producto NO existe, preguntar si se quiere agregar
                int opcion = JOptionPane.showConfirmDialog(null,
                        "El producto no existe. ¿Desea agregarlo?",
                        "Producto no encontrado", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (opcion == JOptionPane.YES_OPTION) {
                    habilitarCamposParaNuevoProducto(); // Activa campos para agregarlo manualmente
                } else {
                    limpiarCampos(); // Limpia si el usuario no quiere agregarlo
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al buscar el producto: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarProveedores() {
        boxProveedorCompra.removeAllItems(); // Limpiar el comboBox antes de llenarlo
        try {
            String query = "SELECT ID, Proveedor FROM proveedor";  // Obtener ID y Nombre
            PreparedStatement ps = conexion.prepareStatement(query);
            res = ps.executeQuery();

            while (res.next()) {
                String id = res.getString("ID");
                String nombre = res.getString("Proveedor");

                // Guardar ambos valores en el JComboBox con un formato personalizado
                boxProveedorCompra.addItem(nombre);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al cargar proveedores: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void configurarTablaCompra() {
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Código", "Descripción", "Cantidad", "Precio Costo", "Precio Venta", "Stock"});
        model.setRowCount(0);
        TablaCompra.setModel(model);
    }

    private void configurarTablaCliente() {
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Nit/Cedula", "Nombres", "Apellidos", "Telefono", "Direccion"});
        model.setRowCount(0);
        TablaCliente.setModel(model);
    }

    private void limpiarCampos() {
        txtRefCodigoCompra.setText("");
        txtDescripcionCompra.setText("");
        txtCantidadCompra.setText("");
        txtPrecioCostoCompra.setText("");
        txtPrecioVentaCompra.setText("");
        txtStockCompra.setText("");
        boxProveedorCompra.setSelectedItem(null);
    }

    private void habilitarCamposParaNuevoProducto() {
        txtDescripcionCompra.setEnabled(true);
        txtPrecioVentaCompra.setEnabled(true);
        txtPrecioCostoCompra.setEnabled(true);
        txtStockCompra.setEnabled(false); // Deshabilitado, pero mostrará 0
        txtCantidadCompra.setEnabled(true);
        boxProveedorCompra.setEnabled(true);

        // Limpiar campos para que el usuario ingrese datos nuevos
        txtDescripcionCompra.setText("");
        txtPrecioVentaCompra.setText("");
        txtPrecioCostoCompra.setText("");
        txtStockCompra.setText("0"); // MOSTRAR STOCK EN 0
        txtCantidadCompra.setText("");
    }

    private void bloquearCampos() {
        txtDescripcionCompra.setEnabled(false);
        txtPrecioVentaCompra.setEnabled(false);
        txtPrecioCostoCompra.setEnabled(false);
        txtStockCompra.setEnabled(false);
        txtCantidadCompra.setEnabled(false);
        boxProveedorCompra.setEnabled(false);
    }

    private void agregarProductoNuevo() {
        String codigo = txtRefCodigoCompra.getText().trim();
        String descripcion = txtDescripcionCompra.getText().trim();
        String precioVenta = txtPrecioVentaCompra.getText().trim();
        String precioCosto = txtPrecioCostoCompra.getText().trim();
        String nombreProveedor = boxProveedorCompra.getSelectedItem().toString().trim();
        String cantidadStr = txtCantidadCompra.getText().trim();

        if (codigo.isEmpty() || descripcion.isEmpty() || precioVenta.isEmpty() || precioCosto.isEmpty() || nombreProveedor.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Completa todos los campos antes de agregar el producto.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadStr); // Convertir cantidad a entero
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(null, "La cantidad debe ser mayor a 0.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            conexion.setAutoCommit(false);

            // 🔹 1. OBTENER O INSERTAR PROVEEDOR
            String sqlProveedor = "SELECT ID FROM Proveedor WHERE Proveedor = ?";
            PreparedStatement pstProveedor = conexion.prepareStatement(sqlProveedor);
            pstProveedor.setString(1, nombreProveedor);
            ResultSet rsProveedor = pstProveedor.executeQuery();

            int idProveedor;
            if (rsProveedor.next()) {
                idProveedor = rsProveedor.getInt("ID");
            } else {
                // 🔹 INSERTAR NUEVO PROVEEDOR
                String sqlInsertProveedor = "INSERT INTO Proveedor (Proveedor) VALUES (?)";
                PreparedStatement pstInsertProveedor = conexion.prepareStatement(sqlInsertProveedor, Statement.RETURN_GENERATED_KEYS);
                pstInsertProveedor.setString(1, nombreProveedor);
                pstInsertProveedor.executeUpdate();

                cargarProveedores();

                // 🔹 OBTENER EL NUEVO ID DEL PROVEEDOR
                ResultSet rsNuevoProveedor = pstInsertProveedor.getGeneratedKeys();
                if (rsNuevoProveedor.next()) {
                    idProveedor = rsNuevoProveedor.getInt(1);
                } else {
                    throw new SQLException("No se pudo obtener el ID del proveedor recién insertado.");
                }
            }

            // 🔹 2. INSERTAR PRODUCTO CON STOCK INICIAL SEGÚN LA CANTIDAD INGRESADA
            String sql = "INSERT INTO Producto (Codigo, Descripcion, ID_Proveedor, Precio, Precio_Costo, Stock) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement pst = conexion.prepareStatement(sql);
            pst.setString(1, codigo);
            pst.setString(2, descripcion);
            pst.setInt(3, idProveedor);
            pst.setDouble(4, Double.parseDouble(precioVenta));
            pst.setDouble(5, Double.parseDouble(precioCosto));
            pst.setInt(6, cantidad); // 🔹 ASIGNAR LA CANTIDAD INGRESADA COMO STOCK INICIAL

            int filas = pst.executeUpdate();
            if (filas > 0) {
                conexion.commit();
                JOptionPane.showMessageDialog(null, "Producto agregado correctamente.");

                // 🔹 3. ACTUALIZAR CAMPOS DE STOCK Y HABILITAR CANTIDAD
                txtStockCompra.setText(String.valueOf(cantidad));
                txtCantidadCompra.setEnabled(true);

                // 🔹 4. AGREGAR EL PRODUCTO A LA TABLA DE COMPRA
                DefaultTableModel model = (DefaultTableModel) TablaCompra.getModel();
                model.addRow(new Object[]{codigo, descripcion, cantidad, precioCosto, precioVenta, cantidad});
            } else {
                conexion.rollback();
                JOptionPane.showMessageDialog(null, "Error: No se pudo agregar el producto.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            try {
                conexion.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(null, "Error al agregar producto: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Error: La cantidad debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    //historial venta
    public void historialVenta() {
        String fechaInicio = txtFechaInicial.getText().trim();
        String fechaFin = txtFechaFin.getText().trim();

        DefaultTableModel modelo = (DefaultTableModel) TablaVentaHistorial.getModel();
        modelo.setColumnIdentifiers(new String[]{"Fecha de Venta", "Cliente", "Producto", "Precio uni", "Cantidad", "Total"});
        modelo.setRowCount(0); // Limpiar tabla antes de agregar nuevos datos

        // Base de la consulta
        String sql = "SELECT v.Fecha_Venta, c.Nombre AS Cliente, p.Descripcion AS Producto, f.Precio, f.Cantidad, (f.Precio * f.Cantidad) AS Total "
                + "FROM Venta v "
                + "JOIN Factura f ON v.ID = f.ID_Venta "
                + "JOIN Cliente c ON v.ID_Cliente = c.ID "
                + "JOIN Producto p ON f.ID_Producto = p.Codigo ";

        // Si se ingresan fechas, se agrega la condición WHERE
        if (!fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
            sql += "WHERE v.Fecha_Venta BETWEEN ? AND ? ";
        }

        sql += "ORDER BY v.Fecha_Venta DESC"; // Ordenar por fecha

        try {
            ps = conexion.prepareStatement(sql);
            // Si hay fechas ingresadas, agregarlas como parámetros
            if (!fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
                ps.setString(1, fechaInicio);
                ps.setString(2, fechaFin);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] fila = {
                    rs.getString("Fecha_Venta"),
                    rs.getString("Cliente"),
                    rs.getString("Producto"),
                    rs.getDouble("Precio"), // Precio unitario del producto
                    rs.getInt("Cantidad"),
                    rs.getDouble("Total")
                };
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al cargar ventas: " + e.getMessage());
        }
    }

    public void exportarHistorialVenta() {
        DefaultTableModel modelo = (DefaultTableModel) TablaVentaHistorial.getModel();
        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Historial de Ventas");

        Row filaEncabezado = hoja.createRow(0);
        for (int i = 0; i < modelo.getColumnCount(); i++) {
            Cell celda = filaEncabezado.createCell(i);
            celda.setCellValue(modelo.getColumnName(i));
        }

        for (int i = 0; i < modelo.getRowCount(); i++) {
            Row fila = hoja.createRow(i + 1);
            for (int j = 0; j < modelo.getColumnCount(); j++) {
                Cell celda = fila.createCell(j);
                Object valor = modelo.getValueAt(i, j);
                if (valor != null) {
                    celda.setCellValue(valor.toString());
                }
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar archivo Excel");
        int seleccionUsuario = fileChooser.showSaveDialog(null);
        if (seleccionUsuario == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream archivo = new FileOutputStream(fileChooser.getSelectedFile() + ".xlsx")) {
                libro.write(archivo);
                JOptionPane.showMessageDialog(null, "Historial exportado correctamente.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error al exportar: " + e.getMessage());
            }
        }
    }

    public void buscarPorFecha() {
        String fechaInicio = txtFechaInicial.getText().trim();
        String fechaFin = txtFechaFin.getText().trim();

        if (fechaInicio.isEmpty() || fechaFin.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Por favor, ingrese ambas fechas para filtrar.");
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) TablaVentaHistorial.getModel();
        modelo.setRowCount(0); // Limpiar la tabla antes de agregar nuevos datos

        String sql = "SELECT v.Fecha_Venta, c.Nombre AS Cliente, p.Nombre AS Producto, f.Precio, f.Cantidad, (f.Precio * f.Cantidad) AS Total "
                + "FROM Venta v "
                + "JOIN Factura f ON v.ID = f.ID_Venta "
                + "JOIN Cliente c ON v.ID_Cliente = c.ID "
                + "JOIN Producto p ON f.ID_Producto = p.Codigo "
                + "WHERE v.Fecha_Venta BETWEEN ? AND ? "
                + "ORDER BY v.Fecha_Venta DESC";

        try {
            ps = conexion.prepareStatement(sql);
            ps.setString(1, fechaInicio);
            ps.setString(2, fechaFin);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] fila = {
                    rs.getString("Fecha_Venta"),
                    rs.getString("Cliente"),
                    rs.getString("Producto"),
                    rs.getDouble("Precio"),
                    rs.getInt("Cantidad"),
                    rs.getDouble("Total")
                };
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al filtrar ventas: " + e.getMessage());
        }
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
        ReporteCaja = new javax.swing.JPanel();
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
        Inventario = new javax.swing.JPanel();
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
        Venta = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        boxCliente = new javax.swing.JComboBox<>();
        txtDescripcionP = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        txtrefcodigo = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jLabel30 = new javax.swing.JLabel();
        jButton10 = new javax.swing.JButton();
        txtPrecioP = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jButton11 = new javax.swing.JButton();
        txtCantidad = new javax.swing.JTextField();
        txtStockP = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        TablaVenta = new javax.swing.JTable();
        txtTotalV = new javax.swing.JTextField();
        btnRegistrar = new javax.swing.JButton();
        jLabel24 = new javax.swing.JLabel();
        txtDescuento = new javax.swing.JTextField();
        Cliente = new javax.swing.JPanel();
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
        CompraInventario = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        TablaCompra = new javax.swing.JTable();
        jPanel17 = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        txtNFactura = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        jPanel18 = new javax.swing.JPanel();
        txtRefCodigoCompra = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        txtCantidadCompra = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        txtDescripcionCompra = new javax.swing.JTextField();
        jLabel37 = new javax.swing.JLabel();
        jLabel39 = new javax.swing.JLabel();
        txtPrecioCostoCompra = new javax.swing.JTextField();
        txtStockCompra = new javax.swing.JTextField();
        jLabel38 = new javax.swing.JLabel();
        txtPrecioVentaCompra = new javax.swing.JTextField();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        boxProveedorCompra = new javax.swing.JComboBox<>();
        btnAgregarCompra = new javax.swing.JButton();
        btnFinalizarCompra = new javax.swing.JButton();
        HistorialVenta = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        TablaVentaHistorial = new javax.swing.JTable();
        jLabel33 = new javax.swing.JLabel();
        btnExportarVenta = new javax.swing.JButton();
        txtFechaFin = new javax.swing.JTextField();
        txtFechaInicial = new javax.swing.JTextField();
        jLabel42 = new javax.swing.JLabel();
        txtBuscarFechaVenta = new javax.swing.JButton();
        jLabel43 = new javax.swing.JLabel();
        jLabel44 = new javax.swing.JLabel();

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 121, Short.MAX_VALUE)
                .addComponent(jLabel34)
                .addGap(4, 4, 4)
                .addComponent(jLabel35)
                .addContainerGap())
        );

        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 200, 750));

        jPanel11.setBackground(new java.awt.Color(255, 255, 255));

        jLabel2.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel2.setText("SISTEMA DE FACTURACION E INVENTARIO TECNIMOTOSJM");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(155, 155, 155)
                .addComponent(jLabel2)
                .addContainerGap(155, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addContainerGap(56, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(25, 25, 25))
        );

        jPanel1.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, -30, 980, 110));

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

        javax.swing.GroupLayout ReporteCajaLayout = new javax.swing.GroupLayout(ReporteCaja);
        ReporteCaja.setLayout(ReporteCajaLayout);
        ReporteCajaLayout.setHorizontalGroup(
            ReporteCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        ReporteCajaLayout.setVerticalGroup(
            ReporteCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("RC", ReporteCaja);

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

        jPanel5.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 160, 940, 420));

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

        txtIngresoTotal.setEditable(false);
        txtIngresoTotal.setBackground(new java.awt.Color(50, 101, 255));
        txtIngresoTotal.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtIngresoTotal.setForeground(new java.awt.Color(255, 255, 0));
        txtIngresoTotal.setBorder(null);
        txtIngresoTotal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtIngresoTotalActionPerformed(evt);
            }
        });
        jPanel5.add(txtIngresoTotal, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 600, 310, 30));

        javax.swing.GroupLayout InventarioLayout = new javax.swing.GroupLayout(Inventario);
        Inventario.setLayout(InventarioLayout);
        InventarioLayout.setHorizontalGroup(
            InventarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 980, Short.MAX_VALUE)
        );
        InventarioLayout.setVerticalGroup(
            InventarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("IT", Inventario);

        jPanel12.setBackground(new java.awt.Color(50, 101, 255));
        jPanel12.setForeground(new java.awt.Color(255, 255, 0));
        jPanel12.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel16.setBackground(new java.awt.Color(204, 204, 204));
        jPanel16.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        boxCliente.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        boxCliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxClienteActionPerformed(evt);
            }
        });
        jPanel16.add(boxCliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 40, 120, 20));

        txtDescripcionP.setEditable(false);
        txtDescripcionP.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtDescripcionP.setBorder(null);
        txtDescripcionP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDescripcionPActionPerformed(evt);
            }
        });
        jPanel16.add(txtDescripcionP, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 130, 360, 20));

        jLabel29.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel29.setText("CLIENTE");
        jPanel16.add(jLabel29, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, -1, -1));

        jLabel16.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel16.setText("PRECIO");
        jPanel16.add(jLabel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 90, -1, -1));

        txtrefcodigo.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
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
        jPanel16.add(txtrefcodigo, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 40, 150, 20));

        jLabel15.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel15.setText("CANTIDAD");
        jPanel16.add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 10, -1, -1));

        jLabel30.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel30.setText("STOCK");
        jPanel16.add(jLabel30, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 10, -1, -1));

        jButton10.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton10.setText("Agregar");
        jButton10.setBorder(null);
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jPanel16.add(jButton10, new org.netbeans.lib.awtextra.AbsoluteConstraints(720, 110, 90, 40));

        txtPrecioP.setEditable(false);
        txtPrecioP.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtPrecioP.setBorder(null);
        txtPrecioP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPrecioPActionPerformed(evt);
            }
        });
        jPanel16.add(txtPrecioP, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 130, 120, 20));

        jLabel13.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel13.setText("REF/CODIGO");
        jPanel16.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 10, -1, -1));

        jButton11.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        jButton11.setText("Eliminar");
        jButton11.setBorder(null);
        jPanel16.add(jButton11, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 110, 90, 40));

        txtCantidad.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtCantidad.setBorder(null);
        txtCantidad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCantidadActionPerformed(evt);
            }
        });
        txtCantidad.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtCantidadKeyReleased(evt);
            }
        });
        jPanel16.add(txtCantidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 40, 70, 20));

        txtStockP.setEditable(false);
        txtStockP.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtStockP.setBorder(null);
        jPanel16.add(txtStockP, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 40, 60, 20));

        jLabel14.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel14.setText("DESCRIPCION");
        jPanel16.add(jLabel14, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 90, -1, -1));

        jPanel12.add(jPanel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 70, 950, 170));

        jLabel11.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 0));
        jLabel11.setText("TOTAL:");
        jPanel12.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 580, -1, -1));

        jLabel12.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("VENTA");
        jPanel12.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 20, -1, -1));

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

        jPanel12.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 250, 950, 300));

        txtTotalV.setEditable(false);
        txtTotalV.setBackground(new java.awt.Color(50, 101, 255));
        txtTotalV.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        txtTotalV.setForeground(new java.awt.Color(255, 255, 0));
        txtTotalV.setBorder(null);
        txtTotalV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTotalVActionPerformed(evt);
            }
        });
        txtTotalV.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtTotalVKeyReleased(evt);
            }
        });
        jPanel12.add(txtTotalV, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 580, 260, 30));

        btnRegistrar.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        btnRegistrar.setText("CONFIRMAR");
        btnRegistrar.setBorder(null);
        btnRegistrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarActionPerformed(evt);
            }
        });
        jPanel12.add(btnRegistrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(810, 570, 90, 40));

        jLabel24.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(255, 255, 255));
        jLabel24.setText("DESCUENTO:");
        jPanel12.add(jLabel24, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 580, -1, -1));

        txtDescuento.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtDescuento.setBorder(null);
        txtDescuento.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtDescuentoKeyReleased(evt);
            }
        });
        jPanel12.add(txtDescuento, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 580, 140, 30));

        javax.swing.GroupLayout VentaLayout = new javax.swing.GroupLayout(Venta);
        Venta.setLayout(VentaLayout);
        VentaLayout.setHorizontalGroup(
            VentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, VentaLayout.createSequentialGroup()
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, 981, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        VentaLayout.setVerticalGroup(
            VentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, VentaLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, 646, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );

        jTabbedPane1.addTab("Venta", Venta);

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

        jPanel13.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 150, 520, -1));

        txtBuscarCliente.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtBuscarCliente.setBorder(null);
        jPanel13.add(txtBuscarCliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(550, 110, 340, 30));

        txtNombre.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtNombre.setBorder(null);
        jPanel13.add(txtNombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, 250, 20));

        jLabel19.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(255, 255, 255));
        jLabel19.setText("NOMBRES*");
        jPanel13.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, -1, -1));

        txtTelefono.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtTelefono.setBorder(null);
        txtTelefono.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtTelefonoKeyTyped(evt);
            }
        });
        jPanel13.add(txtTelefono, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 360, 250, 20));

        jLabel20.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(255, 255, 255));
        jLabel20.setText("TELEFONO*");
        jPanel13.add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 320, -1, -1));

        txtApellido.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtApellido.setBorder(null);
        jPanel13.add(txtApellido, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 280, 250, 20));

        jLabel21.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(255, 255, 255));
        jLabel21.setText("APELLIDOS*");
        jPanel13.add(jLabel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 240, -1, -1));

        txtDireccion.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
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

        txtCedula.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtCedula.setBorder(null);
        txtCedula.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtCedulaKeyTyped(evt);
            }
        });
        jPanel13.add(txtCedula, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 110, 250, 20));

        btnAgregar.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        btnAgregar.setText("ELIMINAR");
        btnAgregar.setBorder(null);
        jPanel13.add(btnAgregar, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 490, 90, 40));

        btnAgregar1.setFont(new java.awt.Font("Roboto Medium", 0, 12)); // NOI18N
        btnAgregar1.setText("AGREGAR");
        btnAgregar1.setBorder(null);
        btnAgregar1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregar1ActionPerformed(evt);
            }
        });
        jPanel13.add(btnAgregar1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 490, 90, 40));

        javax.swing.GroupLayout ClienteLayout = new javax.swing.GroupLayout(Cliente);
        Cliente.setLayout(ClienteLayout);
        ClienteLayout.setHorizontalGroup(
            ClienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        ClienteLayout.setVerticalGroup(
            ClienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Comp", Cliente);

        jPanel14.setBackground(new java.awt.Color(50, 101, 255));
        jPanel14.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        TablaCompra.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane5.setViewportView(TablaCompra);

        jPanel14.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 100, -1, 460));

        jPanel17.setBackground(new java.awt.Color(153, 153, 153));
        jPanel17.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel25.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel25.setText("N FACTURA");
        jPanel17.add(jLabel25, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, 100, 30));

        txtNFactura.setEditable(false);
        txtNFactura.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtNFactura.setBorder(null);
        txtNFactura.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNFacturaActionPerformed(evt);
            }
        });
        jPanel17.add(txtNFactura, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 30, 170, 30));

        jPanel14.add(jPanel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 140, 220, 70));

        jLabel26.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(255, 255, 255));
        jLabel26.setText("INGRESOS DE MERCANCIA");
        jPanel14.add(jLabel26, new org.netbeans.lib.awtextra.AbsoluteConstraints(220, 30, -1, -1));

        jPanel18.setBackground(new java.awt.Color(153, 153, 153));
        jPanel18.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txtRefCodigoCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtRefCodigoCompra.setBorder(null);
        txtRefCodigoCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtRefCodigoCompraActionPerformed(evt);
            }
        });
        txtRefCodigoCompra.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtRefCodigoCompraKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtRefCodigoCompraKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtRefCodigoCompraKeyTyped(evt);
            }
        });
        jPanel18.add(txtRefCodigoCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, 170, -1));

        jLabel27.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel27.setText("REF/CODIGO");
        jPanel18.add(jLabel27, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 10, 120, 30));

        txtCantidadCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtCantidadCompra.setBorder(null);
        jPanel18.add(txtCantidadCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 40, 50, 20));

        jLabel28.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel28.setText("CANTIDAD");
        jPanel18.add(jLabel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 10, 90, 30));

        txtDescripcionCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtDescripcionCompra.setBorder(null);
        txtDescripcionCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtDescripcionCompraActionPerformed(evt);
            }
        });
        jPanel18.add(txtDescripcionCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 130, 300, -1));

        jLabel37.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel37.setText("DESCRIPCION");
        jPanel18.add(jLabel37, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 90, 120, 30));

        jLabel39.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel39.setText("PRECIO COSTO");
        jPanel18.add(jLabel39, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 180, 130, 30));

        txtPrecioCostoCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtPrecioCostoCompra.setBorder(null);
        txtPrecioCostoCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPrecioCostoCompraActionPerformed(evt);
            }
        });
        jPanel18.add(txtPrecioCostoCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 210, 140, -1));

        txtStockCompra.setEditable(false);
        txtStockCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtStockCompra.setBorder(null);
        jPanel18.add(txtStockCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 40, 50, 20));

        jLabel38.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel38.setText("STOCK");
        jPanel18.add(jLabel38, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 10, 60, 30));

        txtPrecioVentaCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtPrecioVentaCompra.setBorder(null);
        txtPrecioVentaCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPrecioVentaCompraActionPerformed(evt);
            }
        });
        jPanel18.add(txtPrecioVentaCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 280, 140, -1));

        jLabel40.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel40.setText("PRECIO VENTA");
        jPanel18.add(jLabel40, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 250, 130, 30));

        jLabel41.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel41.setText("PROVEEDOR");
        jPanel18.add(jLabel41, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 180, 130, 30));

        boxProveedorCompra.setEditable(true);
        boxProveedorCompra.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jPanel18.add(boxProveedorCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 210, 140, 30));

        btnAgregarCompra.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnAgregarCompra.setText("AGREGAR");
        btnAgregarCompra.setBorder(null);
        btnAgregarCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarCompraActionPerformed(evt);
            }
        });
        jPanel18.add(btnAgregarCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 270, 110, 40));

        jPanel14.add(jPanel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 240, 410, 320));

        btnFinalizarCompra.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnFinalizarCompra.setText("FINALIZAR");
        btnFinalizarCompra.setBorder(null);
        btnFinalizarCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFinalizarCompraActionPerformed(evt);
            }
        });
        jPanel14.add(btnFinalizarCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(840, 580, 110, 40));

        javax.swing.GroupLayout CompraInventarioLayout = new javax.swing.GroupLayout(CompraInventario);
        CompraInventario.setLayout(CompraInventarioLayout);
        CompraInventarioLayout.setHorizontalGroup(
            CompraInventarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        CompraInventarioLayout.setVerticalGroup(
            CompraInventarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Clie", CompraInventario);

        jPanel15.setBackground(new java.awt.Color(50, 101, 255));
        jPanel15.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel31.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel31.setForeground(new java.awt.Color(255, 255, 255));
        jLabel31.setText("A");
        jPanel15.add(jLabel31, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 130, -1, -1));

        TablaVentaHistorial.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane4.setViewportView(TablaVentaHistorial);

        jPanel15.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 190, 940, -1));

        jLabel33.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel33.setForeground(new java.awt.Color(255, 255, 255));
        jLabel33.setText("HISTORIAL DE VENTAS");
        jPanel15.add(jLabel33, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 30, -1, -1));

        btnExportarVenta.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnExportarVenta.setText("EXPORTAR");
        btnExportarVenta.setBorder(null);
        btnExportarVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportarVentaActionPerformed(evt);
            }
        });
        jPanel15.add(btnExportarVenta, new org.netbeans.lib.awtextra.AbsoluteConstraints(850, 130, 110, 40));

        txtFechaFin.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtFechaFin.setBorder(null);
        jPanel15.add(txtFechaFin, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 130, 90, 30));

        txtFechaInicial.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtFechaInicial.setBorder(null);
        jPanel15.add(txtFechaInicial, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 130, 90, 30));

        jLabel42.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel42.setForeground(new java.awt.Color(255, 255, 255));
        jLabel42.setText("DD/MM/AA");
        jPanel15.add(jLabel42, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 100, 100, -1));

        txtBuscarFechaVenta.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtBuscarFechaVenta.setText("BUSCAR");
        txtBuscarFechaVenta.setBorder(null);
        txtBuscarFechaVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBuscarFechaVentaActionPerformed(evt);
            }
        });
        jPanel15.add(txtBuscarFechaVenta, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 130, 100, 30));

        jLabel43.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel43.setForeground(new java.awt.Color(255, 255, 255));
        jLabel43.setText("FECHA");
        jPanel15.add(jLabel43, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 130, -1, -1));

        jLabel44.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel44.setForeground(new java.awt.Color(255, 255, 255));
        jLabel44.setText("DD/MM/AA");
        jPanel15.add(jLabel44, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 100, 100, -1));

        javax.swing.GroupLayout HistorialVentaLayout = new javax.swing.GroupLayout(HistorialVenta);
        HistorialVenta.setLayout(HistorialVentaLayout);
        HistorialVentaLayout.setHorizontalGroup(
            HistorialVentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        HistorialVentaLayout.setVerticalGroup(
            HistorialVentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("HV", HistorialVenta);

        jPanel1.add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 40, 980, 680));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 1200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 749, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        jTabbedPane1.setSelectedIndex(0);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        jTabbedPane1.setSelectedIndex(4);
        configurarTablaCompra();
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
        configurarTablaCliente();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        Interfaz_AProducto open = new Interfaz_AProducto();
        open.setVisible(true);
        open.setLocationRelativeTo(null);
    }//GEN-LAST:event_jButton9ActionPerformed

    private void txtBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBuscarActionPerformed

    }//GEN-LAST:event_txtBuscarActionPerformed

    private void txtrefcodigoKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtrefcodigoKeyReleased
        String texto = txtrefcodigo.getText();
        txtrefcodigo.setText(texto.replaceAll("[^0-9]", ""));
    }//GEN-LAST:event_txtrefcodigoKeyReleased

    private void txtCantidadKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtCantidadKeyReleased
        validarCantidad();
    }//GEN-LAST:event_txtCantidadKeyReleased

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        agregarProductoATabla();
    }//GEN-LAST:event_jButton10ActionPerformed

    private void txtTotalVKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTotalVKeyReleased

    }//GEN-LAST:event_txtTotalVKeyReleased

    private void txtExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtExportarActionPerformed
        exportarAExcel();
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

    private void boxClienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxClienteActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_boxClienteActionPerformed

    private void btnRegistrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarActionPerformed
        abrirVentanaPago();
    }//GEN-LAST:event_btnRegistrarActionPerformed

    private void txtTotalVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTotalVActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTotalVActionPerformed

    private void txtCantidadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCantidadActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCantidadActionPerformed

    private void txtCedulaKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtCedulaKeyTyped
        char c = evt.getKeyChar();

        // Verificar si el carácter no es un número ni la tecla de retroceso
        if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
            evt.consume(); // Bloquea la tecla si no es un número
        }
    }//GEN-LAST:event_txtCedulaKeyTyped

    private void txtTelefonoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtTelefonoKeyTyped
        char c = evt.getKeyChar();

        // Verificar si el carácter no es un número ni la tecla de retroceso
        if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
            evt.consume(); // Bloquea la tecla si no es un número
        }
    }//GEN-LAST:event_txtTelefonoKeyTyped

    private void txtRefCodigoCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtRefCodigoCompraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtRefCodigoCompraActionPerformed

    private void txtDescripcionCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtDescripcionCompraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtDescripcionCompraActionPerformed

    private void btnAgregar1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregar1ActionPerformed
        AgregarCliente();
    }//GEN-LAST:event_btnAgregar1ActionPerformed

    private void txtPrecioCostoCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPrecioCostoCompraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtPrecioCostoCompraActionPerformed

    private void txtPrecioVentaCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPrecioVentaCompraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtPrecioVentaCompraActionPerformed

    private void btnAgregarCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarCompraActionPerformed
        String codigo = txtRefCodigoCompra.getText().trim();
        String descripcion = txtDescripcionCompra.getText().trim();
        String precioVenta = txtPrecioVentaCompra.getText().trim();
        String precioCosto = txtPrecioCostoCompra.getText().trim();
        String nombreProveedor = boxProveedorCompra.getSelectedItem().toString().trim();
        String cantidadStr = txtCantidadCompra.getText().trim();

        if (codigo.isEmpty() || descripcion.isEmpty() || precioVenta.isEmpty() || precioCosto.isEmpty() || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Completa todos los campos antes de agregar el producto.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int cantidad = Integer.parseInt(cantidadStr);
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(null, "La cantidad debe ser mayor a 0.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 🔹 Agregar el producto solo a la tabla visual, no a la base de datos
            DefaultTableModel model = (DefaultTableModel) TablaCompra.getModel();
            model.addRow(new Object[]{codigo, descripcion, cantidad, precioCosto, precioVenta, cantidad});

            JOptionPane.showMessageDialog(null, "Producto agregado a la lista. Finaliza la compra para registrarlo.", "Información", JOptionPane.INFORMATION_MESSAGE);

            // Limpiar campos después de agregar
            limpiarCampos();
            bloquearCampos();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Error: La cantidad debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnAgregarCompraActionPerformed

    private void btnFinalizarCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFinalizarCompraActionPerformed
        DefaultTableModel modelo = (DefaultTableModel) TablaCompra.getModel();
        if (modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "No hay productos en la lista. Agregue al menos uno antes de finalizar.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String proveedor = boxProveedorCompra.getSelectedItem().toString().trim();
            String fechaActual = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            conexion.setAutoCommit(false); // 🔹 INICIO TRANSACCIÓN

            // 🔹 1. OBTENER O REGISTRAR EL PROVEEDOR
            String sqlProveedor = "SELECT ID FROM Proveedor WHERE Proveedor = ?";
            PreparedStatement pstProveedor = conexion.prepareStatement(sqlProveedor);
            pstProveedor.setString(1, proveedor);
            ResultSet rsProveedor = pstProveedor.executeQuery();

            int idProveedor;
            if (rsProveedor.next()) {
                idProveedor = rsProveedor.getInt("ID");
            } else {
                String sqlInsertProveedor = "INSERT INTO Proveedor (Proveedor) VALUES (?)";
                PreparedStatement pstInsertProveedor = conexion.prepareStatement(sqlInsertProveedor, Statement.RETURN_GENERATED_KEYS);
                pstInsertProveedor.setString(1, proveedor);
                pstInsertProveedor.executeUpdate();

                ResultSet rsNuevoProveedor = pstInsertProveedor.getGeneratedKeys();
                if (!rsNuevoProveedor.next()) {
                    throw new SQLException("No se pudo registrar el nuevo proveedor.");
                }
                idProveedor = rsNuevoProveedor.getInt(1);
            }

            // 🔹 2. INSERTAR COMPRA
            String sqlCompra = "INSERT INTO Compra (ID_Proveedor, Fecha) VALUES (?, ?)";
            PreparedStatement pstCompra = conexion.prepareStatement(sqlCompra, Statement.RETURN_GENERATED_KEYS);
            pstCompra.setInt(1, idProveedor);
            pstCompra.setString(2, fechaActual);
            pstCompra.executeUpdate();

            ResultSet rsCompra = pstCompra.getGeneratedKeys();
            if (!rsCompra.next()) {
                throw new SQLException("No se pudo obtener el ID de la compra.");
            }
            int idCompra = rsCompra.getInt(1);

            // 🔹 3. INSERTAR PRODUCTOS EN `Detalle_Compra` Y ACTUALIZAR STOCK
            for (int i = 0; i < modelo.getRowCount(); i++) {
                String codigo = modelo.getValueAt(i, 0).toString();
                int cantidad = Integer.parseInt(modelo.getValueAt(i, 2).toString());
                double precioCosto = Double.parseDouble(modelo.getValueAt(i, 3).toString());

                // 🔹 INSERTAR PRODUCTO SI NO EXISTE
                String sqlProducto = "SELECT Codigo FROM Producto WHERE Codigo = ?";
                PreparedStatement pstProducto = conexion.prepareStatement(sqlProducto);
                pstProducto.setString(1, codigo);
                ResultSet rsProducto = pstProducto.executeQuery();

                if (!rsProducto.next()) {
                    // Si el producto no existe, lo insertamos
                    String descripcion = modelo.getValueAt(i, 1).toString();
                    double precioVenta = Double.parseDouble(modelo.getValueAt(i, 4).toString());

                    String sqlInsertProducto = "INSERT INTO Producto (Codigo, Descripcion, ID_Proveedor, Precio, Precio_Costo, Stock) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement pstInsertProducto = conexion.prepareStatement(sqlInsertProducto);
                    pstInsertProducto.setString(1, codigo);
                    pstInsertProducto.setString(2, descripcion);
                    pstInsertProducto.setInt(3, idProveedor);
                    pstInsertProducto.setDouble(4, precioVenta);
                    pstInsertProducto.setDouble(5, precioCosto);
                    pstInsertProducto.setInt(6, cantidad);
                    pstInsertProducto.executeUpdate();
                }

                // 🔹 INSERTAR EN DETALLE_COMPRA
                String sqlDetalle = "INSERT INTO Detalle_Compra (ID_Compra, ID_Producto, Cantidad, Precio_Costo) VALUES (?, ?, ?, ?)";
                PreparedStatement pstDetalle = conexion.prepareStatement(sqlDetalle);
                pstDetalle.setInt(1, idCompra);
                pstDetalle.setString(2, codigo); // Usamos el código como ID_Producto
                pstDetalle.setInt(3, cantidad);
                pstDetalle.setDouble(4, precioCosto);
                pstDetalle.executeUpdate();

                // 🔹 ACTUALIZAR STOCK
                String sqlUpdateStock = "UPDATE Producto SET Stock = Stock + ? WHERE Codigo = ?";
                PreparedStatement pstUpdateStock = conexion.prepareStatement(sqlUpdateStock);
                pstUpdateStock.setInt(1, cantidad);
                pstUpdateStock.setString(2, codigo);
                pstUpdateStock.executeUpdate();
            }

            conexion.commit();
            JOptionPane.showMessageDialog(null, "Compra registrada con éxito.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            modelo.setRowCount(0); // Limpiar la tabla en la interfaz
            obtenerUltimaFactura();

        } catch (SQLException e) {
            try {
                conexion.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(null, "Error al registrar la compra: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }//GEN-LAST:event_btnFinalizarCompraActionPerformed

    private void txtRefCodigoCompraKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRefCodigoCompraKeyReleased
        SwingUtilities.invokeLater(() -> {
            String texto = txtRefCodigoCompra.getText();
            txtRefCodigoCompra.setText(texto.replaceAll("[^0-9]", ""));
        });
    }//GEN-LAST:event_txtRefCodigoCompraKeyReleased

    private void txtRefCodigoCompraKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRefCodigoCompraKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) { // Detecta Enter correctamente
            String codigo = txtRefCodigoCompra.getText().trim();

            if (!codigo.isEmpty()) {
                CargarProductosCompra(codigo); // Pasa el código como argumento para asegurar que se evalúe correctamente
            } else {
                JOptionPane.showMessageDialog(this, "El campo Código está vacío.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_txtRefCodigoCompraKeyPressed

    private void txtRefCodigoCompraKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtRefCodigoCompraKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_txtRefCodigoCompraKeyTyped

    private void btnExportarVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarVentaActionPerformed
        exportarHistorialVenta();
    }//GEN-LAST:event_btnExportarVentaActionPerformed

    private void txtBuscarFechaVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBuscarFechaVentaActionPerformed
        buscarPorFecha();
    }//GEN-LAST:event_txtBuscarFechaVentaActionPerformed

    private void txtNFacturaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNFacturaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNFacturaActionPerformed

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
    private javax.swing.JPanel Cliente;
    private javax.swing.JPanel CompraInventario;
    private javax.swing.JPanel HistorialVenta;
    private javax.swing.JPanel Inventario;
    private javax.swing.JPanel ReporteCaja;
    private javax.swing.JTable TablaCliente;
    private javax.swing.JTable TablaCompra;
    private javax.swing.JTable TablaInventario;
    public javax.swing.JTable TablaVenta;
    private javax.swing.JTable TablaVentaHistorial;
    private javax.swing.JPanel Venta;
    public javax.swing.JComboBox<String> boxCliente;
    private javax.swing.JComboBox<String> boxProveedorCompra;
    private javax.swing.JButton btnAgregar;
    private javax.swing.JButton btnAgregar1;
    private javax.swing.JButton btnAgregarCompra;
    private javax.swing.JButton btnExportarVenta;
    private javax.swing.JButton btnFinalizarCompra;
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
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
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
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtBuscarCliente;
    private javax.swing.JButton txtBuscarFechaVenta;
    private javax.swing.JTextField txtCantidad;
    private javax.swing.JTextField txtCantidadCompra;
    private javax.swing.JTextField txtCedula;
    private javax.swing.JTextField txtDescripcionCompra;
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
    private javax.swing.JTextField txtFechaFin;
    private javax.swing.JTextField txtFechaInicial;
    private javax.swing.JTextField txtIngresoTotal;
    private javax.swing.JTextField txtNFactura;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtPrecioCostoCompra;
    private javax.swing.JTextField txtPrecioP;
    private javax.swing.JTextField txtPrecioVentaCompra;
    private javax.swing.JTextField txtRefCodigoCompra;
    private javax.swing.JTextField txtStockCompra;
    private javax.swing.JTextField txtStockP;
    private javax.swing.JTextField txtTelefono;
    private javax.swing.JTextField txtTotalV;
    private javax.swing.JTextField txtrefcodigo;
    // End of variables declaration//GEN-END:variables
}
