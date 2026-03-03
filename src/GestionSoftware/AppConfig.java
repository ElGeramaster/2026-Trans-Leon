package GestionSoftware;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuración global de apariencia: tipografía y modo oscuro.
 * Persiste las preferencias en ~/.trans-leon.properties.
 */
public class AppConfig {

    // ── Valores actuales ──────────────────────────────────────────────────
    private static String  fontName = "Arial";
    private static boolean darkMode = false;

    // ── Listeners (notificados cuando cambia el tema) ─────────────────────
    private static final List<Runnable> listeners = new ArrayList<>();

    // ── Archivo de configuración ──────────────────────────────────────────
    private static final File CONFIG_FILE =
            new File(System.getProperty("user.home"), ".trans-leon.properties");

    static { load(); }

    // ══════════════════════════════════════════════════════════════════════
    //  Fuente
    // ══════════════════════════════════════════════════════════════════════

    /** Devuelve una fuente con el nombre seleccionado por el usuario. */
    public static Font font(int style, int size) {
        return new Font(fontName, style, size);
    }

    public static String getFontName() { return fontName; }

    public static void setFontName(String name) {
        fontName = name;
        save();
        fireListeners();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Modo oscuro
    // ══════════════════════════════════════════════════════════════════════

    public static boolean isDarkMode() { return darkMode; }

    public static void setDarkMode(boolean dark) {
        darkMode = dark;
        save();
        fireListeners();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Colores del tema
    // ══════════════════════════════════════════════════════════════════════

    /** Fondo principal de la ventana Registros */
    public static Color bgMain() {
        return darkMode ? new Color(43, 45, 48) : new Color(211, 211, 211);
    }

    /** Fondo de paneles / formularios */
    public static Color bgPanel() {
        return darkMode ? new Color(55, 57, 60) : Color.WHITE;
    }

    /** Fondo secundario suave */
    public static Color bgSecundario() {
        return darkMode ? new Color(48, 50, 54) : new Color(245, 247, 250);
    }

    /** Texto principal */
    public static Color fgText() {
        return darkMode ? new Color(220, 220, 220) : Color.BLACK;
    }

    /** Texto secundario */
    public static Color fgSubtext() {
        return darkMode ? new Color(160, 170, 180) : new Color(80, 80, 80);
    }

    /** Cabecera de tabla */
    public static Color bgTableHeader() {
        return darkMode ? new Color(55, 80, 115) : new Color(135, 206, 235);
    }

    /** Fondo de filas de tabla */
    public static Color bgTableRow() {
        return darkMode ? new Color(55, 57, 60) : Color.WHITE;
    }

    /** Barra superior (Datos.java) */
    public static Color bgTopBar() {
        return darkMode ? new Color(35, 37, 40) : new Color(41, 128, 185);
    }

    /** Fondo del Menú principal */
    public static Color bgMenu() {
        return darkMode ? new Color(43, 45, 48) : new Color(243, 183, 182);
    }

    /** Botones del Menú principal */
    public static Color bgMenuButton() {
        return darkMode ? new Color(65, 75, 95) : new Color(229, 91, 114);
    }

    public static Color bgMenuButtonHover() {
        return darkMode ? new Color(85, 100, 125) : new Color(249, 197, 210);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Listeners
    // ══════════════════════════════════════════════════════════════════════

    public static void addListener(Runnable r)    { listeners.add(r); }
    public static void removeListener(Runnable r) { listeners.remove(r); }

    private static void fireListeners() {
        new ArrayList<>(listeners).forEach(Runnable::run);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Persistencia
    // ══════════════════════════════════════════════════════════════════════

    public static void save() {
        Properties p = new Properties();
        p.setProperty("fontName", fontName);
        p.setProperty("darkMode", String.valueOf(darkMode));
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            p.store(out, "Trans-Leon Config");
        } catch (IOException ignored) {}
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) return;
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            p.load(in);
            fontName = p.getProperty("fontName", "Arial");
            darkMode = Boolean.parseBoolean(p.getProperty("darkMode", "false"));
        } catch (IOException ignored) {}
    }
}
