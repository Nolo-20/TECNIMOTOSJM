/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import Config.Conexion;
import Modelo.SessionManager;
import java.awt.event.KeyEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
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
    private String rol;

    public Interfaz_Almacen(String rol) {
        initComponents();
        this.rol = rol;
        configurarPermisos();
        setResizable(false);
        asignarCliente();
        obtenerUltimaFactura();
        cargarProveedores();
        DefaultTableModel modeloCompra = (DefaultTableModel) TablaCompra.getModel();
        txtCantidad.setEnabled(false);
        bloquearCampos();
        AutoCompleteDecorator.decorate(boxProveedorCompra);
        historialVenta();
        ReporteCaja();
        configurarPermisos();
    }

    public Interfaz_Almacen() {

    }

    private void configurarPermisos() {
        String rol = SessionManager.getRolUsuarioActual();
        txtRol.setText(rol);

        // Lista de componentes a bloquear para vendedores
        boolean esVendedor = "Vendedor".equals(rol);

        // Componentes principales a deshabilitar
        btnEliminarCliente.setEnabled(!esVendedor);

        // Tabla (bloquear edición directa)
        if (esVendedor) {
            TablaInventario.setDefaultEditor(Object.class, null);
            TablaCliente.setDefaultEditor(Object.class, null);
        }
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

        TableColumnModel columnModel = TablaInventario.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100);  // Código
        columnModel.getColumn(1).setPreferredWidth(300);  // Descripción
        columnModel.getColumn(2).setPreferredWidth(120);  // Precio Uni
        columnModel.getColumn(3).setPreferredWidth(120);  // Precio Cost
        columnModel.getColumn(4).setPreferredWidth(80);   // Stock
        columnModel.getColumn(5).setPreferredWidth(150);  // Proveedor

        sorter = new TableRowSorter<>(modelo);
        TablaInventario.setRowSorter(sorter);

        try {
            String query = "SELECT p.Codigo, p.Descripcion, p.Precio, p.Precio_Costo, p.Stock, pr.Proveedor "
                    + "FROM producto p INNER JOIN proveedor pr ON p.ID_Proveedor = pr.ID";
            ps = conexion.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getString("Codigo"),
                    rs.getString("Descripcion"),
                    rs.getDouble("Precio"),
                    rs.getDouble("Precio_Costo"),
                    rs.getInt("Stock"),
                    rs.getString("Proveedor")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al cargar inventario: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        CalcularTotalInventario();

        // **Detectar cambios y actualizar la base de datos**
        TablaInventario.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int fila = e.getFirstRow();
                    int columna = e.getColumn();
                    if (fila != -1 && columna != -1) {
                        actualizarProductoDesdeTabla(fila);
                    }
                }
            }
        });

        // **Filtrar en tiempo real**
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
                sorter.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + texto, 0, 1));
            }
        });
    }

    //REPORTE DE CAJA
    public void ReporteCaja() {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");

        // Obtener la fecha actual y la fecha de hace 15 días
        Calendar calendar = Calendar.getInstance();
        String fechaHasta = formato.format(calendar.getTime()); // Fecha actual

        calendar.add(Calendar.DAY_OF_MONTH, -15); // Restar 15 días
        String fechaDesde = formato.format(calendar.getTime());

        // Si el usuario selecciona fechas, se actualizan los valores
        if (dateDesde.getDate() != null) {
            fechaDesde = formato.format(dateDesde.getDate());
        }
        if (dateHasta.getDate() != null) {
            fechaHasta = formato.format(dateHasta.getDate());
        }

        // Mostrar fechas en consola para depuración
        System.out.println("Fecha Desde: " + fechaDesde);
        System.out.println("Fecha Hasta: " + fechaHasta);

        String sql = "SELECT Metodo_Pago, Total FROM ("
                + "SELECT mp.Pago AS Metodo_Pago, COALESCE(SUM(v.Total), 0.0) AS Total "
                + "FROM Metodo_Pago mp "
                + "LEFT JOIN Venta v ON mp.ID = v.ID_Pago "
                + "WHERE DATE(v.Fecha_Venta) BETWEEN ? AND ? "
                + "GROUP BY mp.Pago "
                + "UNION ALL "
                + "SELECT 'DEVOLUCIONES' AS Metodo_Pago, COALESCE(SUM(v.Cambio), 0.0) AS Total "
                + "FROM Venta v "
                + "JOIN Metodo_Pago mp ON v.ID_Pago = mp.ID "
                + "WHERE mp.Pago = 'EFECTIVO' AND DATE(v.Fecha_Venta) BETWEEN ? AND ? "
                + "UNION ALL "
                + "SELECT 'TOTAL' AS Metodo_Pago, COALESCE(SUM(v.Total), 0.0) AS Total "
                + "FROM Venta v "
                + "WHERE DATE(v.Fecha_Venta) BETWEEN ? AND ?) AS Totales";

        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

        try {
            ps = conexion.prepareStatement(sql);

            ps.setString(1, fechaDesde);
            ps.setString(2, fechaHasta);
            ps.setString(3, fechaDesde);
            ps.setString(4, fechaHasta);
            ps.setString(5, fechaDesde);
            ps.setString(6, fechaHasta);

            try {
                res = ps.executeQuery();

                // Inicializar con $0
                txtEfectivo.setText("$0");
                txtTarjeta.setText("$0");
                txtTransferencia.setText("$0");
                txtCuentaCorriente.setText("$0");
                txtDevoluciones.setText("$0");
                txtTotalCaja.setText("$0");

                // Verificar si hay resultados
                if (!res.isBeforeFirst()) {
                    System.out.println("No hay ventas en el rango de fechas seleccionado.");
                }

                while (res.next()) {
                    String metodo = res.getString("Metodo_Pago").trim();
                    double total = res.getDouble("Total");
                    String totalFormateado = formatoMoneda.format(total); // Formatear en moneda

                    switch (metodo) {
                        case "EFECTIVO":
                            txtEfectivo.setText(totalFormateado);
                            break;
                        case "TARJETA":
                            txtTarjeta.setText(totalFormateado);
                            break;
                        case "TRANSFERENCIA":
                            txtTransferencia.setText(totalFormateado);
                            break;
                        case "CUENTA CORRIENTE":
                            txtCuentaCorriente.setText(totalFormateado);
                            break;
                        case "DEVOLUCIONES":
                            txtDevoluciones.setText(totalFormateado);
                            break;
                        case "TOTAL":
                            txtTotalCaja.setText(totalFormateado);
                            break;
                        default:
                            System.out.println("Método de pago desconocido: " + metodo);
                            break;
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error en la consulta: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Forzar actualización de la UI
        txtEfectivo.repaint();
        txtTarjeta.repaint();
        txtTransferencia.repaint();
        txtCuentaCorriente.repaint();
        txtDevoluciones.repaint();
        txtTotalCaja.repaint();
    }

    public void exportarAExcel() {
        DefaultTableModel modelo = (DefaultTableModel) TablaInventario.getModel();
        Workbook libro = new XSSFWorkbook();
        Sheet hoja = libro.createSheet("Inventario");

        // Crear estilos para las celdas
        CellStyle estiloTexto = libro.createCellStyle();
        CellStyle estiloNumero = libro.createCellStyle();
        CellStyle estiloEncabezado = libro.createCellStyle();

        // Fuente en negrita para encabezados
        Font fuenteEncabezado = libro.createFont();
        fuenteEncabezado.setBold(true);
        estiloEncabezado.setFont(fuenteEncabezado);

        // Formato para números
        DataFormat formato = libro.createDataFormat();
        estiloNumero.setDataFormat(formato.getFormat("#,##0.00"));

        // Crear fila de encabezados con estilo
        Row filaEncabezados = hoja.createRow(0);
        for (int i = 0; i < modelo.getColumnCount(); i++) {
            Cell celda = filaEncabezados.createCell(i);
            celda.setCellValue(modelo.getColumnName(i));
            celda.setCellStyle(estiloEncabezado);
        }

        // Llenar la hoja con los datos de la tabla
        for (int i = 0; i < modelo.getRowCount(); i++) {
            Row fila = hoja.createRow(i + 1);
            for (int j = 0; j < modelo.getColumnCount(); j++) {
                Cell celda = fila.createCell(j);
                Object valor = modelo.getValueAt(i, j);

                if (valor != null) {
                    // Detectar tipo de dato y aplicar formato
                    if (valor instanceof Number) {
                        celda.setCellValue(((Number) valor).doubleValue());
                        celda.setCellStyle(estiloNumero);
                    } else if (valor instanceof java.util.Date) {
                        CellStyle estiloFecha = libro.createCellStyle();
                        estiloFecha.setDataFormat(libro.createDataFormat().getFormat("dd/MM/yyyy"));
                        celda.setCellValue((java.util.Date) valor);
                        celda.setCellStyle(estiloFecha);
                    } else {
                        celda.setCellValue(valor.toString());
                        celda.setCellStyle(estiloTexto);
                    }
                } else {
                    celda.setCellValue(""); // Celda vacía si el valor es nulo
                }
            }
        }

        // Ajustar automáticamente el ancho de las columnas
        for (int i = 0; i < modelo.getColumnCount(); i++) {
            hoja.autoSizeColumn(i);
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
                ps = conexion.prepareStatement(sql);
                ps.setString(1, codigo);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int stock = rs.getInt("Stock"); // Obtener el stock del producto

                    if (stock == 0) {
                        JOptionPane.showMessageDialog(null, "No hay stock de este producto.", "Sin Stock", JOptionPane.WARNING_MESSAGE);
                        txtrefcodigo.setText("");
                        return; // No cargar el producto
                    }

                    txtDescripcionP.setText(rs.getString("Descripcion"));
                    txtPrecioP.setText(rs.getString("Precio"));
                    txtStockP.setText(String.valueOf(stock));
                    txtCantidad.setEnabled(true);

                    if (stock == 1) {
                        JOptionPane.showMessageDialog(null, "¡Atención! Solo queda 1 en stock.", "Stock Bajo", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Producto no encontrado", "Error", JOptionPane.ERROR_MESSAGE);
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

        if (TablaVenta.getRowCount() == 0) { // Si no hay productos en la tabla
            txtTotalV.setText("$0.00");
            return total;
        }

        for (int i = 0; i < TablaVenta.getRowCount(); i++) {
            double precio = Double.parseDouble(TablaVenta.getValueAt(i, 3).toString()); // Columna 3 = Precio
            int cantidad = Integer.parseInt(TablaVenta.getValueAt(i, 2).toString()); // Columna 2 = Cantidad
            total += precio * cantidad;
        }

        // 🔹 No dividir por 100 aquí, el total ya está correcto
        DecimalFormat df = new DecimalFormat("#,##0.00");
        txtTotalV.setText("$" + df.format(total));

        return total;
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

                // 🔹 Agregamos "ID - Nombre" en el JComboBox
                boxCliente.addItem(id + " - " + nombre);
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

    //Cliente
    private void CargarClientes() {
        DefaultTableModel modelo = (DefaultTableModel) TablaCliente.getModel();
        modelo.setRowCount(0); // Limpiar tabla antes de cargar datos

        try {
            String sql = "SELECT Cedula, Nombre, Apellido, Telefono, Direccion FROM Cliente WHERE NOT id = 1";
            ps = conexion.prepareStatement(sql);
            res = ps.executeQuery();

            while (res.next()) {
                Object[] fila = {
                    res.getString("Cedula"),
                    res.getString("Nombre"),
                    res.getString("Apellido"),
                    res.getString("Telefono"),
                    res.getString("Direccion")
                };
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al cargar los clientes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void AgregarCliente() {
        String Cedula = txtCedula.getText().trim();
        String Nombre = txtNombre.getText().trim();
        String Apellido = txtApellido.getText().trim();
        String Telefono = txtTelefono.getText().trim();
        String Direccion = txtDireccion.getText().trim();

        if (Nombre.isEmpty() || Apellido.isEmpty() || Telefono.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Llenar los campos obligatorios", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            try {
                String sql = "INSERT INTO Cliente (Cedula, Nombre, Apellido, Telefono, Direccion) VALUES (?, ?, ?, ?, ?)";
                ps = conexion.prepareStatement(sql);
                ps.setString(1, Cedula);
                ps.setString(2, Nombre);
                ps.setString(3, Apellido);
                ps.setString(4, Telefono);
                ps.setString(5, Direccion);

                int res = ps.executeUpdate();
                if (res > 0) {
                    JOptionPane.showMessageDialog(null, "Cliente Agregado Correctamente", "Mensaje", JOptionPane.INFORMATION_MESSAGE);
                    CargarClientes(); // Actualiza la tabla después de agregar
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error en la base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void EliminarCliente() {
        int filaSeleccionada = TablaCliente.getSelectedRow();

        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione un cliente para eliminar", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String Cedula = TablaCliente.getValueAt(filaSeleccionada, 0).toString();

        int confirmacion = JOptionPane.showConfirmDialog(null, "¿Seguro que deseas eliminar este cliente?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                String sql = "DELETE FROM Cliente WHERE Cedula = ?";
                ps = conexion.prepareStatement(sql);
                ps.setString(1, Cedula);

                int res = ps.executeUpdate();
                if (res > 0) {
                    JOptionPane.showMessageDialog(null, "Cliente eliminado correctamente", "Mensaje", JOptionPane.INFORMATION_MESSAGE);
                    CargarClientes(); // Actualiza la tabla después de eliminar
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error en la base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void BuscarCliente() {
        String filtro = txtBuscar.getText().trim();
        DefaultTableModel modelo = (DefaultTableModel) TablaCliente.getModel();
        modelo.setRowCount(0); // Limpiar tabla antes de cargar datos

        try {
            String sql = "SELECT Cedula, Nombre, Apellido, Telefono, Direccion FROM Cliente WHERE "
                    + "Cedula LIKE ? OR Nombre LIKE ? OR Apellido LIKE ? OR Telefono LIKE ?";
            ps = conexion.prepareStatement(sql);
            ps.setString(1, "%" + filtro + "%");
            ps.setString(2, "%" + filtro + "%");
            ps.setString(3, "%" + filtro + "%");
            ps.setString(4, "%" + filtro + "%");

            res = ps.executeQuery();

            while (res.next()) {
                Object[] fila = {
                    res.getString("Cedula"),
                    res.getString("Nombre"),
                    res.getString("Apellido"),
                    res.getString("Telefono"),
                    res.getString("Direccion")
                };
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error en la búsqueda: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarCliente(int fila, int columna) {
        String CedulaAnterior = TablaCliente.getValueAt(fila, 0).toString();
        String Columna = TablaCliente.getColumnName(columna);
        String NuevoValor = TablaCliente.getValueAt(fila, columna).toString();

        try {
            // Mapeo de nombres de columnas de la tabla JTable a la base de datos
            String columnaBD = Columna;
            if (Columna.equals("Nombres")) {
                columnaBD = "Nombre";
            } else if (Columna.equals("Apellidos")) {
                columnaBD = "Apellido";
            } else if (Columna.equals("Nit/Cedula")) {
                columnaBD = "Cedula";
            }

            if (Columna.equalsIgnoreCase("Nit/Cedula")) {
                String NuevaCedula = NuevoValor;
                String Nombre = TablaCliente.getValueAt(fila, 1).toString();
                String Apellido = TablaCliente.getValueAt(fila, 2).toString();
                String Telefono = TablaCliente.getValueAt(fila, 3).toString();
                String Direccion = TablaCliente.getValueAt(fila, 4).toString();

                String sql = "UPDATE Cliente SET Cedula = ?, Nombre = ?, Apellido = ?, Telefono = ?, Direccion = ? WHERE Cedula = ?";
                ps = conexion.prepareStatement(sql);
                ps.setString(1, NuevaCedula);
                ps.setString(2, Nombre);
                ps.setString(3, Apellido);
                ps.setString(4, Telefono);
                ps.setString(5, Direccion);
                ps.setString(6, CedulaAnterior);
            } else {
                String sql = "UPDATE Cliente SET " + columnaBD + " = ? WHERE Cedula = ?";
                ps = conexion.prepareStatement(sql);
                ps.setString(1, NuevoValor);
                ps.setString(2, CedulaAnterior);
            }

            int res = ps.executeUpdate();
            if (res > 0) {
                String nombreCliente = TablaCliente.getValueAt(fila, 1).toString();
                JOptionPane.showMessageDialog(null, "Información del cliente '" + nombreCliente + "' actualizada correctamente.", "Actualización", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al actualizar cliente: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void abrirVentanaPago() {
        double total = calcularTotal();

        if (total <= 0) {
            JOptionPane.showMessageDialog(null, "No hay productos en la venta.");
            return;
        }

        // 🔹 Obtener el cliente seleccionado
        String clienteSeleccionado = (String) boxCliente.getSelectedItem();

        if (clienteSeleccionado == null) {
            JOptionPane.showMessageDialog(null, "No se seleccionó un cliente válido.");
            return;
        }

        // 🔹 Extraer el ID y el nombre del cliente
        String[] partesCliente = clienteSeleccionado.split(" - ", 2);
        int idCliente = Integer.parseInt(partesCliente[0]); // ID del cliente
        String nombreCliente = partesCliente[1]; // Nombre del cliente

        // 🔹 Obtener los productos de la tabla
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

        // 🔹 Formatear el total antes de enviarlo a la ventana de pago
        DecimalFormat df = new DecimalFormat("#,##0.00");
        String totalFormateado = df.format(total);

        // 🔹 Pasamos 'this' para poder limpiar la tabla desde Interfaz_Pago
        Interfaz_Pago ventanaPago = new Interfaz_Pago(this, total, idCliente, nombreCliente, productos);
        ventanaPago.setVisible(true);
        ventanaPago.setLocationRelativeTo(null);
    }

    public void limpiarTablaVenta() {
        DefaultTableModel modelo = (DefaultTableModel) TablaVenta.getModel();
        modelo.setRowCount(0); // Borra todas las filas de la tabla

        calcularTotal();
    }

    private void actualizarProductoDesdeTabla(int fila) {
        DefaultTableModel modelo = (DefaultTableModel) TablaInventario.getModel();

        String codigo = modelo.getValueAt(fila, 0).toString();
        String descripcion = modelo.getValueAt(fila, 1).toString();
        double precio = Double.parseDouble(modelo.getValueAt(fila, 2).toString());
        double precioCosto = Double.parseDouble(modelo.getValueAt(fila, 3).toString());
        int stock = Integer.parseInt(modelo.getValueAt(fila, 4).toString());
        String proveedor = modelo.getValueAt(fila, 5).toString();

        try {
            // **Obtener ID del proveedor**
            String queryProveedor = "SELECT ID FROM proveedor WHERE Proveedor = ?";
            PreparedStatement psProveedor = conexion.prepareStatement(queryProveedor);
            psProveedor.setString(1, proveedor);
            ResultSet rsProveedor = psProveedor.executeQuery();

            int idProveedor = -1;
            if (rsProveedor.next()) {
                idProveedor = rsProveedor.getInt("ID");
            } else {
                // **Si el proveedor no existe, insertarlo**
                String insertProveedor = "INSERT INTO proveedor(Proveedor) VALUES (?)";
                PreparedStatement psInsertProveedor = conexion.prepareStatement(insertProveedor, Statement.RETURN_GENERATED_KEYS);
                psInsertProveedor.setString(1, proveedor);
                psInsertProveedor.executeUpdate();

                ResultSet rsKeys = psInsertProveedor.getGeneratedKeys();
                if (rsKeys.next()) {
                    idProveedor = rsKeys.getInt(1);
                }
            }

            // **Actualizar el producto en la base de datos**
            String query = "UPDATE producto SET Descripcion=?, Precio=?, Precio_Costo=?, Stock=?, ID_Proveedor=? WHERE Codigo=?";
            ps = conexion.prepareStatement(query);
            ps.setString(1, descripcion);
            ps.setDouble(2, precio);
            ps.setDouble(3, precioCosto);
            ps.setInt(4, stock);
            ps.setInt(5, idProveedor);
            ps.setString(6, codigo);

            int resultado = ps.executeUpdate();
            if (resultado > 0) {
                JOptionPane.showMessageDialog(null, "Producto '" + descripcion + "' actualizado correctamente.", "Actualización", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Error al actualizar el producto", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error en la base de datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        CalcularTotalInventario(); // **Recalcular total tras actualización**
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
            ps = conexion.prepareStatement(sql);
            ps.setString(1, codigo);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Si el producto existe, carga los datos
                txtDescripcionCompra.setText(rs.getString("Descripcion"));
                txtPrecioVentaCompra.setText(rs.getString("Precio"));
                txtStockCompra.setText(rs.getString("Stock"));
                txtCantidadCompra.setEnabled(true);
                txtPrecioVentaCompra.setEnabled(true);
                txtPrecioCostoCompra.setEnabled(true);
                boxProveedorCompra.setEnabled(true); // Bloquea proveedor si ya existe
            } else {
                // Producto NO existe, preguntar si se quiere agregar
                int opcion = JOptionPane.showConfirmDialog(null,
                        "El producto no existe. ¿Desea agregarlo?",
                        "Producto no encontrado", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (opcion == JOptionPane.YES_OPTION) {
                    habilitarCamposParaNuevoProducto(); // Activa campos para agregarlo manualmente
                } else {
                    limpiarCampos(); // Limpia si el usuario no quiere agregarlo
                    cargarProveedores();
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
            ps = conexion.prepareStatement(query);
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

    private int obtenerIdProveedor(String proveedor) throws SQLException {
        int idProveedor = -1;

        String query = "SELECT ID FROM Proveedor WHERE Proveedor = ?";
        try (PreparedStatement psSelect = conexion.prepareStatement(query)) {
            psSelect.setString(1, proveedor);
            try (ResultSet resSelect = psSelect.executeQuery()) {
                if (resSelect.next()) {
                    return resSelect.getInt("ID");  // Devuelve el ID si ya existe
                }
            }
        }

        // Si el proveedor no existe, lo insertamos
        String insertProveedor = "INSERT INTO Proveedor (Proveedor) VALUES (?)";
        try (PreparedStatement psInsert = conexion.prepareStatement(insertProveedor, Statement.RETURN_GENERATED_KEYS)) {
            psInsert.setString(1, proveedor);
            psInsert.executeUpdate();
            try (ResultSet resInsert = psInsert.getGeneratedKeys()) {
                if (resInsert.next()) {
                    idProveedor = resInsert.getInt(1);  // Obtiene el ID generado
                }
            }
        }

        return idProveedor;  // Devuelve el ID del nuevo proveedor o -1 si hubo un error
    }

    private void configurarTablaCompra() {
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(new String[]{"Código", "Descripción", "Cantidad", "Precio Costo", "Precio Venta", "Stock", "Proveedor"});
        model.setRowCount(0);
        TablaCompra.setModel(model);
    }

    private void configurarTablaCliente() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true; // Permitir edición en todas las celdas
            }
        };

        model.setColumnIdentifiers(new String[]{"Nit/Cedula", "Nombres", "Apellidos", "Telefono", "Direccion"});
        model.setRowCount(0);
        TablaCliente.setModel(model);

        // Agregar listener para detectar cambios y actualizar en la base de datos
        TablaCliente.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int fila = e.getFirstRow();
                    int columna = e.getColumn();
                    if (fila != -1 && columna != -1) {
                        actualizarCliente(fila, columna);
                    }
                }
            }
        });
    }

    private void limpiarCampos() {
        txtRefCodigoCompra.setText("");
        txtDescripcionCompra.setText("");
        txtCantidadCompra.setText("");
        txtPrecioCostoCompra.setText("");
        txtPrecioVentaCompra.setText("");
        txtStockCompra.setText("");
        boxProveedorCompra.setEnabled(false);
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

    //historial venta
    public void historialVenta() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Obtener fechas de los JDateChooser
        String fechaInicio = (dateChooserInicio.getDate() != null) ? sdf.format(dateChooserInicio.getDate()) : "";
        String fechaFin = (dateChooserFin.getDate() != null) ? sdf.format(dateChooserFin.getDate()) : "";

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
                    rs.getDouble("Precio"),
                    rs.getInt("Cantidad"),
                    rs.getDouble("Total")
                };
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al cargar ventas: " + e.getMessage());
        }
    }

    public void buscarPorFecha() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // Formato SQL

        // Obtener fechas de los JDateChooser
        String fechaInicio = (dateChooserInicio.getDate() != null) ? sdf.format(dateChooserInicio.getDate()) : "";
        String fechaFin = (dateChooserFin.getDate() != null) ? sdf.format(dateChooserFin.getDate()) : "";

        if (fechaInicio.isEmpty() || fechaFin.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese ambas fechas");
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) TablaVentaHistorial.getModel();
        modelo.setRowCount(0); // Limpiar tabla

        String sql = "SELECT v.Fecha_Venta, c.Nombre AS Cliente, p.Descripcion AS Producto, f.Precio, f.Cantidad, (f.Precio * f.Cantidad) AS Total "
                + "FROM Venta v "
                + "JOIN Factura f ON v.ID = f.ID_Venta "
                + "JOIN Cliente c ON v.ID_Cliente = c.ID "
                + "JOIN Producto p ON f.ID_Producto = p.Codigo "
                + "WHERE DATE(v.Fecha_Venta) BETWEEN ? AND ? "
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

    //Historial Compra
    public void historialCompra() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Obtener fechas de los JDateChooser
        String fechaInicio = (dateChooserInicio2.getDate() != null) ? sdf.format(dateChooserInicio2.getDate()) : "";
        String fechaFin = (dateChooserFin2.getDate() != null) ? sdf.format(dateChooserFin2.getDate()) : "";

        DefaultTableModel modelo = (DefaultTableModel) TablaHistorialCompra.getModel();
        modelo.setColumnIdentifiers(new String[]{"Fecha de Compra", "Proveedor", "Producto", "Precio Costo", "Cantidad", "Total"});
        modelo.setRowCount(0); // Limpiar la tabla antes de agregar nuevos datos

        // Base de la consulta SQL
        String sql = "SELECT c.Fecha, p.Proveedor AS Proveedor, pr.Descripcion AS Producto, dc.Precio_Costo, dc.Cantidad, (dc.Precio_Costo * dc.Cantidad) AS Total "
                + "FROM Compra c "
                + "JOIN Detalle_Compra dc ON c.ID = dc.ID_Compra "
                + "JOIN Proveedor p ON c.ID_Proveedor = p.ID "
                + "JOIN Producto pr ON dc.ID_Producto = pr.Codigo ";

        // Agregar filtro de fecha si se ingresan valores
        if (!fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
            sql += "WHERE c.Fecha BETWEEN ? AND ? ";
        }

        sql += "ORDER BY c.Fecha DESC"; // Ordenar por fecha

        try {
            ps = conexion.prepareStatement(sql);
            if (!fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
                ps.setString(1, fechaInicio);
                ps.setString(2, fechaFin);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] fila = {
                    rs.getString("Fecha"),
                    rs.getString("Proveedor"),
                    rs.getString("Producto"),
                    rs.getDouble("Precio_Costo"),
                    rs.getInt("Cantidad"),
                    rs.getDouble("Total")
                };
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al cargar compras: " + e.getMessage());
        }
    }

// Función para exportar el historial de compras a Excel
    public class ExportadorExcelHistorial {

        public void exportarHistorialVenta(JTable TablaVentaHistorial) {
            exportarDatos(TablaVentaHistorial, "Historial de Ventas");
        }

        public void exportarHistorialCompra(JTable TablaHistorialCompra) {
            exportarDatos(TablaHistorialCompra, "Historial de Compras");
        }

        private void exportarDatos(JTable tabla, String nombreHoja) {
            DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
            Workbook libro = new XSSFWorkbook();
            Sheet hoja = libro.createSheet(nombreHoja);
            CellStyle estiloNumerico = libro.createCellStyle();
            estiloNumerico.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));

            CellStyle estiloFecha = libro.createCellStyle();
            estiloFecha.setDataFormat(libro.createDataFormat().getFormat("dd/MM/yyyy"));

            // Crear encabezado
            Row filaEncabezado = hoja.createRow(0);
            for (int i = 0; i < modelo.getColumnCount(); i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(modelo.getColumnName(i));
                CellStyle estiloEncabezado = libro.createCellStyle();
                Font font = libro.createFont();
                font.setBold(true);
                estiloEncabezado.setFont(font);
                celda.setCellStyle(estiloEncabezado);
            }

            // Llenar datos de la tabla
            for (int i = 0; i < modelo.getRowCount(); i++) {
                Row fila = hoja.createRow(i + 1);
                for (int j = 0; j < modelo.getColumnCount(); j++) {
                    Cell celda = fila.createCell(j);
                    Object valor = modelo.getValueAt(i, j);

                    if (valor instanceof Number) {
                        celda.setCellValue(((Number) valor).doubleValue());
                        celda.setCellStyle(estiloNumerico);
                    } else if (valor instanceof Date) {
                        celda.setCellValue((Date) valor);
                        celda.setCellStyle(estiloFecha);
                    } else {
                        celda.setCellValue(valor != null ? valor.toString() : "");
                    }
                }
            }

            // Ajustar automáticamente el tamaño de las columnas
            for (int i = 0; i < modelo.getColumnCount(); i++) {
                hoja.autoSizeColumn(i);
            }

            // Guardar archivo
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Guardar archivo Excel");
            int seleccionUsuario = fileChooser.showSaveDialog(null);
            if (seleccionUsuario == JFileChooser.APPROVE_OPTION) {
                try (FileOutputStream archivo = new FileOutputStream(fileChooser.getSelectedFile() + ".xlsx")) {
                    libro.write(archivo);
                    JOptionPane.showMessageDialog(null, "Datos exportados correctamente.");
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
    }

// Función para buscar compras por fecha
    public void buscarCompraPorFecha() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // Formato SQL

        // Obtener fechas de los JDateChooser
        String fechaInicio = (dateChooserInicio2.getDate() != null) ? sdf.format(dateChooserInicio2.getDate()) : "";
        String fechaFin = (dateChooserFin2.getDate() != null) ? sdf.format(dateChooserFin2.getDate()) : "";

        if (fechaInicio.isEmpty() || fechaFin.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Ingrese ambas fechas");
            return;
        }

        DefaultTableModel modelo = (DefaultTableModel) TablaHistorialCompra.getModel();
        modelo.setRowCount(0); // Limpiar la tabla antes de agregar nuevos datos

        String sql = "SELECT c.Fecha, p.Proveedor AS Proveedor, pr.Descripcion AS Producto, dc.Precio_Costo, dc.Cantidad, (dc.Precio_Costo * dc.Cantidad) AS Total "
                + "FROM Compra c "
                + "JOIN Detalle_Compra dc ON c.ID = dc.ID_Compra "
                + "JOIN Proveedor p ON c.ID_Proveedor = p.ID "
                + "JOIN Producto pr ON dc.ID_Producto = pr.Codigo "
                + "WHERE DATE(c.Fecha) BETWEEN ? AND ? "
                + "ORDER BY c.Fecha DESC";

        try {
            ps = conexion.prepareStatement(sql);
            ps.setString(1, fechaInicio);
            ps.setString(2, fechaFin);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] fila = {
                    rs.getString("Fecha"),
                    rs.getString("Proveedor"),
                    rs.getString("Producto"),
                    rs.getDouble("Precio_Costo"),
                    rs.getInt("Cantidad"),
                    rs.getDouble("Total")
                };
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al filtrar compras: " + e.getMessage());
        }
    }

    //calcular el total en el inventario
    private void CalcularTotalInventario() {
        double total = 0;
        NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.of("es", "CO")); // Formato de moneda colombiana

        for (int i = 0; i < TablaInventario.getRowCount(); i++) {
            try {
                // Asegurar que no hay valores nulos antes de convertir
                Object precioObj = TablaInventario.getValueAt(i, 2);
                Object cantidadObj = TablaInventario.getValueAt(i, 4);

                if (precioObj != null && cantidadObj != null) {
                    double precio = Double.parseDouble(precioObj.toString()); // Precio unitario
                    int cantidad = Integer.parseInt(cantidadObj.toString()); // Stock disponible

                    total += precio * cantidad;
                }
            } catch (NumberFormatException e) {
                System.out.println("Error al convertir valores en la fila " + i + ": " + e.getMessage());
            }
        }

        System.out.println("Total Inventario: " + formatoMoneda.format(total)); // Verificar en la consola

        // Asegurar que txtIngresoTotal no es nulo antes de actualizarlo
        if (txtIngresoTotal != null) {
            txtIngresoTotal.setText(formatoMoneda.format(total)); // Mostrar en formato moneda
        } else {
            System.out.println("txtIngresoTotal es NULL. Verifica su inicialización.");
        }
    }

    //Quitar un producto en la tabla compra
    private void QuitarProductoCompra() {
        DefaultTableModel modelo = (DefaultTableModel) TablaCompra.getModel();
        int filaSeleccionada = TablaCompra.getSelectedRow();

        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una fila para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        modelo.removeRow(filaSeleccionada);
    }

    // Quitar un producto en la Tabla Venta
    private void QuitarProductoVenta() {
        DefaultTableModel modelo = (DefaultTableModel) TablaVenta.getModel();
        int filaSeleccionada = TablaVenta.getSelectedRow();

        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(null, "Seleccione una fila para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Eliminar la fila seleccionada
        modelo.removeRow(filaSeleccionada);
    }

    //stock bajo en la tabla producto
    public void filtrarStockBajo() {
        DefaultTableModel modelo = (DefaultTableModel) TablaInventario.getModel();
        modelo.setRowCount(0); // Limpiar la tabla antes de agregar nuevos datos

        String sql = "SELECT p.Codigo, p.Descripcion, p.Precio, p.Precio_Costo, p.Stock, pr.Proveedor AS Proveedor "
                + "FROM Producto p "
                + "LEFT JOIN Proveedor pr ON p.ID_Proveedor = pr.ID "
                + "WHERE p.Stock <= 1";

        try {
            ps = conexion.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] fila = {
                    rs.getString("Codigo"),
                    rs.getString("Descripcion"),
                    rs.getDouble("Precio"),
                    rs.getDouble("Precio_Costo"),
                    rs.getInt("Stock"),
                    rs.getString("Proveedor")
                };
                modelo.addRow(fila);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al filtrar productos con bajo stock: " + e.getMessage());
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
        jButton7 = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        jLabel34 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtRol = new javax.swing.JTextField();
        btnSalir = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        ReporteCaja = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtTotalCaja = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtEfectivo = new javax.swing.JTextField();
        txtTarjeta = new javax.swing.JTextField();
        txtTransferencia = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        txtCuentaCorriente = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        txtDevoluciones = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        dateHasta = new com.toedter.calendar.JDateChooser();
        jLabel49 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        dateDesde = new com.toedter.calendar.JDateChooser();
        Inventario = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        TablaInventario = new javax.swing.JTable();
        txtBuscar = new javax.swing.JTextField();
        txtExportar = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        btnStock = new javax.swing.JButton();
        jLabel36 = new javax.swing.JLabel();
        txtIngresoTotal = new javax.swing.JTextField();
        btnActualizarInventario = new javax.swing.JButton();
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
        btnEliminarCliente = new javax.swing.JButton();
        btnAgregarCliente = new javax.swing.JButton();
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
        btnQuitarCompra = new javax.swing.JButton();
        btnFinalizarCompra = new javax.swing.JButton();
        HistorialVenta = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        TablaVentaHistorial = new javax.swing.JTable();
        jLabel33 = new javax.swing.JLabel();
        btnExportarVenta = new javax.swing.JButton();
        btnBuscarFechaVenta = new javax.swing.JButton();
        jLabel43 = new javax.swing.JLabel();
        btnActualizarHistorialVenta = new javax.swing.JButton();
        dateChooserInicio = new com.toedter.calendar.JDateChooser();
        dateChooserFin = new com.toedter.calendar.JDateChooser();
        HistorialCompra = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel35 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        TablaHistorialCompra = new javax.swing.JTable();
        jLabel45 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        btnBuscarFechaCompra = new javax.swing.JButton();
        btnExportarCompra = new javax.swing.JButton();
        btnActualizarHistorialCompra = new javax.swing.JButton();
        dateChooserFin2 = new com.toedter.calendar.JDateChooser();
        dateChooserInicio2 = new com.toedter.calendar.JDateChooser();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel4.setBackground(new java.awt.Color(50, 101, 255));

        jButton1.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/cajero-automatico.png"))); // NOI18N
        jButton1.setText("REPORTE CAJA");
        jButton1.setBorder(null);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/anadir-al-carrito.png"))); // NOI18N
        jButton2.setText("COMPRA");
        jButton2.setBorder(null);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/ventas.png"))); // NOI18N
        jButton3.setText("VENTA");
        jButton3.setBorder(null);
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/inventario.png"))); // NOI18N
        jButton4.setText("INVENTARIO");
        jButton4.setBorder(null);
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/beneficio-financiero.png"))); // NOI18N
        jButton5.setText("HISTORIAL VENTA");
        jButton5.setBorder(null);
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/nueva-cuenta.png"))); // NOI18N
        jButton6.setText("CLIENTE");
        jButton6.setBorder(null);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jLabel32.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/Tecnimotos.png"))); // NOI18N

        jButton7.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/historial-de-compras.png"))); // NOI18N
        jButton7.setText("HISTORIAL COMPRA");
        jButton7.setBorder(null);
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel32, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 162, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                .addGap(18, 18, 18)
                .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 200, 750));

        jPanel11.setBackground(new java.awt.Color(255, 255, 255));

        jLabel34.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel34.setText("SISTEMA DE FACTURACION E INVENTARIO TECNIMOTOSJM");

        jLabel2.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel2.setText("ROL: ");

        txtRol.setFont(new java.awt.Font("Roboto", 1, 18)); // NOI18N
        txtRol.setBorder(null);

        btnSalir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/cerrar-sesion.png"))); // NOI18N
        btnSalir.setBorder(null);
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(183, 183, 183)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtRol, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel34))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 171, Short.MAX_VALUE)
                .addComponent(btnSalir)
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnSalir, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(jLabel34)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtRol, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))))
                .addGap(12, 12, 12))
        );

        jPanel1.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, -30, 1080, 110));

        jPanel10.setBackground(new java.awt.Color(50, 101, 255));
        jPanel10.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel24.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(255, 255, 255));
        jLabel24.setText("REPORTE DE CAJA");
        jPanel10.add(jLabel24, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 30, -1, -1));

        jLabel5.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 51, 51));
        jLabel5.setText("TOTAL");
        jPanel10.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 430, -1, -1));

        txtTotalCaja.setEditable(false);
        txtTotalCaja.setBackground(new java.awt.Color(50, 101, 255));
        txtTotalCaja.setFont(new java.awt.Font("Roboto Black", 0, 24)); // NOI18N
        txtTotalCaja.setBorder(null);
        jPanel10.add(txtTotalCaja, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 430, 220, 30));

        jLabel6.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("EFECTIVO");
        jPanel10.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 130, -1, -1));

        jLabel7.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("TARJETA");
        jPanel10.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 190, -1, -1));

        jLabel8.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("TRANSFERENCIA");
        jPanel10.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 250, -1, -1));

        txtEfectivo.setEditable(false);
        txtEfectivo.setBackground(new java.awt.Color(50, 101, 255));
        txtEfectivo.setFont(new java.awt.Font("Roboto Black", 0, 24)); // NOI18N
        txtEfectivo.setBorder(null);
        jPanel10.add(txtEfectivo, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 130, 220, 30));

        txtTarjeta.setEditable(false);
        txtTarjeta.setBackground(new java.awt.Color(50, 101, 255));
        txtTarjeta.setFont(new java.awt.Font("Roboto Black", 0, 24)); // NOI18N
        txtTarjeta.setBorder(null);
        jPanel10.add(txtTarjeta, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 190, 220, 30));

        txtTransferencia.setEditable(false);
        txtTransferencia.setBackground(new java.awt.Color(50, 101, 255));
        txtTransferencia.setFont(new java.awt.Font("Roboto Black", 0, 24)); // NOI18N
        txtTransferencia.setBorder(null);
        jPanel10.add(txtTransferencia, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 250, 220, 30));

        jLabel9.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("CUENTA CORRIENTE");
        jPanel10.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 310, -1, -1));

        txtCuentaCorriente.setEditable(false);
        txtCuentaCorriente.setBackground(new java.awt.Color(50, 101, 255));
        txtCuentaCorriente.setFont(new java.awt.Font("Roboto Black", 0, 24)); // NOI18N
        txtCuentaCorriente.setBorder(null);
        jPanel10.add(txtCuentaCorriente, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 310, 220, 30));

        jLabel10.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(51, 255, 51));
        jLabel10.setText("DEVOLUCIONES");
        jPanel10.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 370, -1, -1));

        txtDevoluciones.setEditable(false);
        txtDevoluciones.setBackground(new java.awt.Color(50, 101, 255));
        txtDevoluciones.setFont(new java.awt.Font("Roboto Black", 0, 24)); // NOI18N
        txtDevoluciones.setBorder(null);
        jPanel10.add(txtDevoluciones, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 370, 220, 30));

        jPanel3.setBackground(new java.awt.Color(204, 204, 204));
        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Fecha"));

        dateHasta.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                dateHastaPropertyChange(evt);
            }
        });

        jLabel49.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel49.setText("DESDE");

        jLabel4.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel4.setText("HASTA");

        dateDesde.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                dateDesdePropertyChange(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(62, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(dateDesde, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(dateHasta, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(65, 65, 65)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(59, 59, 59)
                        .addComponent(jLabel49, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(48, 48, 48))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addComponent(jLabel49)
                .addGap(18, 18, 18)
                .addComponent(dateDesde, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(dateHasta, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(78, Short.MAX_VALUE))
        );

        jPanel10.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(660, 110, 380, 350));

        javax.swing.GroupLayout ReporteCajaLayout = new javax.swing.GroupLayout(ReporteCaja);
        ReporteCaja.setLayout(ReporteCajaLayout);
        ReporteCajaLayout.setHorizontalGroup(
            ReporteCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ReporteCajaLayout.createSequentialGroup()
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, 1078, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 2, Short.MAX_VALUE))
        );
        ReporteCajaLayout.setVerticalGroup(
            ReporteCajaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ReporteCajaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, 639, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("RC", ReporteCaja);

        jPanel5.setBackground(new java.awt.Color(50, 101, 255));
        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("INVENTARIO");
        jPanel5.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 20, -1, -1));

        jLabel3.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("BUSCAR:");
        jPanel5.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 100, -1, 30));

        TablaInventario.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
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

        jPanel5.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 160, 1060, 420));

        txtBuscar.setBorder(null);
        txtBuscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtBuscarActionPerformed(evt);
            }
        });
        jPanel5.add(txtBuscar, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 100, 310, 30));

        txtExportar.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        txtExportar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/sobresalir.png"))); // NOI18N
        txtExportar.setText("EXPORTAR");
        txtExportar.setBorder(null);
        txtExportar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtExportarActionPerformed(evt);
            }
        });
        jPanel5.add(txtExportar, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 90, 110, 40));

        jButton9.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/agregar-producto-inventario.png"))); // NOI18N
        jButton9.setText("AGREGAR");
        jButton9.setBorder(null);
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });
        jPanel5.add(jButton9, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 90, 110, 40));

        btnStock.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnStock.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/agotado.png"))); // NOI18N
        btnStock.setText("STOCK");
        btnStock.setBorder(null);
        btnStock.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStockActionPerformed(evt);
            }
        });
        jPanel5.add(btnStock, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 90, 110, 40));

        jLabel36.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel36.setForeground(new java.awt.Color(255, 255, 255));
        jLabel36.setText("TOTAL DE MERCANCIA:");
        jPanel5.add(jLabel36, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, 30));

        txtIngresoTotal.setEditable(false);
        txtIngresoTotal.setBackground(new java.awt.Color(50, 101, 255));
        txtIngresoTotal.setFont(new java.awt.Font("Roboto Black", 0, 18)); // NOI18N
        txtIngresoTotal.setForeground(new java.awt.Color(255, 255, 0));
        txtIngresoTotal.setBorder(null);
        txtIngresoTotal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtIngresoTotalActionPerformed(evt);
            }
        });
        jPanel5.add(txtIngresoTotal, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 600, 310, 30));

        btnActualizarInventario.setBackground(new java.awt.Color(50, 101, 255));
        btnActualizarInventario.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnActualizarInventario.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/actualizar.png"))); // NOI18N
        btnActualizarInventario.setBorder(null);
        btnActualizarInventario.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActualizarInventarioActionPerformed(evt);
            }
        });
        jPanel5.add(btnActualizarInventario, new org.netbeans.lib.awtextra.AbsoluteConstraints(1020, 90, 40, 40));

        javax.swing.GroupLayout InventarioLayout = new javax.swing.GroupLayout(Inventario);
        Inventario.setLayout(InventarioLayout);
        InventarioLayout.setHorizontalGroup(
            InventarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1080, Short.MAX_VALUE)
        );
        InventarioLayout.setVerticalGroup(
            InventarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        jButton10.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/anadir-al-carrito-venta.png"))); // NOI18N
        jButton10.setText("AGREGAR");
        jButton10.setBorder(null);
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });
        jPanel16.add(jButton10, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 110, 110, 50));

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

        jButton11.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        jButton11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/quitar-del-carrito.png"))); // NOI18N
        jButton11.setText("QUITAR");
        jButton11.setBorder(null);
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });
        jButton11.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jButton11KeyReleased(evt);
            }
        });
        jPanel16.add(jButton11, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 110, 110, 50));

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

        jPanel12.add(jPanel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 70, 940, 170));

        jLabel11.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 0));
        jLabel11.setText("TOTAL:");
        jPanel12.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 580, -1, -1));

        jLabel12.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("VENTA");
        jPanel12.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 20, -1, -1));

        TablaVenta.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
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
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(TablaVenta);

        jPanel12.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 250, 1040, 300));

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

        btnRegistrar.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnRegistrar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/comprando.png"))); // NOI18N
        btnRegistrar.setText("CONFIRMAR");
        btnRegistrar.setBorder(null);
        btnRegistrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarActionPerformed(evt);
            }
        });
        jPanel12.add(btnRegistrar, new org.netbeans.lib.awtextra.AbsoluteConstraints(930, 560, 120, 50));

        javax.swing.GroupLayout VentaLayout = new javax.swing.GroupLayout(Venta);
        Venta.setLayout(VentaLayout);
        VentaLayout.setHorizontalGroup(
            VentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, 1080, Short.MAX_VALUE)
        );
        VentaLayout.setVerticalGroup(
            VentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(VentaLayout.createSequentialGroup()
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, 646, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Venta", Venta);

        jPanel13.setBackground(new java.awt.Color(50, 101, 255));
        jPanel13.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel17.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(255, 255, 255));
        jLabel17.setText("CLIENTES");
        jPanel13.add(jLabel17, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 20, -1, -1));

        jLabel18.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(255, 255, 255));
        jLabel18.setText("BUSCAR:");
        jPanel13.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 110, -1, -1));

        TablaCliente.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
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

        jPanel13.add(jScrollPane3, new org.netbeans.lib.awtextra.AbsoluteConstraints(430, 150, 600, -1));

        txtBuscarCliente.setFont(new java.awt.Font("Roboto Black", 0, 18)); // NOI18N
        txtBuscarCliente.setBorder(null);
        txtBuscarCliente.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBuscarClienteKeyReleased(evt);
            }
        });
        jPanel13.add(txtBuscarCliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(540, 110, 340, 30));

        txtNombre.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtNombre.setBorder(null);
        jPanel13.add(txtNombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 220, 250, 20));

        jLabel19.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(255, 255, 255));
        jLabel19.setText("NOMBRES*");
        jPanel13.add(jLabel19, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 180, -1, -1));

        txtTelefono.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtTelefono.setBorder(null);
        txtTelefono.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtTelefonoKeyTyped(evt);
            }
        });
        jPanel13.add(txtTelefono, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 390, 250, 20));

        jLabel20.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(255, 255, 255));
        jLabel20.setText("TELEFONO*");
        jPanel13.add(jLabel20, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 350, -1, -1));

        txtApellido.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtApellido.setBorder(null);
        jPanel13.add(txtApellido, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 310, 250, 20));

        jLabel21.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(255, 255, 255));
        jLabel21.setText("APELLIDOS*");
        jPanel13.add(jLabel21, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 270, -1, -1));

        txtDireccion.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtDireccion.setBorder(null);
        jPanel13.add(txtDireccion, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 470, 250, 20));

        jLabel22.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(255, 255, 255));
        jLabel22.setText("DIRECCION");
        jPanel13.add(jLabel22, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 430, -1, -1));

        jLabel23.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(255, 255, 255));
        jLabel23.setText("NIT/CEDULA");
        jPanel13.add(jLabel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 100, -1, -1));

        txtCedula.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtCedula.setBorder(null);
        txtCedula.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtCedulaKeyTyped(evt);
            }
        });
        jPanel13.add(txtCedula, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 140, 250, 20));

        btnEliminarCliente.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnEliminarCliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/quitar-usuario.png"))); // NOI18N
        btnEliminarCliente.setText("ELIMINAR");
        btnEliminarCliente.setBorder(null);
        btnEliminarCliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarClienteActionPerformed(evt);
            }
        });
        jPanel13.add(btnEliminarCliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 510, 100, 50));

        btnAgregarCliente.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnAgregarCliente.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/agregar-usuario.png"))); // NOI18N
        btnAgregarCliente.setText("AGREGAR");
        btnAgregarCliente.setBorder(null);
        btnAgregarCliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarClienteActionPerformed(evt);
            }
        });
        jPanel13.add(btnAgregarCliente, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 510, 100, 50));

        javax.swing.GroupLayout ClienteLayout = new javax.swing.GroupLayout(Cliente);
        Cliente.setLayout(ClienteLayout);
        ClienteLayout.setHorizontalGroup(
            ClienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, 1080, Short.MAX_VALUE)
        );
        ClienteLayout.setVerticalGroup(
            ClienteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Comp", Cliente);

        jPanel14.setBackground(new java.awt.Color(50, 101, 255));
        jPanel14.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        TablaCompra.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
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
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane5.setViewportView(TablaCompra);

        jPanel14.add(jScrollPane5, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 100, 550, 460));

        jPanel17.setBackground(new java.awt.Color(153, 153, 153));
        jPanel17.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel25.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel25.setText("N FACTURA");
        jPanel17.add(jLabel25, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, -1, -1));

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
        jPanel14.add(jLabel26, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 20, -1, -1));

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
        jPanel18.add(jLabel27, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 10, -1, -1));

        txtCantidadCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtCantidadCompra.setBorder(null);
        txtCantidadCompra.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtCantidadCompraKeyTyped(evt);
            }
        });
        jPanel18.add(txtCantidadCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 40, 50, 20));

        jLabel28.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel28.setText("CANTIDAD");
        jPanel18.add(jLabel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 10, -1, -1));

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
        jPanel18.add(jLabel37, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 90, -1, -1));

        jLabel39.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel39.setText("PRECIO COSTO");
        jPanel18.add(jLabel39, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 180, -1, -1));

        txtPrecioCostoCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtPrecioCostoCompra.setBorder(null);
        txtPrecioCostoCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPrecioCostoCompraActionPerformed(evt);
            }
        });
        txtPrecioCostoCompra.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtPrecioCostoCompraKeyTyped(evt);
            }
        });
        jPanel18.add(txtPrecioCostoCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 210, 140, -1));

        txtStockCompra.setEditable(false);
        txtStockCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtStockCompra.setBorder(null);
        jPanel18.add(txtStockCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 40, 50, 20));

        jLabel38.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel38.setText("STOCK");
        jPanel18.add(jLabel38, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 10, -1, -1));

        txtPrecioVentaCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        txtPrecioVentaCompra.setBorder(null);
        txtPrecioVentaCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPrecioVentaCompraActionPerformed(evt);
            }
        });
        txtPrecioVentaCompra.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtPrecioVentaCompraKeyTyped(evt);
            }
        });
        jPanel18.add(txtPrecioVentaCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 280, 140, -1));

        jLabel40.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel40.setText("PRECIO VENTA");
        jPanel18.add(jLabel40, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 250, -1, -1));

        jLabel41.setFont(new java.awt.Font("Roboto Medium", 0, 18)); // NOI18N
        jLabel41.setText("PROVEEDOR");
        jPanel18.add(jLabel41, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 180, -1, -1));

        boxProveedorCompra.setEditable(true);
        boxProveedorCompra.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        boxProveedorCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxProveedorCompraActionPerformed(evt);
            }
        });
        jPanel18.add(boxProveedorCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(250, 210, 140, 30));

        btnAgregarCompra.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnAgregarCompra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/agregar-producto.png"))); // NOI18N
        btnAgregarCompra.setText("AGREGAR");
        btnAgregarCompra.setBorder(null);
        btnAgregarCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarCompraActionPerformed(evt);
            }
        });
        jPanel18.add(btnAgregarCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 270, 110, 40));

        jPanel14.add(jPanel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 240, 410, 320));

        btnQuitarCompra.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnQuitarCompra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/eliminar-producto.png"))); // NOI18N
        btnQuitarCompra.setText("QUITAR");
        btnQuitarCompra.setBorder(null);
        btnQuitarCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQuitarCompraActionPerformed(evt);
            }
        });
        jPanel14.add(btnQuitarCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 580, 110, 40));

        btnFinalizarCompra.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnFinalizarCompra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/cheque.png"))); // NOI18N
        btnFinalizarCompra.setText("FINALIZAR");
        btnFinalizarCompra.setBorder(null);
        btnFinalizarCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFinalizarCompraActionPerformed(evt);
            }
        });
        jPanel14.add(btnFinalizarCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(940, 580, 110, 40));

        javax.swing.GroupLayout CompraInventarioLayout = new javax.swing.GroupLayout(CompraInventario);
        CompraInventario.setLayout(CompraInventarioLayout);
        CompraInventarioLayout.setHorizontalGroup(
            CompraInventarioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, 1080, Short.MAX_VALUE)
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
        jPanel15.add(jLabel31, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 130, -1, -1));

        TablaVentaHistorial.setBorder(javax.swing.BorderFactory.createCompoundBorder());
        TablaVentaHistorial.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
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
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane4.setViewportView(TablaVentaHistorial);

        jPanel15.add(jScrollPane4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 190, 1040, -1));

        jLabel33.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel33.setForeground(new java.awt.Color(255, 255, 255));
        jLabel33.setText("HISTORIAL DE VENTAS");
        jPanel15.add(jLabel33, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 20, -1, -1));

        btnExportarVenta.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnExportarVenta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/sobresalir.png"))); // NOI18N
        btnExportarVenta.setText("EXPORTAR");
        btnExportarVenta.setBorder(null);
        btnExportarVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportarVentaActionPerformed(evt);
            }
        });
        jPanel15.add(btnExportarVenta, new org.netbeans.lib.awtextra.AbsoluteConstraints(950, 130, 110, 40));

        btnBuscarFechaVenta.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        btnBuscarFechaVenta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/lupa.png"))); // NOI18N
        btnBuscarFechaVenta.setText("BUSCAR");
        btnBuscarFechaVenta.setBorder(null);
        btnBuscarFechaVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarFechaVentaActionPerformed(evt);
            }
        });
        jPanel15.add(btnBuscarFechaVenta, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 120, 100, 40));

        jLabel43.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel43.setForeground(new java.awt.Color(255, 255, 255));
        jLabel43.setText("FECHA");
        jPanel15.add(jLabel43, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 130, -1, -1));

        btnActualizarHistorialVenta.setBackground(new java.awt.Color(50, 101, 255));
        btnActualizarHistorialVenta.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        btnActualizarHistorialVenta.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/actualizar.png"))); // NOI18N
        btnActualizarHistorialVenta.setBorder(null);
        btnActualizarHistorialVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActualizarHistorialVentaActionPerformed(evt);
            }
        });
        jPanel15.add(btnActualizarHistorialVenta, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 20, 50, 50));
        jPanel15.add(dateChooserInicio, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 130, 140, 30));
        jPanel15.add(dateChooserFin, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 130, 130, 30));

        javax.swing.GroupLayout HistorialVentaLayout = new javax.swing.GroupLayout(HistorialVenta);
        HistorialVenta.setLayout(HistorialVentaLayout);
        HistorialVentaLayout.setHorizontalGroup(
            HistorialVentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, 1080, Short.MAX_VALUE)
        );
        HistorialVentaLayout.setVerticalGroup(
            HistorialVentaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("HV", HistorialVenta);

        jPanel2.setBackground(new java.awt.Color(50, 101, 255));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel35.setFont(new java.awt.Font("Roboto Medium", 0, 36)); // NOI18N
        jLabel35.setForeground(new java.awt.Color(255, 255, 255));
        jLabel35.setText("HISTORIAL INGRESO DE MERCANCIA");
        jPanel2.add(jLabel35, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 20, -1, -1));

        TablaHistorialCompra.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        TablaHistorialCompra.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane6.setViewportView(TablaHistorialCompra);

        jPanel2.add(jScrollPane6, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 190, 1040, -1));

        jLabel45.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel45.setForeground(new java.awt.Color(255, 255, 255));
        jLabel45.setText("FECHA");
        jPanel2.add(jLabel45, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 130, -1, -1));

        jLabel47.setFont(new java.awt.Font("Roboto Medium", 0, 24)); // NOI18N
        jLabel47.setForeground(new java.awt.Color(255, 255, 255));
        jLabel47.setText("A");
        jPanel2.add(jLabel47, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 130, -1, -1));

        btnBuscarFechaCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        btnBuscarFechaCompra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/lupa.png"))); // NOI18N
        btnBuscarFechaCompra.setText("BUSCAR");
        btnBuscarFechaCompra.setBorder(null);
        btnBuscarFechaCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarFechaCompraActionPerformed(evt);
            }
        });
        jPanel2.add(btnBuscarFechaCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 120, 100, 40));

        btnExportarCompra.setFont(new java.awt.Font("Roboto Black", 0, 12)); // NOI18N
        btnExportarCompra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/sobresalir.png"))); // NOI18N
        btnExportarCompra.setText("EXPORTAR");
        btnExportarCompra.setBorder(null);
        btnExportarCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportarCompraActionPerformed(evt);
            }
        });
        jPanel2.add(btnExportarCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(950, 130, 110, 40));

        btnActualizarHistorialCompra.setBackground(new java.awt.Color(50, 101, 255));
        btnActualizarHistorialCompra.setFont(new java.awt.Font("Roboto Black", 0, 14)); // NOI18N
        btnActualizarHistorialCompra.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Imagenes/actualizar.png"))); // NOI18N
        btnActualizarHistorialCompra.setBorder(null);
        btnActualizarHistorialCompra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActualizarHistorialCompraActionPerformed(evt);
            }
        });
        jPanel2.add(btnActualizarHistorialCompra, new org.netbeans.lib.awtextra.AbsoluteConstraints(1010, 20, 50, 50));
        jPanel2.add(dateChooserFin2, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 130, 130, 30));
        jPanel2.add(dateChooserInicio2, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 130, 140, 30));

        javax.swing.GroupLayout HistorialCompraLayout = new javax.swing.GroupLayout(HistorialCompra);
        HistorialCompra.setLayout(HistorialCompraLayout);
        HistorialCompraLayout.setHorizontalGroup(
            HistorialCompraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 1080, Short.MAX_VALUE)
        );
        HistorialCompraLayout.setVerticalGroup(
            HistorialCompraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("tab7", HistorialCompra);

        jPanel1.add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(210, 40, 1080, 680));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1301, 749));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        jTabbedPane1.setSelectedIndex(0);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        jTabbedPane1.setSelectedIndex(4);
        configurarTablaCompra();
        cargarProveedores();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        jTabbedPane1.setSelectedIndex(2);
        asignarCliente();
        configurarTablaVenta();
        calcularTotal();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        jTabbedPane1.setSelectedIndex(1);
        CargarInventario();
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        jTabbedPane1.setSelectedIndex(5);
        dateChooserInicio.setDate(null);
        dateChooserFin.setDate(null);
        historialVenta();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        jTabbedPane1.setSelectedIndex(3);
        configurarTablaCliente();
        CargarClientes();
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

    private void btnAgregarClienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarClienteActionPerformed
        AgregarCliente();
    }//GEN-LAST:event_btnAgregarClienteActionPerformed

    private void txtPrecioCostoCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPrecioCostoCompraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtPrecioCostoCompraActionPerformed

    private void txtPrecioVentaCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPrecioVentaCompraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtPrecioVentaCompraActionPerformed

    private void btnAgregarCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarCompraActionPerformed
        DefaultTableModel model = (DefaultTableModel) TablaCompra.getModel();

        String codigo = txtRefCodigoCompra.getText().trim();
        String descripcion = txtDescripcionCompra.getText().trim();
        String cantidad = txtCantidadCompra.getText().trim();
        String precioCosto = txtPrecioCostoCompra.getText().trim();
        String precioVenta = txtPrecioVentaCompra.getText().trim();
        String stock = txtStockCompra.getText().trim();
        String proveedor = (String) boxProveedorCompra.getSelectedItem();

        if (codigo.isEmpty() || descripcion.isEmpty() || cantidad.isEmpty() || precioCosto.isEmpty() || precioVenta.isEmpty() || proveedor.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Todos los campos son obligatorios.");
            return;
        }

        model.addRow(new Object[]{codigo, descripcion, cantidad, precioCosto, precioVenta, stock, proveedor});
        limpiarCampos();
        bloquearCampos();
        cargarProveedores();
    }//GEN-LAST:event_btnAgregarCompraActionPerformed

    private void btnQuitarCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQuitarCompraActionPerformed
        QuitarProductoCompra();
    }//GEN-LAST:event_btnQuitarCompraActionPerformed

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
        ExportadorExcelHistorial ex = new ExportadorExcelHistorial();
        ex.exportarHistorialVenta(TablaVentaHistorial);
    }//GEN-LAST:event_btnExportarVentaActionPerformed

    private void btnBuscarFechaVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarFechaVentaActionPerformed
        buscarPorFecha();
    }//GEN-LAST:event_btnBuscarFechaVentaActionPerformed

    private void txtNFacturaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNFacturaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNFacturaActionPerformed

    private void btnFinalizarCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFinalizarCompraActionPerformed
        try {
            conexion.setAutoCommit(false);

            DefaultTableModel model = (DefaultTableModel) TablaCompra.getModel();
            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(null, "No hay productos en la compra.");
                return;
            }

            String nombreProveedor = model.getValueAt(0, 6).toString();
            int idProveedor = obtenerIdProveedor(nombreProveedor);

            // Insertar Compra
            String insertCompra = "INSERT INTO Compra (ID_Proveedor, Fecha) VALUES (?, NOW())";
            ps = conexion.prepareStatement(insertCompra, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, idProveedor);
            ps.executeUpdate();

            res = ps.getGeneratedKeys();
            int idCompra = res.next() ? res.getInt(1) : 0;

            for (int i = 0; i < model.getRowCount(); i++) {
                String codigo = model.getValueAt(i, 0).toString();
                String descripcion = model.getValueAt(i, 1).toString();
                int cantidad = Integer.parseInt(model.getValueAt(i, 2).toString());
                double precioCosto = Double.parseDouble(model.getValueAt(i, 3).toString());
                double precioVenta = Double.parseDouble(model.getValueAt(i, 4).toString());

                // Verificar si el producto existe en la tabla Producto
                String verificarProducto = "SELECT COUNT(*) FROM Producto WHERE Codigo = ?";
                ps = conexion.prepareStatement(verificarProducto);
                ps.setString(1, codigo);
                res = ps.executeQuery();
                res.next();
                int existe = res.getInt(1);

                // Si no existe, insertar el producto
                if (existe == 0) {
                    String insertProducto = "INSERT INTO Producto (Codigo, Descripcion, Precio_Costo, Precio, Stock, ID_Proveedor) VALUES (?, ?, ?, ?, ?, ?)";
                    ps = conexion.prepareStatement(insertProducto);
                    ps.setString(1, codigo);
                    ps.setString(2, descripcion);
                    ps.setDouble(3, precioCosto);
                    ps.setDouble(4, precioVenta);
                    ps.setInt(5, cantidad);
                    ps.setInt(6, idProveedor);
                    ps.executeUpdate();
                } else {
                    // Si el producto ya existe, actualizar Stock y Precio de Venta
                    String updateProducto = "UPDATE Producto SET Stock = Stock + ?, Precio = ? WHERE Codigo = ?";
                    ps = conexion.prepareStatement(updateProducto);
                    ps.setInt(1, cantidad);
                    ps.setDouble(2, precioVenta); // Se actualiza el precio de venta
                    ps.setString(3, codigo);
                    ps.executeUpdate();
                }

                // Insertar Detalle_Compra
                String insertDetalle = "INSERT INTO Detalle_Compra (ID_Compra, ID_Producto, Cantidad, Precio_Costo) VALUES (?, ?, ?, ?)";
                ps = conexion.prepareStatement(insertDetalle);
                ps.setInt(1, idCompra);
                ps.setString(2, codigo);
                ps.setInt(3, cantidad);
                ps.setDouble(4, precioCosto);
                ps.executeUpdate();
            }

            conexion.commit();
            JOptionPane.showMessageDialog(null, "Compra registrada exitosamente.");
            configurarTablaCompra();
            obtenerUltimaFactura();
            cargarProveedores();
        } catch (SQLException e) {
            try {
                conexion.rollback();
            } catch (SQLException ex) {
            }
            JOptionPane.showMessageDialog(null, "Error al finalizar compra: " + e.getMessage());
        } finally {
            try {
                conexion.setAutoCommit(true);
            } catch (SQLException e) {
            }
        }
    }//GEN-LAST:event_btnFinalizarCompraActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        QuitarProductoVenta();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void btnActualizarHistorialVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActualizarHistorialVentaActionPerformed
        dateChooserInicio.setDate(null);
        dateChooserFin.setDate(null);
        historialVenta();
    }//GEN-LAST:event_btnActualizarHistorialVentaActionPerformed

    private void btnActualizarInventarioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActualizarInventarioActionPerformed
        CargarInventario();
    }//GEN-LAST:event_btnActualizarInventarioActionPerformed

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
        Interfaz_Login login = new Interfaz_Login();
        login.setVisible(true);
        login.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_btnSalirActionPerformed

    private void txtCantidadCompraKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtCantidadCompraKeyTyped
        char c = evt.getKeyChar();

        // Verificar si el carácter no es un número ni la tecla de retroceso
        if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
            evt.consume(); // Bloquea la tecla si no es un número
        }
    }//GEN-LAST:event_txtCantidadCompraKeyTyped

    private void txtPrecioCostoCompraKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPrecioCostoCompraKeyTyped
        char c = evt.getKeyChar();

        // Verificar si el carácter no es un número ni la tecla de retroceso
        if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
            evt.consume(); // Bloquea la tecla si no es un número
        }
    }//GEN-LAST:event_txtPrecioCostoCompraKeyTyped

    private void txtPrecioVentaCompraKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtPrecioVentaCompraKeyTyped
        char c = evt.getKeyChar();

        // Verificar si el carácter no es un número ni la tecla de retroceso
        if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
            evt.consume(); // Bloquea la tecla si no es un número
        }
    }//GEN-LAST:event_txtPrecioVentaCompraKeyTyped

    private void btnStockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStockActionPerformed
        filtrarStockBajo();
    }//GEN-LAST:event_btnStockActionPerformed

    private void txtBuscarClienteKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBuscarClienteKeyReleased
        BuscarCliente();
    }//GEN-LAST:event_txtBuscarClienteKeyReleased

    private void btnEliminarClienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarClienteActionPerformed
        EliminarCliente();
    }//GEN-LAST:event_btnEliminarClienteActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        jTabbedPane1.setSelectedIndex(6);
        dateChooserInicio2.setDate(null);
        dateChooserFin2.setDate(null);
        historialCompra();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void btnBuscarFechaCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarFechaCompraActionPerformed
        buscarCompraPorFecha();
    }//GEN-LAST:event_btnBuscarFechaCompraActionPerformed

    private void btnExportarCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarCompraActionPerformed
        ExportadorExcelHistorial ex = new ExportadorExcelHistorial();
        ex.exportarHistorialCompra(TablaHistorialCompra);
    }//GEN-LAST:event_btnExportarCompraActionPerformed

    private void btnActualizarHistorialCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActualizarHistorialCompraActionPerformed
        dateChooserInicio2.setDate(null);
        dateChooserFin2.setDate(null);
        historialCompra();
    }//GEN-LAST:event_btnActualizarHistorialCompraActionPerformed

    private void jButton11KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jButton11KeyReleased
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton11KeyReleased

    private void dateDesdePropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_dateDesdePropertyChange
        if ("date".equals(evt.getPropertyName())) {
            ReporteCaja();
        }
    }//GEN-LAST:event_dateDesdePropertyChange

    private void dateHastaPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_dateHastaPropertyChange
        if ("date".equals(evt.getPropertyName())) {
            ReporteCaja();
        }
    }//GEN-LAST:event_dateHastaPropertyChange

    private void boxProveedorCompraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxProveedorCompraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_boxProveedorCompraActionPerformed

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
    private javax.swing.JPanel HistorialCompra;
    private javax.swing.JPanel HistorialVenta;
    private javax.swing.JPanel Inventario;
    private javax.swing.JPanel ReporteCaja;
    private javax.swing.JTable TablaCliente;
    private javax.swing.JTable TablaCompra;
    private javax.swing.JTable TablaHistorialCompra;
    private javax.swing.JTable TablaInventario;
    public javax.swing.JTable TablaVenta;
    private javax.swing.JTable TablaVentaHistorial;
    private javax.swing.JPanel Venta;
    public javax.swing.JComboBox<String> boxCliente;
    private javax.swing.JComboBox<String> boxProveedorCompra;
    private javax.swing.JButton btnActualizarHistorialCompra;
    private javax.swing.JButton btnActualizarHistorialVenta;
    private javax.swing.JButton btnActualizarInventario;
    private javax.swing.JButton btnAgregarCliente;
    private javax.swing.JButton btnAgregarCompra;
    private javax.swing.JButton btnBuscarFechaCompra;
    private javax.swing.JButton btnBuscarFechaVenta;
    private javax.swing.JButton btnEliminarCliente;
    private javax.swing.JButton btnExportarCompra;
    private javax.swing.JButton btnExportarVenta;
    private javax.swing.JButton btnFinalizarCompra;
    private javax.swing.JButton btnQuitarCompra;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JButton btnSalir;
    private javax.swing.JButton btnStock;
    private com.toedter.calendar.JDateChooser dateChooserFin;
    private com.toedter.calendar.JDateChooser dateChooserFin2;
    private com.toedter.calendar.JDateChooser dateChooserInicio;
    private com.toedter.calendar.JDateChooser dateChooserInicio2;
    private com.toedter.calendar.JDateChooser dateDesde;
    private com.toedter.calendar.JDateChooser dateHasta;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
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
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel49;
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
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField txtApellido;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtBuscarCliente;
    private javax.swing.JTextField txtCantidad;
    private javax.swing.JTextField txtCantidadCompra;
    private javax.swing.JTextField txtCedula;
    private javax.swing.JTextField txtCuentaCorriente;
    private javax.swing.JTextField txtDescripcionCompra;
    private javax.swing.JTextField txtDescripcionP;
    private javax.swing.JTextField txtDevoluciones;
    private javax.swing.JTextField txtDireccion;
    private javax.swing.JTextField txtEfectivo;
    private javax.swing.JButton txtExportar;
    private javax.swing.JTextField txtIngresoTotal;
    private javax.swing.JTextField txtNFactura;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtPrecioCostoCompra;
    private javax.swing.JTextField txtPrecioP;
    private javax.swing.JTextField txtPrecioVentaCompra;
    private javax.swing.JTextField txtRefCodigoCompra;
    private javax.swing.JTextField txtRol;
    private javax.swing.JTextField txtStockCompra;
    private javax.swing.JTextField txtStockP;
    private javax.swing.JTextField txtTarjeta;
    private javax.swing.JTextField txtTelefono;
    private javax.swing.JTextField txtTotalCaja;
    private javax.swing.JTextField txtTotalV;
    private javax.swing.JTextField txtTransferencia;
    private javax.swing.JTextField txtrefcodigo;
    // End of variables declaration//GEN-END:variables
}
