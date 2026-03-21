package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GUI extends JFrame {
    private final Simulator simulator;
    
    //Register displays
    private JTextField[] rDisplays = new JTextField[4];
    private JTextField[] ixDisplays = new JTextField[4];
    private JTextField pcDisplay, marDisplay, mbrDisplay, irDisplay, memAtMARDisplay;
    private JTextField ccDisplay;
    
    //Console I/O
    private JTextArea consoleOutput;
    private JTextField consoleInput;
    private JButton sendInputButton;
    
    //Cache display
    private JTextArea cacheDisplay;
    
    //Buttons
    private JButton iplButton, stepButton, runButton;

    public GUI() {
        simulator = new Simulator(2048, 16);
        
        setTitle("C6461 Simulator - Part 2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        
        //Top: Registers
        add(createDisplayPanel(), BorderLayout.NORTH);
        
        //Center: Console I/O and Cache side by side
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        centerPanel.add(createConsolePanel());
        centerPanel.add(createCachePanel());
        add(centerPanel, BorderLayout.CENTER);
        
        //Bottom: Buttons
        add(createButtonPanel(), BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        updateDisplays();
    }

    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 5, 5));
        
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
        
        //Memory at MAR + CC
        JPanel memCC = new JPanel(new FlowLayout(FlowLayout.LEFT));
        memCC.add(new JLabel("MEM[MAR]:"));
        memAtMARDisplay = new JTextField(6);
        memAtMARDisplay.setEditable(false);
        memCC.add(memAtMARDisplay);
        
        memCC.add(new JLabel("  CC:"));
        ccDisplay = new JTextField(10);
        ccDisplay.setEditable(false);
        memCC.add(ccDisplay);
        
        panel.add(memCC);
        
        return panel;
    }

    //Console I/O Panel
    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(3, 3));
        panel.setBorder(BorderFactory.createTitledBorder("Console I/O"));
        
        //Output area
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Console Printer (Output)"));
        consoleOutput = new JTextArea(5, 30);
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane outputScroll = new JScrollPane(consoleOutput);
        outputPanel.add(outputScroll);
        panel.add(outputPanel, BorderLayout.CENTER);
        
        //Input area
        JPanel inputPanel = new JPanel(new BorderLayout(3, 3));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Console Keyboard (Input)"));
        consoleInput = new JTextField(25);
        consoleInput.setFont(new Font("Monospaced", Font.PLAIN, 11));
        sendInputButton = new JButton("Send");
        sendInputButton.addActionListener(e -> sendInput());
        
        JPanel inputControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputControls.add(new JLabel("Input:"));
        inputControls.add(consoleInput);
        inputControls.add(sendInputButton);
        inputPanel.add(inputControls);
        
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    //Cache Panel
    private JPanel createCachePanel() {
        JPanel panel = new JPanel(new BorderLayout(3, 3));
        panel.setBorder(BorderFactory.createTitledBorder("Cache Content"));
        
        cacheDisplay = new JTextArea(8, 40);
        cacheDisplay.setEditable(false);
        cacheDisplay.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane cacheScroll = new JScrollPane(cacheDisplay);
        panel.add(cacheScroll);
        
        cacheDisplay.setText("");
        
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
        
        JButton clearConsoleButton = new JButton("Clear Console");
        clearConsoleButton.addActionListener(e -> clearConsole());
        panel.add(clearConsoleButton);
        
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
            updateDisplays();
        }
        JOptionPane.showMessageDialog(this, "Halted");
    }

    private void reset() {
        simulator.reset();
        updateDisplays();
        clearConsole();
    }
    
    private void sendInput() {
        String input = consoleInput.getText();
        simulator.getCPU().setConsoleInput(input);
        consoleInput.setText("");
        updateDisplays();
    }
    
    private void clearConsole() {
        consoleOutput.setText("");
        simulator.getCPU().clearConsoleOutput();
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
        
        // Update CC display
        int cc = cpu.getCC();
        String ccStr = Integer.toBinaryString(0x10 | cc).substring(1) + " (";
        if (cpu.getOverflow()) ccStr += "O";
        if (cpu.getUnderflow()) ccStr += "U";
        if (cpu.getDivZero()) ccStr += "D";
        if (cpu.getEqualOrNot()) ccStr += "E";
        if (cc == 0) ccStr += "-";
        ccStr += ")";
        ccDisplay.setText(ccStr);
        
        // Update console output
        consoleOutput.setText(cpu.getConsoleOutput());
        
        // Cache display stays empty for now
        
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