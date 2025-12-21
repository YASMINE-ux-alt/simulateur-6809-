package MAIN;


import javax.swing.SwingUtilities;
import gui.GUI;

public class Main {

    public static void main(String[] args) {

        // Lancer l’interface graphique 
        SwingUtilities.invokeLater(() -> {
            GUI gui = new GUI();
            gui.setVisible(true);
        });
    }
}
