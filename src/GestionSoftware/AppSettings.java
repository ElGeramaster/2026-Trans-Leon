package GestionSoftware;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Configuracion global de la aplicacion: tema (claro/oscuro) y tamano de fuente.
 *
 * Las preferencias se persisten con {@link java.util.prefs.Preferences} para que
 * la seleccion del usuario sobreviva entre ejecuciones. Las pantallas que quieran
 * reaccionar a los cambios deben registrar un {@link Runnable} via
 * {@link #addChangeListener(Runnable)} y desregistrarlo al cerrarse.
 */
public final class AppSettings {

    // ==================== ENUMS ====================

    public enum Theme {
        LIGHT("Claro"),
        DARK("Oscuro");

        private final String label;
        Theme(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum FontSize {
        PEQUENA ("Pequena",      0.85f),
        NORMAL  ("Normal",       1.00f),
        GRANDE  ("Grande",       1.20f),
        EXTRA   ("Extra Grande", 1.40f);

        private final String label;
        private final float scale;
        FontSize(String label, float scale) { this.label = label; this.scale = scale; }
        public float getScale() { return scale; }
        @Override public String toString() { return label; }
    }

    // ==================== PALETAS ====================

    // Paleta clara (rosa/rojo suave, consistente con los colores originales)
    private static final Color LIGHT_BG      = new Color(243, 183, 182);
    private static final Color LIGHT_PANEL   = new Color(249, 220, 220);
    private static final Color LIGHT_FG      = new Color( 20,  20,  20);
    private static final Color LIGHT_ACCENT  = new Color(229,  91, 114);
    private static final Color LIGHT_HOVER   = new Color(249, 197, 210);
    private static final Color LIGHT_BORDER  = new Color(180,  70,  90);
    private static final Color LIGHT_MUTED   = new Color(120, 120, 120);

    // Paleta oscura inspirada en Nord/One Dark: azul profundo con acentos suaves
    private static final Color DARK_BG       = new Color( 30,  33,  41);
    private static final Color DARK_PANEL    = new Color( 42,  46,  56);
    private static final Color DARK_FG       = new Color(226, 230, 240);
    private static final Color DARK_ACCENT   = new Color( 94, 129, 172);
    private static final Color DARK_HOVER    = new Color(129, 161, 193);
    private static final Color DARK_BORDER   = new Color( 67,  76,  94);
    private static final Color DARK_MUTED    = new Color(180, 185, 200);

    // ==================== SINGLETON ====================

    private static final AppSettings INSTANCE = new AppSettings();

    private static final String KEY_THEME     = "theme";
    private static final String KEY_FONT_SIZE = "fontSize";

    private final Preferences prefs = Preferences.userNodeForPackage(AppSettings.class);
    private final List<Runnable> listeners = new ArrayList<>();

    private Theme theme;
    private FontSize fontSize;

    private AppSettings() {
        Theme t;
        try { t = Theme.valueOf(prefs.get(KEY_THEME, Theme.LIGHT.name())); }
        catch (Exception ex) { t = Theme.LIGHT; }
        FontSize f;
        try { f = FontSize.valueOf(prefs.get(KEY_FONT_SIZE, FontSize.NORMAL.name())); }
        catch (Exception ex) { f = FontSize.NORMAL; }
        this.theme = t;
        this.fontSize = f;
    }

    public static AppSettings get() { return INSTANCE; }

    // ==================== GETTERS/SETTERS ====================

    public Theme getTheme()       { return theme; }
    public FontSize getFontSize() { return fontSize; }
    public boolean isDark()       { return theme == Theme.DARK; }

    public void setTheme(Theme t) {
        if (t == null || t == theme) return;
        theme = t;
        prefs.put(KEY_THEME, t.name());
        fireChanged();
    }

    public void setFontSize(FontSize f) {
        if (f == null || f == fontSize) return;
        fontSize = f;
        prefs.put(KEY_FONT_SIZE, f.name());
        fireChanged();
    }

    // ==================== LISTENERS ====================

    public void addChangeListener(Runnable r) {
        if (r != null && !listeners.contains(r)) listeners.add(r);
    }

    public void removeChangeListener(Runnable r) {
        if (r != null) listeners.remove(r);
    }

    private void fireChanged() {
        for (Runnable r : new ArrayList<>(listeners)) {
            try { r.run(); } catch (Exception ignore) {}
        }
    }

    // ==================== PALETA ACTIVA ====================

    public Color bg()          { return isDark() ? DARK_BG      : LIGHT_BG;     }
    public Color panel()       { return isDark() ? DARK_PANEL   : LIGHT_PANEL;  }
    public Color fg()          { return isDark() ? DARK_FG      : LIGHT_FG;     }
    public Color accent()      { return isDark() ? DARK_ACCENT  : LIGHT_ACCENT; }
    public Color accentHover() { return isDark() ? DARK_HOVER   : LIGHT_HOVER;  }
    public Color border()      { return isDark() ? DARK_BORDER  : LIGHT_BORDER; }
    public Color muted()       { return isDark() ? DARK_MUTED   : LIGHT_MUTED;  }
    public Color onAccent()    { return Color.WHITE; }

    // ==================== FUENTES ====================

    public float scale() { return fontSize.getScale(); }

    /** Retorna una nueva fuente escalada segun la configuracion actual. */
    public Font scaled(String family, int style, int baseSize) {
        int size = Math.max(8, Math.round(baseSize * fontSize.getScale()));
        return fontOrFallback(family, style, size);
    }

    /** Intenta crear la fuente solicitada, o devuelve Dialog si no existe. */
    public static Font fontOrFallback(String family, int style, int size) {
        try {
            Font f = new Font(family, style, size);
            // Font(..) nunca retorna null; comprobamos si la familia existe
            if (!f.getFamily().equalsIgnoreCase(family)) {
                return new Font(Font.DIALOG, style, size);
            }
            return f;
        } catch (Exception ex) {
            return new Font(Font.DIALOG, style, size);
        }
    }

    // ==================== DIALOGO DE CONFIGURACION ====================

    /** Abre el dialogo modal de configuracion centrado en el propietario. */
    public static void showDialog(Window owner) {
        SettingsDialog dlg = new SettingsDialog(owner);
        dlg.setVisible(true);
    }

    private static final class SettingsDialog extends JDialog {
        SettingsDialog(Window owner) {
            super(owner, "Configuracion", ModalityType.APPLICATION_MODAL);
            final AppSettings s = AppSettings.get();

            setLayout(new BorderLayout());
            setSize(500, 400);
            setLocationRelativeTo(owner);
            setResizable(false);
            getContentPane().setBackground(s.bg());

            // ----- Titulo -----
            JLabel title = new JLabel("Personalizacion");
            title.setFont(s.scaled("Poppins", Font.BOLD, 24));
            title.setForeground(s.fg());
            title.setBorder(new EmptyBorder(20, 25, 10, 25));
            add(title, BorderLayout.NORTH);

            // ----- Centro -----
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(new EmptyBorder(10, 25, 10, 25));
            content.setBackground(s.bg());

            // Modo de color
            JLabel themeLbl = new JLabel("Modo de color:");
            themeLbl.setFont(s.scaled("Poppins", Font.BOLD, 15));
            themeLbl.setForeground(s.fg());
            themeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(themeLbl);
            content.add(Box.createVerticalStrut(8));

            JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
            themeRow.setOpaque(false);
            themeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            ButtonGroup themeGroup = new ButtonGroup();
            JRadioButton rbLight = new JRadioButton("Claro");
            JRadioButton rbDark  = new JRadioButton("Oscuro");
            for (JRadioButton rb : new JRadioButton[] { rbLight, rbDark }) {
                rb.setFont(s.scaled("Poppins", Font.PLAIN, 14));
                rb.setOpaque(false);
                rb.setForeground(s.fg());
                rb.setFocusPainted(false);
                themeGroup.add(rb);
                themeRow.add(rb);
            }
            if (s.getTheme() == Theme.DARK) rbDark.setSelected(true);
            else                            rbLight.setSelected(true);
            content.add(themeRow);
            content.add(Box.createVerticalStrut(22));

            // Tamano de fuente
            JLabel fontLbl = new JLabel("Tamano de la fuente:");
            fontLbl.setFont(s.scaled("Poppins", Font.BOLD, 15));
            fontLbl.setForeground(s.fg());
            fontLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(fontLbl);
            content.add(Box.createVerticalStrut(8));

            JComboBox<FontSize> cbSize = new JComboBox<>(FontSize.values());
            cbSize.setSelectedItem(s.getFontSize());
            cbSize.setFont(s.scaled("Poppins", Font.PLAIN, 14));
            cbSize.setMaximumSize(new Dimension(280, 34));
            cbSize.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(cbSize);
            content.add(Box.createVerticalStrut(20));

            // Nota
            JLabel note = new JLabel(
                "<html><i>Los cambios se aplicaran al presionar <b>Guardar</b> y "
                + "se recordaran la proxima vez que abras la aplicacion.</i></html>");
            note.setFont(s.scaled("Poppins", Font.PLAIN, 12));
            note.setForeground(s.muted());
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(note);

            add(content, BorderLayout.CENTER);

            // ----- Botones -----
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
            buttons.setBackground(s.panel());

            JButton btnCancel = botonPlano("Cancelar", s.panel(), s.fg());
            JButton btnSave   = botonPlano("Guardar",  s.accent(), s.onAccent());
            btnCancel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(s.border(), 1),
                    new EmptyBorder(10, 22, 10, 22)));
            btnSave.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(s.border(), 1),
                    new EmptyBorder(10, 22, 10, 22)));

            buttons.add(btnCancel);
            buttons.add(btnSave);
            add(buttons, BorderLayout.SOUTH);

            btnCancel.addActionListener(e -> dispose());
            btnSave.addActionListener(e -> {
                Theme nuevoTema = rbDark.isSelected() ? Theme.DARK : Theme.LIGHT;
                FontSize nuevoTam = (FontSize) cbSize.getSelectedItem();
                AppSettings.get().setTheme(nuevoTema);
                if (nuevoTam != null) AppSettings.get().setFontSize(nuevoTam);
                dispose();
            });

            // ESC cierra el dialogo
            getRootPane().registerKeyboardAction(
                    e -> dispose(),
                    KeyStroke.getKeyStroke("ESCAPE"),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        private static JButton botonPlano(String texto, Color bg, Color fg) {
            final AppSettings s = AppSettings.get();
            JButton b = new JButton(texto);
            b.setFont(s.scaled("Poppins", Font.BOLD, 14));
            b.setBackground(bg);
            b.setForeground(fg);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final Color original = bg;
            final Color hover = s.accentHover();
            b.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { b.setBackground(hover); }
                @Override public void mouseExited (MouseEvent e) { b.setBackground(original); }
            });
            return b;
        }
    }

    // ==================== BOTON GEAR REUTILIZABLE ====================

    /**
     * Crea un boton de engrane ya estilizado con el tema actual, que al pulsarse
     * abre el dialogo de configuracion. El llamador es responsable de agregarlo
     * al layout.
     */
    public static JButton createSettingsButton(final Window owner) {
        final AppSettings s = AppSettings.get();
        final JButton btn = new JButton("\u2699");
        btn.setToolTipText("Configuracion (tema y tamano de fuente)");
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(4, 10, 4, 10));
        aplicarEstiloGear(btn);
        btn.addActionListener(e -> showDialog(owner));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(s.accentHover()); }
            @Override public void mouseExited (MouseEvent e) { aplicarEstiloGear(btn); }
        });
        return btn;
    }

    /** Re-aplica el estilo del gear al cambiar el tema. */
    public static void aplicarEstiloGear(JButton btn) {
        final AppSettings s = AppSettings.get();
        btn.setFont(s.scaled("Dialog", Font.BOLD, 26));
        btn.setBackground(s.panel());
        btn.setForeground(s.fg());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(s.border(), 2, true),
                new EmptyBorder(4, 10, 4, 10)));
    }
}
