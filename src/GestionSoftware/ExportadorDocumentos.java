package GestionSoftware;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Exporta datos de una JTable a formato Excel (XML Spreadsheet 2003).
 * No requiere librerías externas — Excel, LibreOffice y Google Sheets
 * abren este formato nativamente con estilos, anchos y colores.
 */
public class ExportadorDocumentos {

    /**
     * Muestra un JFileChooser y exporta la tabla completa a un archivo .xls.
     * Solo exporta las filas visibles (respeta filtros del sorter).
     */
    public static void exportarDesdeTabla(JFrame parent, JTable tabla) {
        if (tabla == null || tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(parent,
                "No hay datos para exportar.",
                "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar como Excel");
        fc.setSelectedFile(new File("Registros.xls"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Excel (*.xls)", "xls"));

        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        File archivo = fc.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".xls")) {
            archivo = new File(archivo.getAbsolutePath() + ".xls");
        }

        if (archivo.exists()) {
            int resp = JOptionPane.showConfirmDialog(parent,
                "El archivo ya existe. ¿Deseas reemplazarlo?",
                "Confirmar", JOptionPane.YES_NO_OPTION);
            if (resp != JOptionPane.YES_OPTION) return;
        }

        try {
            escribirXmlSpreadsheet(tabla, archivo);
            JOptionPane.showMessageDialog(parent,
                "Archivo exportado correctamente:\n" + archivo.getAbsolutePath(),
                "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                "Error al exportar: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Genera un archivo XML Spreadsheet 2003 (.xls) con encabezados,
     * estilos de columna y datos formateados.
     */
    private static void escribirXmlSpreadsheet(JTable tabla, File archivo) throws Exception {
        TableModel model = tabla.getModel();
        int colCount = tabla.getColumnCount();
        int rowCount = tabla.getRowCount();

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {

            // Cabecera XML Spreadsheet 2003
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<?mso-application progid=\"Excel.Sheet\"?>");
            pw.println("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
            pw.println(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");

            // Estilos
            pw.println("<Styles>");

            // Estilo encabezado
            pw.println(" <Style ss:ID=\"header\">");
            pw.println("  <Font ss:Bold=\"1\" ss:Size=\"12\" ss:Color=\"#FFFFFF\"/>");
            pw.println("  <Interior ss:Color=\"#2E86C1\" ss:Pattern=\"Solid\"/>");
            pw.println("  <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>");
            pw.println("  <Borders>");
            pw.println("   <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#1A5276\"/>");
            pw.println("  </Borders>");
            pw.println(" </Style>");

            // Estilo datos normal
            pw.println(" <Style ss:ID=\"data\">");
            pw.println("  <Font ss:Size=\"11\"/>");
            pw.println("  <Alignment ss:Vertical=\"Center\" ss:WrapText=\"1\"/>");
            pw.println("  <Borders>");
            pw.println("   <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#D5D8DC\"/>");
            pw.println("  </Borders>");
            pw.println(" </Style>");

            // Estilo fila alterna
            pw.println(" <Style ss:ID=\"dataAlt\">");
            pw.println("  <Font ss:Size=\"11\"/>");
            pw.println("  <Interior ss:Color=\"#EBF5FB\" ss:Pattern=\"Solid\"/>");
            pw.println("  <Alignment ss:Vertical=\"Center\" ss:WrapText=\"1\"/>");
            pw.println("  <Borders>");
            pw.println("   <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#D5D8DC\"/>");
            pw.println("  </Borders>");
            pw.println(" </Style>");

            // Estilo numérico
            pw.println(" <Style ss:ID=\"numero\">");
            pw.println("  <Font ss:Size=\"11\"/>");
            pw.println("  <NumberFormat ss:Format=\"#,##0.00\"/>");
            pw.println("  <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>");
            pw.println("  <Borders>");
            pw.println("   <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#D5D8DC\"/>");
            pw.println("  </Borders>");
            pw.println(" </Style>");

            // Estilo numérico alterno
            pw.println(" <Style ss:ID=\"numeroAlt\">");
            pw.println("  <Font ss:Size=\"11\"/>");
            pw.println("  <Interior ss:Color=\"#EBF5FB\" ss:Pattern=\"Solid\"/>");
            pw.println("  <NumberFormat ss:Format=\"#,##0.00\"/>");
            pw.println("  <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>");
            pw.println("  <Borders>");
            pw.println("   <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#D5D8DC\"/>");
            pw.println("  </Borders>");
            pw.println(" </Style>");

            pw.println("</Styles>");

            // Hoja
            pw.println("<Worksheet ss:Name=\"Registros\">");
            pw.println("<Table>");

            // Anchos de columna
            for (int c = 0; c < colCount; c++) {
                int viewCol = c;
                int ancho = tabla.getColumnModel().getColumn(viewCol).getPreferredWidth();
                if (ancho < 60) ancho = 80;
                pw.println(" <Column ss:AutoFitWidth=\"1\" ss:Width=\"" + ancho + "\"/>");
            }

            // Fila de encabezados
            pw.println(" <Row ss:Height=\"30\">");
            for (int c = 0; c < colCount; c++) {
                String nombre = esc(tabla.getColumnName(c));
                pw.println("  <Cell ss:StyleID=\"header\"><Data ss:Type=\"String\">" + nombre + "</Data></Cell>");
            }
            pw.println(" </Row>");

            // Filas de datos (usa vista, respeta filtros y orden)
            for (int r = 0; r < rowCount; r++) {
                boolean alt = (r % 2 == 1);
                String estiloTexto = alt ? "dataAlt" : "data";
                String estiloNum   = alt ? "numeroAlt" : "numero";

                pw.println(" <Row>");
                for (int c = 0; c < colCount; c++) {
                    Object val = tabla.getValueAt(r, c);
                    String texto = (val == null) ? "" : val.toString().trim();

                    // Detectar si es valor numérico/moneda
                    String limpio = texto.replaceAll("[^0-9.\\-]", "");
                    boolean esNumero = false;
                    double numVal = 0;

                    if (!limpio.isEmpty() && texto.contains("$")) {
                        try {
                            numVal = Double.parseDouble(limpio);
                            esNumero = true;
                        } catch (NumberFormatException ignore) {}
                    }

                    if (esNumero) {
                        pw.println("  <Cell ss:StyleID=\"" + estiloNum + "\"><Data ss:Type=\"Number\">" + numVal + "</Data></Cell>");
                    } else {
                        pw.println("  <Cell ss:StyleID=\"" + estiloTexto + "\"><Data ss:Type=\"String\">" + esc(texto) + "</Data></Cell>");
                    }
                }
                pw.println(" </Row>");
            }

            pw.println("</Table>");
            pw.println("</Worksheet>");
            pw.println("</Workbook>");
        }
    }

    /** Escapa caracteres especiales para XML. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
