package GestionSoftware;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Base64;

/**
 * Exporta un registro seleccionado como factura en formato HTML/.xls.
 * Excel, LibreOffice y Google Sheets abren este formato nativamente.
 * No requiere librerías externas.
 */
public class ExportadorDocumentos {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/EmpresaLog"
            + "?useSSL=false&allowPublicKeyRetrieval=true"
            + "&useUnicode=true&characterEncoding=UTF-8"
            + "&serverTimezone=America/Merida";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "12345ñ";

    private static final String LOGO_RESOURCE = "/GestionSoftware/imagenes/LogoLeon.png";

    public static void exportarFactura(JFrame parent, JTable tabla, int viewRow, String[] columnas) {
        if (tabla == null || viewRow < 0) {
            JOptionPane.showMessageDialog(parent,
                "Selecciona un registro para exportar.",
                "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int colCount = tabla.getColumnCount();
        String[] valores = new String[colCount];
        for (int c = 0; c < colCount; c++) {
            Object val = tabla.getValueAt(viewRow, c);
            valores[c] = (val == null) ? "" : val.toString().trim();
        }

        // Obtener Carta_Porte_id para consultar datos extra
        String cartaPorteId = obtenerValor(columnas, valores, "CARTA PORTE");

        // Consultar datos extra de remitente y consignatario
        String[] datosExtra = consultarDatosExtra(cartaPorteId);

        String cpLimpio = cartaPorteId.replaceAll("[^a-zA-Z0-9]", "");
        String cliLimpio = obtenerValor(columnas, valores, "CLIENTE")
                .replaceAll("[^a-zA-Z0-9_ ]", "").trim().replace(" ", "_");
        String nombreSugerido = "Factura_" + cpLimpio;
        if (!cliLimpio.isEmpty()) nombreSugerido += "_" + cliLimpio;
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
            escribirFacturaHTML(columnas, valores, datosExtra, archivo);
            JOptionPane.showMessageDialog(parent,
                "Factura exportada correctamente:\n" + archivo.getAbsolutePath(),
                "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                "Error al exportar: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // datosExtra: [0]=dirRemitente, [1]=telRemitente, [2]=correoRemitente,
    //             [3]=dirConsignatario, [4]=telConsignatario, [5]=correoConsignatario
    private static String[] consultarDatosExtra(String cartaPorteId) {
        String[] datos = new String[6];
        for (int i = 0; i < datos.length; i++) datos[i] = "";

        if (cartaPorteId == null || cartaPorteId.trim().isEmpty()) return datos;

        String sql =
            "SELECT " +
            "  rem.Direccion, rem.Telefono, rem.Correo, " +
            "  con.Direccion, con.Telefono, con.Correo " +
            "FROM Carta_Porte cp " +
            "LEFT JOIN Remitentes     rem ON rem.ID_Remitente     = cp.ID_Remitente " +
            "LEFT JOIN Consignatarios con ON con.ID_Consignatario = cp.ID_Consignatario " +
            "WHERE cp.Carta_Porte_id = ?";

        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cartaPorteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    for (int i = 0; i < 6; i++) {
                        String v = rs.getString(i + 1);
                        datos[i] = (v == null) ? "" : v.trim();
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("[ExportadorDocumentos] Error BD: " + ex.getMessage());
        }
        return datos;
    }

    private static String cargarLogoBase64() {
        try (InputStream is = ExportadorDocumentos.class.getResourceAsStream(LOGO_RESOURCE)) {
            if (is == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    private static void escribirFacturaHTML(String[] columnas, String[] valores,
                                            String[] datosExtra, File archivo) throws Exception {
        String logoB64 = cargarLogoBase64();

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {

            pw.println("<html xmlns:o=\"urn:schemas-microsoft-com:office:office\"");
            pw.println(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\">");
            pw.println("<head><meta charset=\"UTF-8\">");
            pw.println("<style>");
            pw.println("  body { font-family: 'Segoe UI', Arial, sans-serif; margin: 20px; }");
            pw.println("  table { border-collapse: collapse; width: 600px; margin: 0 auto; }");
            pw.println("  .header-titulo { font-size: 20px; font-weight: bold; color: #1A5276;");
            pw.println("                   text-align: left; padding: 10px; vertical-align: middle; }");
            pw.println("  .header-logo { text-align: right; padding: 10px; vertical-align: middle; }");
            pw.println("  .header-logo img { height: 55px; }");
            pw.println("  .carta-porte { font-size: 16px; font-weight: bold; color: #C0392B;");
            pw.println("                 text-align: center; padding: 6px; }");
            pw.println("  .seccion { background: #2E86C1; color: #FFFFFF; font-weight: bold;");
            pw.println("             font-size: 12px; padding: 7px 10px; }");
            pw.println("  .label { background: #D6EAF8; font-weight: bold; color: #2C3E50;");
            pw.println("           font-size: 11px; padding: 5px 10px; text-align: right;");
            pw.println("           width: 200px; border-bottom: 1px solid #AED6F1; }");
            pw.println("  .valor { font-size: 11px; padding: 5px 10px; text-align: left;");
            pw.println("           border-bottom: 1px solid #D5D8DC; }");
            pw.println("  .valor-money { font-size: 11px; font-weight: bold; color: #1A5276;");
            pw.println("                 padding: 5px 10px; text-align: left;");
            pw.println("                 border-bottom: 1px solid #D5D8DC; }");
            pw.println("  .sub-info { font-size: 10px; color: #5D6D7E; padding: 2px 10px;");
            pw.println("              text-align: left; border-bottom: 1px solid #EAECEE; }");
            pw.println("  .sub-label { font-size: 10px; color: #7F8C8D; padding: 2px 10px;");
            pw.println("               text-align: right; background: #EBF5FB;");
            pw.println("               border-bottom: 1px solid #EAECEE; }");
            pw.println("  .separador td { height: 6px; background: #F8F9FA; }");
            pw.println("  .obs { font-size: 11px; padding: 8px 10px; text-align: left;");
            pw.println("         border-bottom: 1px solid #D5D8DC; }");
            pw.println("</style></head>");
            pw.println("<body>");
            pw.println("<table>");

            // ===== TÍTULO + LOGO en la misma fila =====
            pw.println("<tr>");
            pw.println("  <td class=\"header-titulo\">TRANSPORTES DE LE&Oacute;N</td>");
            if (logoB64 != null) {
                pw.println("  <td class=\"header-logo\"><img src=\"data:image/png;base64," + logoB64 + "\" /></td>");
            } else {
                pw.println("  <td></td>");
            }
            pw.println("</tr>");

            // ===== CARTA PORTE EN ROJO =====
            String numCP = esc(obtenerValor(columnas, valores, "CARTA PORTE"));
            pw.println("<tr><td colspan=\"2\" class=\"carta-porte\">FACTURA - CARTA PORTE #" + numCP + "</td></tr>");

            separador(pw);

            // ===== DATOS DEL CLIENTE =====
            seccion(pw, "DATOS DEL CLIENTE");
            fila(pw, "CLIENTE", obtenerValor(columnas, valores, "CLIENTE"));
            fila(pw, "DESTINO", obtenerValor(columnas, valores, "DESTINO"));

            separador(pw);

            // ===== REMITENTE (con dirección y teléfono) =====
            seccion(pw, "REMITENTE");
            fila(pw, "NOMBRE", obtenerValor(columnas, valores, "REMITENTE"));
            if (!datosExtra[0].isEmpty()) subFila(pw, "Direcci\u00f3n", datosExtra[0]);
            if (!datosExtra[1].isEmpty()) subFila(pw, "Tel\u00e9fono", datosExtra[1]);
            if (!datosExtra[2].isEmpty()) subFila(pw, "Correo", datosExtra[2]);

            separador(pw);

            // ===== CONSIGNATARIO (con dirección y teléfono) =====
            seccion(pw, "CONSIGNATARIO");
            fila(pw, "NOMBRE", obtenerValor(columnas, valores, "CONSIGNATORIO"));
            if (!datosExtra[3].isEmpty()) subFila(pw, "Direcci\u00f3n", datosExtra[3]);
            if (!datosExtra[4].isEmpty()) subFila(pw, "Tel\u00e9fono", datosExtra[4]);
            if (!datosExtra[5].isEmpty()) subFila(pw, "Correo", datosExtra[5]);

            separador(pw);

            // ===== DATOS DE LA FACTURA =====
            seccion(pw, "DATOS DE LA FACTURA");
            fila(pw, "FACTURA", obtenerValor(columnas, valores, "FACTURA"));
            fila(pw, "FECHA FACTURA", obtenerValor(columnas, valores, "FECHA FACTURA"));
            fila(pw, "REFERENCIA", obtenerValor(columnas, valores, "REFERENCIA"));
            fila(pw, "FECHA DE PAGO", obtenerValor(columnas, valores, "FECHA DE PAGO"));
            fila(pw, "FECHA DE PAGADO", obtenerValor(columnas, valores, "FECHA DE PAGADO"));

            separador(pw);

            // ===== VALORES =====
            seccion(pw, "VALORES Y MONTOS");
            filaMoney(pw, "VALOR", obtenerValor(columnas, valores, "VALOR"));
            filaMoney(pw, "VALOR FLETE", obtenerValor(columnas, valores, "VALOR FLETE"));
            filaMoney(pw, "ANTICIPO", obtenerValor(columnas, valores, "ANTICIPO"));
            filaMoney(pw, "A.CANCELACION", obtenerValor(columnas, valores, "A.CANCELACION"));
            filaMoney(pw, "VALOR CUSTODIO", obtenerValor(columnas, valores, "VALOR CUSTODIO"));

            separador(pw);

            // ===== TRANSPORTE =====
            seccion(pw, "DATOS DE TRANSPORTE");
            fila(pw, "OPERADOR", obtenerValor(columnas, valores, "OPERADOR"));
            fila(pw, "PLACA CABEZAL", obtenerValor(columnas, valores, "PLACA CABEZAL"));
            fila(pw, "PLACA DEL FURG\u00d3N", obtenerValor(columnas, valores, "PLACA DEL FURGON"));
            fila(pw, "SEGURIDAD PRIVADA", obtenerValor(columnas, valores, "SEGURIDAD PRIVADA"));

            separador(pw);

            // ===== FECHAS DE TRÁNSITO =====
            seccion(pw, "FECHAS DE TR\u00c1NSITO");
            fila(pw, "F. DE CARGA", obtenerValor(columnas, valores, "F. DE CARGA"));
            fila(pw, "F. DE CRUCE", obtenerValor(columnas, valores, "F. DE CRUCE"));
            fila(pw, "F. SAL. T.U.", obtenerValor(columnas, valores, "F. SAL. T.U."));
            fila(pw, "F.F. DESTINO", obtenerValor(columnas, valores, "F.F. DESTINO"));
            fila(pw, "F. EN. DESTINO", obtenerValor(columnas, valores, "F. EN. DESTINO"));
            fila(pw, "F. DESCARGA", obtenerValor(columnas, valores, "F. DESCARGA"));
            fila(pw, "F.E. DE DOCTOS.", obtenerValor(columnas, valores, "F.E. DE DOCTOS."));
            fila(pw, "F. PAGO CUSTODIO", obtenerValor(columnas, valores, "F. PAGO CUSTODIO"));

            separador(pw);

            // ===== OBSERVACIONES =====
            seccion(pw, "OBSERVACIONES");
            String obs = obtenerValor(columnas, valores, "OBSERVACIONES");
            pw.println("<tr><td colspan=\"2\" class=\"obs\">" + esc(obs) + "</td></tr>");

            pw.println("</table>");
            pw.println("</body></html>");
        }
    }

    // ========== Helpers ==========

    private static String obtenerValor(String[] columnas, String[] valores, String nombre) {
        for (int i = 0; i < columnas.length; i++) {
            if (columnas[i].equalsIgnoreCase(nombre)) {
                return (i < valores.length) ? valores[i] : "";
            }
        }
        return "";
    }

    private static void seccion(PrintWriter pw, String titulo) {
        pw.println("<tr><td colspan=\"2\" class=\"seccion\">" + esc(titulo) + "</td></tr>");
    }

    private static void fila(PrintWriter pw, String label, String valor) {
        pw.println("<tr><td class=\"label\">" + esc(label) + "</td>");
        pw.println("    <td class=\"valor\">" + esc(valor) + "</td></tr>");
    }

    private static void filaMoney(PrintWriter pw, String label, String valor) {
        pw.println("<tr><td class=\"label\">" + esc(label) + "</td>");
        pw.println("    <td class=\"valor-money\">" + esc(valor) + "</td></tr>");
    }

    private static void subFila(PrintWriter pw, String label, String valor) {
        pw.println("<tr><td class=\"sub-label\">" + esc(label) + "</td>");
        pw.println("    <td class=\"sub-info\">" + esc(valor) + "</td></tr>");
    }

    private static void separador(PrintWriter pw) {
        pw.println("<tr class=\"separador\"><td colspan=\"2\"></td></tr>");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
