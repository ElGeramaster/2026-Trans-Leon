//package GestionSoftware;
//
//import com.mysql.cj.xdevapi.Row;
//import org.apache.poi.xssf.usermodel.*;
//import org.apache.poi.ss.usermodel.*;
//import javax.swing.*;
//import java.io.FileOutputStream;
//
//public class ExportadorDocumentos {
//
//    // Exportar a Excel
//    public static void exportarAExcel(JTable tabla, String nombreArchivo) {
//        try {
//            XSSFWorkbook workbook = new XSSFWorkbook();
//            XSSFSheet sheet = workbook.createSheet("Registros");
//            
//            // Headers
//            Row headerRow = sheet.createRow(0);
//            for (int i = 0; i < tabla.getColumnCount(); i++) {
//                Cell cell = headerRow.createCell(i);
//                cell.setCellValue(tabla.getColumnName(i));
//            }
//            
//            // Datos
//            for (int i = 0; i < tabla.getRowCount(); i++) {
//                Row row = sheet.createRow(i + 1);
//                for (int j = 0; j < tabla.getColumnCount(); j++) {
//                    Cell cell = row.createCell(j);
//                    Object valor = tabla.getValueAt(i, j);
//                    cell.setCellValue(valor != null ? valor.toString() : "");
//                }
//            }
//            
//            // Guardar
//            try (FileOutputStream fos = new FileOutputStream(nombreArchivo)) {
//                workbook.write(fos);
//            }
//            workbook.close();
//            
//            JOptionPane.showMessageDialog(null, 
//                "Excel exportado correctamente: " + nombreArchivo,
//                "Éxito", JOptionPane.INFORMATION_MESSAGE);
//                
//        } catch (Exception e) {
//            JOptionPane.showMessageDialog(null,
//                "Error al exportar: " + e.getMessage(),
//                "Error", JOptionPane.ERROR_MESSAGE);
//        }
//    }
//
//    // Exportar como Factura (formato especial)
//    public static void exportarComoFactura(JTable tabla, int filaSeleccionada, 
//                                           String nombreArchivo) {
//        // Lógica especial para formato factura
//        // Datos del cliente, totales, etc.
//    }
//}
