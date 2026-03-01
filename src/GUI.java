package src;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class GUI extends JFrame {
    private final Simulator simulator;
    
    //reg displays
    private JTextField[] rDisplays = new JTextField[4];
    private JTextField[] ixDisplays = new JTextField[4];
    private JTextField pcDisplay, marDisplay, mbrDisplay, irDisplay, memAtMARDisplay;
    
    //buttons
    private JButton iplButton, stepButton, runButton;

    public GUI() {
        simulator = new Simulator(2048, 16);
        
        setTitle("C6461 Simulator - Part 1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        add(createDisplayPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        updateDisplays();
    }

    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
        
        //R0-R3
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 0; i < 4; i++) {
            r.add(new JLabel("R" + i + ":"));
            rDisplays[i] = new JTextField(6);
            rDisplays[i].setEditable(false);
            r.add(rDisplays[i]);
        }
        panel.add(r);
        
        //IX1-IX3
        JPanel ix = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 1; i < 4; i++) {
            ix.add(new JLabel("IX" + i + ":"));
            ixDisplays[i] = new JTextField(6);
            ixDisplays[i].setEditable(false);
            ix.add(ixDisplays[i]);
        }
        panel.add(ix);
        
        //PC, MAR, MBR, IR
        JPanel special = new JPanel(new FlowLayout(FlowLayout.LEFT));
        special.add(new JLabel("PC:"));
        pcDisplay = new JTextField(6);
        pcDisplay.setEditable(false);
        special.add(pcDisplay);
        
        special.add(new JLabel("MAR:"));
        marDisplay = new JTextField(6);
        marDisplay.setEditable(false);
        special.add(marDisplay);
        
        special.add(new JLabel("MBR:"));
        mbrDisplay = new JTextField(6);
        mbrDisplay.setEditable(false);
        special.add(mbrDisplay);
        
        special.add(new JLabel("IR:"));
        irDisplay = new JTextField(6);
        irDisplay.setEditable(false);
        special.add(irDisplay);
        
        panel.add(special);
        
        //mmory at MAR display
        JPanel memPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        memPanel.add(new JLabel("MEM[MAR]:"));
        memAtMARDisplay = new JTextField(6);
        memAtMARDisplay.setEditable(false);
        memPanel.add(memAtMARDisplay);
        panel.add(memPanel);
        
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        
        iplButton = new JButton("IPL (Load Program)");
        iplButton.addActionListener(e -> loadProgram());
        panel.add(iplButton);
        
        stepButton = new JButton("Single Step");
        stepButton.addActionListener(e -> singleStep());
        panel.add(stepButton);
        
        runButton = new JButton("Run");
        runButton.addActionListener(e -> run());
        panel.add(runButton);
        
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> reset());
        panel.add(resetButton);
        
        return panel;
    }

    private void loadProgram() {
        JFileChooser chooser = new JFileChooser(".");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                simulator.reset();
                simulator.loadProgramFromFile(chooser.getSelectedFile().getAbsolutePath());
                simulator.setPC(20); 
                JOptionPane.showMessageDialog(this, "Program loaded");
                updateDisplays();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void singleStep() {
        simulator.singleStep();
        updateDisplays();
        if (simulator.getCPU().isHalted()) {
            JOptionPane.showMessageDialog(this, "Halted");
        }
    }

    private void run() {
        while (!simulator.getCPU().isHalted()) {
            simulator.singleStep();
        }
        updateDisplays();
        JOptionPane.showMessageDialog(this, "Halted");
    }

    private void reset() {
        simulator.reset();
        updateDisplays();
    }

    private void updateDisplays() {
        CPU cpu = simulator.getCPU();
        
        for (int i = 0; i < 4; i++) {
            rDisplays[i].setText(String.format("%04X", cpu.getR(i)));
        }
        
        for (int i = 1; i < 4; i++) {
            ixDisplays[i].setText(String.format("%04X", cpu.getIX(i)));
        }
        
        pcDisplay.setText(String.format("%04X", cpu.getPC()));
        marDisplay.setText(String.format("%04X", cpu.getMAR()));
        mbrDisplay.setText(String.format("%04X", cpu.getMBR()));
        irDisplay.setText(String.format("%04X", cpu.getIR()));
        
        try {
            memAtMARDisplay.setText(String.format("%04X", simulator.getMemoryAtMAR()));
        } catch (Exception e) {
            memAtMARDisplay.setText("----");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new GUI().setVisible(true);
        });
    }
}