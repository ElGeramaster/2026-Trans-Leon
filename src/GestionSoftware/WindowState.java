package GestionSoftware;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public final class WindowState {

    private static int lastExtendedState = JFrame.NORMAL;
    private static Rectangle lastBounds = new Rectangle(100, 100, 1400, 800);

    private WindowState() {}

    public static void capture(JFrame frame) {
        if (frame == null) return;

        lastExtendedState = frame.getExtendedState();

        // Solo guardamos tamaño/posición si NO está maximizada
        if ((lastExtendedState & JFrame.MAXIMIZED_BOTH) != JFrame.MAXIMIZED_BOTH) {
            Rectangle b = frame.getBounds();
            if (b != null && b.width > 0 && b.height > 0) lastBounds = b;
        }
    }

    public static void apply(JFrame frame) {
        if (frame == null) return;

        // Si la última fue normal, restauramos bounds antes de abrir
        if ((lastExtendedState & JFrame.MAXIMIZED_BOTH) != JFrame.MAXIMIZED_BOTH) {
            if (lastBounds != null) frame.setBounds(lastBounds);
        }

        // Maximizar (o restaurar) cuando la ventana ya abrió
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> frame.setExtendedState(lastExtendedState));
                frame.removeWindowListener(this);
            }
        });
    }

    // Cambiar de ventana conservando pantalla completa/estado
    public static void switchTo(JFrame current, JFrame next) {
        capture(current);
        apply(next);
        next.setVisible(true);
        if (current != null) current.dispose();
    }
}
