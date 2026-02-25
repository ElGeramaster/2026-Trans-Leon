package GestionSoftware;

import java.awt.event.InputEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class Registros extends JFrame {

    // Columnas visibles (UI)
    private static final String[] COLUMNAS = {
            "CARTA PORTE", "CLIENTE", "FACTURA", "FECHA FACTURA", "VALOR", "FECHA DE PAGO",
            "DESTINO", "REFERENCIA", "REMITENTE", "CONSIGNATORIO", "FACTURA",
            "OPERADOR", "PLACA CABEZAL", "PLACA DEL FURGON", "VALOR FLETE", "ANTICIPO",
            "A.CANCELACION", "FECHA DE PAGADO", "F. DE CARGA", "F. DE CRUCE", "F. SAL. T.U.",
            "F.F. DESTINO", "F. EN. DESTINO", "F. DESCARGA", "F.E. DE DOCTOS.", "SEGURIDAD PRIVADA",
            "VALOR CUSTODIO", "F. PAGO CUSTODIO", "OBSERVACIONES"
    };

    // Columnas en BD (mismo orden que COLUMNAS, pero con nombres reales)
    private static final String[] COLUMNAS_DB = {
            "Carta_Porte_id",      // 0
            "ID_Cliente",          // 1
            "FACTURA",             // 2
            "FECHA_FACTURA",       // 3
            "VALOR",               // 4
            "FECHA_DE_PAGO",       // 5
            "DESTINO",             // 6
            "REFERENCIA",          // 7
            "ID_Remitente",        // 8
            "ID_Consignatario",    // 9
            "FACTURA2",            // 10
            "ID_Operador",         // 11
            "ID_Placa_Cabezal",    // 12
            "ID_Placa_Del_Furgon", // 13
            "VALOR_FLETE",         // 14
            "ANTICIPO",            // 15
            "A_CANCELACION",       // 16
            "FECHA_DE_PAGADO",     // 17
            "F_DE_CARGA",          // 18
            "F_DE_CRUCE",          // 19
            "F_SAL_T_U",           // 20
            "F_F_DESTINO",         // 21
            "F_EN_DESTINO",        // 22
            "F_DESCARGA",          // 23
            "F_E_DE_DOCTOS",       // 24
            "ID_Custodio",         // 25
            "VALOR_CUSTODIO",      // 26
            "FECHA_PAGO_CUSTODIO", // 27
            "OBSERVACIONES"        // 28
    };

    // Índices útiles (modelo)
    private static final int IDX_ID              = 0;
    private static final int IDX_CLIENTE         = 1;
    private static final int IDX_FECHA_FACTURA   = 3;
    private static final int IDX_VALOR           = 4;
    private static final int IDX_DESTINO         = 6;
    private static final int IDX_REMITENTE       = 8;
    private static final int IDX_CONSIGNATORIO   = 9;
    private static final int IDX_OPERADOR        = 11;
    private static final int IDX_PLACA_CABEZAL   = 12;
    private static final int IDX_PLACA_FURGON    = 13;
    private static final int IDX_VALOR_FLETE     = 14;
    private static final int IDX_ANTICIPO        = 15;
    private static final int IDX_A_CANCELACION   = 16;
    private static final int IDX_VALOR_CUSTODIO  = 26;
    private static final int IDX_CUSTODIO        = 25;

    // Tabla
    public static DefaultTableModel modeloTabla;
    private JTable tabla;
    private JScrollPane scrollPane;

    // Buscador
    private JTextField txtBuscar;
    private JComboBox<String> cbFiltro; // (si la usas en algún momento)
    private TableRowSorter<DefaultTableModel> sorter;

    // Selector de vistas de columnas
    private JComboBox<String> cbMostrarColumnas;
    private final Map<String, int[]> FiltrarColumnas = new LinkedHashMap<>();
    private static final String CREAR_FILTRO_LABEL = "➕ Crear filtro…";
    private static final String ELIMINAR_FILTRO_LABEL = "🗑 Eliminar filtro actual…";
    private String ultimaVistaSeleccionada = "Todos";
    private TableColumn[] columnasOriginales;

    // LOGO E ICONOS
    private JLabel logoLabel;
    private static final String LOGOImagen = "/GestionSoftware/imagenes/LogoLeon.png";
    private static final String CambiosIcono = "/GestionSoftware/imagenes/Cambios.png";
    private static final String DatosIcono   = "/GestionSoftware/imagenes/DATOS.png";

    // Locale
    private static final java.util.Locale LOCALE_ES_MX = new java.util.Locale("es","MX");

    // === MONEDAS ===
    private enum Moneda {
        DOLAR("$"),
        MXN("$ MXN"),
        QUETZAL("$Q");

        private final String label;
        Moneda(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    /**
     * Normaliza texto con moneda a solo número crudo.
     */
    private static String stripMoneyToRaw(String s) {
        if (s == null) return "";
        String t = s.trim().toUpperCase(Locale.ROOT);
        t = t.replace("MXN", "");
        t = t.replace("GTQ", "");
        t = t.replace("Q", "");
        t = t.replace("$", "");
        t = t.replace(" ", "");
        t = t.replace(",", "");
        t = t.replaceAll("[^0-9.\\-]", "");
        return t.trim();
    }

    // Filtros por columna (CLIENTE / OPERADOR)
    private final Map<Integer, LinkedHashSet<String>> filtrosPorColumna = new HashMap<>();
    private RowFilter<DefaultTableModel, Object> rfBusqueda = null;

    // Botones principales
    private JButton btnDatos;
    private JButton btnAgregar;
    private JButton btnModificar;
    private JButton btnEliminar;
    private JButton btnModificaciones;

    // Sugerencias (autocompletado)
    private final Map<Integer, LinkedHashSet<String>> sugerencias = new HashMap<>();
    private final Map<Integer, TableCellEditor> editorsAuto = new HashMap<>();
    private final int[] COLS_AUTOCOMPLETE = {
            IDX_CLIENTE, IDX_OPERADOR, IDX_DESTINO, IDX_REMITENTE, IDX_CONSIGNATORIO,
            IDX_PLACA_CABEZAL, IDX_PLACA_FURGON
    };

    // Layout
    private JLabel lblTitulo;
    private JButton B1;
    private JLabel lblBuscarLabel;
    private JLabel lblFiltrarColsLabel;

    // Pantalla completa
    private boolean fullScreen = false;
    private Rectangle windowedBounds = null;

    // Cache logo
    private int lastLogoW = -1, lastLogoH = -1;

    // ==== FECHAS ===========================
    private static boolean esColumnaFecha(String col) {
        if (col == null) return false;
        String c = col.toUpperCase(Locale.ROOT);
        return c.startsWith("FECHA") || c.startsWith("F_");
    }

    private static String formatearFechaBonita(java.util.Date d) {
        if (d == null) return "";
        java.text.DateFormatSymbols dfs = new java.text.DateFormatSymbols(LOCALE_ES_MX);
        String[] months = dfs.getMonths();
        for (int i = 0; i < months.length; i++) {
            if (months[i] != null && !months[i].isEmpty()) {
                months[i] = months[i].substring(0,1).toUpperCase(LOCALE_ES_MX) + months[i].substring(1);
            }
        }
        dfs.setMonths(months);
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("d 'de' MMMM 'del' yyyy", dfs);
        fmt.setLenient(false);
        return fmt.format(d);
    }

    private static java.util.Date parseFechaFlexible(String input) {
        if (input == null) return null;
        String t = input.trim();
        if (t.isEmpty()) return null;

        String tl = t.toLowerCase(LOCALE_ES_MX).trim();
        if (tl.equals("hoy")) return new java.util.Date();

        final String[] patronesConAnio = {
                "yyyy-MM-dd",
                "yyyy-MM-dd HH:mm",
                "dd/MM/yyyy",
                "d/M/yyyy",
                "dd-MM-yyyy",
                "d-M-yyyy",
                "d 'de' MMMM 'del' yyyy"
        };

        for (String patron : patronesConAnio) {
            try {
                java.text.SimpleDateFormat in = new java.text.SimpleDateFormat(patron, LOCALE_ES_MX);
                in.setLenient(false);
                return in.parse(t);
            } catch (Exception ignore) {}
        }

        try {
            if (t.matches("^\\d{1,2}[-/]\\d{1,2}$")) {
                String[] parts = t.split("[-/]");
                int dia = Integer.parseInt(parts[0]);
                int mes = Integer.parseInt(parts[1]);

                Calendar cal = Calendar.getInstance(LOCALE_ES_MX);
                int anio = cal.get(Calendar.YEAR);

                cal.setLenient(false);
                cal.set(Calendar.YEAR, anio);
                cal.set(Calendar.MONTH, mes - 1);
                cal.set(Calendar.DAY_OF_MONTH, dia);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                return cal.getTime();
            }
        } catch (Exception ignore) {}

        return null;
    }

    private static String convertirEntradaFechaBonita(String texto) {
        if (texto == null) return "";
        String t = texto.trim();
        if (t.isEmpty()) return "";
        java.util.Date d = parseFechaFlexible(t);
        return (d != null) ? formatearFechaBonita(d) : texto;
    }

    private static String normalizeFecha(String fecha) {
        if (fecha == null) return "";
        String t = fecha.trim();
        if (t.isEmpty()) return "";

        java.util.Date d = parseFechaFlexible(t);
        if (d != null) {
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("yyyy-MM-dd");
            out.setLenient(false);
            return out.format(d);
        }
        return t;
    }

    // ==== MONEDA helpers ===================
    private static String formatearMoneda(String s) {
        if (s == null || s.trim().isEmpty()) return "";
        try {
            java.math.BigDecimal bd = new java.math.BigDecimal(s.replace("$","").replace(",","").trim());
            java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(LOCALE_ES_MX);
            return nf.format(bd);
        } catch (Exception ex) { return s; }
    }

    private BigDecimal parseDecimal(String moneda) {
        if (moneda == null) return null;
        String raw = stripMoneyToRaw(moneda);
        if (raw.isEmpty() || raw.equals("-") || raw.equals(".")) return null;
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatearConMoneda(BigDecimal valor, Moneda moneda) {
        if (valor == null) return "";
        if (moneda == null) moneda = Moneda.DOLAR;

        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(java.util.Locale.US);
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String numero = nf.format(valor);

        switch (moneda) {
            case MXN:
                return "$ " + numero + " MXN";
            case QUETZAL:
                return "Q " + numero;
            case DOLAR:
            default:
                return "$ " + numero;
        }
    }

    private String monedaToDbCode(Moneda moneda) {
        if (moneda == null) return "USD";
        switch (moneda) {
            case MXN:      return "MXN";
            case QUETZAL:  return "GTQ";
            case DOLAR:
            default:       return "USD";
        }
    }

    // NUEVO: leer código de moneda desde la BD y convertir a enum
    private Moneda monedaFromDbCode(String code) {
        if (code == null) return Moneda.DOLAR;
        String c = code.trim().toUpperCase(java.util.Locale.ROOT);
        switch (c) {
            case "MXN":
                return Moneda.MXN;
            case "GTQ":
            case "Q":
            case "QTZ":
                return Moneda.QUETZAL;
            case "USD":
            default:
                return Moneda.DOLAR;
        }
    }

    private static Moneda inferirMonedaPorTexto(String txt) {
        if (txt == null) return Moneda.DOLAR;
        String t = txt.toUpperCase(Locale.ROOT).trim();
        if (t.contains("MXN")) return Moneda.MXN;
        if (t.startsWith("Q") || t.contains(" GTQ") || t.contains(" Q")) return Moneda.QUETZAL;
        if (t.contains("GTQ") || t.contains("QTZ")) return Moneda.QUETZAL;
        if (t.contains("USD")) return Moneda.DOLAR;
        return Moneda.DOLAR;
    }

    private String asStr(Object objeto) { return (objeto == null) ? "" : String.valueOf(objeto).trim(); }

    // ==== Carta Porte consecutivo ==========
    private int obtenerSiguienteCartaPorte() {
        String sql = "SELECT COALESCE(MAX(Carta_Porte_id), 0) + 1 AS next_id FROM Carta_Porte";
        try (Connection c = obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("next_id");
        } catch (SQLException ex) {
            System.err.println("[Carta_Porte] No se pudo calcular siguiente ID: " + ex.getMessage());
        }
        return 1;
    }

    private Integer parseEnteroPositivo(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            int n = Integer.parseInt(t);
            return (n > 0) ? n : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean existeCartaPorte(Connection conn, int id) throws SQLException {
        String sql = "SELECT 1 FROM Carta_Porte WHERE Carta_Porte_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Registros () {
        setTitle("Registros");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setSize(1500, 850);
        setMinimumSize(new Dimension(1100, 700));
        getContentPane().setLayout(null);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(211,211,211));
        setResizable(true);

        lblTitulo = new JLabel("ARCHIVO GENERAL DE VIAJES", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("ethnocentric", Font.BOLD, 30));
        getContentPane().add(lblTitulo);

        B1 = new JButton("<");
        B1.setBackground(Color.WHITE);
        B1.setFocusPainted(false);
        getContentPane().add(B1);
        B1.setBounds(10, 15, 60, 60);

        B1.setForeground(Color.BLACK);
        B1.setFocusPainted(false);
        B1.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        B1.setToolTipText("Regresar a Registros");
        B1.setPreferredSize(new Dimension(60, 60));
        B1.setMinimumSize(new Dimension(60, 60));
        B1.setMaximumSize(new Dimension(60, 60));

        B1.addActionListener(e -> {
            try {
                WindowState.switchTo(this, new Ingresar());
            } catch (Throwable ignore) {
                dispose();
            }
        });

        B1.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent evt) { B1.setBackground(Color.RED); }
            @Override public void mouseExited (MouseEvent evt) { B1.setBackground(Color.WHITE); }
        });

        JRootPane raizEsc = getRootPane();
        KeyStroke TeclaSalir = KeyStroke.getKeyStroke("ESCAPE");
        raizEsc.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(TeclaSalir, "ESCAPE");
        raizEsc.getActionMap().put("ESCAPE", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { B1.doClick(); }
        });

        // MODELO TABLA
        modeloTabla = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == IDX_ID) return Integer.class;
                return Object.class;
            }
        };
        for (String c : COLUMNAS) modeloTabla.addColumn(c);

        tabla = new JTable(modeloTabla);
        tabla.setRowHeight(24);
        tabla.setFont(new Font("Arial", Font.BOLD, 12));
        tabla.setBackground(Color.WHITE);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.setCellSelectionEnabled(true);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.putClientProperty("terminateEditOnFocusLost", Boolean.FALSE);

        JTableHeader header = tabla.getTableHeader();
        header.setFont(new Font("Poppins", Font.BOLD, 14));
        header.setBackground(new Color(135, 206, 235));
        header.setReorderingAllowed(true);
        header.setResizingAllowed(true);

        sorter = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorter);
        sorter.setSortKeys(java.util.List.of(new RowSorter.SortKey(IDX_ID, SortOrder.ASCENDING)));
        sorter.sort();

        instalarFiltrosPorColumna();

        scrollPane = new JScrollPane(tabla);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        getContentPane().add(scrollPane);

        scrollPane.addMouseWheelListener(e -> {
            if (e.isShiftDown()) {
                JScrollBar h = scrollPane.getHorizontalScrollBar();
                int amount = e.getUnitsToScroll() * h.getUnitIncrement();
                h.setValue(h.getValue() + amount);
                e.consume();
            }
        });

        // ===== COPIAR CELDAS SELECCIONADAS CON Ctrl+C =====
        KeyStroke copiar = KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.CTRL_DOWN_MASK);
        tabla.getInputMap(JComponent.WHEN_FOCUSED).put(copiar, "copiarCeldas");
        tabla.getActionMap().put("copiarCeldas", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                copiarSeleccionAlPortapapeles();
            }
        });

        // ===== DOBLE CLIC SELECCIONA FILA ENTERA =====
        tabla.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = tabla.rowAtPoint(e.getPoint());
                    if (viewRow < 0) return;
                    tabla.setRowSelectionInterval(viewRow, viewRow);
                    tabla.setColumnSelectionInterval(0, tabla.getColumnCount() - 1);
                }
            }
        });

        configurarAnchosColumnas();
        aplicarRendererNumericos();
        aplicarColoresColumnasTexto();
        crearBarraBusqueda();
        crearSelectorMostrarColumnas();

        initSugerencias();

       
        
        ImageIcon iconDatos      = scaledIcon(DatosIcono, 44, 44);
        ImageIcon iconDatosOver  = scaledIcon(DatosIcono, 48, 48);
        ImageIcon iconDatosPress = scaledIcon(DatosIcono, 42, 42);

        if (iconDatos == null) {
            btnDatos = new JButton("Clientes");
            btnDatos.setFont(new Font("Poppins", Font.BOLD,16));
            btnDatos.setBackground(new Color(192, 192, 192));
            btnDatos.setForeground(Color.WHITE);
            btnDatos.setFocusPainted(false);
        } else {
            btnDatos = new JButton(iconDatos);
            if (iconDatosOver  != null) btnDatos.setRolloverIcon(iconDatosOver);
            if (iconDatosPress != null) btnDatos.setPressedIcon(iconDatosPress);
            btnDatos.setBorder(BorderFactory.createEmptyBorder());
            btnDatos.setContentAreaFilled(false);
            btnDatos.setFocusPainted(false);
            btnDatos.setOpaque(false);
            btnDatos.setToolTipText("Clientes");
            btnDatos.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        
        
        getContentPane().add(btnDatos);

        btnDatos.addActionListener(e -> {
            try {
                WindowState.switchTo(this, new Datos());
            } catch (Throwable ignore) {
                dispose();
            }
        });

        btnAgregar = new JButton("Agregar");
        btnAgregar.setFont(new Font("Poppins", Font.BOLD,16));
        btnAgregar.setBackground(new Color(163, 231, 214));
        btnAgregar.setForeground(Color.BLACK);
        btnAgregar.setFocusPainted(false);
        getContentPane().add(btnAgregar);

        btnModificar = new JButton("Modificar");
        btnModificar.setFont(new Font("Poppins", Font.BOLD,16));
        btnModificar.setBackground(new Color(218, 194, 254));
        btnModificar.setForeground(Color.BLACK);
        btnModificar.setFocusPainted(false);
        getContentPane().add(btnModificar);

        btnEliminar = new JButton("Eliminar");
        btnEliminar.setFont(new Font("Poppins", Font.BOLD,16));
        btnEliminar.setBackground(new Color(229, 115, 115));
        btnEliminar.setForeground(Color.BLACK);
        btnEliminar.setFocusPainted(false);
        getContentPane().add(btnEliminar);

        ImageIcon iconCambios      = scaledIcon(CambiosIcono, 44, 44);
        ImageIcon iconCambiosOver  = scaledIcon(CambiosIcono, 48, 48);
        ImageIcon iconCambiosPress = scaledIcon(CambiosIcono, 42, 42);

        if (iconCambios == null) {
            btnModificaciones = new JButton("Cambios");
            btnModificaciones.setFont(new Font("Poppins", Font.BOLD,16));
            btnModificaciones.setBackground(new Color(229, 115, 115));
            btnModificaciones.setForeground(Color.BLACK);
        } else {
            btnModificaciones = new JButton(iconCambios);
            if (iconCambiosOver  != null) btnModificaciones.setRolloverIcon(iconCambiosOver);
            if (iconCambiosPress != null) btnModificaciones.setPressedIcon(iconCambiosPress);
            btnModificaciones.setBorder(BorderFactory.createEmptyBorder());
            btnModificaciones.setContentAreaFilled(false);
            btnModificaciones.setFocusPainted(false);
            btnModificaciones.setOpaque(false);
            btnModificaciones.setToolTipText("Ver modificaciones anteriores");
            btnModificaciones.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        getContentPane().add(btnModificaciones);

        btnModificaciones.addActionListener(e -> {
            try {
                WindowState.switchTo(this, new Modificaciones_Anteriores());
            } catch (Throwable ignore) {
                dispose();
            }
        });

        // Botón Agregar: abre ventana Ingresar para nuevo registro
        btnAgregar.addActionListener(e -> {
            try {
                WindowState.switchTo(this, new Ingresar());
            } catch (Throwable ignore) {
                dispose();
            }
        });

        btnModificar.addActionListener(e -> {
            int filaSeleccionada = tabla.getSelectedRow();
            if (filaSeleccionada == -1) {
                JOptionPane.showMessageDialog(this, "Selecciona un registro para modificar.");
                return;
            }

            // Obtener el ID de Carta Porte del registro seleccionado
            int modelRow = tabla.convertRowIndexToModel(filaSeleccionada);
            Object cartaPorteObj = modeloTabla.getValueAt(modelRow, IDX_ID);

            if (cartaPorteObj == null) {
                JOptionPane.showMessageDialog(this, "No se pudo obtener el ID de la Carta Porte.");
                return;
            }

            int cartaPorteId;
            try {
                cartaPorteId = Integer.parseInt(cartaPorteObj.toString());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID de Carta Porte inválido.");
                return;
            }

            // Abrir ventana Ingresar con el ID para cargar y editar
            try {
                Ingresar ventanaIngresar = new Ingresar();
                ventanaIngresar.cargarYEditarCartaPorte(cartaPorteId);
                WindowState.switchTo(this, ventanaIngresar);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al abrir la ventana de edición: " + ex.getMessage());
            }
        });

        btnEliminar.addActionListener(e -> {
            int filaSeleccionada = tabla.getSelectedRow();
            if (filaSeleccionada == -1) {
                JOptionPane.showMessageDialog(this, "Selecciona un registro para eliminar.");
                return;
            }
            int modelRow = tabla.convertRowIndexToModel(filaSeleccionada);
            String nCaso = modeloTabla.getValueAt(modelRow, IDX_ID).toString();
            eliminarRegistro(nCaso);
        });

        logoLabel = new JLabel();
        logoLabel.setOpaque(false);
        getContentPane().add(logoLabel);

        ImageIcon appIcon = scaledIcon(LOGOImagen, 32, 32);
        if (appIcon != null) setIconImage(appIcon.getImage());

        snapshotColumnasOriginales();
        cargarDatos();
        instalarResponsiveUI();
        SwingUtilities.invokeLater(this::ajustarLayout);
        WindowState.installF11(this);
        
    }

    // ================== COPIAR / SELECCIONAR TEXTO ========

    /**
     * Copia las celdas seleccionadas al portapapeles.
     * Formato: columnas separadas por TAB, filas separadas por salto de línea.
     * Compatible para pegar en Excel, Google Sheets, etc.
     */
    private void copiarSeleccionAlPortapapeles() {
        int[] selRows = tabla.getSelectedRows();
        int[] selCols = tabla.getSelectedColumns();

        if (selRows.length == 0 || selCols.length == 0) return;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < selRows.length; i++) {
            for (int j = 0; j < selCols.length; j++) {
                if (j > 0) sb.append('\t');
                Object val = tabla.getValueAt(selRows[i], selCols[j]);
                sb.append(val == null ? "" : val.toString());
            }
            if (i < selRows.length - 1) sb.append('\n');
        }

        java.awt.datatransfer.StringSelection sel =
                new java.awt.datatransfer.StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
    }

    // ================== SUGERENCIAS =====================

    private void initSugerencias() {
        for (int col : COLS_AUTOCOMPLETE) {
            sugerencias.put(col, new LinkedHashSet<>());
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}+","").toLowerCase(java.util.Locale.ROOT).trim();
        return n;
    }

    private void RegistrosGuardados(int col, String value) {
        LinkedHashSet<String> set = sugerencias.get(col);
        if (set == null) return;
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        String nv = normalize(v);
        boolean existe = false;
        for (String s : set) {
            if (normalize(s).equals(nv)) { existe = true; break; }
        }
        if (!existe) set.add(v);
    }

    private void addSuggestionsFromRow(Object[] row) {
        if (row == null) return;
        RegistrosGuardados(IDX_CLIENTE,       asStr(row[IDX_CLIENTE]));
        RegistrosGuardados(IDX_OPERADOR,      asStr(row[IDX_OPERADOR]));
        RegistrosGuardados(IDX_DESTINO,       asStr(row[IDX_DESTINO]));
        RegistrosGuardados(IDX_REMITENTE,     asStr(row[IDX_REMITENTE]));
        RegistrosGuardados(IDX_CONSIGNATORIO, asStr(row[IDX_CONSIGNATORIO]));
        RegistrosGuardados(IDX_PLACA_CABEZAL, asStr(row[IDX_PLACA_CABEZAL]));
        RegistrosGuardados(IDX_PLACA_FURGON,  asStr(row[IDX_PLACA_FURGON]));
    }

    // ================== RENDERERS ===============

    private void aplicarRendererNumericos() {
        javax.swing.table.DefaultTableCellRenderer right = new javax.swing.table.DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                if (value == null) { super.setValue(""); return; }
                String s = value.toString();
                try {
                    new java.math.BigDecimal(s.replace("$","").replace(",","").trim());
                    super.setValue(formatearMoneda(s));
                } catch (Exception e) {
                    super.setValue(s);
                }
            }
        };
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        int[] numCols = {IDX_VALOR, IDX_VALOR_FLETE, IDX_ANTICIPO, IDX_A_CANCELACION, IDX_VALOR_CUSTODIO};
        TableColumnModel tcm = tabla.getColumnModel();
        for (int modelIdx : numCols) {
            int viewIdx = tabla.convertColumnIndexToView(modelIdx);
            if (viewIdx >= 0 && viewIdx < tcm.getColumnCount()) {
                tcm.getColumn(viewIdx).setCellRenderer(right);
            }
        }
    }

    private void aplicarColoresColumnasTexto() {
        if (tabla == null) return;
        TableColumnModel tcm = tabla.getColumnModel();

        int viewIdxCarta = tabla.convertColumnIndexToView(IDX_ID);
        if (viewIdxCarta >= 0 && viewIdxCarta < tcm.getColumnCount()) {
            tcm.getColumn(viewIdxCarta).setCellRenderer(
                    new javax.swing.table.DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(
                                JTable table, Object value,
                                boolean isSelected, boolean hasFocus,
                                int row, int column) {
                            Component c = super.getTableCellRendererComponent(
                                    table, value, isSelected, hasFocus, row, column);
                            if (!isSelected) c.setForeground(Color.RED);
                            else c.setForeground(table.getSelectionForeground());
                            return c;
                        }
                    }
            );
        }

        int viewIdxCliente = tabla.convertColumnIndexToView(IDX_CLIENTE);
        if (viewIdxCliente >= 0 && viewIdxCliente < tcm.getColumnCount()) {
            tcm.getColumn(viewIdxCliente).setCellRenderer(
                    new javax.swing.table.DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(
                                JTable table, Object value,
                                boolean isSelected, boolean hasFocus,
                                int row, int column) {
                            Component c = super.getTableCellRendererComponent(
                                    table, value, isSelected, hasFocus, row, column);
                            if (!isSelected) c.setForeground(new Color(0, 0, 128));
                            else c.setForeground(table.getSelectionForeground());
                            return c;
                        }
                    }
            );
        }
    }

    // ================== BUSCADOR ========================

    private void crearBarraBusqueda() {
        lblBuscarLabel = new JLabel("Buscar:");
        lblBuscarLabel.setFont(new Font("Poppins", Font.BOLD, 14));
        getContentPane().add(lblBuscarLabel);

        txtBuscar = new JTextField();
        txtBuscar.setToolTipText("Escribe para filtrar filas… (ENTER aplica, ESC limpia)");
        getContentPane().add(txtBuscar);

        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltro(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltro(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { aplicarFiltro(); }
        });

        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) aplicarFiltro();
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { txtBuscar.setText(""); aplicarFiltro(); }
            }
        });
    }

    private void aplicarFiltro() {
        if (sorter == null) return;

        final String query = (txtBuscar.getText() == null) ? "" : txtBuscar.getText().trim();
        if (query.isEmpty()) {
            rfBusqueda = null;
            aplicarFiltrosCombinados();
            return;
        }

        final int[] columnas = columnasSegunFiltro();
        final String[] tokens = Arrays.stream(query.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        rfBusqueda = new RowFilter<DefaultTableModel, Object>() {
            private String norm(String s) {
                if (s == null) return "";
                String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
                return n.replaceAll("\\p{M}+","").toLowerCase(java.util.Locale.ROOT);
            }
            private boolean containsNorm(String haystack, String needle) {
                return norm(haystack).contains(norm(needle));
            }
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                int[] cols = (columnas == null || columnas.length == 0)
                        ? java.util.stream.IntStream.range(0, modeloTabla.getColumnCount()).toArray()
                        : columnas;

                for (String t : tokens) {
                    boolean okToken = false;
                    for (int c : cols) {
                        if (c >= 0 && c < entry.getValueCount()) {
                            Object v = entry.getValue(c);
                            if (v != null && containsNorm(v.toString(), t)) { okToken = true; break; }
                        }
                    }
                    if (!okToken) return false;
                }
                return true;
            }
        };

        aplicarFiltrosCombinados();
    }

    private int[] columnasSegunFiltro() {
        if (cbFiltro == null) return columnasDeVistaActual();
        String op = (String) cbFiltro.getSelectedItem();
        if (op == null || op.startsWith("Auto")) return columnasDeVistaActual();
        return columnasParaFiltrarFilas(op);
    }

    private int[] columnasDeVistaActual() {
        int[] keep = FiltrarColumnas.getOrDefault(ultimaVistaSeleccionada, FiltrarColumnas.get("Todos"));
        if (keep == null || keep.length == 0) {
            int cols = modeloTabla.getColumnCount();
            int[] all = new int[cols];
            for (int i = 0; i < cols; i++) all[i] = i;
            return all;
        }
        return keep;
    }

    private int[] columnasParaFiltrarFilas(String opcion) {
        if (opcion == null) opcion = "todos";
        String k = opcion.toLowerCase(java.util.Locale.ROOT);
        k = java.text.Normalizer.normalize(k, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","")
                .replaceAll("\\s+"," ")
                .trim();

        switch (k) {
            case "cliente":
                return new int[]{1};
            case "datos del operador":
            case "datos del operador y custodio":
                return new int[]{11, 25};
            case "placas de camion y conductor":
            case "placas del camion y conductor":
                return new int[]{12, 13, 11};
            case "datos del furgon":
                return new int[]{13};
            case "referencia del cliente":
            case "referencia del cliente (viaje)":
                return new int[]{7};
            case "informacion del viaje":
            case "información del viaje":
                return new int[]{6, 18, 19, 20, 21, 22, 23};
            case "medio de carta de poder":
                return new int[]{0, 2, 10};
            case "todos":
            default:
                int cols = modeloTabla.getColumnCount();
                int[] all = new int[cols];
                for (int i = 0; i < cols; i++) all[i] = i;
                return all;
        }
    }

    // ============== SELECTOR DE COLUMNAS ===================

    private void crearSelectorMostrarColumnas() {
        FiltrarColumnas.clear();
        FiltrarColumnas.put("Todos", new int[] {
                0,1,2,3,4,5,6,7,8,9,10,
                11,12,13,14,15,16,17,18,19,20,
                21,22,23,24,25,26,27,28
        });

        asegurarTablaFiltros();
        cargarFiltrosPersonalizadosDesdeDB();

        lblFiltrarColsLabel = new JLabel("Filtrar Columnas:");
        lblFiltrarColsLabel.setFont(new Font("Poppins", Font.BOLD, 14));
        getContentPane().add(lblFiltrarColsLabel);

        cbMostrarColumnas = new JComboBox<>();
        cbMostrarColumnas.setToolTipText("Cambia la vista de columnas mostradas o crea un filtro personalizado");
        getContentPane().add(cbMostrarColumnas);

        refrescarComboVistas("Todos");

        cbMostrarColumnas.addActionListener(e -> {
            String key = (String) cbMostrarColumnas.getSelectedItem();
            if (key == null) return;

            if (CREAR_FILTRO_LABEL.equals(key)) {
                boolean creado = mostrarDialogoCrearFiltro();
                if (!creado) cbMostrarColumnas.setSelectedItem(ultimaVistaSeleccionada);
                return;
            }

            if (ELIMINAR_FILTRO_LABEL.equals(key)) {
                eliminarFiltroActual();
                return;
            }

            ultimaVistaSeleccionada = key;
            int[] keep = FiltrarColumnas.getOrDefault(key, FiltrarColumnas.get("Todos"));
            aplicarVistaColumnas(keep);
        });
    }

    private void refrescarComboVistas(String seleccionar) {
        cbMostrarColumnas.removeAllItems();
        for (String k : FiltrarColumnas.keySet()) cbMostrarColumnas.addItem(k);
        cbMostrarColumnas.addItem(CREAR_FILTRO_LABEL);
        cbMostrarColumnas.addItem(ELIMINAR_FILTRO_LABEL);

        if (seleccionar == null || !FiltrarColumnas.containsKey(seleccionar)) seleccionar = "Todos";
        cbMostrarColumnas.setSelectedItem(seleccionar);
        ultimaVistaSeleccionada = seleccionar;
    }

    private boolean mostrarDialogoCrearFiltro() {
        JDialog dlg = new JDialog(this, "Nuevo Filtro", true);
        dlg.setLayout(new BorderLayout(10,10));
        dlg.setSize(520, 560);
        dlg.setLocationRelativeTo(this);

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        top.add(new JLabel("Nombre del filtro:"), gc);

        JTextField tfNombre = new JTextField(26);
        gc.gridx = 1;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        top.add(tfNombre, gc);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBorder(BorderFactory.createTitledBorder("Selecciona las columnas a mostrar"));
        GridBagConstraints cc = new GridBagConstraints();
        cc.insets = new Insets(4,10,4,10);
        cc.anchor = GridBagConstraints.WEST;
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.weightx = 1.0;

        JCheckBox[] Casillas = new JCheckBox[COLUMNAS.length];
        int row = 0;

        JCheckBox chkSeleccionarTodo = new JCheckBox("Seleccionar todo");
        chkSeleccionarTodo.setFont(new Font("Poppins", Font.BOLD, 12));
        cc.gridx = 0;
        cc.gridy = row++;
        center.add(chkSeleccionarTodo, cc);

        for (int i = 0; i < COLUMNAS.length; i++) {
            Casillas[i] = new JCheckBox("[" + i + "] " + COLUMNAS[i]);
            Casillas[i].setSelected(false);
            cc.gridx = 0;
            cc.gridy = row++;
            center.add(Casillas[i], cc);
        }

        JScrollPane sp = new JScrollPane(center);
        sp.setPreferredSize(new Dimension(480, 380));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnGuardar = new JButton("Guardar filtro");
        bottom.add(btnCancelar);
        bottom.add(btnGuardar);

        dlg.add(top, BorderLayout.NORTH);
        dlg.add(sp, BorderLayout.CENTER);
        dlg.add(bottom, BorderLayout.SOUTH);

        final boolean[] ok = {false};
        final boolean[] actualizando = {false};

        chkSeleccionarTodo.addActionListener(ev -> {
            actualizando[0] = true;
            boolean sel = chkSeleccionarTodo.isSelected();
            for (JCheckBox cb : Casillas) if (cb != null) cb.setSelected(sel);
            actualizando[0] = false;
        });

        for (JCheckBox cb : Casillas) {
            cb.addItemListener(e -> {
                if (actualizando[0]) return;
                boolean todas = true;
                boolean alguna = false;
                for (JCheckBox cbox : Casillas) {
                    if (cbox != null) {
                        if (cbox.isSelected()) alguna = true;
                        else todas = false;
                    }
                }
                chkSeleccionarTodo.setSelected(todas && alguna);
            });
        }

        btnCancelar.addActionListener(ev -> dlg.dispose());

        btnGuardar.addActionListener(ev -> {
            String nombre = tfNombre.getText().trim();
            if (nombre.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Asigna un nombre al filtro.", "Falta nombre", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (CREAR_FILTRO_LABEL.equals(nombre) || ELIMINAR_FILTRO_LABEL.equals(nombre)) {
                JOptionPane.showMessageDialog(dlg, "Ese nombre está reservado. Elige otro.", "Nombre inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (FiltrarColumnas.containsKey(nombre)) {
                int r2 = JOptionPane.showConfirmDialog(dlg,
                        "Ya existe un filtro llamado \""+nombre+"\".\n¿Deseas reemplazarlo?",
                        "Duplicado",
                        JOptionPane.YES_NO_OPTION);
                if (r2 != JOptionPane.YES_OPTION) return;
            }

            java.util.List<Integer> sel = new ArrayList<>();
            for (int i = 0; i < Casillas.length; i++) if (Casillas[i].isSelected()) sel.add(i);

            if (sel.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Selecciona al menos una columna.", "Nada seleccionado", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int[] keep = new int[sel.size()];
            for (int i = 0; i < sel.size(); i++) keep[i] = sel.get(i);

            FiltrarColumnas.put(nombre, keep);
            refrescarComboVistas(nombre);
            aplicarVistaColumnas(keep);
            guardarFiltroPersonalizadoEnDB(nombre, keep);

            ok[0] = true;
            dlg.dispose();
        });

        dlg.setVisible(true);
        return ok[0];
    }

    private void snapshotColumnasOriginales() {
        TableColumnModel tcm = tabla.getColumnModel();
        int count = tcm.getColumnCount();
        columnasOriginales = new TableColumn[modeloTabla.getColumnCount()];
        for (int viewIndex = 0; viewIndex < count; viewIndex++) {
            TableColumn tc = tcm.getColumn(viewIndex);
            int modelIdx = tabla.convertColumnIndexToModel(viewIndex);
            tc.setIdentifier(modelIdx);
            columnasOriginales[modelIdx] = tc;
        }
    }

    private void aplicarVistaColumnas(int[] keep) {
        if (columnasOriginales == null) snapshotColumnasOriginales();
        Set<Integer> mantener = new LinkedHashSet<>();
        for (int k : keep) if (k >= 0 && k < columnasOriginales.length) mantener.add(k);

        TableColumnModel tcm = tabla.getColumnModel();
        for (int i = tcm.getColumnCount() - 1; i >= 0; i--) tcm.removeColumn(tcm.getColumn(i));

        if (mantener.size() == columnasOriginales.length) {
            for (int i = 0; i < columnasOriginales.length; i++) if (columnasOriginales[i] != null) tcm.addColumn(columnasOriginales[i]);
        } else {
            for (int k : keep) {
                TableColumn col = columnasOriginales[k];
                if (col != null) tcm.addColumn(col);
            }
        }

        configurarAnchosColumnas();
        aplicarRendererNumericos();
        aplicarColoresColumnasTexto();
        tabla.revalidate();
        tabla.repaint();
        aplicarFiltro();
        ajustarLayout();
    }

    // ================== CONEXIÓN BD ========================

    private Connection obtenerConexion() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/EmpresaLog"
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=America/Merida";
        return DriverManager.getConnection(url, "admin", "12345ñ");
    }

    private Integer obtenerIdCatalogo(Connection conn, String tabla, String colId, String colNombre, String valorNombre) throws SQLException {
        if (valorNombre == null) return null;
        String nombre = valorNombre.trim();
        if (nombre.isEmpty()) return null;

        String select = "SELECT " + colId + " FROM " + tabla + " WHERE " + colNombre + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        String insert = "INSERT INTO " + tabla + " (" + colNombre + ") VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        return null;
    }

    private Integer obtenerIdVehiculo(Connection conn, String placa, String tipoPlaca) throws SQLException {
        if (placa == null) return null;
        String p = placa.trim();
        if (p.isEmpty()) return null;

        String select = "SELECT ID_Vehiculo, tipo_placa FROM Vehiculos WHERE Placa = ?";
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("ID_Vehiculo");
                    String tipoActual = rs.getString("tipo_placa");
                    if (tipoPlaca != null && !tipoPlaca.equalsIgnoreCase(tipoActual)) {
                        String upd = "UPDATE Vehiculos SET tipo_placa = ? WHERE ID_Vehiculo = ?";
                        try (PreparedStatement ps2 = conn.prepareStatement(upd)) {
                            ps2.setString(1, tipoPlaca);
                            ps2.setInt(2, id);
                            ps2.executeUpdate();
                        }
                    }
                    return id;
                }
            }
        }

        String insert = "INSERT INTO Vehiculos (Placa, tipo_placa) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p);
            ps.setString(2, tipoPlaca);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        return null;
    }

    // ========= TABLA FILTRO_COLUMNAS (persistencia) =======

    private void asegurarTablaFiltros() {
        String ddl = "CREATE TABLE IF NOT EXISTS Filtro_Columnas ("
                + " nombre VARCHAR(60) PRIMARY KEY,"
                + " columnas VARCHAR(512) NOT NULL,"
                + " usuario VARCHAR(100) NULL,"
                + " actualizado TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)";
        try (Connection c = obtenerConexion(); Statement st = c.createStatement()) {
            st.execute(ddl);
        } catch (SQLException ex) {
            System.err.println("[Filtro_Columnas] No se pudo asegurar tabla: " + ex.getMessage());
        }
    }

    private String serializeCols(int[] keep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keep.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(keep[i]);
        }
        return sb.toString();
    }

    private int[] parseCols(String s) {
        if (s == null || s.trim().isEmpty()) return new int[0];
        String[] parts = s.split(",");
        int[] arr = new int[parts.length];
        int k = 0;
        for (String p : parts) {
            try { arr[k++] = Integer.parseInt(p.trim()); } catch (Exception ignore) {}
        }
        return (k == arr.length) ? arr : java.util.Arrays.copyOf(arr, k);
    }

    private void guardarFiltroPersonalizadoEnDB(String nombre, int[] keep) {
        String upsert = "INSERT INTO Filtro_Columnas (nombre, columnas) VALUES (?,?) "
                + "ON DUPLICATE KEY UPDATE columnas = VALUES(columnas)";
        try (Connection c = obtenerConexion(); PreparedStatement ps = c.prepareStatement(upsert)) {
            ps.setString(1, nombre);
            ps.setString(2, serializeCols(keep));
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[Filtro_Columnas] No se pudo guardar '" + nombre + "': " + ex.getMessage());
        }
    }

    private void eliminarFiltroPersonalizadoDeDB(String nombre) {
        String sql = "DELETE FROM Filtro_Columnas WHERE nombre = ?";
        try (Connection c = obtenerConexion();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[Filtro_Columnas] No se pudo eliminar '" + nombre + "': " + ex.getMessage());
        }
    }

    private void eliminarFiltroActual() {
        String filtroAEliminar = ultimaVistaSeleccionada;

        if (filtroAEliminar == null || "Todos".equalsIgnoreCase(filtroAEliminar)) {
            JOptionPane.showMessageDialog(this,
                    "No puedes eliminar la vista \"Todos\".\n" +
                            "Primero selecciona un filtro personalizado.",
                    "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
            cbMostrarColumnas.setSelectedItem(ultimaVistaSeleccionada);
            return;
        }

        int r = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el filtro \"" + filtroAEliminar + "\"?\n" +
                        "Se quitará del combo y de la tabla Filtro_Columnas.",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION);

        if (r != JOptionPane.YES_OPTION) {
            cbMostrarColumnas.setSelectedItem(ultimaVistaSeleccionada);
            return;
        }

        FiltrarColumnas.remove(filtroAEliminar);
        eliminarFiltroPersonalizadoDeDB(filtroAEliminar);

        refrescarComboVistas("Todos");
        int[] keep = FiltrarColumnas.get("Todos");
        if (keep != null) aplicarVistaColumnas(keep);
    }

    private void cargarFiltrosPersonalizadosDesdeDB() {
        String sql = "SELECT nombre, columnas FROM Filtro_Columnas ORDER BY nombre";
        try (Connection c = obtenerConexion(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int[] keep = parseCols(rs.getString("columnas"));
                if (keep != null && keep.length > 0) {
                    if (!FiltrarColumnas.containsKey(nombre)) FiltrarColumnas.put(nombre, keep);
                }
            }
        } catch (SQLException ex) {
            System.err.println("[Filtro_Columnas] No se pudieron cargar filtros: " + ex.getMessage());
        }
    }

    // ================== CARGA DE DATOS ====================

    private void cargarDatos() {
        String sql =
                "SELECT cp.Carta_Porte_id, " +
                        "c.Nom_Cliente        AS Cliente, " +
                        "cp.FACTURA, " +
                        "cp.FECHA_FACTURA, " +
                        "cp.VALOR, " +
                        "cp.MONEDA_VALOR, " +
                        "cp.FECHA_DE_PAGO, " +
                        "cp.DESTINO, " +
                        "cp.REFERENCIA, " +
                        "r.Nom_Remitente      AS REMITENTE, " +
                        "co.Nom_Consignatario AS CONSIGNATORIO, " +
                        "cp.FACTURA2, " +
                        "o.Nom_Operador       AS OPERADOR, " +
                        "vc.Placa             AS PLACA_CABEZAL, " +
                        "vf.Placa             AS PLACA_DEL_FURGON, " +
                        "cp.VALOR_FLETE, " +
                        "cp.MONEDA_VALOR_FLETE, " +
                        "cp.ANTICIPO, " +
                        "cp.MONEDA_ANTICIPO, " +
                        "cp.A_CANCELACION, " +
                        "cp.MONEDA_A_CANCELACION, " +
                        "cp.FECHA_DE_PAGADO, " +
                        "cp.F_DE_CARGA, " +
                        "cp.F_DE_CRUCE, " +
                        "cp.F_SAL_T_U, " +
                        "cp.F_F_DESTINO, " +
                        "cp.F_EN_DESTINO, " +
                        "cp.F_DESCARGA, " +
                        "cp.F_E_DE_DOCTOS, " +
                        "cu.Nom_Custodio      AS CUSTODIO, " +
                        "cp.VALOR_CUSTODIO, " +
                        "cp.MONEDA_VALOR_CUSTODIO, " +
                        "cp.FECHA_PAGO_CUSTODIO, " +
                        "cp.OBSERVACIONES " +
                "FROM Carta_Porte cp " +
                "JOIN Clientes       c  ON cp.ID_Cliente          = c.ID_Cliente " +
                "LEFT JOIN Remitentes     r  ON cp.ID_Remitente        = r.ID_Remitente " +
                "LEFT JOIN Consignatarios co ON cp.ID_Consignatario    = co.ID_Consignatario " +
                "LEFT JOIN Operadores     o  ON cp.ID_Operador         = o.ID_Operador " +
                "LEFT JOIN Vehiculos      vc ON cp.ID_Placa_Cabezal    = vc.ID_Vehiculo " +
                "LEFT JOIN Vehiculos      vf ON cp.ID_Placa_Del_Furgon = vf.ID_Vehiculo " +
                "LEFT JOIN Custodios      cu ON cp.ID_Custodio         = cu.ID_Custodio";

        try (Connection conn = obtenerConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            modeloTabla.setRowCount(0);
            initSugerencias();

            while (rs.next()) {
                // Decimales crudos desde la BD
                BigDecimal valorBD         = rs.getBigDecimal("VALOR");
                BigDecimal valorFleteBD    = rs.getBigDecimal("VALOR_FLETE");
                BigDecimal anticipoBD      = rs.getBigDecimal("ANTICIPO");
                BigDecimal aCancelBD       = rs.getBigDecimal("A_CANCELACION");
                BigDecimal valorCustodioBD = rs.getBigDecimal("VALOR_CUSTODIO");

                // Monedas desde las columnas MONEDA_*
                Moneda mValor        = monedaFromDbCode(rs.getString("MONEDA_VALOR"));
                Moneda mFlete        = monedaFromDbCode(rs.getString("MONEDA_VALOR_FLETE"));
                Moneda mAnticipo     = monedaFromDbCode(rs.getString("MONEDA_ANTICIPO"));
                Moneda mACancel      = monedaFromDbCode(rs.getString("MONEDA_A_CANCELACION"));
                Moneda mValCustodio  = monedaFromDbCode(rs.getString("MONEDA_VALOR_CUSTODIO"));

                Object[] row = new Object[] {
                        rs.getInt("Carta_Porte_id"),
                        v(rs, "Cliente"),
                        v(rs, "FACTURA"),
                        v(rs, "FECHA_FACTURA"),
                        formatearConMoneda(valorBD, mValor),
                        v(rs, "FECHA_DE_PAGO"),
                        v(rs, "DESTINO"),
                        v(rs, "REFERENCIA"),
                        v(rs, "REMITENTE"),
                        v(rs, "CONSIGNATORIO"),
                        v(rs, "FACTURA2"),
                        v(rs, "OPERADOR"),
                        v(rs, "PLACA_CABEZAL"),
                        v(rs, "PLACA_DEL_FURGON"),
                        formatearConMoneda(valorFleteBD, mFlete),
                        formatearConMoneda(anticipoBD, mAnticipo),
                        formatearConMoneda(aCancelBD, mACancel),
                        v(rs, "FECHA_DE_PAGADO"),
                        v(rs, "F_DE_CARGA"),
                        v(rs, "F_DE_CRUCE"),
                        v(rs, "F_SAL_T_U"),
                        v(rs, "F_F_DESTINO"),
                        v(rs, "F_EN_DESTINO"),
                        v(rs, "F_DESCARGA"),
                        v(rs, "F_E_DE_DOCTOS"),
                        v(rs, "CUSTODIO"),
                        formatearConMoneda(valorCustodioBD, mValCustodio),
                        v(rs, "FECHA_PAGO_CUSTODIO"),
                        v(rs, "OBSERVACIONES")
                };
                modeloTabla.addRow(row);
                addSuggestionsFromRow(row);
            }

            if (sorter != null) sorter.sort();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar los datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        ajustarLayout();
    }

    private String v(ResultSet rs, String col) {
        try {
            Object o = rs.getObject(col);
            if (o == null) return "";

            if (o instanceof java.sql.Timestamp) {
                java.sql.Timestamp t = (java.sql.Timestamp) o;
                java.util.Date d = new java.util.Date(t.getTime());
                if (esColumnaFecha(col)) return formatearFechaBonita(d);
                boolean tieneHora = (t.toLocalDateTime().getHour()
                        + t.toLocalDateTime().getMinute()
                        + t.toLocalDateTime().getSecond()) != 0;
                return new SimpleDateFormat(tieneHora ? "yyyy-MM-dd HH:mm" : "yyyy-MM-dd").format(t);
            }

            if (o instanceof java.sql.Date) {
                java.sql.Date dSql = (java.sql.Date) o;
                java.util.Date d = new java.util.Date(dSql.getTime());
                if (esColumnaFecha(col)) return formatearFechaBonita(d);
                return new SimpleDateFormat("yyyy-MM-dd").format(dSql);
            }

            if (o instanceof java.sql.Time) return new SimpleDateFormat("HH:mm").format((java.sql.Time) o);

            if (esColumnaFecha(col)) {
                String s = String.valueOf(o);
                String normalizada = normalizeFecha(s);
                try {
                    java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(normalizada);
                    return formatearFechaBonita(d);
                } catch (Exception ex) {
                    return s;
                }
            }

            return String.valueOf(o);
        } catch (SQLException ex) {
            return "";
        }
    }

    // ================== ELIMINAR REGISTRO =================

    private void eliminarRegistro(String cartaPorteId) {
        int confirmacion = JOptionPane.showConfirmDialog(this, "¿Estás seguro de eliminar este registro?", "Confirmación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM Carta_Porte WHERE Carta_Porte_id = ?";
        try (Connection conn = obtenerConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cartaPorteId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Registro eliminado con éxito.");
                cargarDatos();
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el registro.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================== CONFIG COLUMNA ANCHO ==============

    private void configurarAnchosColumnas() {
        int[] anchos = {
                120, 220, 120, 160, 160, 160,
                160, 150, 160, 160, 110, 150,
                140, 160, 160, 160, 130, 165,
                165, 165, 165, 165, 165, 165,
                165, 120, 160, 165, 260
        };
        TableColumnModel tcm = tabla.getColumnModel();
        for (int i = 0; i < anchos.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(anchos[i]);
            tcm.getColumn(i).setMinWidth(70);
        }
    }

    // ================== ICONOS ============================

    private ImageIcon scaledIcon(String resourcePath, int w, int h) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("No se encontró el recurso: " + resourcePath);
                return null;
            }
            BufferedImage img = ImageIO.read(is);
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (IOException e) { e.printStackTrace(); return null; }
    }

    // ================== FILTROS POR COLUMNA (header) ======

    private void instalarFiltrosPorColumna() {
        JTableHeader header = tabla.getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int viewCol = header.columnAtPoint(e.getPoint());
                if (viewCol < 0) return;

                int modelCol = tabla.convertColumnIndexToModel(viewCol);
                boolean esColFiltrable = (modelCol == IDX_CLIENTE ||modelCol == IDX_DESTINO||
                        modelCol == IDX_REMITENTE||modelCol == IDX_CONSIGNATORIO
                        ||modelCol == IDX_PLACA_CABEZAL||modelCol == IDX_PLACA_FURGON|| modelCol == IDX_OPERADOR);
                if (!esColFiltrable) return;

                boolean abrir = SwingUtilities.isRightMouseButton(e)
                        || (SwingUtilities.isLeftMouseButton(e) && e.isShiftDown());

                if (abrir) {
                    mostrarPopupFiltroColumna(modelCol, header, e.getX(), e.getY());
                    e.consume();
                }
            }
        });
    }

    private void aplicarFiltrosCombinados() {
        if (sorter == null) return;

        List<RowFilter<DefaultTableModel, Object>> filtros = new ArrayList<>();
        if (rfBusqueda != null) filtros.add(rfBusqueda);

        RowFilter<DefaultTableModel, Object> rfCols = construirRowFilterColumnas();
        if (rfCols != null) filtros.add(rfCols);

        if (filtros.isEmpty()) sorter.setRowFilter(null);
        else if (filtros.size() == 1) sorter.setRowFilter(filtros.get(0));
        else sorter.setRowFilter(RowFilter.andFilter(filtros));
    }

    private RowFilter<DefaultTableModel, Object> construirRowFilterColumnas() {
        if (filtrosPorColumna.isEmpty()) return null;

        return new RowFilter<DefaultTableModel, Object>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Object> entry) {
                for (Map.Entry<Integer, LinkedHashSet<String>> f : filtrosPorColumna.entrySet()) {
                    int col = f.getKey();
                    Set<String> allowed = f.getValue();
                    if (allowed == null || allowed.isEmpty()) continue;

                    Object v = (col >= 0 && col < entry.getValueCount()) ? entry.getValue(col) : null;
                    String s = (v == null) ? "" : v.toString().trim();
                    String key = s.isEmpty() ? "" : normalize(s);

                    if (!allowed.contains(key)) return false;
                }
                return true;
            }
        };
    }

    private LinkedHashMap<String, String> obtenerValoresUnicosDeColumna(int modelCol) {
        LinkedHashMap<String, String> normToDisplay = new LinkedHashMap<>();
        boolean hayVacio = false;

        for (int r = 0; r < modeloTabla.getRowCount(); r++) {
            Object o = modeloTabla.getValueAt(r, modelCol);
            String s = (o == null) ? "" : o.toString().trim();
            if (s.isEmpty()) { hayVacio = true; continue; }

            String k = normalize(s);
            if (!normToDisplay.containsKey(k)) normToDisplay.put(k, s);
        }

        List<Map.Entry<String, String>> entries = new ArrayList<>(normToDisplay.entrySet());
        java.text.Collator coll = java.text.Collator.getInstance(LOCALE_ES_MX);
        coll.setStrength(java.text.Collator.PRIMARY);
        entries.sort((a, b) -> coll.compare(a.getValue(), b.getValue()));

        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (hayVacio) out.put("", "(Vacío)");
        for (Map.Entry<String, String> e : entries) out.put(e.getKey(), e.getValue());
        return out;
    }

    private void mostrarPopupFiltroColumna(int modelCol, JComponent invoker, int x, int y) {
        final LinkedHashMap<String, String> opciones = obtenerValoresUnicosDeColumna(modelCol);
        final int totalOpciones = opciones.size();

        final LinkedHashSet<String> selActual = filtrosPorColumna.get(modelCol);

        final JPopupMenu popup = new JPopupMenu();
        JPanel root = new JPanel(new BorderLayout(6, 6));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField tfBuscar = new JTextField();
        tfBuscar.setToolTipText("Buscar opción...");
        root.add(tfBuscar, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane sp = new JScrollPane(listPanel);
        sp.setPreferredSize(new Dimension(320, 260));
        root.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnAplicar = new JButton("Aplicar");
        JButton btnLimpiar = new JButton("Limpiar");
        JButton btnCancelar = new JButton("Cancelar");
        bottom.add(btnLimpiar);
        bottom.add(btnCancelar);
        bottom.add(btnAplicar);
        root.add(bottom, BorderLayout.SOUTH);

        final JCheckBox chkTodo = new JCheckBox("Seleccionar todo");
        chkTodo.setFont(new Font("Poppins", Font.BOLD, 12));
        listPanel.add(chkTodo);

        final java.util.List<JCheckBox> items = new ArrayList<>();

        for (Map.Entry<String, String> e : opciones.entrySet()) {
            String key = e.getKey();
            String label = e.getValue();

            JCheckBox cb = new JCheckBox(label);
            cb.putClientProperty("key", key);

            boolean selected = (selActual == null || selActual.isEmpty()) || selActual.contains(key);
            cb.setSelected(selected);

            items.add(cb);
            listPanel.add(cb);
        }

        Runnable refreshChkTodo = () -> {
            boolean all = true;
            boolean any = false;
            for (JCheckBox cb : items) {
                if (cb.isSelected()) any = true;
                else all = false;
            }
            chkTodo.setSelected(all && any);
        };
        refreshChkTodo.run();

        chkTodo.addActionListener(ev -> {
            boolean sel = chkTodo.isSelected();
            for (JCheckBox cb : items) cb.setSelected(sel);
        });

        for (JCheckBox cb : items) cb.addItemListener(ev -> refreshChkTodo.run());

        tfBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }

            private void filtrar() {
                String needle = normalize(tfBuscar.getText());
                for (JCheckBox cb : items) {
                    String txt = cb.getText() == null ? "" : cb.getText();
                    boolean show = needle.isEmpty() || normalize(txt).contains(needle);
                    cb.setVisible(show);
                }
                listPanel.revalidate();
                listPanel.repaint();
            }
        });

        btnCancelar.addActionListener(ev -> popup.setVisible(false));

        btnLimpiar.addActionListener(ev -> {
            filtrosPorColumna.remove(modelCol);
            aplicarFiltrosCombinados();
            popup.setVisible(false);
        });

        btnAplicar.addActionListener(ev -> {
            LinkedHashSet<String> sel = new LinkedHashSet<>();
            for (JCheckBox cb : items) {
                if (cb.isSelected()) {
                    Object k = cb.getClientProperty("key");
                    sel.add(k == null ? "" : k.toString());
                }
            }

            if (sel.size() == 0 || sel.size() == totalOpciones) filtrosPorColumna.remove(modelCol);
            else filtrosPorColumna.put(modelCol, sel);

            aplicarFiltrosCombinados();
            popup.setVisible(false);
        });

        popup.add(root);
        popup.show(invoker, x, y);
    }

    // ================== UI RESPONSIVE / F11 ===============

    private void instalarResponsiveUI() {
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                ajustarLayout();
            }
        });

        JRootPane raiz = getRootPane();
        KeyStroke ksF11 = KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0);
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ksF11, "TOGGLE_FULLSCREEN");
        raiz.getActionMap().put("TOGGLE_FULLSCREEN", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                toggleFullScreen();
            }
        });
    }

    private void ajustarLayout() {
        Container cp = getContentPane();
        int w = cp.getWidth();
        int h = cp.getHeight();
        if (w <= 0 || h <= 0) return;

        final int filaFiltrosH = 30;
        final int tablaY = 120;

        int tablaH = h - tablaY - 120 - 10;
        if (tablaH < 220) tablaH = 220;

        if (lblTitulo != null) lblTitulo.setBounds(0, 20, w, 50);

        int logoW = 130, logoH = 100;

        logoLabel.setBounds(60, 10, 130, 100);

        ImageIcon ic = scaledIcon(LOGOImagen, logoW, logoH);
        logoLabel.setIcon(ic);
        lastLogoW = logoW;
        lastLogoH = logoH;

        int filtroLabelW = 140;
        int filtroComboW = 220;

        if (lblFiltrarColsLabel != null) {
            lblFiltrarColsLabel.setBounds(680, 85, 140, 20);
        }
        if (cbMostrarColumnas != null) {
            cbMostrarColumnas.setBounds(670 + filtroLabelW + 6, 80, filtroComboW, filaFiltrosH);
        }

        if (lblBuscarLabel != null) lblBuscarLabel.setBounds(1100, 85, 60, 20);
        if (txtBuscar != null) txtBuscar.setBounds(1170, 80, 220, filaFiltrosH);

        if (scrollPane != null) scrollPane.setBounds(10, tablaY, w - 20, tablaH);

        int btnY = tablaY + tablaH + 20;
        int btnH = 50;
        int btnW = 150;
        int gap = 30;
        
        
        int wDatos   = (btnDatos != null && btnDatos.getIcon() != null) ? 56 : btnW;    
        int wCambios = (btnModificaciones != null && btnModificaciones.getIcon() != null) ? 56 : btnW;
        int totalNormal = wDatos + gap + btnW * 3 + gap * 3 + wCambios;
        
        
        
        int startX = Math.max(10, (w - totalNormal) / 2);

        
        int xCursor = startX;
        if (btnDatos != null)        { btnDatos.setBounds(50, btnY , wDatos, btnH);           xCursor += wDatos + gap; }
        if (btnAgregar != null)      { btnAgregar.setBounds(xCursor, btnY, btnW, btnH);           xCursor += btnW + gap; }
        if (btnModificar != null)    { btnModificar.setBounds(xCursor, btnY, btnW, btnH);         xCursor += btnW + gap; }
        if (btnEliminar != null)     { btnEliminar.setBounds(xCursor, btnY, btnW, btnH);          xCursor += btnW + gap; }
        if (btnModificaciones != null) btnModificaciones.setBounds(xCursor +350, btnY, wCambios, btnH);
        
        
        
        
        cp.revalidate();
        cp.repaint();
    }

    private void toggleFullScreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (!gd.isFullScreenSupported()) {
            if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
                setExtendedState(JFrame.NORMAL);
            } else {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
            SwingUtilities.invokeLater(this::ajustarLayout);
            return;
        }

        if (!fullScreen) {
            windowedBounds = getBounds();
            dispose();
            setUndecorated(true);
            setVisible(true);
            gd.setFullScreenWindow(this);
            fullScreen = true;
        } else {
            gd.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setVisible(true);
            if (windowedBounds != null) setBounds(windowedBounds);
            fullScreen = false;
        }

        SwingUtilities.invokeLater(this::ajustarLayout);
    }

    // ================== MAIN =============================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Registros registros = new Registros();
            registros.setVisible(true);
        });
    }
}