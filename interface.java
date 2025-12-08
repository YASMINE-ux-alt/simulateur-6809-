package gui;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.EventQueue;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Color;
import javax.swing.JLabel;
import java.awt.Font;

import javax.swing.table.DefaultTableModel;

public class GUI extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table;
	private JTable table_1;
	private JTable table_2;
	//declaration RAM et ROM
	private JScrollPane scrollPane_1; // AJOUTÉ pour le panneau ROM
	private JScrollPane scrollPane_2; // AJOUTÉ pour le panneau RAM

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI frame = new GUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public GUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1308, 882);
		contentPane = new JPanel();
		contentPane.setForeground(new Color(0, 0, 0));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		//panel+textarea
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 23, 216, 529);
		contentPane.add(scrollPane);
		
		JTextArea textArea = new JTextArea();
		scrollPane.setViewportView(textArea);
		
		JButton BtnNew = new JButton("New");
		BtnNew.setBounds(0, 0, 64, 20);
		contentPane.add(BtnNew);
		BtnNew.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        textArea.setText("");
		    }
		});
		JButton btnNewButton_1 = new JButton("Save");
		btnNewButton_1.setBounds(59, 0, 64, 20);
		contentPane.add(btnNewButton_1);
		
		JButton btnRam = new JButton("RAM");
		btnRam.setBounds(121, 0, 64, 20);
		contentPane.add(btnRam);
		//action performed 
		btnRam.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        
		        scrollPane_2.setVisible(true);  // Afficher la RAM
		        contentPane.repaint();          // Rafraîchir
		    }
		});
		
		JButton btnRom = new JButton("ROM");
		btnRom.setBounds(183, 0, 64, 20);
		contentPane.add(btnRom);
		//actionperformed 
		btnRom.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        scrollPane_1.setVisible(true);  // Afficher la ROM

		        contentPane.repaint();          // Rafraîchir
		    }
		});
		
		JButton btnRun = new JButton("Run");
		btnRun.setBounds(244, 0, 64, 20);
		contentPane.add(btnRun);
		
		JButton btnPasPas = new JButton("Pas à pas");
		btnPasPas.setBounds(307, 0, 93, 20);
		contentPane.add(btnPasPas);
		
		JButton btnReset = new JButton("Reset");
		btnReset.setBounds(398, 0, 80, 20);
		contentPane.add(btnReset);
		
		
		
		JPanel panel = new JPanel();
		panel.setBounds(606, 0, 628, 676);
		panel.setBackground(new Color(200, 200, 200));
		contentPane.add(panel);
		panel.setLayout(null);
		
		JLabel Labelinstruction = new JLabel("");
		Labelinstruction.setBounds(165, 10, 243, 33);
		Labelinstruction.setOpaque(true);
		Labelinstruction.setBackground(new Color(255, 255, 255));
		panel.add(Labelinstruction);
		
		JLabel LabelPC = new JLabel("");
		LabelPC.setBounds(109, 74, 205, 39);
		LabelPC.setBackground(new Color(255, 255, 255));
		LabelPC.setOpaque(true);
		panel.add(LabelPC);
		
		JLabel LabelA = new JLabel("");
		LabelA.setBounds(81, 160, 189, 33);
		LabelA.setOpaque(true);
		LabelA.setBackground(Color.WHITE);
		panel.add(LabelA);
		
		JLabel LabelB = new JLabel("");
		LabelB.setBounds(81, 228, 189, 33);
		LabelB.setOpaque(true);
		LabelB.setBackground(Color.WHITE);
		panel.add(LabelB);
		
		JLabel LabelD = new JLabel("");
		LabelD.setBounds(81, 295, 189, 33);
		LabelD.setOpaque(true);
		LabelD.setBackground(Color.WHITE);
		panel.add(LabelD);
		
		JLabel LabelS = new JLabel("");
		LabelS.setBounds(81, 362, 189, 33);
		LabelS.setOpaque(true);
		LabelS.setBackground(Color.WHITE);
		panel.add(LabelS);
		
		JLabel LabelU = new JLabel("");
		LabelU.setBounds(367, 362, 189, 33);
		LabelU.setOpaque(true);
		LabelU.setBackground(Color.WHITE);
		panel.add(LabelU);
		
		JLabel LabelX = new JLabel("");
		LabelX.setBounds(81, 453, 189, 33);
		LabelX.setOpaque(true);
		LabelX.setBackground(Color.WHITE);
		panel.add(LabelX);
		
		JLabel LabelY = new JLabel("");
		LabelY.setBounds(367, 453, 189, 33);
		LabelY.setOpaque(true);
		LabelY.setBackground(Color.WHITE);
		panel.add(LabelY);
		
		JLabel LabelDP = new JLabel("");
		LabelDP.setBounds(81, 515, 189, 33);
		LabelDP.setOpaque(true);
		LabelDP.setBackground(Color.WHITE);
		panel.add(LabelDP);
		
		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setBounds(252, 139, 230, 142);
		panel.add(lblNewLabel);
		//IMAGE
		// Chemin d'accès relatif à votre projet ou chemin absolu
		ImageIcon icon = new ImageIcon("C:\\Users\\salma\\Downloads\\DIFTAR.jpg");
		lblNewLabel.setIcon(icon);
		
		JLabel lblNewLabel_1 = new JLabel("PC");
		lblNewLabel_1.setBounds(65, 61, 44, 64);
		lblNewLabel_1.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		panel.add(lblNewLabel_1);
		
		JLabel lblNewLabel_1_1 = new JLabel("A");
		lblNewLabel_1_1.setBounds(51, 145, 44, 64);
		lblNewLabel_1_1.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		panel.add(lblNewLabel_1_1);
		
		JLabel lblNewLabel_1_2 = new JLabel("B");
		lblNewLabel_1_2.setBounds(51, 217, 44, 64);
		lblNewLabel_1_2.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		panel.add(lblNewLabel_1_2);
		
		JLabel lblNewLabel_1_3 = new JLabel("D");
		lblNewLabel_1_3.setBounds(51, 284, 44, 61);
		lblNewLabel_1_3.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		panel.add(lblNewLabel_1_3);
		
		JLabel lblNewLabel_1_3_1 = new JLabel("S");
		lblNewLabel_1_3_1.setBounds(51, 342, 44, 64);
		lblNewLabel_1_3_1.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		panel.add(lblNewLabel_1_3_1);
		
		JLabel lblNewLabel_1_3_2 = new JLabel("U");
		lblNewLabel_1_3_2.setBounds(338, 342, 44, 64);
		lblNewLabel_1_3_2.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		panel.add(lblNewLabel_1_3_2);
		
		JLabel lblNewLabel_1_3_3 = new JLabel("X");
		lblNewLabel_1_3_3.setBounds(52, 441, 44, 64);
		lblNewLabel_1_3_3.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		panel.add(lblNewLabel_1_3_3);
		
		JLabel lblNewLabel_1_3_4 = new JLabel("Y");
		lblNewLabel_1_3_4.setBounds(338, 441, 44, 64);
		lblNewLabel_1_3_4.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		panel.add(lblNewLabel_1_3_4);
		
		JLabel lblNewLabel_1_3_5 = new JLabel("DP");
		lblNewLabel_1_3_5.setBounds(41, 502, 44, 64);
		lblNewLabel_1_3_5.setFont(new Font("Trebuchet MS", Font.BOLD, 25));
		panel.add(lblNewLabel_1_3_5);
		
		JButton btnE = new JButton("E");
		btnE.setFont(new Font("Tahoma", Font.BOLD, 20));
		btnE.setBounds(65, 597, 47, 39);
		panel.add(btnE);
		
		JButton btnF = new JButton("F");
		btnF.setFont(new Font("Tahoma", Font.BOLD, 20));
		btnF.setBounds(112, 597, 48, 39);
		panel.add(btnF);
		
		JButton btnH = new JButton("H");
		btnH.setFont(new Font("Tahoma", Font.BOLD, 20));
		btnH.setBounds(154, 597, 52, 39);
		panel.add(btnH);
		
		JButton btnI = new JButton("I");
		btnI.setFont(new Font("Tahoma", Font.BOLD, 20));
		btnI.setBounds(207, 597, 44, 39);
		panel.add(btnI);
		
		JButton btnN = new JButton("N");
		btnN.setFont(new Font("Tahoma", Font.BOLD, 20));
		btnN.setBounds(252, 597, 53, 39);
		panel.add(btnN);
		
		JButton btnZ = new JButton("Z");
		btnZ.setFont(new Font("Tahoma", Font.BOLD, 20));
		btnZ.setBounds(304, 597, 47, 39);
		panel.add(btnZ);
		
		JButton btnV = new JButton("V");
		btnV.setFont(new Font("Tahoma", Font.BOLD, 20));
		btnV.setBounds(350, 597, 52, 39);
		panel.add(btnV);
		
		JButton btnC = new JButton("C");
		btnC.setFont(new Font("Tahoma", Font.BOLD, 20));
		btnC.setBounds(399, 597, 53, 39);
		panel.add(btnC);
		//ROM
		scrollPane_1 = new JScrollPane(); // MODIFIÉ : utilise le champ de classe
		scrollPane_1.setBounds(235, 28, 115, 174);
		contentPane.add(scrollPane_1);
		//
		/*JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(235, 28, 115, 174);
		contentPane.add(scrollPane_1); */
		
		table = new JTable();
		scrollPane_1.setViewportView(table);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setBackground(new Color(120, 128, 128));
		table.setForeground(new Color(0, 0, 0));
		//
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("Adresse");
		model.addColumn("Valeur");

		
         //AFFECTATION DES 00 AUX ADRESSES 
		int row = 0;
		for (int i = 0xFC00; i <= 0xFFFF; i++) {
		    model.insertRow(row, new Object[]{String.format("%04X", i), "00"});
		    row++;
		}
		table.setModel(model);
		scrollPane_1.setVisible(false) ;
		//RAM table
		
		// Déclaration et configuration du JScrollPane
		scrollPane_2 = new JScrollPane(); // MODIFIÉ : utilise le champ de classe
		scrollPane_2.setBounds(360, 30, 115, 172);
		contentPane.add(scrollPane_2);
		/*JScrollPane scrollPane_2 = new JScrollPane();
		scrollPane_2.setBounds(360, 30, 115, 172);
		contentPane.add(scrollPane_2); // <-- On ajoute le JScrollPane au ContentPane */

		// Déclaration et configuration du JTable (doit être un champ de classe pour y accéder)
		// Si 'table_2' est un champ de classe, utilisez juste 'table_2 = new JTable();'
		JTable table_2 = new JTable(); // Supprimez le 'JTable' si déjà déclaré en champ
		scrollPane_2.setViewportView(table_2); // <-- On met le JTable DANS le JScrollPane

		table_2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table_2.setBackground(new Color(120, 128, 128));
		table_2.setForeground(new Color(0, 0, 0));

		// --- Modèle de données et initialisation ---

		DefaultTableModel model_2 = new DefaultTableModel();
		model_2.addColumn("Adresse");
		model_2.addColumn("Valeur");

		// AFFECTATION DES '00' AUX ADRESSES (Exemple: de 0xFC00 à 0xFFFF)
		// Vous pouvez définir la plage d'adresses que vous souhaitez afficher ici
		int row_2 = 0;
		for (int i = 0x0000; i <= 0x03FF; i++) {
		    // Utiliser String.format("%04X", i) pour l'adresse sur 4 chiffres Hexa
		    model_2.insertRow(row_2, new Object[]{String.format("%04X", i), "00"});
		    row_2++;
		}

		// Appliquer le modèle de données à la JTable
		table_2.setModel(model_2);
		scrollPane_2.setVisible(false);
		
		
        
		
		
		
	}
}
 
