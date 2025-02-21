/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Utilidades;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

/**
 *
 * @author Juanm
 */
import com.itextpdf.text.*;
import java.awt.Desktop;
import java.io.*;
import java.nio.file.Paths;
import java.sql.*;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;

public class Ticket {

     private static final String RUTA_PDF = Paths.get(System.getProperty("user.home"), "Downloads").toString() + "\\";
    private static final String LOGO_PATH = "src/imagenes/Tecnimotos-logo-grande.png";
    private static final String DB_URL = "jdbc:mysql://127.0.0.1/tecnimotosjm";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public void generarTicketPDF(int idVenta, String usuarioActual) {
        String rutaPDF = RUTA_PDF + "Ticket_" + idVenta + ".pdf";

        try (Connection con = getConnection()) {
            Document documento = new Document(new Rectangle(226, 800));
            PdfWriter.getInstance(documento, new FileOutputStream(rutaPDF));
            documento.open();

            agregarLogo(documento);
            agregarDatosEmpresa(documento);
            documento.add(new Paragraph("--------------------------------------------------\n", new Font(Font.FontFamily.HELVETICA, 8)));
            agregarDatosFactura(documento, con, idVenta, usuarioActual);
            documento.add(new Paragraph("--------------------------------------------------\n", new Font(Font.FontFamily.HELVETICA, 8)));
            agregarDetalleProductos(documento, con, idVenta);
            documento.add(new Paragraph("--------------------------------------------------\n", new Font(Font.FontFamily.HELVETICA, 8)));
            agregarTotales(documento, con, idVenta);
            documento.add(new Paragraph("--------------------------------------------------\nGracias por su compra\n", new Font(Font.FontFamily.HELVETICA, 8, Font.BOLDITALIC)));

            documento.close();
            abrirPDF(rutaPDF);
            imprimirPDF(rutaPDF);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private void agregarLogo(Document documento) {
        try {
            Image logo = Image.getInstance(LOGO_PATH);
            logo.scaleToFit(260, 200);
            logo.setAlignment(Element.ALIGN_CENTER);
            documento.add(logo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void agregarDatosEmpresa(Document documento) throws DocumentException {
        Paragraph empresa = new Paragraph(
                "TECNIMOTOS JM\nNIT: 79828023-1\nNO RESPONSABLE DE IVA\nDG 58 SUR # 29A 22\nCEL: 3209160538\n",
                new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD));
        empresa.setAlignment(Element.ALIGN_CENTER);
        documento.add(empresa);
    }

    private void agregarDatosFactura(Document documento, Connection con, int idVenta, String usuarioActual) throws SQLException, DocumentException {
        String sql = "SELECT v.Fecha_Venta, CONCAT(c.Nombre, ' ', c.Apellido), c.Telefono, c.Cedula, c.Direccion, v.Total "
                + "FROM Venta v "
                + "JOIN Cliente c ON v.ID_Cliente = c.ID "
                + "WHERE v.ID = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PdfPTable tablaDatos = new PdfPTable(2);
                    tablaDatos.setWidthPercentage(100);
                    tablaDatos.setWidths(new float[]{40, 60});

                    tablaDatos.addCell(getCell("Fecha:", Font.BOLD, Element.ALIGN_LEFT));
                    tablaDatos.addCell(getCell(rs.getString(1), Font.NORMAL, Element.ALIGN_RIGHT));

                    tablaDatos.addCell(getCell("Cliente:", Font.BOLD, Element.ALIGN_LEFT));
                    tablaDatos.addCell(getCell(rs.getString(2), Font.NORMAL, Element.ALIGN_RIGHT));

                    tablaDatos.addCell(getCell("Celular:", Font.BOLD, Element.ALIGN_LEFT));
                    tablaDatos.addCell(getCell(rs.getString(3), Font.NORMAL, Element.ALIGN_RIGHT));

                    tablaDatos.addCell(getCell("CC/NIT:", Font.BOLD, Element.ALIGN_LEFT));
                    tablaDatos.addCell(getCell(rs.getString(4), Font.NORMAL, Element.ALIGN_RIGHT));

                    tablaDatos.addCell(getCell("Dirección:", Font.BOLD, Element.ALIGN_LEFT));
                    tablaDatos.addCell(getCell(rs.getString(5), Font.NORMAL, Element.ALIGN_RIGHT));

                    tablaDatos.addCell(getCell("Vendedor:", Font.BOLD, Element.ALIGN_LEFT));
                    tablaDatos.addCell(getCell(usuarioActual, Font.NORMAL, Element.ALIGN_RIGHT));

                    documento.add(tablaDatos);
                }
            }
        }
    }

    private void agregarDetalleProductos(Document documento, Connection con, int idVenta) throws SQLException, DocumentException {
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{50, 15, 20, 25});

        tabla.addCell(getCell("DETALLE", Font.BOLD, Element.ALIGN_LEFT));
        tabla.addCell(getCell("CANT", Font.BOLD, Element.ALIGN_CENTER));
        tabla.addCell(getCell("UNIT", Font.BOLD, Element.ALIGN_RIGHT));
        tabla.addCell(getCell("TOTAL", Font.BOLD, Element.ALIGN_RIGHT));

        String sql = "SELECT p.Descripcion, f.Cantidad, p.Precio, (f.Cantidad * p.Precio) "
                + "FROM Factura f "
                + "JOIN Producto p ON f.ID_Producto = p.Codigo "
                + "WHERE f.ID_Venta = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tabla.addCell(getCell(rs.getString(1), Font.NORMAL, Element.ALIGN_LEFT));
                    tabla.addCell(getCell(rs.getString(2), Font.NORMAL, Element.ALIGN_CENTER));
                    tabla.addCell(getCell("$ " + String.format("%.2f", rs.getDouble(3)), Font.NORMAL, Element.ALIGN_RIGHT));
                    tabla.addCell(getCell("$ " + String.format("%.2f", rs.getDouble(4)), Font.NORMAL, Element.ALIGN_RIGHT));
                }
            }
        }

        documento.add(tabla);
        documento.add(new Paragraph("\n"));
    }

    private void agregarTotales(Document documento, Connection con, int idVenta) throws SQLException, DocumentException {
        // Consulta para obtener el total y el método de pago
        String sql = "SELECT v.Total, m.Pago FROM Venta v "
                + "JOIN Metodo_Pago m ON v.ID_Pago = m.ID "
                + "WHERE v.ID = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double total = rs.getDouble(1);
                    String metodoPago = rs.getString(2); // Obtener el nombre del método de pago

                    Paragraph totales = new Paragraph(
                            "TOTAL: $" + total + "\nForma de Pago: " + metodoPago,
                            new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD));
                    totales.setAlignment(Element.ALIGN_RIGHT);
                    documento.add(totales);
                }
            }
        }
    }

    private PdfPCell getCell(String texto, int estilo, int alineacion) {
        Font fuente = new Font(Font.FontFamily.HELVETICA, 7, estilo);
        PdfPCell cell = new PdfPCell(new Phrase(texto, fuente));
        cell.setHorizontalAlignment(alineacion);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private void abrirPDF(String rutaPDF) {
        try {
            File file = new File(rutaPDF);
            if (file.exists()) {
                Desktop.getDesktop().open(file); // Abre el PDF en el visor predeterminado
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void imprimirPDF(String rutaPDF) {
        try {
            FileInputStream fis = new FileInputStream(rutaPDF);
            Doc pdfDoc = new SimpleDoc(fis, DocFlavor.INPUT_STREAM.AUTOSENSE, null);

            // Buscar la impresora predeterminada
            PrintService service = PrintServiceLookup.lookupDefaultPrintService();

            if (service != null) {
                DocPrintJob job = service.createPrintJob();
                job.print(pdfDoc, new HashPrintRequestAttributeSet());
            } else {
                System.out.println("No se encontró una impresora predeterminada.");
            }
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
