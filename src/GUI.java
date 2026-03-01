package src;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Locale;
import java.util.List;

/**
 * GUI.java
 * 
 * Minimal front panel
 */
public class GUI extends JFrame {
    private static final int INSTALLED_MEMORY_WORDS = 2048;

    private static final int DEFAULT_ENTRY_PC = 020;

    private static final int RUN_MAX_STEPS = 20000;

    private final Simulator simulator;
    
    // Register displays
    private JTextField[] rDisplays = new JTextField[4];
    private JTextField[] ixDisplays = new JTextField[4];
    private JTextField pcDisplay, marDisplay, mbrDisplay, irDisplay, memAtMARDisplay;
    
    // Buttons
    private JButton iplButton, stepButton, runButton;

    public GUI() {
        simulator = new Simulator(INSTALLED_MEMORY_WORDS);
        
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
        
        // R0-R3
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 0; i < 4; i++) {
            r.add(new JLabel("R" + i + ":"));
            rDisplays[i] = new JTextField(6);
            rDisplays[i].setEditable(false);
            r.add(rDisplays[i]);
        }
        panel.add(r);
        
        // IX1-IX3
        JPanel ix = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 1; i < 4; i++) {
            ix.add(new JLabel("IX" + i + ":"));
            ixDisplays[i] = new JTextField(6);
            ixDisplays[i].setEditable(false);
            ix.add(ixDisplays[i]);
        }
        panel.add(ix);
        
        // PC, MAR, MBR, IR
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
        
        // Memory at MAR display
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
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();
        String name = selected.getName().toLowerCase(Locale.ROOT);

        try {
            simulator.reset();

            if (name.endsWith(".asm")) {
                // 1) Assemble source.asm -> Listing.txt + Load.txt
                Assembler asm = new Assembler();

                File listing = new File(selected.getParentFile(), "Listing.txt");
                File load = new File(selected.getParentFile(), "Load.txt");

                Assembler.AssemblerResult res = asm.assemble(selected, listing, load);
                if (!res.success) {
                    String msg = "Assembly failed:\n" + String.join("\n", res.errors);
                    JOptionPane.showMessageDialog(this, msg);
                    return;
                }

                // 2) Load the generated Load.txt into memory
                simulator.loadProgramFromFile(load.getAbsolutePath());

                // 3) Set PC to the first generated address (better than hardcoding 020)
                Integer entry = firstInstructionAddress(res.lines);
                if (entry != null) simulator.setPC(entry);

                JOptionPane.showMessageDialog(this,
                        "Assembled + loaded.\n" +
                        "Listing: " + listing.getName() + "\n" +
                        "Load: " + load.getName() + "\n" +
                        "PC set to: " + String.format("%04X", simulator.getCPU().getPC()));

            } else {
                // Assume it's already a Load.txt-style file (octal addr/value pairs)
                simulator.loadProgramFromFile(selected.getAbsolutePath());

                // Pick an entry policy; either keep your old default or set to first non-zero location.
                simulator.setPC(020); // octal 20 (decimal 16) — Java octal literal
                JOptionPane.showMessageDialog(this, "Loaded load-file.\nPC set to 0020(octal).");
            }

            updateDisplays();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading program: " + ex.getMessage());
        }
    }

    private static Integer firstInstructionAddress(List<Assembler.AsmLine> lines) {
        // Prefer the first actual instruction (not DATA/LOC).
        // This prevents starting execution on DATA words like at LOC 6.
        for (Assembler.AsmLine al : lines) {
            if (al.address == null || al.op == null) continue;

            String op = al.op.toUpperCase(Locale.ROOT);

            // Skip directives / non-executable pseudo-ops
            if (op.equals("LOC") || op.equals("DATA")) continue;

            // Anything else here is an instruction mnemonic in your assembler.
            return al.address;
        }

        // Fallback: if no instruction found, fall back to first generated word
        for (Assembler.AsmLine al : lines) {
            if (al.address != null) return al.address;
        }

        return null;
    }

    private void singleStep() {
        simulator.singleStep();
        updateDisplays();
        if (simulator.getCPU().isHalted()) {
            JOptionPane.showMessageDialog(this, "Halted");
        }
    }

    private void run() {
        simulator.run(RUN_MAX_STEPS);
        updateDisplays();
        JOptionPane.showMessageDialog(this, simulator.getCPU().isHalted() ? "Halted" : "Stopped (max steps reached)");
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