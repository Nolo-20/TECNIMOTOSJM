/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controladores;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
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
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;

public class Ticket {

    private static final String RUTA_PDF = "C:\\Users\\Juanm\\Downloads\\";
    private static final String LOGO_PATH = "src/imagenes/Tecnimotos-logo-grande.png";
    private static final String DB_URL = "jdbc:mysql://127.0.0.1/tecnimotosjm";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public void generarTicketPDF(int idVenta) {
        String rutaPDF = RUTA_PDF + "Ticket_" + idVenta + ".pdf";

        try (Connection con = getConnection()) {
            Document documento = new Document(PageSize.A7);
            PdfWriter.getInstance(documento, new FileOutputStream(rutaPDF));
            documento.open();

            agregarLogo(documento);
            agregarDatosEmpresa(documento);
            documento.add(new Paragraph("\n"));
            agregarDatosFactura(documento, con, idVenta);
            documento.add(new Paragraph("\n"));
            agregarDetalleProductos(documento, con, idVenta);
            documento.close();

            abrirPDF(rutaPDF);
            imprimirPDF(rutaPDF); // Imprime el ticket automáticamente

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
            logo.scaleToFit(120, 80);  // Aumentar tamaño del logo
            logo.setAlignment(Element.ALIGN_CENTER);
            documento.add(logo);
            documento.add(new Paragraph("\n"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void agregarDatosEmpresa(Document documento) throws DocumentException {
        Paragraph empresa = new Paragraph(
                "NIT: 123456789\nNo responsable de IVA\nDirección: Calle 123\nTeléfono: 555-1234\n\n",
                new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD));
        empresa.setAlignment(Element.ALIGN_CENTER);
        documento.add(empresa);
    }

    private void agregarDatosFactura(Document documento, Connection con, int idVenta) throws SQLException, DocumentException {
        String sql = "SELECT v.Fecha_Venta, CONCAT(c.Nombre, ' ', c.Apellido), v.Total, v.Descuento, v.Cambio "
                + "FROM Venta v "
                + "JOIN Cliente c ON v.ID_Cliente = c.ID "
                + "WHERE v.ID = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Paragraph datosFactura = new Paragraph(
                            "Factura #" + idVenta + "\nFecha: " + rs.getString(1)
                            + "\nCliente: " + rs.getString(2) + "\n",
                            new Font(Font.FontFamily.HELVETICA, 10));
                    datosFactura.setAlignment(Element.ALIGN_LEFT);
                    documento.add(datosFactura);
                }
            }
        }
    }

    private void agregarDetalleProductos(Document documento, Connection con, int idVenta) throws SQLException, DocumentException {
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{50, 15, 20, 25});

        tabla.addCell(getCell("Descripción", Font.BOLD, Element.ALIGN_LEFT));
        tabla.addCell(getCell("Cant.", Font.BOLD, Element.ALIGN_CENTER));
        tabla.addCell(getCell("Precio", Font.BOLD, Element.ALIGN_RIGHT));
        tabla.addCell(getCell("Total", Font.BOLD, Element.ALIGN_RIGHT));

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
                    tabla.addCell(getCell(String.format("$ %.2f", rs.getDouble(3)), Font.NORMAL, Element.ALIGN_RIGHT));
                    tabla.addCell(getCell(String.format("$ %.2f", rs.getDouble(4)), Font.NORMAL, Element.ALIGN_RIGHT));
                }
            }
        }

        documento.add(tabla);
        documento.add(new Paragraph("\n"));
        agregarTotales(documento, con, idVenta);
    }

    private void agregarTotales(Document documento, Connection con, int idVenta) throws SQLException, DocumentException {
        String sql = "SELECT Total FROM Venta WHERE ID = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Paragraph totales = new Paragraph(
                            "\nTotal: $" + rs.getDouble(1),
                            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD));
                    totales.setAlignment(Element.ALIGN_RIGHT);
                    documento.add(totales);
                }
            }
        }
    }

    private PdfPCell getCell(String texto, int estilo, int alineacion) {
        Font fuente = new Font(Font.FontFamily.HELVETICA, 8, estilo);
        PdfPCell cell = new PdfPCell(new Phrase(texto, fuente));
        cell.setHorizontalAlignment(alineacion);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private void abrirPDF(String rutaPDF) {
        try {
            File file = new File(rutaPDF);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void imprimirPDF(String rutaPDF) {
        try {
            FileInputStream fis = new FileInputStream(rutaPDF);
            Doc pdfDoc = new SimpleDoc(fis, DocFlavor.INPUT_STREAM.AUTOSENSE, null);
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
