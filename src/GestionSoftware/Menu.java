package GestionSoftware;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class Menu extends JFrame {
    private JButton BConsOrigen, BConsIdentidad;

    public Menu() {
        setTitle("GENERADOR DE CONSTANCIAS WORD - GerardoA.inc");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700); // Tamaño personalizado
        setLocationRelativeTo(null); // Centrar en pantalla
        setResizable(true); // Evita que el usuario redimensione
        getContentPane().setBackground(new Color(243, 183, 182));
        setLayout(new BorderLayout(10, 10));

        // Panel superior
        JPanel panelNorte = new JPanel(new BorderLayout());
        panelNorte.setOpaque(false);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setOpaque(false);
        JLabel T1 = new JLabel("MENU DE CONSTANCIAS");
        T1.setFont(new Font("Poppins", Font.BOLD, 40));
        T1.setForeground(Color.BLACK);
        T1.setBorder(BorderFactory.createEmptyBorder(50, 10, 0, 00));
        titlePanel.add(T1);
        panelNorte.add(titlePanel, BorderLayout.CENTER);
        add(panelNorte, BorderLayout.NORTH);
        
        // Panel central para botones
        JPanel panelCentro = new JPanel();
        panelCentro.setOpaque(false);
        iniciarComponentes(panelCentro);
        add(panelCentro, BorderLayout.CENTER);
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
        button.setFont(new Font("Poppins", Font.BOLD, 16));
        button.setForeground(Color.BLACK);

        button.setPreferredSize(new Dimension(300, 300));
        button.setMinimumSize(new Dimension(200, 200));

        button.setBorder(BorderFactory.createLineBorder(Color.BLACK, 4));
        button.setBackground(new Color(229, 91, 114));
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
                    button.setBackground(new Color(249, 197, 210));
                }

                @Override
                public void mouseExited(MouseEvent evt) {
                    button.setBackground(new Color(229, 91, 114));
                }
            });
        }
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

// MI PRIMER COMMIT COÑO 
//AJJAAJ
