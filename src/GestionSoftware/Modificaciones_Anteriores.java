
package GestionSoftware;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Vector;
import java.math.BigDecimal;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class Modificaciones_Anteriores extends JFrame {

    private JTable tabla;
    private DefaultTableModel modelo;
    private TableRowSorter<DefaultTableModel> sorter;
    private JComboBox<String> cbRango;
    private JComboBox<String> cbTipo;
    private JButton btnDetalle;
    private JButton btnEliminar;
    private JButton btnLimpiarTodo;
    private JButton B1;
    private JLabel lblTitulo;
    private JLabel lblSubtitulo;
    private JScrollPane scrollPane;

    private JLabel logoLabel;
    private static final String LOGOImagen = "/GestionSoftware/imagenes/LogoLeon.png";

    private static final int MARGIN_X   = 30;
    private static final int SCROLL_Y   = 180;

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/EmpresaLog"
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&useUnicode=true&characterEncoding=UTF-8"
            + "&serverTimezone=America/Merida";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "12345ñ";

    private static final String AUDIT_TABLE = "AUDITORIA";

    // Configuración: días de retención (configurable)
    private static final int DIAS_RETENCION = 30;

    // Índices de columnas del modelo
    private static final int COL_USUARIO  = 0;
    private static final int COL_FECHA    = 1;
    private static final int COL_HORA     = 2;
    private static final int COL_DESC     = 3;
    private static final int COL_ID_CP    = 4;
    private static final int COL_AUDIT_ID = 5;
    private static final int COL_ACCION   = 6;

    public Modificaciones_Anteriores() {
        setTitle("Historial de Modificaciones - Sistema de Gestión");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1600, 900);
        setResizable(true);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Color scheme moderno
        Color colorSecundario = new Color(46, 204, 113);
        Color colorFondo = new Color(248, 249, 250);
        Color colorPanel = new Color(255, 255, 255);
        
        getContentPane().setBackground(colorFondo);

        ImageIcon appIcon = scaledIcon(LOGOImagen, 32, 32);
        if (appIcon != null) setIconImage(appIcon.getImage());

        // ========== PANEL SUPERIOR (Header) ==========
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBackground(colorPanel);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Botón regresar mejorado
        B1 = new JButton("<");
      //B1.setFont(new Font("Arial", Font.BOLD, 24));
        B1.setBackground(new Color(52, 152, 219));
        B1.setForeground(Color.BLACK);
        B1.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        B1.setFocusPainted(false);
        B1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        B1.setToolTipText("Regresar al menú principal");
        B1.setPreferredSize(new Dimension(60, 60));
        B1.setMinimumSize(new Dimension(60, 60));
        B1.setMaximumSize(new Dimension(60, 60));

        B1.addActionListener(e -> {
            try {
                WindowState.switchTo(this, new Registros());
            } catch (Throwable ignore) {
                dispose();
            }
        });

        B1.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { 
                B1.setBackground(new Color(231, 76, 60)); 
            }
            @Override public void mouseExited(MouseEvent e) { 
                B1.setBackground(new Color(52, 152, 219)); 
            }
        });

        // Panel de títulos
        JPanel titulosPanel = new JPanel();
        titulosPanel.setLayout(new BoxLayout(titulosPanel, BoxLayout.Y_AXIS));
        titulosPanel.setOpaque(false);

        lblTitulo = new JLabel("HISTORIAL DE MODIFICACIONES");
        lblTitulo.setFont(new Font("Poppins", Font.BOLD, 28));
        lblTitulo.setForeground(new Color(44, 62, 80));
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblSubtitulo = new JLabel("Consulta y gestiona los cambios realizados en el sistema");
        lblSubtitulo.setFont(new Font("Poppins", Font.PLAIN, 14));
        lblSubtitulo.setForeground(new Color(127, 140, 141));
        lblSubtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        titulosPanel.add(lblTitulo);
        titulosPanel.add(Box.createVerticalStrut(5));
        titulosPanel.add(lblSubtitulo);

        // Logo
        logoLabel = new JLabel();
        logoLabel.setOpaque(false);
        ImageIcon logoIcon = scaledIcon(LOGOImagen, 150, 120);
        if (logoIcon != null) logoLabel.setIcon(logoIcon);

        // Panel para envolver el botón B1 con tamaño fijo
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelBoton.setOpaque(false);
        panelBoton.add(B1);

        headerPanel.add(panelBoton, BorderLayout.WEST);
        headerPanel.add(titulosPanel, BorderLayout.CENTER);
        headerPanel.add(logoLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // ========== PANEL DE CONTROLES (Filtros) ==========
        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(colorPanel);
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(15, 30, 15, 30)
        ));
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));

        // Estilo para labels
        Font labelFont = new Font("Poppins", Font.BOLD, 13);
        Color labelColor = new Color(52, 73, 94);

        JLabel lblRango = new JLabel("Período:");
        lblRango.setFont(labelFont);
        lblRango.setForeground(labelColor);

        cbRango = new JComboBox<>(new String[] { 
            "Esta semana", "Este mes", "Este año", "Todo" 
        });
        cbRango.setFont(new Font("Poppins", Font.PLAIN, 13));
        cbRango.setPreferredSize(new Dimension(150, 35));
        cbRango.setFocusable(false);
        cbRango.setBackground(Color.WHITE);
        cbRango.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblTipo = new JLabel("Tipo de Movimiento:");
        lblTipo.setFont(labelFont);
        lblTipo.setForeground(labelColor);

        cbTipo = new JComboBox<>(new String[] { 
            "Ver todo", "Agregados", "Eliminados", "Modificados" 
        });
        cbTipo.setFont(new Font("Poppins", Font.PLAIN, 13));
        cbTipo.setPreferredSize(new Dimension(150, 35));
        cbTipo.setFocusable(false);
        cbTipo.setBackground(Color.WHITE);
        cbTipo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnDetalle     = crearBotonModerno("📄 Ver Detalles", colorSecundario);
        btnEliminar    = crearBotonModerno("🗑️ Eliminar", new Color(231, 76, 60));
        btnLimpiarTodo = crearBotonModerno("🧹 Limpiar Todo", new Color(230, 126, 34));

        controlPanel.add(lblRango);
        controlPanel.add(cbRango);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(lblTipo);
        controlPanel.add(cbTipo);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(btnDetalle);
        controlPanel.add(btnEliminar);
        controlPanel.add(btnLimpiarTodo);

        // ========== PANEL PRINCIPAL (Tabla) ==========
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(colorFondo);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));

        modelo = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int col) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) { return String.class; }
        };

        String[] cols = {
            "Usuario", "Fecha", "Hora", "Descripción", "Carta Porte", 
            "AUDIT_ID", "ACCION"
        };
        for (String c : cols) modelo.addColumn(c);

        tabla = new JTable(modelo);
        tabla.setRowHeight(32);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabla.setBackground(Color.WHITE);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabla.setSelectionBackground(new Color(224, 247, 250));
        tabla.setSelectionForeground(new Color(33, 33, 33));
        tabla.setGridColor(new Color(240, 240, 240));
        tabla.setShowVerticalLines(true);
        tabla.setShowHorizontalLines(true);

        JTableHeader header = tabla.getTableHeader();
        header.setFont(new Font("Poppins", Font.BOLD, 14));
        header.setBackground(new Color(236, 240, 241));
        header.setForeground(new Color(44, 62, 80));
        header.setPreferredSize(new Dimension(header.getWidth(), 45));
        header.setReorderingAllowed(true);
        header.setResizingAllowed(true);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.WHITE));

        sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);

        scrollPane = new JScrollPane(tabla);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        tabla.addMouseWheelListener(e -> {
            if (e.isShiftDown()) {
                JScrollBar h = scrollPane.getHorizontalScrollBar();
                h.setValue(h.getValue() + e.getUnitsToScroll() * h.getUnitIncrement());
                e.consume();
            }
        });

        configurarAnchosColumnas();
        aplicarRendererConColores();
        ocultarColumna(COL_AUDIT_ID);
        ocultarColumna(COL_ACCION);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(colorFondo);
        centerWrapper.add(controlPanel, BorderLayout.NORTH);
        centerWrapper.add(mainPanel, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // ========== LISTENERS ==========
        cbRango.addActionListener(e -> {
            cargarDatos();
            aplicarFiltroTipo();
        });
        
        cbTipo.addActionListener(e -> aplicarFiltroTipo());
        
        btnDetalle.addActionListener(e -> verDetalleSeleccionado());
        btnEliminar.addActionListener(e -> eliminarSeleccionados());
        btnLimpiarTodo.addActionListener(e -> limpiarTodoPorFiltro());

        // ESC para regresar
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "ESC");
        getRootPane().getActionMap().put("ESC", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { B1.doClick(); }
        });

        // Auto-limpieza al iniciar
        asegurarTablaAuditoria();
        limpiarRegistrosAntiguosAutomatico();
        cargarDatos();
        aplicarFiltroTipo();
        WindowState.installF11(this);
        
    }

    private JButton crearBotonModerno(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("Poppins", Font.BOLD, 13));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        Color hoverColor = color.darker();
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hoverColor); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(color); }
        });
        
        return btn;
    }

    private void limpiarRegistrosAntiguosAutomatico() {
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_RETENCION);
        Timestamp timestamp = Timestamp.valueOf(limite);
        
        String sql = "DELETE FROM " + AUDIT_TABLE + " WHERE modificado_en < ?";
        
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setTimestamp(1, timestamp);
            int eliminados = ps.executeUpdate();
            
            if (eliminados > 0) {
                System.out.println("[Auto-limpieza] Se eliminaron " + eliminados + 
                                   " registros de auditoría con más de " + DIAS_RETENCION + " días.");
            }
            
        } catch (SQLException ex) {
            System.err.println("[Auto-limpieza] Error: " + ex.getMessage());
        }
    }
    
    private void eliminarSeleccionados() {
        int[] selRows = tabla.getSelectedRows();
        if (selRows.length == 0) {
            JOptionPane.showMessageDialog(this,
                "Selecciona uno o más registros para eliminar.",
                "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Eliminar " + selRows.length + " registro(s) seleccionado(s)?\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Eliminación",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) return;

        // Recolectar los AUDIT_IDs de las filas seleccionadas
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (int viewRow : selRows) {
            int modelRow = tabla.convertRowIndexToModel(viewRow);
            String auditStr = String.valueOf(modelo.getValueAt(modelRow, COL_AUDIT_ID));
            try { ids.add(Long.valueOf(auditStr)); } catch (NumberFormatException ignore) {}
        }

        if (ids.isEmpty()) return;

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
        }

        String sql = "DELETE FROM " + AUDIT_TABLE + " WHERE id IN (" + placeholders + ")";
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            int eliminados = ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                "Se eliminaron " + eliminados + " registro(s) correctamente.",
                "Eliminación Completada", JOptionPane.INFORMATION_MESSAGE);

            cargarDatos();
            aplicarFiltroTipo();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Error al eliminar registros: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarTodoPorFiltro() {
        String tipoSel = (String) cbTipo.getSelectedItem();
        String rangoSel = (String) cbRango.getSelectedItem();

        // Determinar qué acción filtrar
        String accionDb = null;
        if ("Agregados".equals(tipoSel))   accionDb = "INSERT";
        if ("Modificados".equals(tipoSel)) accionDb = "UPDATE";
        if ("Eliminados".equals(tipoSel))  accionDb = "DELETE";

        String descripcion = (accionDb != null) ? "todos los registros de tipo \"" + tipoSel + "\"" : "TODOS los registros";
        if (!"Todo".equals(rangoSel)) descripcion += " del período \"" + rangoSel + "\"";

        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Eliminar " + descripcion + "?\nEsta acción no se puede deshacer.",
            "Confirmar Limpieza",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) return;

        Timestamp desde = calcularInicioPorRango(rangoSel);

        StringBuilder sql = new StringBuilder("DELETE FROM " + AUDIT_TABLE + " WHERE 1=1");
        if (accionDb != null) sql.append(" AND accion = ?");
        if (desde != null)    sql.append(" AND modificado_en >= ?");

        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int idx = 1;
            if (accionDb != null) ps.setString(idx++, accionDb);
            if (desde != null)    ps.setTimestamp(idx, desde);

            int eliminados = ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                "Se eliminaron " + eliminados + " registro(s) correctamente.",
                "Limpieza Completada", JOptionPane.INFORMATION_MESSAGE);

            cargarDatos();
            aplicarFiltroTipo();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Error al limpiar registros: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarFiltroTipo() {
        if (sorter == null) return;

        String sel = (String) cbTipo.getSelectedItem();
        if (sel == null || "Ver todo".equals(sel)) {
            sorter.setRowFilter(null);
            return;
        }

        String patron;
        switch (sel) {
            case "Agregados":   patron = "(?i)^\\s*INSERT\\s*$"; break;
            case "Eliminados":  patron = "(?i)^\\s*DELETE\\s*$"; break;
            case "Modificados": patron = "(?i)^\\s*UPDATE\\s*$"; break;
            default: sorter.setRowFilter(null); return;
        }
        sorter.setRowFilter(RowFilter.regexFilter(patron, COL_ACCION));
    }

    private void cargarDatos() {
        modelo.setRowCount(0);

        String filtro = (String) cbRango.getSelectedItem();
        Timestamp desde = calcularInicioPorRango(filtro);

        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "  a.modificado_en, " +
            "  a.descripcion, " +
            "  a.id, " +
            "  a.accion, " +
            "  a.usuario, " +
            "  a.Carta_Porte_id, " +
            "  cli.Nom_Cliente " +
            "FROM " + AUDIT_TABLE + " a " +
            "LEFT JOIN Carta_Porte cp ON cp.Carta_Porte_id = a.Carta_Porte_id " +
            "LEFT JOIN Clientes cli ON cli.ID_Cliente = cp.ID_Cliente "
        );

        if (desde != null) sql.append("WHERE a.modificado_en >= ? ");
        sql.append("ORDER BY a.modificado_en DESC");

        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            if (desde != null) ps.setTimestamp(1, desde);

            try (ResultSet rs = ps.executeQuery()) {
                DateTimeFormatter fmtFecha = DateTimeFormatter
                        .ofPattern("d 'de' MMMM 'del' yyyy")
                        .withLocale(new java.util.Locale("es","MX"));
                DateTimeFormatter fmtHora = DateTimeFormatter
                        .ofPattern("hh:mm a")
                        .withLocale(new java.util.Locale("es","MX"));

                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    String fechaNice = "";
                    String horaNice  = "";
                    if (ts != null) {
                        LocalDateTime ldt = ts.toLocalDateTime();
                        fechaNice = fmtFecha.format(ldt);
                        horaNice  = fmtHora.format(ldt);
                    }

                    long auditId   = rs.getLong(3);
                    String accion  = nz(rs.getString(4));
                    String usuarioDB = nz(rs.getString(5));
                    String nombreCliente = nz(rs.getString(7));

                    String descVisible;
                    if ("INSERT".equalsIgnoreCase(accion)) {
                        descVisible = "Nuevo Registro";
                    } else if ("UPDATE".equalsIgnoreCase(accion)) {
                        descVisible = "Modificación";
                    } else if ("DELETE".equalsIgnoreCase(accion)) {
                        descVisible = "Eliminado";
                    } else {
                        descVisible = nz(rs.getString(2));
                    }

                    String usuario = usuarioDB.isEmpty() ? nombreCliente : usuarioDB;
                    if (usuario.isEmpty()) usuario = "Sistema";

                    Vector<Object> row = new Vector<>();
                    row.add(usuario);
                    row.add(fechaNice);
                    row.add(horaNice);
                    row.add(descVisible);
                    row.add(rs.getObject(6));
                    row.add(String.valueOf(auditId));
                    row.add(accion);

                    modelo.addRow(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "No pude cargar el historial de '" + AUDIT_TABLE + "'.\n" + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String nz(String s) {
        return (s == null) ? "" : s;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "$  ";
        return "$ " + value.toPlainString();
    }

    private Timestamp calcularInicioPorRango(String rango) {
        if (rango == null) return null;
        ZoneId zone = ZoneId.systemDefault();
        LocalDate hoy = LocalDate.now(zone);

        switch (rango) {
            case "Esta semana": {
                LocalDate monday = hoy.with(java.time.DayOfWeek.MONDAY);
                return Timestamp.valueOf(LocalDateTime.of(monday, LocalTime.MIDNIGHT));
            }
            case "Este mes": {
                LocalDate first = hoy.withDayOfMonth(1);
                return Timestamp.valueOf(LocalDateTime.of(first, LocalTime.MIDNIGHT));
            }
            case "Este año": {
                LocalDate first = hoy.with(TemporalAdjusters.firstDayOfYear());
                return Timestamp.valueOf(LocalDateTime.of(first, LocalTime.MIDNIGHT));
            }
            case "Todo":
            default:
                return null;
        }
    }

    private void verDetalleSeleccionado() {
        int r = tabla.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, 
                "Por favor, selecciona un registro para ver sus detalles.", 
                "Aviso", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = tabla.convertRowIndexToModel(r);

        Object usuario = modelo.getValueAt(modelRow, COL_USUARIO);
        Object fecha   = modelo.getValueAt(modelRow, COL_FECHA);
        Object hora    = modelo.getValueAt(modelRow, COL_HORA);
        Object idCp    = modelo.getValueAt(modelRow, COL_ID_CP);
        String accion  = String.valueOf(modelo.getValueAt(modelRow, COL_ACCION));

        String auditStr = String.valueOf(modelo.getValueAt(modelRow, COL_AUDIT_ID));
        Long auditId    = auditStr.isBlank() ? null : Long.valueOf(auditStr);

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("         DETALLE DEL REGISTRO\n");
        sb.append("═══════════════════════════════════════════\n\n");
        sb.append("📋 Carta Porte: ").append(idCp).append("\n");
        sb.append("📅 Fecha: ").append(fecha).append(" ").append(hora).append("\n");
        sb.append("⚡ Acción: ").append(accion).append("\n");
        sb.append("👤 Usuario: ").append(usuario).append("\n\n");

        if ("DELETE".equalsIgnoreCase(accion)) {
            String detalleEliminado = leerDetallePorAuditId(auditId, true);
            if (detalleEliminado != null && !detalleEliminado.isBlank()) {
                sb.append("───────────────────────────────────────────\n");
                sb.append("  DATOS DEL REGISTRO ELIMINADO (JSON)\n");
                sb.append("───────────────────────────────────────────\n")
                  .append(detalleEliminado).append("\n\n");
            } else {
                sb.append("⚠️ No se encontraron los datos eliminados.\n\n");
            }
            sb.append("ℹ️ Este registro fue eliminado de la base de datos.\n");
        } else {
            String sql =
                "SELECT cp.*, " +
                "       cli.Nom_Cliente        AS NombreCliente, " +
                "       rem.Nom_Remitente      AS NombreRemitente, " +
                "       con.Nom_Consignatario  AS NombreConsignatario, " +
                "       op.Nom_Operador        AS NombreOperador, " +
                "       cab.Placa              AS PlacaCabezal, " +
                "       fur.Placa              AS PlacaFurgon, " +
                "       cus.Nom_Custodio       AS NombreCustodio " +
                "FROM Carta_Porte cp " +
                "LEFT JOIN Clientes       cli ON cli.ID_Cliente       = cp.ID_Cliente " +
                "LEFT JOIN Remitentes     rem ON rem.ID_Remitente     = cp.ID_Remitente " +
                "LEFT JOIN Consignatarios con ON con.ID_Consignatario = cp.ID_Consignatario " +
                "LEFT JOIN Operadores     op  ON op.ID_Operador       = cp.ID_Operador " +
                "LEFT JOIN Vehiculos      cab ON cab.ID_Vehiculo      = cp.ID_Placa_Cabezal " +
                "LEFT JOIN Vehiculos      fur ON fur.ID_Vehiculo      = cp.ID_Placa_Del_Furgon " +
                "LEFT JOIN Custodios      cus ON cus.ID_Custodio      = cp.ID_Custodio " +
                "WHERE cp.Carta_Porte_id = ?";

            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setObject(1, idCp);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        sb.append("───────────────────────────────────────────\n");
                        sb.append("       INFORMACIÓN ACTUAL DEL REGISTRO\n");
                        sb.append("───────────────────────────────────────────\n\n");
                        
                        sb.append("🏢 Cliente: ").append(nz(rs.getString("NombreCliente"))).append("\n");
                        sb.append("📄 Factura: ").append(nz(rs.getString("FACTURA"))).append("\n");
                        sb.append("📅 Fecha Factura: ").append(nz(rs.getString("FECHA_FACTURA"))).append("\n");
                        sb.append("💰 Valor: ").append(formatMoney(rs.getBigDecimal("VALOR"))).append("\n");
                        sb.append("📍 Destino: ").append(nz(rs.getString("DESTINO"))).append("\n");
                        sb.append("📝 Referencia: ").append(nz(rs.getString("REFERENCIA"))).append("\n");
                        sb.append("👤 Remitente: ").append(nz(rs.getString("NombreRemitente"))).append("\n");
                        sb.append("👥 Consignatario: ").append(nz(rs.getString("NombreConsignatario"))).append("\n");
                        sb.append("🚛 Operador: ").append(nz(rs.getString("NombreOperador"))).append("\n");
                        sb.append("🚗 Placa Cabezal: ").append(nz(rs.getString("PlacaCabezal"))).append("\n");
                        sb.append("🚐 Placa Furgón: ").append(nz(rs.getString("PlacaFurgon"))).append("\n");
                        sb.append("💵 Valor Flete: ").append(formatMoney(rs.getBigDecimal("VALOR_FLETE"))).append("\n");
                        sb.append("💳 Anticipo: ").append(formatMoney(rs.getBigDecimal("ANTICIPO"))).append("\n");
                        sb.append("📊 A Cancelación: ").append(formatMoney(rs.getBigDecimal("A_CANCELACION"))).append("\n");
                        sb.append("✅ Pagado: ").append(nz(rs.getString("PAGADO"))).append("\n");
                        sb.append("📝 Observaciones: ").append(nz(rs.getString("OBSERVACIONES"))).append("\n");
                    } else {
                        sb.append("⚠️ El registro no se encuentra en la base de datos.\n");
                    }
                }
            } catch (SQLException ex) {
                sb.append("\n❌ Error al cargar detalles: ").append(ex.getMessage()).append("\n");
            }

            if ("UPDATE".equalsIgnoreCase(accion)) {
                String detalleJson = leerDetallePorAuditId(auditId, true);
                if (detalleJson != null && !detalleJson.isBlank()) {
                    sb.append("\n───────────────────────────────────────────\n");
                    sb.append("       DETALLE JSON DEL EVENTO\n");
                    sb.append("───────────────────────────────────────────\n")
                      .append(detalleJson).append("\n");
                }
            }
        }

        JTextArea ta = new JTextArea(sb.toString(), 30, 80);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setEditable(false);
        ta.setFont(new Font("Consolas", Font.PLAIN, 13));
        ta.setBackground(new Color(248, 249, 250));
        ta.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JScrollPane jsp = new JScrollPane(ta);
        jsp.setPreferredSize(new Dimension(900, 600));
        
        JOptionPane.showMessageDialog(
            this, 
            jsp, 
            "📋 Detalle Completo del Registro", 
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private String leerDetallePorAuditId(Long auditId, boolean pretty) {
        if (auditId == null) return "";
        String col = pretty ? "JSON_PRETTY(detalle)" : "detalle";
        String sql = "SELECT " + col + " FROM " + AUDIT_TABLE + " WHERE id = ?";
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, auditId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return nz(rs.getString(1));
            }
        } catch (SQLException ignore) {}
        return "";
    }

    private void asegurarTablaAuditoria() {
        String ddl =
          "CREATE TABLE IF NOT EXISTS " + AUDIT_TABLE + " (" +
          " id BIGINT AUTO_INCREMENT PRIMARY KEY," +
          " Carta_Porte_id INT NOT NULL," +
          " modificado_en  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
          " usuario        VARCHAR(100) NULL," +
          " accion         ENUM('INSERT','UPDATE','DELETE') NOT NULL," +
          " descripcion    VARCHAR(255) NULL," +
          " detalle        JSON NULL," +
          " INDEX (Carta_Porte_id)," +
          " INDEX (modificado_en)," +
          " INDEX (accion)" +
          ")";
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement st = c.createStatement()) {
            st.execute(ddl);
        } catch (SQLException ex) {
            System.err.println("[Auditoria] No se pudo asegurar la tabla: " + ex.getMessage());
        }
    }

    private void configurarAnchosColumnas() {
        int[] widths = {
            120,  // Usuario
            220,  // Fecha
            100,  // Hora
            300,  // Descripción
            130,  // Carta Porte
            0,    // AUDIT_ID
            0     // ACCION
        };
        TableColumnModel tcm = tabla.getColumnModel();
        for (int i = 0; i < Math.min(widths.length, tcm.getColumnCount()); i++) {
            tcm.getColumn(i).setPreferredWidth(widths[i]);
            if (i == COL_AUDIT_ID || i == COL_ACCION) {
                tcm.getColumn(i).setMinWidth(0);
                tcm.getColumn(i).setMaxWidth(0);
            } else {
                tcm.getColumn(i).setMinWidth(80);
            }
        }
    }

    private void ocultarColumna(int colIndex) {
        TableColumnModel tcm = tabla.getColumnModel();
        if (colIndex >= 0 && colIndex < tcm.getColumnCount()) {
            TableColumn col = tcm.getColumn(colIndex);
            col.setMinWidth(0);
            col.setMaxWidth(0);
            col.setPreferredWidth(0);
            col.setResizable(false);
        }
    }

    private void aplicarRendererConColores() {
        final Color zebraA = Color.WHITE;
        final Color zebraB = new Color(249, 250, 251);
        
        // Colores pastel para cada tipo de acción
        final Color colorAgregar = new Color(200, 250, 205);     // Verde pastel
        final Color colorEliminar = new Color(255, 200, 210);    // Rosa pastel rojito
        final Color colorModificar = new Color(230, 210, 250);   // Púrpura pastel claro

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    try {
                        // Obtener la acción de la fila (columna oculta ACCION)
                        int modelRow = table.convertRowIndexToModel(row);
                        String accion = String.valueOf(modelo.getValueAt(modelRow, COL_ACCION));
                        
                        // Aplicar color solo a la columna Descripción (COL_DESC = 3)
                        if (column == COL_DESC) {
                            if ("INSERT".equalsIgnoreCase(accion)) {
                                c.setBackground(colorAgregar);
                            } else if ("DELETE".equalsIgnoreCase(accion)) {
                                c.setBackground(colorEliminar);
                            } else if ("UPDATE".equalsIgnoreCase(accion)) {
                                c.setBackground(colorModificar);
                            } else {
                                c.setBackground((row % 2 == 0) ? zebraA : zebraB);
                            }
                        } else {
                            // Efecto zebra para las demás columnas
                            c.setBackground((row % 2 == 0) ? zebraA : zebraB);
                        }
                    } catch (Exception e) {
                        c.setBackground((row % 2 == 0) ? zebraA : zebraB);
                    }
                }
                
                if (c instanceof JComponent) {
                    ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                }
                
                return c;
            }
        });
    }

    private ImageIcon scaledIcon(String resourcePath, int w, int h) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            BufferedImage img = ImageIO.read(is);
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Modificaciones_Anteriores().setVisible(true));
    }
}
