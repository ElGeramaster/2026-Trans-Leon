package GestionSoftware;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Exporta un registro seleccionado de la JTable como factura en formato
 * Excel (XML Spreadsheet 2003). No requiere librerías externas.
 */
public class ExportadorDocumentos {

    /**
     * Exporta la fila seleccionada como factura Excel.
     *
     * @param parent     ventana padre
     * @param tabla      la JTable de registros
     * @param viewRow    índice de fila en la vista (ya convertido si hay sorter)
     * @param columnas   nombres de las columnas visibles
     */
    public static void exportarFactura(JFrame parent, JTable tabla, int viewRow, String[] columnas) {
        if (tabla == null || viewRow < 0) {
            JOptionPane.showMessageDialog(parent,
                "Selecciona un registro para exportar.",
                "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Leer datos de la fila
        int colCount = tabla.getColumnCount();
        String[] valores = new String[colCount];
        for (int c = 0; c < colCount; c++) {
            Object val = tabla.getValueAt(viewRow, c);
            valores[c] = (val == null) ? "" : val.toString().trim();
        }

        // Nombre sugerido: Factura_CartaPorte_Cliente.xls
        String cartaPorte = valores.length > 0 ? valores[0].replaceAll("[^a-zA-Z0-9]", "") : "registro";
        String cliente    = valores.length > 1 ? valores[1].replaceAll("[^a-zA-Z0-9_ ]", "").trim().replace(" ", "_") : "";
        String nombreSugerido = "Factura_" + cartaPorte;
        if (!cliente.isEmpty()) nombreSugerido += "_" + cliente;
        nombreSugerido += ".xls";

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Factura");
        fc.setSelectedFile(new File(nombreSugerido));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel (*.xls)", "xls"));

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
            escribirFactura(columnas, valores, archivo);
            JOptionPane.showMessageDialog(parent,
                "Factura exportada correctamente:\n" + archivo.getAbsolutePath(),
                "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                "Error al exportar: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void escribirFactura(String[] columnas, String[] valores, File archivo) throws Exception {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {

            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<?mso-application progid=\"Excel.Sheet\"?>");
            pw.println("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
            pw.println(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");

            // ========== ESTILOS ==========
            pw.println("<Styles>");

            // Título empresa
            pw.println(" <Style ss:ID=\"titulo\">");
            pw.println("  <Font ss:Bold=\"1\" ss:Size=\"18\" ss:Color=\"#1A5276\"/>");
            pw.println("  <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>");
            pw.println(" </Style>");

            // Subtítulo factura
            pw.println(" <Style ss:ID=\"subtitulo\">");
            pw.println("  <Font ss:Bold=\"1\" ss:Size=\"14\" ss:Color=\"#2E86C1\"/>");
            pw.println("  <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\"/>");
            pw.println(" </Style>");

            // Sección header (ej: DATOS DEL CLIENTE)
            pw.println(" <Style ss:ID=\"seccion\">");
            pw.println("  <Font ss:Bold=\"1\" ss:Size=\"11\" ss:Color=\"#FFFFFF\"/>");
            pw.println("  <Interior ss:Color=\"#2E86C1\" ss:Pattern=\"Solid\"/>");
            pw.println("  <Alignment ss:Horizontal=\"Left\" ss:Vertical=\"Center\"/>");
            pw.println("  <Borders>");
            pw.println("   <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#1A5276\"/>");
            pw.println("  </Borders>");
            pw.println(" </Style>");

            // Label (nombre del campo)
            pw.println(" <Style ss:ID=\"label\">");
            pw.println("  <Font ss:Bold=\"1\" ss:Size=\"10\" ss:Color=\"#2C3E50\"/>");
            pw.println("  <Interior ss:Color=\"#D6EAF8\" ss:Pattern=\"Solid\"/>");
            pw.println("  <Alignment ss:Horizontal=\"Right\" ss:Vertical=\"Center\"/>");
            pw.println("  <Borders>");
            pw.println("   <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#AED6F1\"/>");
            pw.println("   <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#AED6F1\"/>");
            pw.println("  </Borders>");
            pw.println(" </Style>");

            // Valor (dato)
            pw.println(" <Style ss:ID=\"valor\">");
            pw.println("  <Font ss:Size=\"10\"/>");
            pw.println("  <Alignment ss:Horizontal=\"Left\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>");
            pw.println("  <Borders>");
            pw.println("   <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#D5D8DC\"/>");
            pw.println("  </Borders>");
            pw.println(" </Style>");

            // Valor moneda
            pw.println(" <Style ss:ID=\"valorMoney\">");
            pw.println("  <Font ss:Bold=\"1\" ss:Size=\"10\" ss:Color=\"#1A5276\"/>");
            pw.println("  <Alignment ss:Horizontal=\"Left\" ss:Vertical=\"Center\"/>");
            pw.println("  <Borders>");
            pw.println("   <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#D5D8DC\"/>");
            pw.println("  </Borders>");
            pw.println(" </Style>");

            // Línea separadora
            pw.println(" <Style ss:ID=\"separador\">");
            pw.println("  <Interior ss:Color=\"#F8F9FA\" ss:Pattern=\"Solid\"/>");
            pw.println(" </Style>");

            pw.println("</Styles>");

            // ========== HOJA ==========
            pw.println("<Worksheet ss:Name=\"Factura\">");
            pw.println("<Table ss:DefaultRowHeight=\"20\">");

            // Columnas: A=label(200), B=valor(300)
            pw.println(" <Column ss:Width=\"200\"/>");
            pw.println(" <Column ss:Width=\"350\"/>");

            // Fila vacía superior
            pw.println(" <Row ss:Height=\"10\"/>");

            // Título
            pw.println(" <Row ss:Height=\"35\">");
            celda(pw, "titulo", "TRANSPORTES DE LEÓN", 2);
            pw.println(" </Row>");

            // Subtítulo
            String numFactura = obtenerValor(columnas, valores, "CARTA PORTE");
            pw.println(" <Row ss:Height=\"28\">");
            celda(pw, "subtitulo", "FACTURA - CARTA PORTE #" + numFactura, 2);
            pw.println(" </Row>");

            // Separador
            pw.println(" <Row ss:Height=\"8\">");
            celda(pw, "separador", "", 2);
            pw.println(" </Row>");

            // === SECCIÓN: DATOS DEL CLIENTE ===
            seccion(pw, "DATOS DEL CLIENTE");
            fila(pw, columnas, valores, "CLIENTE");
            fila(pw, columnas, valores, "REMITENTE");
            fila(pw, columnas, valores, "CONSIGNATORIO");
            fila(pw, columnas, valores, "DESTINO");

            separador(pw);

            // === SECCIÓN: DATOS DE LA FACTURA ===
            seccion(pw, "DATOS DE LA FACTURA");
            fila(pw, columnas, valores, "FACTURA");
            fila(pw, columnas, valores, "FECHA FACTURA");
            fila(pw, columnas, valores, "REFERENCIA");
            fila(pw, columnas, valores, "FECHA DE PAGO");
            fila(pw, columnas, valores, "FECHA DE PAGADO");

            separador(pw);

            // === SECCIÓN: VALORES ===
            seccion(pw, "VALORES Y MONTOS");
            filaMoney(pw, columnas, valores, "VALOR");
            filaMoney(pw, columnas, valores, "VALOR FLETE");
            filaMoney(pw, columnas, valores, "ANTICIPO");
            filaMoney(pw, columnas, valores, "A.CANCELACION");
            filaMoney(pw, columnas, valores, "VALOR CUSTODIO");

            separador(pw);

            // === SECCIÓN: TRANSPORTE ===
            seccion(pw, "DATOS DE TRANSPORTE");
            fila(pw, columnas, valores, "OPERADOR");
            fila(pw, columnas, valores, "PLACA CABEZAL");
            fila(pw, columnas, valores, "PLACA DEL FURGON");
            fila(pw, columnas, valores, "SEGURIDAD PRIVADA");

            separador(pw);

            // === SECCIÓN: FECHAS DE TRÁNSITO ===
            seccion(pw, "FECHAS DE TRÁNSITO");
            fila(pw, columnas, valores, "F. DE CARGA");
            fila(pw, columnas, valores, "F. DE CRUCE");
            fila(pw, columnas, valores, "F. SAL. T.U.");
            fila(pw, columnas, valores, "F.F. DESTINO");
            fila(pw, columnas, valores, "F. EN. DESTINO");
            fila(pw, columnas, valores, "F. DESCARGA");
            fila(pw, columnas, valores, "F.E. DE DOCTOS.");
            fila(pw, columnas, valores, "F. PAGO CUSTODIO");

            separador(pw);

            // === SECCIÓN: OBSERVACIONES ===
            seccion(pw, "OBSERVACIONES");
            String obs = obtenerValor(columnas, valores, "OBSERVACIONES");
            pw.println(" <Row ss:Height=\"60\">");
            pw.println("  <Cell ss:StyleID=\"valor\" ss:MergeAcross=\"1\"><Data ss:Type=\"String\">" + esc(obs) + "</Data></Cell>");
            pw.println(" </Row>");

            pw.println("</Table>");
            pw.println("</Worksheet>");
            pw.println("</Workbook>");
        }
    }

    // ========== Helpers de escritura ==========

    private static String obtenerValor(String[] columnas, String[] valores, String nombre) {
        for (int i = 0; i < columnas.length; i++) {
            if (columnas[i].equalsIgnoreCase(nombre)) {
                return (i < valores.length) ? valores[i] : "";
            }
        }
        return "";
    }

    private static void seccion(PrintWriter pw, String titulo) {
        pw.println(" <Row ss:Height=\"25\">");
        pw.println("  <Cell ss:StyleID=\"seccion\" ss:MergeAcross=\"1\"><Data ss:Type=\"String\">" + esc(titulo) + "</Data></Cell>");
        pw.println(" </Row>");
    }

    private static void fila(PrintWriter pw, String[] columnas, String[] valores, String campo) {
        String valor = obtenerValor(columnas, valores, campo);
        pw.println(" <Row ss:Height=\"22\">");
        pw.println("  <Cell ss:StyleID=\"label\"><Data ss:Type=\"String\">" + esc(campo) + "</Data></Cell>");
        pw.println("  <Cell ss:StyleID=\"valor\"><Data ss:Type=\"String\">" + esc(valor) + "</Data></Cell>");
        pw.println(" </Row>");
    }

    private static void filaMoney(PrintWriter pw, String[] columnas, String[] valores, String campo) {
        String valor = obtenerValor(columnas, valores, campo);
        pw.println(" <Row ss:Height=\"22\">");
        pw.println("  <Cell ss:StyleID=\"label\"><Data ss:Type=\"String\">" + esc(campo) + "</Data></Cell>");
        pw.println("  <Cell ss:StyleID=\"valorMoney\"><Data ss:Type=\"String\">" + esc(valor) + "</Data></Cell>");
        pw.println(" </Row>");
    }

    private static void separador(PrintWriter pw) {
        pw.println(" <Row ss:Height=\"6\">");
        pw.println("  <Cell ss:StyleID=\"separador\"><Data ss:Type=\"String\"></Data></Cell>");
        pw.println(" </Row>");
    }

    private static void celda(PrintWriter pw, String estilo, String texto, int mergeAcross) {
        if (mergeAcross > 1) {
            pw.println("  <Cell ss:StyleID=\"" + estilo + "\" ss:MergeAcross=\"" + (mergeAcross - 1) + "\"><Data ss:Type=\"String\">" + esc(texto) + "</Data></Cell>");
        } else {
            pw.println("  <Cell ss:StyleID=\"" + estilo + "\"><Data ss:Type=\"String\">" + esc(texto) + "</Data></Cell>");
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
