package GestionSoftware;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class Ingresar extends JFrame {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/EmpresaLog" +
                    "?useSSL=false" +
                    "&allowPublicKeyRetrieval=true" +
                    "&useUnicode=true&characterEncoding=UTF-8" +
                    "&serverTimezone=America/Merida";

    private static final String DB_USER = "admin";
    private static final String DB_PASS = "12345ñ";

    private static final Locale FechaMX = new Locale("es", "MX");
    private static final Locale LOCALE_NUM = Locale.US;

    private static final Font POP18B = fontOrFallback("Poppins", Font.BOLD, 20);
    private static final Font POP16B = fontOrFallback("Poppins", Font.PLAIN, 14);
    private static final Font POP14B = fontOrFallback("Poppins", Font.BOLD, 14);

    private enum Moneda {
        QUETZAL("Q", "", "Q (Quetzal)"),
        DOLAR("$", "", "$ (Dólar)"),
        MXN("$", " MXN", "$ MXN (Pesos Mexicanos)");

        final String prefix;
        final String suffix;
        final String label;

        Moneda(String prefix, String suffix, String label) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.label = label;
        }

        @Override public String toString() { return label; }
    }

    private static String monedaToDbCode(Moneda moneda) {
        if (moneda == null) return "USD";
        switch (moneda) {
            case MXN: return "MXN";
            case QUETZAL: return "GTQ";
            case DOLAR:
            default: return "USD";
        }
    }

    private static Moneda dbCodeToMoneda(String code) {
        if (code == null) return Moneda.DOLAR;
        String c = code.trim().toUpperCase(Locale.ROOT);
        switch (c) {
            case "MXN": return Moneda.MXN;
            case "GTQ": case "Q": case "QTZ": return Moneda.QUETZAL;
            case "USD":
            default: return Moneda.DOLAR;
        }
    }

    // Moneda para VALOR_FLETE, ANTICIPO, A_CANCELACION
    private final JComboBox<Moneda> cbMoneda = new JComboBox<>(Moneda.values());
    private Moneda monedaActual = Moneda.QUETZAL;

    // Moneda para VALOR
    private final JComboBox<Moneda> cbMonedaValor = new JComboBox<>(Moneda.values());
    private Moneda monedaValorActual = Moneda.DOLAR;

    // Moneda para VALOR_CUSTODIO
    private final JComboBox<Moneda> cbMonedaCustodio = new JComboBox<>(Moneda.values());
    private Moneda monedaCustodioActual = Moneda.DOLAR;

    private final JTextField cpId = tf("CARTA PORTE");
    private final JTextField cliente = tf("CLIENTE");
    private final JTextField factura1 = tf("FACTURA (000000)");
    private final JTextField fechaFactura = tf("FECHA DE LA FACTURA");
    private final JTextField valor = tf("VALOR");
    private final JTextField fechaPago = tf("FECHA DE PAGO");
    private final JTextField destino = tf("DESTINO (Lugar y Pais)");
    private final JTextField referencia = tf("REFERENCIA");
    private final JTextField remitente = tf("REMITENTE");
    private final JTextField consignat = tf("CONSIGNATARIO");
    private final JTextField factura2 = tf("FACTURA");
    private final JTextField operador = tf("OPERADOR");
    private final JTextField placaCab = tf("PLACA CABEZAL");
    private final JTextField placaFur = tf("PLACA FURGON");
    private final JTextField valorFlete = tf("VALOR FLETE");
    private final JTextField anticipo = tf("ANTICIPO");
    private final JTextField aCancel = tf("A CANCELACION");
    private final JTextField fechaPagado = tf("PAGADO (fecha)");
    private final JTextField fCarga = tf("FECHA DE CARGA");
    private final JTextField fCruce = tf("FECHA DE CRUCE");
    private final JTextField fSalTU = tf("FECHA SAL. T.U.");
    private final JTextField fFDestino = tf("FECHA F. DESTINO");
    private final JTextField fEnDestino = tf("FECHA EN DESTINO");
    private final JTextField fDescarga = tf("F. DESCARGA");
    private final JTextField fEDoctos = tf("FECHA E. DE DOCTOS");
    private final JTextField custodio = tf("SEGURIDAD PRIVADA");
    private final JTextField valorCustodio = tf("VALOR CUSTODIO");
    private final JTextField fechaPagoCustodio = tf("FECHA PAGO CUSTODIO");
    private final JTextArea observaciones = ta("OBSERVACIONES");

    private boolean modoEdicion = false;
    private boolean dirty = false;
    private boolean programmaticChange = false;

    private JButton btnGuardar;
    private JButton btnBuscar;
    private JButton btnRegresar;
    private JLabel lblUltimaCp;

    private ImageIcon CargarLogo(int heightPx) {
        java.net.URL url = Ingresar.class.getResource("/GestionSoftware/imagenes/LogoLeon.png");
        if (url == null) {
            System.err.println("⚠ No se encontró el recurso: /GestionSoftware/imagenes/LogoLeon.png");
            return null;
        }
        ImageIcon raw = new ImageIcon(url);
        int w = (int) Math.round((double) raw.getIconWidth() * heightPx / raw.getIconHeight());
        Image img = raw.getImage().getScaledInstance(w, heightPx, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    public Ingresar() {
        setTitle("Nueva Carta de Porte - Administrador");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1500, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(248, 250, 252));
        setResizable(true);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(186, 185, 181));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 240)));

        JButton B1 = new JButton("<");
        B1.setBackground(Color.WHITE);
        B1.setForeground(Color.BLACK);
        B1.setFocusPainted(false);
        B1.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        B1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        B1.setToolTipText("Regresar a Registros");
        B1.setPreferredSize(new Dimension(60, 60));
        B1.setMinimumSize(new Dimension(60, 60));
        B1.setMaximumSize(new Dimension(60, 60));

        JLabel titulo = new JLabel("AGREGAR UN NUEVO REGISTRO");
        titulo.setFont(POP18B);
        titulo.setForeground(new Color(0, 0, 0));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 20));
        leftPanel.setOpaque(false);
        leftPanel.add(B1);
        leftPanel.add(titulo);

        lblUltimaCp = new JLabel("Ultima Carta Porte:");
        lblUltimaCp.setFont(POP18B);
        lblUltimaCp.setForeground(new Color(30, 30, 30));
    

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 30));
        rightPanel.setOpaque(false);
        rightPanel.add(lblUltimaCp);

        header.add(leftPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "ESC");
        getRootPane().getActionMap().put("ESC", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { B1.doClick(); }
        });

        B1.addActionListener(e -> {
            try {
                WindowState.switchTo(this, new Registros());
            } catch (Throwable ignore) {
                dispose();
            }
        });
        
        addHover(B1, new Color(225, 90, 90));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(POP18B);
        tabs.setBackground(Color.white);

        // === Moneda VALOR_FLETE ===
        cbMoneda.setFont(fontOrFallback("Poppins", Font.BOLD, 14));
        cbMoneda.setSelectedItem(Moneda.QUETZAL);
        monedaActual = (Moneda) cbMoneda.getSelectedItem();

        instalarSoporteMonedaEnCampo(valorFlete);
        instalarSoporteMonedaEnCampo(anticipo);
        instalarSoporteMonedaEnCampo(aCancel);

        JComponent valorFleteWrap = withCurrencyPicker(valorFlete, cbMoneda);

        cbMoneda.addActionListener(e -> {
            if (programmaticChange) return;
            Moneda m = (Moneda) cbMoneda.getSelectedItem();
            if (m == null) return;
            monedaActual = m;
            programmaticChange = true;
            reformatMoneyField(valorFlete, monedaActual);
            reformatMoneyField(anticipo, monedaActual);
            recalc();
            programmaticChange = false;
        });

        // === Moneda VALOR ===
        cbMonedaValor.setFont(fontOrFallback("Poppins", Font.BOLD, 14));
        cbMonedaValor.setSelectedItem(Moneda.DOLAR);
        monedaValorActual = (Moneda) cbMonedaValor.getSelectedItem();

        instalarSoporteMonedaEnCampo(valor, () -> monedaValorActual);
        JComponent valorWrap = withCurrencyPicker(valor, cbMonedaValor);

        cbMonedaValor.addActionListener(e -> {
            if (programmaticChange) return;
            Moneda m = (Moneda) cbMonedaValor.getSelectedItem();
            if (m == null) return;
            monedaValorActual = m;
            programmaticChange = true;
            reformatMoneyField(valor, monedaValorActual);
            programmaticChange = false;
        });

        // === Moneda VALOR_CUSTODIO ===
        cbMonedaCustodio.setFont(fontOrFallback("Poppins", Font.BOLD, 14));
        cbMonedaCustodio.setSelectedItem(Moneda.DOLAR);
        monedaCustodioActual = (Moneda) cbMonedaCustodio.getSelectedItem();

        instalarSoporteMonedaEnCampo(valorCustodio, () -> monedaCustodioActual);
        JComponent valorCustodioWrap = withCurrencyPicker(valorCustodio, cbMonedaCustodio);

        cbMonedaCustodio.addActionListener(e -> {
            if (programmaticChange) return;
            Moneda m = (Moneda) cbMonedaCustodio.getSelectedItem();
            if (m == null) return;
            monedaCustodioActual = m;
            programmaticChange = true;
            reformatMoneyField(valorCustodio, monedaCustodioActual);
            programmaticChange = false;
        });

        JComponent obsWrap = wrapTextArea("OBSERVACIONES", observaciones);
        obsWrap.setName("FULL_ROW");

        JPanel panelDatosPrincipales = buildGrid(
                cpId, cliente,
                factura1, withDatePicker(fechaFactura),
                valorWrap, withDatePicker(fechaPago),
                destino, referencia,
                remitente, consignat,
                factura2
        );

        JScrollPane spDatosPrincipales = new JScrollPane(
                panelDatosPrincipales,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        spDatosPrincipales.setBorder(BorderFactory.createEmptyBorder());
        spDatosPrincipales.getVerticalScrollBar().setUnitIncrement(18);
        spDatosPrincipales.getViewport().setBackground(new Color(211, 211, 211));

        tabs.addTab("1. Datos Principales", spDatosPrincipales);

        tabs.addTab("2. Participantes & Unidades", buildGrid(
                operador,
                placaCab, placaFur, valorFleteWrap, anticipo, aCancel,
                withDatePicker(fechaPagado)
        ));

        JPanel panelFechas = buildGrid(
                withDatePicker(fCarga),
                withDatePicker(fCruce),
                withDatePicker(fSalTU),
                withDatePicker(fFDestino),
                withDatePicker(fEnDestino),
                withDatePicker(fDescarga),
                withDatePicker(fEDoctos),
                custodio,
                valorCustodioWrap,
                withDatePicker(fechaPagoCustodio),
                obsWrap
        );

        JScrollPane spFechas = new JScrollPane(
                panelFechas,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        spFechas.setBorder(BorderFactory.createEmptyBorder());
        spFechas.getVerticalScrollBar().setUnitIncrement(18);
        spFechas.getViewport().setBackground(new Color(211, 211, 211));
        tabs.addTab("3. Fechas del Viaje", spFechas);

        add(tabs, BorderLayout.CENTER);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recalc(); }
            public void removeUpdate(DocumentEvent e) { recalc(); }
            public void changedUpdate(DocumentEvent e) { recalc(); }
        };
        valorFlete.getDocument().addDocumentListener(dl);
        anticipo.getDocument().addDocumentListener(dl);
        aCancel.setEditable(false);

        JPanel footer = new JPanel(new BorderLayout(10, 10));
        footer.setBackground(new Color(52, 57, 63));

        JLabel logoLeft = new JLabel();
        logoLeft.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        logoLeft.setOpaque(false);
        ImageIcon logoIcon = CargarLogo(96);
        if (logoIcon != null) logoLeft.setIcon(logoIcon);
        footer.add(logoLeft, BorderLayout.WEST);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 24));
        rightBtns.setOpaque(false);

        btnRegresar = new JButton("Regresar");
        btnRegresar.setFocusPainted(false);
        btnRegresar.setBackground(new Color(231, 76, 60));
        btnRegresar.setForeground(Color.WHITE);
        btnRegresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRegresar.setFont(fontOrFallback("Poppins", Font.BOLD, 18));
        btnRegresar.setBorder(BorderFactory.createEmptyBorder(14, 32, 14, 32));
        btnRegresar.setPreferredSize(new Dimension(180, 50));
        btnRegresar.setVisible(false);
        addHover(btnRegresar, new Color(200, 60, 45));
        btnRegresar.addActionListener(e -> {
            if (confirmarSalirSinGuardarSiAplica("regresar sin guardar")) {
                limpiar();
            }
        });

        btnBuscar = new JButton("Buscar");
        btnBuscar.setFocusPainted(false);
        btnBuscar.setBackground(new Color(46, 204, 113));
        btnBuscar.setForeground(Color.WHITE);
        btnBuscar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBuscar.setFont(fontOrFallback("Poppins", Font.BOLD, 18));
        btnBuscar.setBorder(BorderFactory.createEmptyBorder(14, 32, 14, 32));
        btnBuscar.setPreferredSize(new Dimension(180, 50));
        addHover(btnBuscar, new Color(35, 170, 94));
        btnBuscar.addActionListener(e -> {
            if (confirmarSalirSinGuardarSiAplica("buscar otra Carta Porte")) {
                abrirDialogoBuscar();
            }
        });

        btnGuardar = new JButton("Registrar");
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBackground(new Color(33, 150, 243));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGuardar.setFont(fontOrFallback("Poppins", Font.BOLD, 18));
        btnGuardar.setBorder(BorderFactory.createEmptyBorder(14, 32, 14, 32));
        btnGuardar.setPreferredSize(new Dimension(180, 50));
        addHover(btnGuardar, new Color(0, 194, 250));
        btnGuardar.addActionListener(e -> {
            if (modoEdicion) actualizar();
            else guardar();
        });

        rightBtns.add(btnRegresar);
        rightBtns.add(btnBuscar);
        rightBtns.add(btnGuardar);

        footer.add(rightBtns, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        instalarConversorFechasEnCampos();
        instalarTrackingCambios();

        programmaticChange = true;
        reformatMoneyField(valorFlete, monedaActual);
        reformatMoneyField(anticipo, monedaActual);
        reformatMoneyField(valor, monedaValorActual);
        reformatMoneyField(valorCustodio, monedaCustodioActual);
        recalc();
        programmaticChange = false;

        cargarSugerenciasDesdeDB();
        instalarAutocompletadoEnCampos();
        actualizarLabelUltimaCp();
    }

    private static JTextField tf(String title) {
        JTextField t = new JTextField();
        t.setBorder(titled(title));
        t.setOpaque(true);
        t.setBackground(Color.white);
        t.setForeground(Color.BLACK);
        t.setFont(new Font("Poppins", Font.BOLD, 20));
        t.setColumns(28);
        return t;
    }

    private static JTextArea ta(String title) {
        JTextArea t = new JTextArea(7, 80);
        t.setBorder(titled(title));
        t.setFont(new Font("Poppins", Font.BOLD, 20));
        t.setForeground(Color.BLACK);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        return t;
    }

    private static TitledBorder titled(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255), 10), title);
        tb.setTitleFont(POP16B);
        tb.setTitleColor(Color.BLACK);
        return tb;
    }

    private static void addHover(JButton b, Color hover) {
        Color base = b.getBackground();
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(hover); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { b.setBackground(base); }
        });
    }

    private JPanel buildGrid(JComponent... comps) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(true);
        p.setBackground(new Color(211, 211, 211));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 80, 50));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;

        for (JComponent comp : comps) {
            comp.setFont(new Font("Poppins", Font.BOLD, 22));

            boolean fullRow = "FULL_ROW".equals(comp.getName());
            if (fullRow) {
                if (c.gridx == 1) { c.gridx = 0; c.gridy++; }
                c.gridwidth = 2;
                c.fill = GridBagConstraints.BOTH;
                c.weighty = 1.0;
                p.add(comp, c);
                c.gridwidth = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weighty = 0;
                c.gridx = 0;
                c.gridy++;
                continue;
            }

            p.add(comp, c);
            if (c.gridx == 0) c.gridx = 1;
            else { c.gridx = 0; c.gridy++; }
        }
        return p;
    }

    private void recalc() {
        double ValorFle = parseMoney(valorFlete.getText());
        double Anti = parseMoney(anticipo.getText());
        if (Double.isNaN(ValorFle)) ValorFle = 0;
        if (Double.isNaN(Anti)) Anti = 0;
        programmaticChange = true;
        aCancel.setText(formatMoney(ValorFle - Anti, monedaActual));
        programmaticChange = false;
    }

    private static String fmtNumber2(double d) {
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_NUM);
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(d);
    }

    private static String formatMoney(double value, Moneda m) {
        if (m == null) m = Moneda.QUETZAL;
        String num = fmtNumber2(value);
        if (m == Moneda.MXN) return m.prefix + num + m.suffix;
        return m.prefix + " " + num + m.suffix;
    }

    private static String formatMoney(BigDecimal bd, Moneda m) {
        if (bd == null) return "";
        try { return formatMoney(bd.doubleValue(), m); }
        catch (Exception e) { return ""; }
    }

    private static String stripMoneyToRaw(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return "";
        t = t.toUpperCase(Locale.ROOT);
        t = t.replace("MXN", "").replace("Q", "").replace("$", "").replace(" ", "").replace(",", "");
        t = t.replaceAll("[^0-9.\\-]", "");
        return t.trim();
    }

    private static double parseMoney(String s) {
        String raw = stripMoneyToRaw(s);
        if (raw.isEmpty() || raw.equals("-") || raw.equals(".")) return Double.NaN;
        try { return Double.parseDouble(raw); }
        catch (NumberFormatException e) { return Double.NaN; }
    }

    private void reformatMoneyField(JTextField field, Moneda m) {
        if (field == null) return;
        double d = parseMoney(field.getText());
        if (Double.isNaN(d)) {
            String raw = stripMoneyToRaw(field.getText());
            if (raw.isEmpty()) field.setText("");
            return;
        }
        field.setText(formatMoney(d, m));
    }

    private void instalarSoporteMonedaEnCampo(JTextField field) {
        instalarSoporteMonedaEnCampo(field, () -> monedaActual);
    }

    private void instalarSoporteMonedaEnCampo(JTextField field, Supplier<Moneda> monedaSupplier) {
        if (field == null) return;

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                if (programmaticChange) return;
                programmaticChange = true;
                reformatMoneyField(field, monedaSupplier.get());
                if (field == valorFlete || field == anticipo) recalc();
                programmaticChange = false;
            }
        });

        field.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (programmaticChange) return;
                    programmaticChange = true;
                    reformatMoneyField(field, monedaSupplier.get());
                    if (field == valorFlete || field == anticipo) recalc();
                    programmaticChange = false;
                    e.consume();
                }
            }
        });
    }

    private static boolean empty(JTextField t) { return t.getText().trim().isEmpty(); }

    private static Double toNullableDouble(String s) {
        double d = parseMoney(s);
        return Double.isNaN(d) ? null : d;
    }

    private static void setNullableDouble(PreparedStatement ps, int idx, Double val) throws SQLException {
        if (val == null) ps.setNull(idx, java.sql.Types.DECIMAL);
        else ps.setDouble(idx, val);
    }

    private static String nvl(String s) { return (s == null) ? "" : s; }

    private static void configurarUsuarioAuditoria(Connection cn) {
        String usuarioApp = System.getProperty("user.name");
        if (usuarioApp == null || usuarioApp.isBlank()) usuarioApp = "app_desktop";
        String seguro = usuarioApp.replace("'", "''");
        try (Statement st = cn.createStatement()) {
            st.execute("SET @app_user = '" + seguro + "'");
        } catch (SQLException ex) {
            System.err.println("[AUDITORIA] No se pudo establecer @app_user: " + ex.getMessage());
        }
    }

    private static Connection getConnection() throws SQLException {
        Connection cn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        configurarUsuarioAuditoria(cn);
        return cn;
    }

    private static int getOrCreateIdSimple(Connection cn, String table, String idCol, String nameCol, String nombre) throws SQLException {
        String sqlSel = "SELECT " + idCol + " FROM " + table + " WHERE UPPER(" + nameCol + ") = UPPER(?)";
        try (PreparedStatement ps = cn.prepareStatement(sqlSel)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        String sqlIns = "INSERT INTO " + table + " (" + nameCol + ") VALUES (?)";
        try (PreparedStatement ps = cn.prepareStatement(sqlIns, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener/crear registro en " + table);
    }

    private static int getOrCreateVehiculo(Connection cn, String placa, String tipoPlaca) throws SQLException {
        String sqlSel = "SELECT ID_Vehiculo FROM Vehiculos WHERE Placa = ?";
        try (PreparedStatement ps = cn.prepareStatement(sqlSel)) {
            ps.setString(1, placa);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        String sqlIns = "INSERT INTO Vehiculos (Placa, tipo_placa) VALUES (?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sqlIns, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, placa);
            ps.setString(2, tipoPlaca);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener/crear Vehiculo con placa " + placa);
    }

    private boolean confirmarSalirSinGuardarSiAplica(String accion) {
        if (modoEdicion && dirty) {
            int op = JOptionPane.showConfirmDialog(
                    this,
                    "Tienes cambios sin guardar.\n¿Deseas " + accion + " sin guardar los cambios?",
                    "Advertencia",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            return op == JOptionPane.YES_OPTION;
        }
        return true;
    }

    private void setModoEdicion(boolean enabled) {
        modoEdicion = enabled;
        if (btnRegresar != null) btnRegresar.setVisible(enabled);
        if (btnGuardar != null) btnGuardar.setText(enabled ? "Guardar" : "Registrar");
        cpId.setEditable(!enabled);
        dirty = false;
    }

    private void abrirDialogoBuscar() {
        JDialog d = new JDialog(this, "Buscar Carta Porte", true);
        d.setSize(420, 220);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(10, 10));
        ((JComponent) d.getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        d.getContentPane().setBackground(Color.white);

        JLabel lbl = new JLabel("Escribe la CARTA PORTE a buscar:");
        lbl.setFont(POP14B);

        JTextField tfId = new JTextField();
        tfId.setFont(new Font("Poppins", Font.BOLD, 18));
        tfId.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JPanel center = new JPanel(new GridLayout(2, 1, 8, 8));
        center.setOpaque(false);
        center.add(lbl);
        center.add(tfId);

        JButton btnCancelar = new JButton("Cancelar");
        JButton btnOk = new JButton("Buscar");
        btnOk.setBackground(new Color(46, 204, 113));
        btnOk.setForeground(Color.WHITE);
        btnOk.setFocusPainted(false);
        btnOk.setFont(POP14B);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        south.setOpaque(false);
        south.add(btnCancelar);
        south.add(btnOk);

        btnCancelar.addActionListener(e -> d.dispose());
        btnOk.addActionListener(e -> {
            String txt = tfId.getText().trim();
            if (txt.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Escribe una CARTA PORTE.");
                return;
            }
            int id;
            try { id = Integer.parseInt(txt); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(d, "La CARTA PORTE debe ser un número entero.");
                return;
            }
            if (cargarCartaPorte(id)) d.dispose();
        });

        d.getRootPane().setDefaultButton(btnOk);
        d.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "ESC");
        d.getRootPane().getActionMap().put("ESC", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { d.dispose(); }
        });

        d.add(center, BorderLayout.CENTER);
        d.add(south, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private boolean cargarCartaPorte(int idCartaPorte) {
        String sql =
                "SELECT cp.*, " +
                        "c.Nom_Cliente AS NomCliente, " +
                        "re.Nom_Remitente AS NomRemitente, " +
                        "co.Nom_Consignatario AS NomConsignatario, " +
                        "op.Nom_Operador AS NomOperador, " +
                        "cu.Nom_Custodio AS NomCustodio, " +
                        "vc.Placa AS PlacaCabezal, " +
                        "vf.Placa AS PlacaFurgon " +
                        "FROM Carta_Porte cp " +
                        "LEFT JOIN Clientes c ON cp.ID_Cliente = c.ID_Cliente " +
                        "LEFT JOIN Remitentes re ON cp.ID_Remitente = re.ID_Remitente " +
                        "LEFT JOIN Consignatarios co ON cp.ID_Consignatario = co.ID_Consignatario " +
                        "LEFT JOIN Operadores op ON cp.ID_Operador = op.ID_Operador " +
                        "LEFT JOIN Custodios cu ON cp.ID_Custodio = cu.ID_Custodio " +
                        "LEFT JOIN Vehiculos vc ON cp.ID_Placa_Cabezal = vc.ID_Vehiculo " +
                        "LEFT JOIN Vehiculos vf ON cp.ID_Placa_Del_Furgon = vf.ID_Vehiculo " +
                        "WHERE cp.Carta_Porte_id = ? " +
                        "LIMIT 1";

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idCartaPorte);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this,
                            "No se encontró la Carta Porte: " + idCartaPorte,
                            "Sin resultados", JOptionPane.WARNING_MESSAGE);
                    return false;
                }

                programmaticChange = true;

                cpId.setText(String.valueOf(rs.getInt("Carta_Porte_id")));
                cliente.setText(nvl(rs.getString("NomCliente")));
                remitente.setText(nvl(rs.getString("NomRemitente")));
                consignat.setText(nvl(rs.getString("NomConsignatario")));
                operador.setText(nvl(rs.getString("NomOperador")));
                custodio.setText(nvl(rs.getString("NomCustodio")));

                placaCab.setText(nvl(rs.getString("PlacaCabezal")));
                placaFur.setText(nvl(rs.getString("PlacaFurgon")));

                factura1.setText(nvl(rs.getString("FACTURA")));
                fechaFactura.setText(convertirEntradaFechaBonita(nvl(rs.getString("FECHA_FACTURA"))));

                String codMonValor = rs.getString("MONEDA_VALOR");
                monedaValorActual = dbCodeToMoneda(codMonValor);
                cbMonedaValor.setSelectedItem(monedaValorActual);
                try { valor.setText(formatMoney(rs.getBigDecimal("VALOR"), monedaValorActual)); }
                catch (Exception ex) { valor.setText(nvl(rs.getString("VALOR"))); }

                fechaPago.setText(convertirEntradaFechaBonita(nvl(rs.getString("FECHA_DE_PAGO"))));
                destino.setText(nvl(rs.getString("DESTINO")));
                referencia.setText(nvl(rs.getString("REFERENCIA")));
                factura2.setText(nvl(rs.getString("FACTURA2")));

                String codMonFlete = rs.getString("MONEDA_VALOR_FLETE");
                monedaActual = dbCodeToMoneda(codMonFlete);
                cbMoneda.setSelectedItem(monedaActual);

                valorFlete.setText(formatMoney(rs.getBigDecimal("VALOR_FLETE"), monedaActual));
                anticipo.setText(formatMoney(rs.getBigDecimal("ANTICIPO"), monedaActual));
                aCancel.setText(formatMoney(rs.getBigDecimal("A_CANCELACION"), monedaActual));

                fechaPagado.setText(convertirEntradaFechaBonita(nvl(rs.getString("FECHA_DE_PAGADO"))));

                fCarga.setText(convertirEntradaFechaBonita(nvl(rs.getString("F_DE_CARGA"))));
                fCruce.setText(convertirEntradaFechaBonita(nvl(rs.getString("F_DE_CRUCE"))));
                fSalTU.setText(convertirEntradaFechaBonita(nvl(rs.getString("F_SAL_T_U"))));
                fFDestino.setText(convertirEntradaFechaBonita(nvl(rs.getString("F_F_DESTINO"))));
                fEnDestino.setText(convertirEntradaFechaBonita(nvl(rs.getString("F_EN_DESTINO"))));
                fDescarga.setText(convertirEntradaFechaBonita(nvl(rs.getString("F_DESCARGA"))));
                fEDoctos.setText(convertirEntradaFechaBonita(nvl(rs.getString("F_E_DE_DOCTOS"))));

                // VALOR CUSTODIO con moneda
                String codMonCustodio = rs.getString("MONEDA_VALOR_CUSTODIO");
                monedaCustodioActual = dbCodeToMoneda(codMonCustodio);
                cbMonedaCustodio.setSelectedItem(monedaCustodioActual);
                try { valorCustodio.setText(formatMoney(rs.getBigDecimal("VALOR_CUSTODIO"), monedaCustodioActual)); }
                catch (Exception ex) { valorCustodio.setText(""); }

                fechaPagoCustodio.setText(convertirEntradaFechaBonita(nvl(rs.getString("FECHA_PAGO_CUSTODIO"))));

                observaciones.setText(nvl(rs.getString("OBSERVACIONES")));

                recalc();
                setModoEdicion(true);

                programmaticChange = false;

                JOptionPane.showMessageDialog(this,
                        "Carta Porte encontrada. Lista para modificar.",
                        "Encontrada",
                        JOptionPane.INFORMATION_MESSAGE);

                return true;
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al buscar: " + ex.getMessage());
            ex.printStackTrace();
            programmaticChange = false;
            return false;
        }
    }

    // ================== Guardar (INSERT) ==================
    private void guardar() {
        if (empty(cpId)) {
            JOptionPane.showMessageDialog(this, "El campo CARTA PORTE es obligatorio.");
            return;
        }

        int idCartaPorte;
        try { idCartaPorte = Integer.parseInt(cpId.getText().trim()); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "CARTA PORTE debe ser un número entero.");
            return;
        }

        Double v = toNullableDouble(valor.getText());
        Double vf = toNullableDouble(valorFlete.getText());
        Double an = toNullableDouble(anticipo.getText());
        Double ac = toNullableDouble(aCancel.getText());
        Double vc = toNullableDouble(valorCustodio.getText());

        Moneda monedaValor = (monedaValorActual != null) ? monedaValorActual : Moneda.DOLAR;
        Moneda monedaFlete = (monedaActual != null) ? monedaActual : Moneda.DOLAR;
        Moneda monedaAnticipo = monedaFlete;
        Moneda monedaACancel = monedaFlete;
        Moneda monedaValCustodio = (monedaCustodioActual != null) ? monedaCustodioActual : Moneda.DOLAR;

        String nomCliente = cliente.getText().trim();
        if (nomCliente.isEmpty()) nomCliente = "";

        String nomRemitente = remitente.getText().trim();
        if (nomRemitente.isEmpty()) nomRemitente = "";

        String nomConsignatario = consignat.getText().trim();
        if (nomConsignatario.isEmpty()) nomConsignatario = "";

        String nomOperador = operador.getText().trim();
        if (nomOperador.isEmpty()) nomOperador = "";

        String nomCustodio = custodio.getText().trim();
        if (nomCustodio.isEmpty()) nomCustodio = "";

        String placaCabezalTxt = placaCab.getText().trim();
        if (placaCabezalTxt.isEmpty()) placaCabezalTxt = "";

        String placaFurgonTxt = placaFur.getText().trim();
        if (placaFurgonTxt.isEmpty()) placaFurgonTxt = "";

        String sql = "INSERT INTO Carta_Porte (" +
                "Carta_Porte_id, ID_Cliente, ID_Remitente, ID_Consignatario, ID_Operador, " +
                "ID_Placa_Cabezal, ID_Placa_Del_Furgon, ID_Custodio, " +
                "FACTURA, FECHA_FACTURA, VALOR, MONEDA_VALOR, FECHA_DE_PAGO, DESTINO, REFERENCIA, FACTURA2, " +
                "VALOR_FLETE, MONEDA_VALOR_FLETE, ANTICIPO, MONEDA_ANTICIPO, A_CANCELACION, MONEDA_A_CANCELACION, FECHA_DE_PAGADO, " +
                "F_DE_CARGA, F_DE_CRUCE, F_SAL_T_U, F_F_DESTINO, F_EN_DESTINO, " +
                "F_DESCARGA, F_E_DE_DOCTOS, " +
                "VALOR_CUSTODIO, MONEDA_VALOR_CUSTODIO, FECHA_PAGO_CUSTODIO, OBSERVACIONES) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection cn = getConnection()) {

            try (PreparedStatement chk = cn.prepareStatement(
                    "SELECT 1 FROM Carta_Porte WHERE Carta_Porte_id = ?")) {
                chk.setInt(1, idCartaPorte);
                try (ResultSet r = chk.executeQuery()) {
                    if (r.next()) {
                        JOptionPane.showMessageDialog(this,
                                "Ya existe una Carta de Porte con ese ID.",
                                "Duplicado", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }

            int idCliente = getOrCreateIdSimple(cn, "Clientes", "ID_Cliente", "Nom_Cliente", nomCliente);
            int idRemitente = getOrCreateIdSimple(cn, "Remitentes", "ID_Remitente", "Nom_Remitente", nomRemitente);
            int idConsignatario = getOrCreateIdSimple(cn, "Consignatarios", "ID_Consignatario", "Nom_Consignatario", nomConsignatario);
            int idOperador = getOrCreateIdSimple(cn, "Operadores", "ID_Operador", "Nom_Operador", nomOperador);
            int idPlacaCab = getOrCreateVehiculo(cn, placaCabezalTxt, "CABEZAL");
            int idPlacaFur = getOrCreateVehiculo(cn, placaFurgonTxt, "FURGON");
            int idCustodio = getOrCreateIdSimple(cn, "Custodios", "ID_Custodio", "Nom_Custodio", nomCustodio);

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                int i = 1;
                ps.setInt(i++, idCartaPorte);           // 1
                ps.setInt(i++, idCliente);              // 2
                ps.setInt(i++, idRemitente);            // 3
                ps.setInt(i++, idConsignatario);        // 4
                ps.setInt(i++, idOperador);             // 5
                ps.setInt(i++, idPlacaCab);             // 6
                ps.setInt(i++, idPlacaFur);             // 7
                ps.setInt(i++, idCustodio);             // 8

                ps.setString(i++, factura1.getText().trim());                    // 9
                ps.setString(i++, normalizeFecha(fechaFactura.getText().trim())); // 10
                setNullableDouble(ps, i++, v);                                    // 11
                ps.setString(i++, monedaToDbCode(monedaValor));                   // 12

                ps.setString(i++, normalizeFecha(fechaPago.getText().trim()));   // 13
                ps.setString(i++, destino.getText().trim());                     // 14
                ps.setString(i++, referencia.getText().trim());                  // 15
                ps.setString(i++, factura2.getText().trim());                    // 16

                setNullableDouble(ps, i++, vf);                                  // 17
                ps.setString(i++, monedaToDbCode(monedaFlete));                  // 18

                setNullableDouble(ps, i++, an);                                  // 19
                ps.setString(i++, monedaToDbCode(monedaAnticipo));               // 20

                setNullableDouble(ps, i++, ac);                                  // 21
                ps.setString(i++, monedaToDbCode(monedaACancel));                // 22

                ps.setString(i++, normalizeFecha(fechaPagado.getText().trim())); // 23

                ps.setString(i++, normalizeFecha(fCarga.getText().trim()));      // 24
                ps.setString(i++, normalizeFecha(fCruce.getText().trim()));      // 25
                ps.setString(i++, normalizeFecha(fSalTU.getText().trim()));      // 26
                ps.setString(i++, normalizeFecha(fFDestino.getText().trim()));   // 27
                ps.setString(i++, normalizeFecha(fEnDestino.getText().trim()));  // 28
                ps.setString(i++, normalizeFecha(fDescarga.getText().trim()));   // 29
                ps.setString(i++, normalizeFecha(fEDoctos.getText().trim()));    // 30

                setNullableDouble(ps, i++, vc);                                  // 31
                ps.setString(i++, monedaToDbCode(monedaValCustodio));            // 32
                ps.setString(i++, normalizeFecha(fechaPagoCustodio.getText().trim())); // 33
                ps.setString(i++, observaciones.getText().trim());               // 34

                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Carta de Porte registrada correctamente.");

                actualizarSugerenciasEnMemoriaDesdeCampos();
                limpiar();
                actualizarLabelUltimaCp();
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void actualizar() {
        if (empty(cpId)) {
            JOptionPane.showMessageDialog(this, "El campo CARTA PORTE es obligatorio.");
            return;
        }

        int idCartaPorte;
        try { idCartaPorte = Integer.parseInt(cpId.getText().trim()); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "CARTA PORTE debe ser un número entero.");
            return;
        }

        Double v = toNullableDouble(valor.getText());
        Double vf = toNullableDouble(valorFlete.getText());
        Double an = toNullableDouble(anticipo.getText());
        Double ac = toNullableDouble(aCancel.getText());
        Double vc = toNullableDouble(valorCustodio.getText());

        Moneda monedaValor = (monedaValorActual != null) ? monedaValorActual : Moneda.DOLAR;
        Moneda monedaFlete = (monedaActual != null) ? monedaActual : Moneda.DOLAR;
        Moneda monedaAnticipo = monedaFlete;
        Moneda monedaACancel = monedaFlete;
        Moneda monedaValCustodio = (monedaCustodioActual != null) ? monedaCustodioActual : Moneda.DOLAR;

        String nomCliente = cliente.getText().trim();
        if (nomCliente.isEmpty()) nomCliente = "SIN CLIENTE";

        String nomRemitente = remitente.getText().trim();
        if (nomRemitente.isEmpty()) nomRemitente = "SIN REMITENTE";

        String nomConsignatario = consignat.getText().trim();
        if (nomConsignatario.isEmpty()) nomConsignatario = "SIN CONSIGNATARIO";

        String nomOperador = operador.getText().trim();
        if (nomOperador.isEmpty()) nomOperador = "SIN OPERADOR";

        String nomCustodio = custodio.getText().trim();
        if (nomCustodio.isEmpty()) nomCustodio = "SIN CUSTODIO";

        String placaCabezalTxt = placaCab.getText().trim();
        if (placaCabezalTxt.isEmpty()) placaCabezalTxt = "SIN-PLACA-CAB";

        String placaFurgonTxt = placaFur.getText().trim();
        if (placaFurgonTxt.isEmpty()) placaFurgonTxt = "SIN-PLACA-FUR";

        String sqlUpd =
                "UPDATE Carta_Porte SET " +
                        "ID_Cliente=?, ID_Remitente=?, ID_Consignatario=?, ID_Operador=?, " +
                        "ID_Placa_Cabezal=?, ID_Placa_Del_Furgon=?, ID_Custodio=?, " +
                        "FACTURA=?, FECHA_FACTURA=?, VALOR=?, MONEDA_VALOR=?, FECHA_DE_PAGO=?, DESTINO=?, REFERENCIA=?, FACTURA2=?, " +
                        "VALOR_FLETE=?, MONEDA_VALOR_FLETE=?, ANTICIPO=?, MONEDA_ANTICIPO=?, A_CANCELACION=?, MONEDA_A_CANCELACION=?, FECHA_DE_PAGADO=?, " +
                        "F_DE_CARGA=?, F_DE_CRUCE=?, F_SAL_T_U=?, F_F_DESTINO=?, F_EN_DESTINO=?, " +
                        "F_DESCARGA=?, F_E_DE_DOCTOS=?, " +
                        "VALOR_CUSTODIO=?, MONEDA_VALOR_CUSTODIO=?, FECHA_PAGO_CUSTODIO=?, OBSERVACIONES=? " +
                        "WHERE Carta_Porte_id=?";

        try (Connection cn = getConnection()) {

            try (PreparedStatement chk = cn.prepareStatement(
                    "SELECT 1 FROM Carta_Porte WHERE Carta_Porte_id = ?")) {
                chk.setInt(1, idCartaPorte);
                try (ResultSet r = chk.executeQuery()) {
                    if (!r.next()) {
                        JOptionPane.showMessageDialog(this,
                                "No existe esa Carta de Porte. Usa Registrar para crearla.",
                                "No existe", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }

            int idCliente = getOrCreateIdSimple(cn, "Clientes", "ID_Cliente", "Nom_Cliente", nomCliente);
            int idRemitente = getOrCreateIdSimple(cn, "Remitentes", "ID_Remitente", "Nom_Remitente", nomRemitente);
            int idConsignatario = getOrCreateIdSimple(cn, "Consignatarios", "ID_Consignatario", "Nom_Consignatario", nomConsignatario);
            int idOperador = getOrCreateIdSimple(cn, "Operadores", "ID_Operador", "Nom_Operador", nomOperador);
            int idPlacaCab = getOrCreateVehiculo(cn, placaCabezalTxt, "CABEZAL");
            int idPlacaFur = getOrCreateVehiculo(cn, placaFurgonTxt, "FURGON");
            int idCustodio = getOrCreateIdSimple(cn, "Custodios", "ID_Custodio", "Nom_Custodio", nomCustodio);

            try (PreparedStatement ps = cn.prepareStatement(sqlUpd)) {
                int i = 1;

                ps.setInt(i++, idCliente);
                ps.setInt(i++, idRemitente);
                ps.setInt(i++, idConsignatario);
                ps.setInt(i++, idOperador);
                ps.setInt(i++, idPlacaCab);
                ps.setInt(i++, idPlacaFur);
                ps.setInt(i++, idCustodio);

                ps.setString(i++, factura1.getText().trim());
                ps.setString(i++, normalizeFecha(fechaFactura.getText().trim()));
                setNullableDouble(ps, i++, v);
                ps.setString(i++, monedaToDbCode(monedaValor));

                ps.setString(i++, normalizeFecha(fechaPago.getText().trim()));
                ps.setString(i++, destino.getText().trim());
                ps.setString(i++, referencia.getText().trim());
                ps.setString(i++, factura2.getText().trim());

                setNullableDouble(ps, i++, vf);
                ps.setString(i++, monedaToDbCode(monedaFlete));

                setNullableDouble(ps, i++, an);
                ps.setString(i++, monedaToDbCode(monedaAnticipo));

                setNullableDouble(ps, i++, ac);
                ps.setString(i++, monedaToDbCode(monedaACancel));

                ps.setString(i++, normalizeFecha(fechaPagado.getText().trim()));

                ps.setString(i++, normalizeFecha(fCarga.getText().trim()));
                ps.setString(i++, normalizeFecha(fCruce.getText().trim()));
                ps.setString(i++, normalizeFecha(fSalTU.getText().trim()));
                ps.setString(i++, normalizeFecha(fFDestino.getText().trim()));
                ps.setString(i++, normalizeFecha(fEnDestino.getText().trim()));
                ps.setString(i++, normalizeFecha(fDescarga.getText().trim()));
                ps.setString(i++, normalizeFecha(fEDoctos.getText().trim()));

                setNullableDouble(ps, i++, vc);
                ps.setString(i++, monedaToDbCode(monedaValCustodio));
                ps.setString(i++, normalizeFecha(fechaPagoCustodio.getText().trim()));
                ps.setString(i++, observaciones.getText().trim());

                ps.setInt(i++, idCartaPorte);

                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Cambios guardados correctamente.");

                actualizarSugerenciasEnMemoriaDesdeCampos();
                limpiar();
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void limpiar() {
        programmaticChange = true;

        JTextField[] all = {
                cpId, cliente, factura1, fechaFactura, valor, fechaPago, destino, referencia,
                remitente, consignat, factura2, operador, placaCab, placaFur,
                valorFlete, anticipo, aCancel, fechaPagado, fCarga, fCruce,
                fSalTU, fFDestino, fEnDestino, fDescarga, fEDoctos, custodio,
                valorCustodio, fechaPagoCustodio
        };
        for (JTextField t : all) t.setText("");

        observaciones.setText("");

        cbMonedaValor.setSelectedItem(Moneda.DOLAR);
        monedaValorActual = (Moneda) cbMonedaValor.getSelectedItem();

        cbMonedaCustodio.setSelectedItem(Moneda.DOLAR);
        monedaCustodioActual = (Moneda) cbMonedaCustodio.getSelectedItem();

        setModoEdicion(false);

        recalc();

        programmaticChange = false;
    }

    private void instalarConversorFechasEnCampos() {
        JTextField[] dateFields = {
                fechaFactura, fechaPago, fechaPagado, fCarga, fCruce, fSalTU,
                fFDestino, fEnDestino, fDescarga, fEDoctos, fechaPagoCustodio
        };
        for (JTextField f : dateFields) instalarConversorFechaEn(f);
    }

    private void instalarConversorFechaEn(JTextField field) {
        if (field == null) return;

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                field.setText(convertirEntradaFechaBonita(field.getText()));
            }
        });

        field.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    field.setText(convertirEntradaFechaBonita(field.getText()));
                    e.consume();
                }
            }
        });
    }

    private static String formatearFechaBonita(java.util.Date d) {
        if (d == null) return "";
        SimpleDateFormat fmt = new SimpleDateFormat("d 'de' MMMM 'del' yyyy", FechaMX);
        fmt.setLenient(false);
        return fmt.format(d);
    }

    private static java.util.Date parseFechaFlexibleDate(String input) {
        if (input == null) return null;
        String t = input.trim();
        if (t.isEmpty()) return null;

        String tl = t.toLowerCase(FechaMX).trim();
        if (tl.equals("hoy")) return new java.util.Date();

        final String[] patronesConAnio = {
                "yyyy-MM-dd", "yyyy-MM-dd HH:mm", "dd/MM/yyyy", "d/M/yyyy",
                "dd-MM-yyyy", "d-M-yyyy", "d 'de' MMMM 'del' yyyy", "d 'de' MMMM 'de' yyyy"
        };

        for (String patron : patronesConAnio) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(patron, FechaMX);
                in.setLenient(false);
                return in.parse(t);
            } catch (Exception ignore) {}
        }

        try {
            if (t.matches("^\\d{1,2}[-/]\\d{1,2}$")) {
                String[] parts = t.split("[-/]");
                int dia = Integer.parseInt(parts[0]);
                int mes = Integer.parseInt(parts[1]);

                Calendar cal = Calendar.getInstance(FechaMX);
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
        java.util.Date d = parseFechaFlexibleDate(t);
        return (d != null) ? formatearFechaBonita(d) : texto;
    }

    private static String normalizeFecha(String fecha) {
        if (fecha == null) return "";
        String t = fecha.trim();
        if (t.isEmpty()) return "";
        java.util.Date d = parseFechaFlexibleDate(t);
        if (d != null) {
            SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd", FechaMX);
            out.setLenient(false);
            return out.format(d);
        }
        return t;
    }

    private static final String[] MESES_ES = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    private JComponent withDatePicker(JTextField field) {
        Border original = field.getBorder();
        field.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        wrapper.setOpaque(true);
        wrapper.setBackground(Color.white);
        if (original != null) wrapper.setBorder(original);
        wrapper.add(field, BorderLayout.CENTER);

        JButton btn = new JButton("📅");
        btn.setMargin(new Insets(2, 8, 2, 8));
        btn.setFocusPainted(false);
        btn.setBackground(Color.CYAN);
        btn.setToolTipText("Seleccionar fecha");
        btn.setFont(POP14B);
        btn.addActionListener(e -> showFechaDialog(field));
        wrapper.add(btn, BorderLayout.EAST);

        return wrapper;
    }

    private JComponent wrapTextArea(String title, JTextArea area) {
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setPreferredSize(new Dimension(900, 180));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(Color.white);
        wrapper.setBorder(titled(title));
        wrapper.add(sp, BorderLayout.CENTER);
        return wrapper;
    }

    private JComponent withCurrencyPicker(JTextField field, JComboBox<Moneda> monedaCombo) {
        Border original = field.getBorder();
        field.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        wrapper.setOpaque(true);
        wrapper.setBackground(Color.white);
        if (original != null) wrapper.setBorder(original);

        monedaCombo.setFocusable(false);
        monedaCombo.setPreferredSize(new Dimension(160, field.getPreferredSize().height));
        monedaCombo.setFont(fontOrFallback("Poppins", Font.BOLD, 12));

        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(monedaCombo, BorderLayout.EAST);
        return wrapper;
    }

    private void showFechaDialog(JTextField target) {
        LocalDate pre = parseFechaFlexibleLocalDate(target.getText());
        if (pre == null) pre = LocalDate.now();
        new FechaDialog(this, target, pre).setVisible(true);
    }

    private static LocalDate parseFechaFlexibleLocalDate(String s) {
        java.util.Date d = parseFechaFlexibleDate(s);
        if (d == null) return null;
        Calendar cal = Calendar.getInstance(FechaMX);
        cal.setTime(d);
        return LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private static int daysInMonth(int year, int month1to12) {
        return java.time.Month.of(month1to12).length(java.time.Year.isLeap(year));
    }

    private class FechaDialog extends JDialog {
        private final JTextField target;
        private final JComboBox<Integer> cbDia;
        private final JComboBox<String> cbMes;
        private final JComboBox<Integer> cbAnio;
        private final JLabel preview;

        FechaDialog(Frame owner, JTextField target, LocalDate initial) {
            super(owner, "Seleccionar fecha", true);
            this.target = target;

            setSize(420, 240);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10, 10));
            ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            getContentPane().setBackground(Color.white);

            JPanel center = new JPanel(new GridBagLayout());
            center.setOpaque(true);
            center.setBackground(Color.white);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6, 6, 6, 6);
            c.fill = GridBagConstraints.HORIZONTAL;

            int yNow = LocalDate.now().getYear();
            java.util.List<Integer> años = new ArrayList<>();
            for (int y = yNow - 70; y <= yNow + 10; y++) años.add(y);

            cbDia = new JComboBox<>();
            cbMes = new JComboBox<>(MESES_ES);
            cbAnio = new JComboBox<>(años.toArray(new Integer[0]));
            cbDia.setFont(POP14B);
            cbMes.setFont(POP14B);
            cbAnio.setFont(POP14B);

            cbMes.setSelectedIndex(initial.getMonthValue() - 1);
            cbAnio.setSelectedItem(initial.getYear());
            refillDias();
            cbDia.setSelectedItem(initial.getDayOfMonth());

            preview = new JLabel(formatearFechaBonita(java.sql.Date.valueOf(getDate())));
            preview.setFont(POP14B);

            ActionListener upd = e -> {
                refillDias();
                preview.setText(formatearFechaBonita(java.sql.Date.valueOf(getDate())));
            };
            cbMes.addActionListener(upd);
            cbAnio.addActionListener(upd);
            cbDia.addActionListener(e -> preview.setText(formatearFechaBonita(java.sql.Date.valueOf(getDate()))));

            JLabel lDia = new JLabel("Día:"); lDia.setFont(POP14B);
            JLabel lMes = new JLabel("Mes:"); lMes.setFont(POP14B);
            JLabel lAnio = new JLabel("Año:"); lAnio.setFont(POP14B);

            c.gridx = 0; c.gridy = 0; center.add(lDia, c);
            c.gridx = 1; c.gridy = 0; center.add(cbDia, c);
            c.gridx = 0; c.gridy = 1; center.add(lMes, c);
            c.gridx = 1; c.gridy = 1; center.add(cbMes, c);
            c.gridx = 0; c.gridy = 2; center.add(lAnio, c);
            c.gridx = 1; c.gridy = 2; center.add(cbAnio, c);

            JPanel pv = new JPanel(new FlowLayout(FlowLayout.CENTER));
            pv.setOpaque(true);
            pv.setBackground(Color.white);
            pv.add(preview);

            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            south.setOpaque(true);
            south.setBackground(Color.white);

            JButton btnAsignar = new JButton("Asignar");
            JButton btnCancelar = new JButton("Cancelar");

            south.add(btnCancelar);
            south.add(btnAsignar);

            btnCancelar.addActionListener(e -> dispose());
            btnAsignar.addActionListener(e -> {
                target.setText(formatearFechaBonita(java.sql.Date.valueOf(getDate())));
                dispose();
            });

            getRootPane().setDefaultButton(btnAsignar);
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke("ESCAPE"), "ESC");
            getRootPane().getActionMap().put("ESC", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { dispose(); }
            });

            add(center, BorderLayout.WEST);
            add(pv, BorderLayout.CENTER);
            add(south, BorderLayout.SOUTH);
        }

        private void refillDias() {
            Integer yObj = (Integer) cbAnio.getSelectedItem();
            int y = (yObj != null) ? yObj : LocalDate.now().getYear();
            int m = cbMes.getSelectedIndex() + 1;
            int max = daysInMonth(y, m);
            Integer sel = (Integer) cbDia.getSelectedItem();

            cbDia.removeAllItems();
            for (int d = 1; d <= max; d++) cbDia.addItem(d);

            int toSelect = 1;
            if (sel != null) toSelect = Math.min(sel, max);
            cbDia.setSelectedItem(toSelect);
        }

        private LocalDate getDate() {
            Integer dObj = (Integer) cbDia.getSelectedItem();
            Integer yObj = (Integer) cbAnio.getSelectedItem();
            int d = (dObj != null) ? dObj : 1;
            int m = cbMes.getSelectedIndex() + 1;
            int y = (yObj != null) ? yObj : LocalDate.now().getYear();
            return LocalDate.of(y, m, d);
        }
    }

    private void instalarTrackingCambios() {
        DocumentListener markDirty = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { touch(); }
            @Override public void removeUpdate(DocumentEvent e) { touch(); }
            @Override public void changedUpdate(DocumentEvent e) { touch(); }
            private void touch() {
                if (programmaticChange) return;
                if (!modoEdicion) return;
                dirty = true;
            }
        };

        JTextField[] allFields = {
                cpId, cliente, factura1, fechaFactura, valor, fechaPago,
                destino, referencia, remitente, consignat, factura2,
                operador, placaCab, placaFur, valorFlete, anticipo, aCancel,
                fechaPagado, fCarga, fCruce, fSalTU, fFDestino, fEnDestino,
                fDescarga, fEDoctos, custodio, valorCustodio, fechaPagoCustodio
        };

        for (JTextField f : allFields) {
            if (f != null && f.getDocument() != null) {
                f.getDocument().addDocumentListener(markDirty);
            }
        }

        if (observaciones != null && observaciones.getDocument() != null) {
            observaciones.getDocument().addDocumentListener(markDirty);
        }
    }

    private static Font fontOrFallback(String name, int style, int size) {
        Font f = new Font(name, style, size);
        if (!name.equalsIgnoreCase(f.getFamily())) {
            f = new Font(Font.SANS_SERIF, style, size);
        }
        return f;
    }

    private final Map<String, LinkedHashSet<String>> sugerenciasInput = new HashMap<>();

    private static String normalizeAuto(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}+","").toLowerCase(java.util.Locale.ROOT).trim();
        return n;
    }

    private static void addUnique(LinkedHashSet<String> set, String value) {
        if (set == null) return;
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;

        String nv = normalizeAuto(v);
        for (String s : set) {
            if (normalizeAuto(s).equals(nv)) return;
        }
        set.add(v);
    }

    private void initSugerenciasInput() {
        sugerenciasInput.put("CLIENTE",        new LinkedHashSet<>());
        sugerenciasInput.put("DESTINO",        new LinkedHashSet<>());
        sugerenciasInput.put("REMITENTE",      new LinkedHashSet<>());
        sugerenciasInput.put("CONSIGNATARIO",  new LinkedHashSet<>());
        sugerenciasInput.put("OPERADOR",       new LinkedHashSet<>());
        sugerenciasInput.put("PLACA_CABEZAL",  new LinkedHashSet<>());
        sugerenciasInput.put("PLACA_FURGON",   new LinkedHashSet<>());
        sugerenciasInput.put("CUSTODIO",       new LinkedHashSet<>());
    }

    private void cargarSugerenciasDesdeDB() {
        initSugerenciasInput();

        try (Connection cn = getConnection()) {
            cargarColumna(cn, "SELECT Nom_Cliente FROM Clientes ORDER BY Nom_Cliente", "Nom_Cliente",
                    sugerenciasInput.get("CLIENTE"));
            cargarColumna(cn, "SELECT Nom_Remitente FROM Remitentes ORDER BY Nom_Remitente", "Nom_Remitente",
                    sugerenciasInput.get("REMITENTE"));
            cargarColumna(cn, "SELECT Nom_Consignatario FROM Consignatarios ORDER BY Nom_Consignatario", "Nom_Consignatario",
                    sugerenciasInput.get("CONSIGNATARIO"));
            cargarColumna(cn, "SELECT Nom_Operador FROM Operadores ORDER BY Nom_Operador", "Nom_Operador",
                    sugerenciasInput.get("OPERADOR"));
            cargarColumna(cn, "SELECT Nom_Custodio FROM Custodios ORDER BY Nom_Custodio", "Nom_Custodio",
                    sugerenciasInput.get("CUSTODIO"));
            cargarColumna(cn,
                    "SELECT DISTINCT DESTINO FROM Carta_Porte WHERE DESTINO IS NOT NULL AND TRIM(DESTINO) <> '' ORDER BY DESTINO",
                    "DESTINO", sugerenciasInput.get("DESTINO"));
            cargarColumna(cn,
                    "SELECT DISTINCT Placa FROM Vehiculos WHERE tipo_placa='CABEZAL' AND Placa IS NOT NULL AND TRIM(Placa) <> '' ORDER BY Placa",
                    "Placa", sugerenciasInput.get("PLACA_CABEZAL"));
            cargarColumna(cn,
                    "SELECT DISTINCT Placa FROM Vehiculos WHERE tipo_placa='FURGON' AND Placa IS NOT NULL AND TRIM(Placa) <> '' ORDER BY Placa",
                    "Placa", sugerenciasInput.get("PLACA_FURGON"));
        } catch (SQLException ex) {
            System.err.println("[AUTOCOMPLETADO] No se pudieron cargar sugerencias: " + ex.getMessage());
        }
    }

    private void cargarColumna(Connection cn, String sql, String col, LinkedHashSet<String> out) throws SQLException {
        if (out == null) return;
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                addUnique(out, rs.getString(col));
            }
        }
    }

    private void instalarAutocompletadoEnCampos() {
        new FieldSuggestor(cliente,   sugerenciasInput.get("CLIENTE"));
        new FieldSuggestor(destino,   sugerenciasInput.get("DESTINO"));
        new FieldSuggestor(remitente, sugerenciasInput.get("REMITENTE"));
        new FieldSuggestor(consignat, sugerenciasInput.get("CONSIGNATARIO"));
        new FieldSuggestor(operador,  sugerenciasInput.get("OPERADOR"));
        new FieldSuggestor(placaCab,  sugerenciasInput.get("PLACA_CABEZAL"));
        new FieldSuggestor(placaFur,  sugerenciasInput.get("PLACA_FURGON"));
        new FieldSuggestor(custodio,  sugerenciasInput.get("CUSTODIO"));
    }

    private void actualizarSugerenciasEnMemoriaDesdeCampos() {
        addUnique(sugerenciasInput.get("CLIENTE"),        cliente.getText());
        addUnique(sugerenciasInput.get("DESTINO"),        destino.getText());
        addUnique(sugerenciasInput.get("REMITENTE"),      remitente.getText());
        addUnique(sugerenciasInput.get("CONSIGNATARIO"),  consignat.getText());
        addUnique(sugerenciasInput.get("OPERADOR"),       operador.getText());
        addUnique(sugerenciasInput.get("PLACA_CABEZAL"),  placaCab.getText());
        addUnique(sugerenciasInput.get("PLACA_FURGON"),   placaFur.getText());
        addUnique(sugerenciasInput.get("CUSTODIO"),       custodio.getText());
    }

    private class FieldSuggestor {
    private final JTextField field;
    private final Set<String> base;

    private final JPopupMenu popup = new JPopupMenu();
    private final JList<String> list = new JList<>(new DefaultListModel<>());
    private final JScrollPane sp = new JScrollPane(list);

    private String lastText = "";
    private boolean navigating = false;

    FieldSuggestor(JTextField field, Set<String> base) {
        this.field = field;
        this.base = base;

        popup.setFocusable(false);
        popup.setBorder(BorderFactory.createLineBorder(new Color(180,180,180)));
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        popup.add(sp);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(field.getFont());

        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onTextChange(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onTextChange(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onTextChange(); }
            
            private void onTextChange() {
                if (navigating || programmaticChange) return;
                SwingUtilities.invokeLater(() -> updateSuggestions());
            }
        });

        field.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (!popup.isVisible()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        updateSuggestions();
                        if (popup.isVisible() && list.getModel().getSize() > 0) {
                            list.setSelectedIndex(0);
                            list.ensureIndexIsVisible(0);
                        }
                        e.consume();
                    }
                    return;
                }

                int size = list.getModel().getSize();
                
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (size > 0) {
                        int idx = list.getSelectedIndex();
                        if (idx < 0) {
                            list.setSelectedIndex(0);
                        } else if (idx < size - 1) {
                            list.setSelectedIndex(idx + 1);
                        }
                        list.ensureIndexIsVisible(list.getSelectedIndex());
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (size > 0) {
                        int idx = list.getSelectedIndex();
                        if (idx > 0) {
                            list.setSelectedIndex(idx - 1);
                            list.ensureIndexIsVisible(list.getSelectedIndex());
                        } else if (idx == 0) {
                            list.clearSelection();
                        }
                    }
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.setVisible(false);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (list.getSelectedValue() != null) {
                        applySelection();
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    if (list.getSelectedValue() != null) {
                        applySelection();
                    } else {
                        popup.setVisible(false);
                    }
                }
            }
        });

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                applySelection();
            }
        });

        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0 && idx != list.getSelectedIndex()) {
                    list.setSelectedIndex(idx);
                }
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (!list.hasFocus()) {
                        popup.setVisible(false);
                    }
                });
            }
        });
    }

    private void applySelection() {
        String sel = list.getSelectedValue();
        if (sel != null) {
            navigating = true;
            programmaticChange = true;
            
            field.setText(sel);
            lastText = sel;
            
            programmaticChange = false;
            navigating = false;
            
            popup.setVisible(false);
            SwingUtilities.invokeLater(field::requestFocusInWindow);
        }
    }

    private void updateSuggestions() {
        if (navigating || programmaticChange) return;
        if (base == null || base.isEmpty()) { 
            popup.setVisible(false); 
            return; 
        }

        String txt = field.getText();
        
        if (txt.equals(lastText) && popup.isVisible()) {
            return;
        }
        lastText = txt;

        DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
        model.clear();

        String needle = normalizeAuto(txt);

        int limit = needle.isEmpty() ? 10 : 20;
        int count = 0;

        if (needle.isEmpty()) {
            for (String s : base) {
                model.addElement(s);
                if (++count >= limit) break;
            }
        } else {
            for (String s : base) {
                if (normalizeAuto(s).contains(needle)) {
                    model.addElement(s);
                    if (++count >= limit) break;
                }
            }
        }

        if (model.getSize() == 0) {
            popup.setVisible(false);
            return;
        }

        int w = Math.max(240, field.getWidth());
        int rowH = field.getFontMetrics(field.getFont()).getHeight();
        int visibleRows = Math.min(8, model.getSize());
        int h = Math.max(120, Math.min(220, visibleRows * rowH + 18));

        list.setVisibleRowCount(visibleRows);
        sp.setPreferredSize(new Dimension(w, h));
        popup.setPopupSize(new Dimension(w, h));

        if (!popup.isVisible() && field.hasFocus()) {
            popup.show(field, 0, field.getHeight());
        } else {
            popup.revalidate();
            popup.repaint();
        }
    }
}

    private int obtenerUltimaCartaPorte() {
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT MAX(Carta_Porte_id) FROM Carta_Porte");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int max = rs.getInt(1);
                return rs.wasNull() ? 0 : max;
            }
        } catch (SQLException ex) {
            System.err.println("[INFO] No se pudo obtener ultima Carta Porte: " + ex.getMessage());
        }
        return 0;
    }

    private void actualizarLabelUltimaCp() {
        if (lblUltimaCp == null) return;
        int ultimo = obtenerUltimaCartaPorte();
        if (ultimo > 0) {
            lblUltimaCp.setText("Ultima Carta Porte: " + ultimo);
        } else {
            lblUltimaCp.setText("Ultima Carta Porte: Sin registros");
        }
    }

    public void cargarYEditarCartaPorte(int idCartaPorte) {
        if (cargarCartaPorte(idCartaPorte)) {
            SwingUtilities.invokeLater(() -> {
                if (cliente != null) {
                    cliente.requestFocusInWindow();
                }
            });
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("System".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            Ingresar clase = new Ingresar();
            clase.setVisible(true);
        });
    }
}