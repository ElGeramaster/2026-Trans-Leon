package GestionSoftware;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

public class Datos extends JFrame {
    
    private JLabel logoLabel;
    private static final String LOGOImagen = "/GestionSoftware/imagenes/LogoLeon.png";
    
    private ImageIcon scaledIcon(String resourcePath, int w, int h) {
        java.net.URL url = getClass().getResource(resourcePath);
        if (url == null) {
            System.err.println("No se encontró el recurso: " + resourcePath);
            return null;
        }
        ImageIcon ic = new ImageIcon(url);
        Image scaled = ic.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private JButton B1;

    // ====== CONFIG DB ======
    private Connection obtenerConexion() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/EmpresaLog"
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=America/Merida";
        return DriverManager.getConnection(url, "admin", "12345ñ");
    }

    // ====== UI ======
    private JTabbedPane tabs;

    private EntidadPanel panelClientes;
    private EntidadPanel panelRemitentes;
    private EntidadPanel panelConsignatarios;
    private EntidadPanel panelOperadores;
    private EntidadPanel panelCustodios;

    // ====== VALIDACIONES ======
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public Datos() {
        setTitle("Gestión de Contactos");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1600, 900);
        setLocationRelativeTo(null);
        setResizable(true);
        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(245, 247, 250));
        
        // Asegurar columnas
        asegurarColumnasDatos();

        // ====== BARRA SUPERIOR ======
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(true);
        topBar.setBackground(new Color(41, 128, 185));
        topBar.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        B1 = new JButton("<");
        B1.setBackground(Color.WHITE);
        B1.setForeground(Color.BLACK);
        B1.setFocusPainted(false);
        B1.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        B1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        B1.setToolTipText("Regresar a Registros");
        B1.setPreferredSize(new Dimension(60, 60));
        B1.setMinimumSize(new Dimension(60, 60));
        B1.setMaximumSize(new Dimension(60, 60));
        
        // Panel para envolver el botón B1 con tamaño fijo
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelBoton.setOpaque(false);
        panelBoton.add(B1);
        topBar.add(panelBoton, BorderLayout.WEST);

        JLabel titulo = new JLabel("Gestión de Contactos", SwingConstants.CENTER);
        titulo.setFont(new Font("Poppins", Font.BOLD, 25));
        titulo.setForeground(Color.WHITE);
        topBar.add(titulo, BorderLayout.CENTER);

        // Logo
        logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        ImageIcon logo = scaledIcon(LOGOImagen, 150, 120);
        if (logo != null) logoLabel.setIcon(logo);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        topBar.add(logoLabel, BorderLayout.EAST);
        
        getContentPane().add(topBar, BorderLayout.NORTH);
        
        B1.addActionListener(e -> {
            try {
                WindowState.switchTo(this, new Registros());
            } catch (Throwable ignore) {
                dispose();
            }
        });

        B1.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { 
                B1.setBackground(new Color(192, 57, 43)); 
            }
            @Override public void mouseExited(MouseEvent e) { 
                B1.setBackground(Color.WHITE); 
            }
        });

        // ====== TABS ======
        tabs = new JTabbedPane();
        tabs.setFont(new Font("Poppins", Font.BOLD, 14));
        tabs.setBackground(Color.WHITE);
        tabs.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelClientes = new EntidadPanel(
            "Clientes", "ID_Cliente", "Nom_Cliente", "CLIENTES", 
            new Color(46, 204, 113)
        );
        panelRemitentes = new EntidadPanel(
            "Remitentes", "ID_Remitente", "Nom_Remitente", "REMITENTES",
            new Color(52, 152, 219)
        );
        panelConsignatarios = new EntidadPanel(
            "Consignatarios", "ID_Consignatario", "Nom_Consignatario", "CONSIGNATARIOS",
            new Color(155, 89, 182)
        );
        panelOperadores = new EntidadPanel(
            "Operadores", "ID_Operador", "Nom_Operador", "OPERADORES",
            new Color(230, 126, 34)
        );
        panelCustodios = new EntidadPanel(
            "Custodios", "ID_Custodio", "Nom_Custodio", "CUSTODIOS",
            new Color(231, 76, 60)
        );

        tabs.addTab("👥 Clientes", panelClientes);
        tabs.addTab("📦 Remitentes", panelRemitentes);
        tabs.addTab("🏢 Consignatarios", panelConsignatarios);
        tabs.addTab("🚛 Operadores", panelOperadores);
        tabs.addTab("🔐 Custodios", panelCustodios);

        getContentPane().add(tabs, BorderLayout.CENTER);

        // ESC = regresar
        JRootPane raiz = getRootPane();
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "ESCAPE");
        raiz.getActionMap().put("ESCAPE", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { B1.doClick(); }
        });

        WindowState.installF11(this);
    }

    // ====== ASEGURAR COLUMNAS EN BD ======
    private void asegurarColumnasDatos() {
        try (Connection c = obtenerConexion()) {
            asegurarColumna(c, "Clientes", "Telefono", "VARCHAR(30) NULL");
            asegurarColumna(c, "Clientes", "Direccion", "VARCHAR(255) NULL");
            asegurarColumna(c, "Clientes", "Correo", "VARCHAR(120) NULL");

            asegurarColumna(c, "Remitentes", "Telefono", "VARCHAR(30) NULL");
            asegurarColumna(c, "Remitentes", "Direccion", "VARCHAR(255) NULL");
            asegurarColumna(c, "Remitentes", "Correo", "VARCHAR(120) NULL");

            asegurarColumna(c, "Consignatarios", "Telefono", "VARCHAR(30) NULL");
            asegurarColumna(c, "Consignatarios", "Direccion", "VARCHAR(255) NULL");
            asegurarColumna(c, "Consignatarios", "Correo", "VARCHAR(120) NULL");

            asegurarColumna(c, "Operadores", "Telefono", "VARCHAR(30) NULL");
            asegurarColumna(c, "Operadores", "Direccion", "VARCHAR(255) NULL");
            asegurarColumna(c, "Operadores", "Correo", "VARCHAR(120) NULL");

            asegurarColumna(c, "Custodios", "Telefono", "VARCHAR(30) NULL");
            asegurarColumna(c, "Custodios", "Direccion", "VARCHAR(255) NULL");
            asegurarColumna(c, "Custodios", "Correo", "VARCHAR(120) NULL");
        } catch (SQLException ex) {
            System.err.println("[Datos] No se pudieron asegurar columnas: " + ex.getMessage());
        }
    }

    private void asegurarColumna(Connection c, String tabla, String columna, String defSql) throws SQLException {
        String check =
                "SELECT COUNT(*) " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = c.prepareStatement(check)) {
            ps.setString(1, tabla);
            ps.setString(2, columna);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int n = rs.getInt(1);
                if (n == 0) {
                    try (Statement st = c.createStatement()) {
                        st.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + defSql);
                        System.out.println("[Datos] Columna agregada: " + tabla + "." + columna);
                    }
                }
            }
        }
    }

    // ====== PANEL REUTILIZABLE POR ENTIDAD ======
    private class EntidadPanel extends JPanel {
        private final String tabla;
        private final String colId;
        private final String colNombre;
        private final String etiqueta;
        private final Color colorTema;

        private JComboBox<String> cbEntidad;
        private JTextField tfNombreNuevo;  // NUEVO: campo para editar nombre
        private JTextField tfTelefono;
        private JTextField tfCorreo;
        private JTextArea taDireccion;
        private JLabel lblUsoCount;

        private JTextField tfBuscar;
        private JTable table;
        private DefaultTableModel model;
        private TableRowSorter<DefaultTableModel> sorter;
        private JCheckBox chkSoloConUso;

        private final LinkedHashMap<String, RegistroInfo> mapNombreInfo = new LinkedHashMap<>();

        // Clase para almacenar info del registro
        private class RegistroInfo {
            int id;
            int usoCount;
            RegistroInfo(int id, int usoCount) {
                this.id = id;
                this.usoCount = usoCount;
            }
        }

        EntidadPanel(String tabla, String colId, String colNombre, String etiqueta, Color colorTema) {
            this.tabla = tabla;
            this.colId = colId;
            this.colNombre = colNombre;
            this.etiqueta = etiqueta;
            this.colorTema = colorTema;

            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            setBackground(new Color(245, 247, 250));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 0.0; // El formulario no se expande verticalmente
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 12, 0);
            add(crearTop(), gbc);

            gbc.gridy = 1;
            gbc.weighty = 1.0; // La tabla se expande para ocupar todo el espacio vertical disponible
            gbc.insets = new Insets(0, 0, 0, 0);
            add(crearCenter(), gbc);

            cargarListaYTabla();
        }

        private JComponent crearTop() {
            // Panel de formulario
            JPanel form = new JPanel(new GridBagLayout());
            form.setOpaque(true);
            form.setBackground(Color.WHITE);
            form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(8, 8, 8, 8);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;

            // Fila 0: Selector
            c.gridx = 0; c.gridy = 0; c.weightx = 0;
            JLabel lbl = new JLabel(" Seleccionar " + etiqueta + ":");
            lbl.setFont(new Font("Poppins", Font.BOLD, 14));
            form.add(lbl, c);

            c.gridx = 1; c.gridy = 0; c.weightx = 1.0;
            cbEntidad = new JComboBox<>();
            cbEntidad.setFont(new Font("Poppins", Font.PLAIN, 14));
            cbEntidad.setPreferredSize(new Dimension(400, 32));
            form.add(cbEntidad, c);

            c.gridx = 2; c.gridy = 0; c.weightx = 0;
            lblUsoCount = new JLabel("");
            lblUsoCount.setFont(new Font("Poppins", Font.BOLD, 12));
            lblUsoCount.setForeground(new Color(127, 140, 141));
            form.add(lblUsoCount, c);

            c.gridx = 3; c.gridy = 0; c.weightx = 0;
            JButton btnRecargar = new JButton("↻ Recargar");
            btnRecargar.setFont(new Font("Poppins", Font.BOLD, 13));
            btnRecargar.setBackground(colorTema);
            btnRecargar.setForeground(Color.WHITE);
            btnRecargar.setFocusPainted(false);
            btnRecargar.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
            btnRecargar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnRecargar.addActionListener(e -> cargarListaYTabla());
            form.add(btnRecargar, c);

            // NUEVA Fila 1: Campo para editar nombre
            c.gridx = 0; c.gridy = 1; c.weightx = 0; c.gridwidth = 1;
            JLabel lNombre = new JLabel("✏️ Editar Nombre:");
            lNombre.setFont(new Font("Poppins", Font.BOLD, 14));
            form.add(lNombre, c);

            c.gridx = 1; c.gridy = 1; c.weightx = 1.0; c.gridwidth = 2;
            tfNombreNuevo = new JTextField();
            tfNombreNuevo.setFont(new Font("Poppins", Font.PLAIN, 14));
            tfNombreNuevo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            tfNombreNuevo.setToolTipText("Ingresa el nuevo nombre para actualizar en todos los registros");
            form.add(tfNombreNuevo, c);

            c.gridx = 3; c.gridy = 1; c.weightx = 0; c.gridwidth = 1;
            JButton btnActualizarNombre = new JButton("💾 Actualizar Nombre");
            btnActualizarNombre.setFont(new Font("Poppins", Font.BOLD, 13));
            btnActualizarNombre.setBackground(new Color(230, 126, 34));
            btnActualizarNombre.setForeground(Color.WHITE);
            btnActualizarNombre.setFocusPainted(false);
            btnActualizarNombre.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
            btnActualizarNombre.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnActualizarNombre.setToolTipText("Actualiza el nombre en todos los registros");
            btnActualizarNombre.addActionListener(e -> actualizarNombreEntidad());
            form.add(btnActualizarNombre, c);

            // Fila 2: Teléfono
            c.gridx = 0; c.gridy = 2; c.weightx = 0; c.gridwidth = 1;
            JLabel lTel = new JLabel("📞 Teléfono:");
            lTel.setFont(new Font("Poppins", Font.BOLD, 14));
            form.add(lTel, c);

            c.gridx = 1; c.gridy = 2; c.gridwidth = 3; c.weightx = 1.0;
            tfTelefono = new JTextField();
            tfTelefono.setFont(new Font("Poppins", Font.PLAIN, 14));
            tfTelefono.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            form.add(tfTelefono, c);

            // Fila 3: Correo
            c.gridx = 0; c.gridy = 3; c.gridwidth = 1; c.weightx = 0;
            JLabel lCor = new JLabel("📧 Correo:");
            lCor.setFont(new Font("Poppins", Font.BOLD, 14));
            form.add(lCor, c);

            c.gridx = 1; c.gridy = 3; c.gridwidth = 3; c.weightx = 1.0;
            tfCorreo = new JTextField();
            tfCorreo.setFont(new Font("Poppins", Font.PLAIN, 14));
            tfCorreo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            form.add(tfCorreo, c);

            // Fila 4: Dirección
            c.gridx = 0; c.gridy = 4; c.gridwidth = 1; c.weightx = 0;
            JLabel lDir = new JLabel("📍 Dirección:");
            lDir.setFont(new Font("Poppins", Font.BOLD, 14));
            form.add(lDir, c);

            c.gridx = 1; c.gridy = 4; c.gridwidth = 3; c.weightx = 1.0;
            taDireccion = new JTextArea(3, 20);
            taDireccion.setFont(new Font("Poppins", Font.PLAIN, 14));
            taDireccion.setLineWrap(true);
            taDireccion.setWrapStyleWord(true);
            taDireccion.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            JScrollPane spDir = new JScrollPane(taDireccion);
            spDir.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));
            spDir.setPreferredSize(new Dimension(400, 80));
            form.add(spDir, c);

            // Fila 5: Botones
            JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            acciones.setOpaque(false);

            JButton btnGuardar = new JButton("💾 Guardar cambios");
            btnGuardar.setFont(new Font("Poppins", Font.BOLD, 14));
            btnGuardar.setBackground(new Color(39, 174, 96));
            btnGuardar.setForeground(Color.WHITE);
            btnGuardar.setFocusPainted(false);
            btnGuardar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnGuardar.addActionListener(e -> guardarCambios());

            JButton btnLimpiar = new JButton("🧹 Limpiar");
            btnLimpiar.setFont(new Font("Poppins", Font.BOLD, 14));
            btnLimpiar.setBackground(new Color(241, 196, 15));
            btnLimpiar.setForeground(Color.WHITE);
            btnLimpiar.setFocusPainted(false);
            btnLimpiar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            btnLimpiar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnLimpiar.addActionListener(e -> limpiarCampos());

            JButton btnEliminar = new JButton("🗑 Eliminar sin uso");
            btnEliminar.setFont(new Font("Poppins", Font.BOLD, 14));
            btnEliminar.setBackground(new Color(231, 76, 60));
            btnEliminar.setForeground(Color.WHITE);
            btnEliminar.setFocusPainted(false);
            btnEliminar.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
            btnEliminar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEliminar.setToolTipText("Elimina registros que no tienen cartas de porte asociadas");
            btnEliminar.addActionListener(e -> eliminarSinUso());

            acciones.add(btnGuardar);
            acciones.add(btnLimpiar);
            acciones.add(btnEliminar);

            c.gridx = 1; c.gridy = 5; c.gridwidth = 3; c.weightx = 1.0;
            form.add(acciones, c);

            cbEntidad.addActionListener(e -> cargarSeleccion());

            return form;
        }

        private JComponent crearCenter() {
            JPanel main = new JPanel(new BorderLayout(0, 10));
            main.setOpaque(false);

            // Panel superior: filtros
            JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            filtros.setOpaque(true);
            filtros.setBackground(Color.WHITE);
            filtros.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));

            JLabel lb = new JLabel("🔍 Buscar:");
            lb.setFont(new Font("Poppins", Font.BOLD, 13));
            filtros.add(lb);

            tfBuscar = new JTextField(28);
            tfBuscar.setFont(new Font("Poppins", Font.PLAIN, 13));
            tfBuscar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            filtros.add(tfBuscar);

            chkSoloConUso = new JCheckBox("Solo con cartas de porte activas");
            chkSoloConUso.setFont(new Font("Poppins", Font.PLAIN, 13));
            chkSoloConUso.setOpaque(false);
            chkSoloConUso.addActionListener(e -> cargarListaYTabla());
            filtros.add(chkSoloConUso);

            main.add(filtros, BorderLayout.NORTH);

            // Tabla
            model = new DefaultTableModel(
                new String[]{"ID", "Nombre", "Teléfono", "Correo", "Dirección", "Uso"}, 0
            ) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
                @Override public Class<?> getColumnClass(int col) {
                    return col == 5 ? Integer.class : String.class;
                }
            };

            table = new JTable(model);
            table.setRowHeight(28);
            table.setFont(new Font("Poppins", Font.PLAIN, 13));
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setShowGrid(true);
            table.setGridColor(new Color(220, 220, 220));

            JTableHeader header = table.getTableHeader();
            header.setFont(new Font("Poppins", Font.BOLD, 13));
            header.setBackground(colorTema);
            header.setForeground(Color.WHITE);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));

            // Renderer para columna "Uso"
            table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                    
                    if (!isSelected) {
                        int uso = value == null ? 0 : (Integer) value;
                        if (uso == 0) {
                            c.setBackground(new Color(255, 235, 235));
                            c.setForeground(new Color(192, 57, 43));
                        } else if (uso < 5) {
                            c.setBackground(new Color(254, 249, 231));
                            c.setForeground(new Color(243, 156, 18));
                        } else {
                            c.setBackground(new Color(232, 248, 245));
                            c.setForeground(new Color(39, 174, 96));
                        }
                    }
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(new Font("Poppins", Font.BOLD, 12));
                    return c;
                }
            });

            // Anchos de columnas
            table.getColumnModel().getColumn(0).setPreferredWidth(60);
            table.getColumnModel().getColumn(1).setPreferredWidth(250);
            table.getColumnModel().getColumn(2).setPreferredWidth(120);
            table.getColumnModel().getColumn(3).setPreferredWidth(200);
            table.getColumnModel().getColumn(4).setPreferredWidth(250);
            table.getColumnModel().getColumn(5).setPreferredWidth(80);

            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);

            tfBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrarTabla(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrarTabla(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrarTabla(); }
            });

            table.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int vr = table.getSelectedRow();
                    if (vr < 0) return;
                    int mr = table.convertRowIndexToModel(vr);
                    String nombre = String.valueOf(model.getValueAt(mr, 1));
                    cbEntidad.setSelectedItem(nombre);
                }
            });

            JScrollPane sp = new JScrollPane(table);
            sp.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
            main.add(sp, BorderLayout.CENTER);

            return main;
        }

        private void filtrarTabla() {
            String q = (tfBuscar.getText() == null) ? "" : tfBuscar.getText().trim();
            if (q.isEmpty()) {
                sorter.setRowFilter(null);
                return;
            }
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
        }

        private void cargarListaYTabla() {
            mapNombreInfo.clear();
            cbEntidad.removeAllItems();
            model.setRowCount(0);

            boolean soloConUso = chkSoloConUso.isSelected();

            String sql = 
                "SELECT t." + colId + " AS id, " +
                "       t." + colNombre + " AS nombre, " +
                "       t.Telefono, t.Correo, t.Direccion, " +
                "       COUNT(cp.Carta_Porte_id) AS uso_count " +
                "FROM " + tabla + " t " +
                "LEFT JOIN Carta_Porte cp ON t." + colId + " = cp." + colId + " " +
                "GROUP BY t." + colId + ", t." + colNombre + ", t.Telefono, t.Correo, t.Direccion " +
                (soloConUso ? "HAVING uso_count > 0 " : "") +
                "ORDER BY t." + colNombre;

            try (Connection c = obtenerConexion();
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    String tel = rs.getString("Telefono");
                    String cor = rs.getString("Correo");
                    String dir = rs.getString("Direccion");
                    int usoCount = rs.getInt("uso_count");

                    if (nombre == null) nombre = "";
                    
                    mapNombreInfo.put(nombre, new RegistroInfo(id, usoCount));
                    cbEntidad.addItem(nombre);

                    model.addRow(new Object[]{
                        id,
                        nombre,
                        tel == null ? "" : tel,
                        cor == null ? "" : cor,
                        dir == null ? "" : dir,
                        usoCount
                    });
                }

                if (cbEntidad.getItemCount() > 0) {
                    cbEntidad.setSelectedIndex(0);
                    cargarSeleccion();
                } else {
                    limpiarCampos();
                    lblUsoCount.setText("");
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error al cargar " + etiqueta + ": " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void cargarSeleccion() {
            String nombre = (String) cbEntidad.getSelectedItem();
            if (nombre == null || nombre.trim().isEmpty()) {
                limpiarCampos();
                lblUsoCount.setText("");
                return;
            }
            
            RegistroInfo info = mapNombreInfo.get(nombre);
            if (info == null) {
                limpiarCampos();
                lblUsoCount.setText("");
                return;
            }

            // Cargar el nombre en el campo de edición
            tfNombreNuevo.setText(nombre);

            // Actualizar etiqueta de uso
            if (info.usoCount == 0) {
                lblUsoCount.setText("⚠️ Sin uso");
                lblUsoCount.setForeground(new Color(192, 57, 43));
            } else {
                lblUsoCount.setText("✓ " + info.usoCount + " carta(s) de porte");
                lblUsoCount.setForeground(new Color(39, 174, 96));
            }

            String sql = "SELECT Telefono, Correo, Direccion FROM " + tabla + 
                        " WHERE " + colId + " = ?";

            try (Connection c = obtenerConexion();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setInt(1, info.id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tfTelefono.setText(rs.getString("Telefono") == null ? "" : rs.getString("Telefono"));
                        tfCorreo.setText(rs.getString("Correo") == null ? "" : rs.getString("Correo"));
                        taDireccion.setText(rs.getString("Direccion") == null ? "" : rs.getString("Direccion"));
                    } else {
                        limpiarCampos();
                    }
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error al leer datos: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // NUEVA FUNCIONALIDAD: Actualizar nombre de la entidad
        private void actualizarNombreEntidad() {
            String nombreActual = (String) cbEntidad.getSelectedItem();
            if (nombreActual == null || nombreActual.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Selecciona un registro para actualizar su nombre.", 
                    "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String nombreNuevo = tfNombreNuevo.getText();
            if (nombreNuevo == null || nombreNuevo.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "El nuevo nombre no puede estar vacío.", 
                    "Validación", JOptionPane.WARNING_MESSAGE);
                tfNombreNuevo.requestFocusInWindow();
                return;
            }
            
            nombreNuevo = nombreNuevo.trim();

            // Verificar si el nombre ya existe (distinto al actual)
            if (!nombreActual.equals(nombreNuevo)) {
                RegistroInfo infoExistente = mapNombreInfo.get(nombreNuevo);
                if (infoExistente != null) {
                    JOptionPane.showMessageDialog(this, 
                        "Ya existe un registro con el nombre '" + nombreNuevo + "'.\n" +
                        "Por favor elige un nombre diferente.", 
                        "Nombre Duplicado", JOptionPane.WARNING_MESSAGE);
                    tfNombreNuevo.requestFocusInWindow();
                    return;
                }
            }

            // Si el nombre no cambió, no hacer nada
            if (nombreActual.equals(nombreNuevo)) {
                JOptionPane.showMessageDialog(this, 
                    "El nombre no ha cambiado.", 
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            RegistroInfo info = mapNombreInfo.get(nombreActual);
            if (info == null) {
                JOptionPane.showMessageDialog(this, 
                    "No se encontró el registro seleccionado.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Confirmar la actualización
            int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Deseas actualizar el nombre de:\n\n" +
                "   '" + nombreActual + "'\n\n" +
                "a:\n\n" +
                "   '" + nombreNuevo + "'?\n\n" +
                "Este cambio se reflejará en todos los registros (" + info.usoCount + " carta(s) de porte).",
                "Confirmar Actualización de Nombre",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (confirmacion != JOptionPane.YES_OPTION) {
                return;
            }

            // Ejecutar la actualización
            String sqlUpdate = "UPDATE " + tabla + " SET " + colNombre + " = ? WHERE " + colId + " = ?";

            try (Connection c = obtenerConexion();
                 PreparedStatement ps = c.prepareStatement(sqlUpdate)) {

                // Establecer el usuario de la aplicación para la auditoría
                try (Statement st = c.createStatement()) {
                    st.execute("SET @app_user = '" + nombreActual + " → " + nombreNuevo + "'");
                }

                ps.setString(1, nombreNuevo);
                ps.setInt(2, info.id);

                int rows = ps.executeUpdate();
                
                if (rows > 0) {
                    // Registrar en auditoría para cada carta de porte afectada
                    registrarCambioNombreEnAuditoria(c, info.id, nombreActual, nombreNuevo);

                    JOptionPane.showMessageDialog(this, 
                        "✓ Nombre actualizado exitosamente.\n\n" +
                        "Se actualizó en " + info.usoCount + " carta(s) de porte.",
                        "Actualización Exitosa", JOptionPane.INFORMATION_MESSAGE);
                    
                    // Recargar todo y seleccionar el nuevo nombre
                    cargarListaYTabla();
                    cbEntidad.setSelectedItem(nombreNuevo);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "No se pudo actualizar el nombre.", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error al actualizar nombre: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Método para registrar el cambio de nombre en la tabla AUDITORIA
        private void registrarCambioNombreEnAuditoria(Connection c, int entidadId, 
                                                      String nombreAnterior, String nombreNuevo) {
            // Obtener todos los Carta_Porte_id asociados a esta entidad
            String sqlCartasPorte = 
                "SELECT Carta_Porte_id FROM Carta_Porte WHERE " + colId + " = ?";
            
            String sqlInsertAudit = 
                "INSERT INTO AUDITORIA (Carta_Porte_id, usuario, accion, descripcion, detalle) " +
                "VALUES (?, ?, 'UPDATE', ?, ?)";

            try (PreparedStatement psCartas = c.prepareStatement(sqlCartasPorte);
                 PreparedStatement psAudit = c.prepareStatement(sqlInsertAudit)) {

                psCartas.setInt(1, entidadId);
                
                try (ResultSet rs = psCartas.executeQuery()) {
                    while (rs.next()) {
                        int cartaPorteId = rs.getInt("Carta_Porte_id");
                        
                        // Crear el detalle JSON del cambio
                        String detalleJson = String.format(
                            "{\"cambio_nombre_%s\": {\"ANTES\": \"%s\", \"DESPUES\": \"%s\"}}",
                            etiqueta.toLowerCase(),
                            nombreAnterior.replace("\"", "\\\""),
                            nombreNuevo.replace("\"", "\\\"")
                        );

                        String descripcion = String.format(
                            "Cambio de nombre en %s: '%s' → '%s'",
                            etiqueta,
                            nombreAnterior,
                            nombreNuevo
                        );

                        psAudit.setInt(1, cartaPorteId);
                        psAudit.setString(2, nombreAnterior + " → " + nombreNuevo);
                        psAudit.setString(3, descripcion);
                        psAudit.setString(4, detalleJson);
                        psAudit.addBatch();
                    }
                    
                    psAudit.executeBatch();
                }

            } catch (SQLException ex) {
                System.err.println("[Auditoría] Error al registrar cambio de nombre: " + ex.getMessage());
            }
        }

        private void guardarCambios() {
            String nombre = (String) cbEntidad.getSelectedItem();
            if (nombre == null || nombre.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Selecciona un registro.", 
                    "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            RegistroInfo info = mapNombreInfo.get(nombre);
            if (info == null) {
                JOptionPane.showMessageDialog(this, 
                    "No se encontró el ID del registro seleccionado.", 
                    "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String telefono = tfTelefono.getText() == null ? "" : tfTelefono.getText().trim();
            String correo = tfCorreo.getText() == null ? "" : tfCorreo.getText().trim();
            String direccion = taDireccion.getText() == null ? "" : taDireccion.getText().trim();

            if (!correo.isEmpty() && !EMAIL_PATTERN.matcher(correo).matches()) {
                JOptionPane.showMessageDialog(this, 
                    "Correo inválido. Ejemplo válido: usuario@dominio.com", 
                    "Validación", JOptionPane.WARNING_MESSAGE);
                tfCorreo.requestFocusInWindow();
                return;
            }

            String sql = "UPDATE " + tabla + 
                        " SET Telefono = ?, Correo = ?, Direccion = ? " +
                        "WHERE " + colId + " = ?";

            try (Connection c = obtenerConexion();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, telefono.isEmpty() ? null : telefono);
                ps.setString(2, correo.isEmpty() ? null : correo);
                ps.setString(3, direccion.isEmpty() ? null : direccion);
                ps.setInt(4, info.id);

                int rows = ps.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(this, 
                        "✓ Datos actualizados correctamente.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    String keep = nombre;
                    cargarListaYTabla();
                    cbEntidad.setSelectedItem(keep);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "No se actualizó ningún registro.", 
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void eliminarSinUso() {
            // Contar registros sin uso
            String sqlCount = 
                "SELECT COUNT(*) AS total " +
                "FROM " + tabla + " t " +
                "WHERE NOT EXISTS (" +
                "  SELECT 1 FROM Carta_Porte cp WHERE cp." + colId + " = t." + colId +
                ")";

            try (Connection c = obtenerConexion();
                 PreparedStatement ps = c.prepareStatement(sqlCount);
                 ResultSet rs = ps.executeQuery()) {

                rs.next();
                int total = rs.getInt("total");

                if (total == 0) {
                    JOptionPane.showMessageDialog(this,
                        "No hay registros sin uso para eliminar.\n" +
                        "Todos los " + etiqueta.toLowerCase() + " tienen cartas de porte asociadas.",
                        "Información", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                // Confirmar eliminación
                int respuesta = JOptionPane.showConfirmDialog(this,
                    "Se encontraron " + total + " registro(s) sin cartas de porte asociadas.\n\n" +
                    "¿Deseas eliminar estos registros?\n" +
                    "Esta acción NO se puede deshacer.",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

                if (respuesta != JOptionPane.YES_OPTION) {
                    return;
                }

                // Eliminar registros sin uso
                String sqlDelete = 
                    "DELETE FROM " + tabla + " " +
                    "WHERE NOT EXISTS (" +
                    "  SELECT 1 FROM Carta_Porte cp WHERE cp." + colId + " = " + tabla + "." + colId +
                    ")";

                try (PreparedStatement psDelete = c.prepareStatement(sqlDelete)) {
                    int eliminados = psDelete.executeUpdate();
                    
                    JOptionPane.showMessageDialog(this,
                        "✓ Se eliminaron " + eliminados + " registro(s) sin uso.",
                        "Eliminación exitosa",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    cargarListaYTabla();
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error al eliminar registros: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void limpiarCampos() {
            tfNombreNuevo.setText("");
            tfTelefono.setText("");
            tfCorreo.setText("");
            taDireccion.setText("");
            lblUsoCount.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Datos().setVisible(true));
    }
}