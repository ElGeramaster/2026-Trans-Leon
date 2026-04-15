package GestionSoftware;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class Menu extends JFrame {
    private JButton BConsOrigen, BConsIdentidad;
    private JButton btnSettings;
    private JLabel T1;
    private final Runnable themeListener = this::aplicarTema;

    public Menu() {
        setTitle("GENERADOR DE CONSTANCIAS WORD - GerardoA.inc");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700); // Tamano personalizado
        setLocationRelativeTo(null); // Centrar en pantalla
        setResizable(true);
        setLayout(new BorderLayout(10, 10));

        // Panel superior
        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.setOpaque(false);

        // ---- Boton de configuracion (parte izquierda superior) ----
        btnSettings = AppSettings.createSettingsButton(this);
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 0));
        leftPanel.add(btnSettings);
        panelNorte.add(leftPanel, BorderLayout.WEST);

        // ---- Titulo central ----
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);
        T1 = new JLabel("MENU DE CONSTANCIAS");
        T1.setBorder(BorderFactory.createEmptyBorder(50, 10, 0, 0));
        titlePanel.add(T1);
        panelNorte.add(titlePanel, BorderLayout.CENTER);
        add(panelNorte, BorderLayout.NORTH);

        // Panel central para botones
        JPanel panelCentro = new JPanel();
        panelCentro.setOpaque(false);
        iniciarComponentes(panelCentro);
        add(panelCentro, BorderLayout.CENTER);

        // Registrar listener de tema y aplicar estado actual
        AppSettings.get().addChangeListener(themeListener);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                AppSettings.get().removeChangeListener(themeListener);
            }
        });
        aplicarTema();
    }

    @SuppressWarnings("empty-statement")
        private void iniciarComponentes(JPanel contenedorBotones) {
        JPanel panelGrid = new JPanel(new GridLayout(2, 2, 30, 15));
        panelGrid.setOpaque(false);

        // Crear botones
        BConsIdentidad = crearBoton("resources/Usuario.png", "Ingresar Datos");
        BConsOrigen = crearBoton("resources/Usuario.png", "Registros y Consultas");


        // Agregar al panel
        panelGrid.add(BConsIdentidad);
        panelGrid.add(BConsOrigen);


        contenedorBotones.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        contenedorBotones.add(panelGrid, gbc);

        // Listeners
        BConsIdentidad.addActionListener((ActionEvent e) -> {
            Ingresar inicio = new Ingresar();
            inicio.setVisible(true);
            Menu.this.dispose();
        });;
        BConsOrigen.addActionListener(e -> {
            Registros inicio = new Registros();
            inicio.setVisible(true);
            Menu.this.dispose();
        });;



        agregarEfectoBotones();
    }

    private JButton crearBoton(String imagePath, String etiqueta) {
        ImageIcon icon = loadImage(imagePath);
        JButton button = new JButton();

        if (icon != null) {
            Image image = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(image));
        }

        button.setText("<html><center>" + etiqueta + "</center></html>");
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setToolTipText(etiqueta);

        button.setPreferredSize(new Dimension(300, 300));
        button.setMinimumSize(new Dimension(200, 200));

        button.setFocusPainted(false);
        button.setBorderPainted(true);

        return button;
    }

    private void agregarEfectoBotones() {
        JButton[] buttons = {BConsOrigen, BConsIdentidad};
        for (JButton button : buttons) {
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent evt) {
                    button.setBackground(AppSettings.get().accentHover());
                }

                @Override
                public void mouseExited(MouseEvent evt) {
                    button.setBackground(AppSettings.get().accent());
                }
            });
        }
    }

    /** Aplica la paleta y el tamano de fuente actuales a todos los componentes. */
    private void aplicarTema() {
        AppSettings s = AppSettings.get();

        getContentPane().setBackground(s.bg());

        if (T1 != null) {
            T1.setFont(s.scaled("Poppins", Font.BOLD, 40));
            T1.setForeground(s.fg());
        }

        if (btnSettings != null) {
            AppSettings.aplicarEstiloGear(btnSettings);
        }

        JButton[] buttons = {BConsOrigen, BConsIdentidad};
        for (JButton b : buttons) {
            if (b == null) continue;
            b.setFont(s.scaled("Poppins", Font.BOLD, 16));
            b.setForeground(s.fg());
            b.setBackground(s.accent());
            b.setBorder(BorderFactory.createLineBorder(s.border(), 4));
        }

        revalidate();
        repaint();
    }

    private ImageIcon loadImage(String path) {
        try {
            return new ImageIcon(getClass().getResource(path));
        } catch (Exception e) {
            System.err.println("No se pudo cargar la imagen: " + path);
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Menu menu = new Menu();
            menu.setVisible(true);
        });
    }
}
