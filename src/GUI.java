package src;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * GUI.java
 *
 * Front panel for the simulator.
 *
 * Key fixes:
 * - Run executes on a background thread so the GUI stays responsive.
 * - User can send search-word input while Program 2 is waiting on IN 0,0.
 * - Input is sent without trimming.
 */
public class GUI extends JFrame {
    private static final int INSTALLED_MEMORY_WORDS = 2048;
    private static final int RUN_MAX_STEPS = 20000;
    private static final int UI_REFRESH_MS = 100;

    private final Simulator simulator;

    // Register displays
    private final JTextField[] rDisplays = new JTextField[4];
    private final JTextField[] ixDisplays = new JTextField[4];
    private JTextField pcDisplay, marDisplay, mbrDisplay, irDisplay, memAtMARDisplay;
    private JTextField ccDisplay, mfrDisplay;

    // Console I/O
    private JTextArea consoleOutput;
    private JTextField consoleInput;
    private JButton sendInputButton;

    // Cache display
    private JTextArea cacheDisplay;

    // Buttons
    private JButton iplButton, stepButton, runButton;

    // Background execution
    private volatile boolean runInProgress = false;
    private SwingWorker<Void, Void> runWorker;
    private Timer refreshTimer;

    public GUI() {
        simulator = new Simulator(INSTALLED_MEMORY_WORDS);

        setTitle("C6461 Simulator - Part 3");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        add(createDisplayPanel(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        centerPanel.add(createConsolePanel());
        centerPanel.add(createCachePanel());
        add(centerPanel, BorderLayout.CENTER);

        add(createButtonPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        refreshTimer = new Timer(UI_REFRESH_MS, e -> updateDisplays());
        updateDisplays();
    }

    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));

        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 0; i < 4; i++) {
            r.add(new JLabel("R" + i + ":"));
            rDisplays[i] = new JTextField(6);
            rDisplays[i].setEditable(false);
            r.add(rDisplays[i]);
        }
        panel.add(r);

        JPanel ix = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 1; i < 4; i++) {
            ix.add(new JLabel("IX" + i + ":"));
            ixDisplays[i] = new JTextField(6);
            ixDisplays[i].setEditable(false);
            ix.add(ixDisplays[i]);
        }
        panel.add(ix);

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

        JPanel memCC = new JPanel(new FlowLayout(FlowLayout.LEFT));
        memCC.add(new JLabel("MEM[MAR]:"));
        memAtMARDisplay = new JTextField(6);
        memAtMARDisplay.setEditable(false);
        memCC.add(memAtMARDisplay);

        memCC.add(new JLabel("  CC:"));
        ccDisplay = new JTextField(10);
        ccDisplay.setEditable(false);
        memCC.add(ccDisplay);

        memCC.add(new JLabel("  MFR:"));
        mfrDisplay = new JTextField(4);
        mfrDisplay.setEditable(false);
        memCC.add(mfrDisplay);

        panel.add(memCC);

        return panel;
    }

    private JPanel createConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(3, 3));
        panel.setBorder(BorderFactory.createTitledBorder("Console I/O"));

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Console Printer (Output)"));
        consoleOutput = new JTextArea(8, 30);
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font("Monospaced", Font.PLAIN, 11));
        outputPanel.add(new JScrollPane(consoleOutput), BorderLayout.CENTER);
        panel.add(outputPanel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(3, 3));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Search Word Input"));
        consoleInput = new JTextField(25);
        consoleInput.setFont(new Font("Monospaced", Font.PLAIN, 11));

        sendInputButton = new JButton("Send Search Word");
        sendInputButton.addActionListener(e -> sendInput());

        JPanel inputControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputControls.add(new JLabel("Input:"));
        inputControls.add(consoleInput);
        inputControls.add(sendInputButton);
        inputPanel.add(inputControls, BorderLayout.CENTER);

        panel.add(inputPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createCachePanel() {
        JPanel panel = new JPanel(new BorderLayout(3, 3));
        panel.setBorder(BorderFactory.createTitledBorder("Cache Content"));

        cacheDisplay = new JTextArea(12, 40);
        cacheDisplay.setEditable(false);
        cacheDisplay.setFont(new Font("Monospaced", Font.PLAIN, 10));
        panel.add(new JScrollPane(cacheDisplay), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        iplButton = new JButton("IPL (Load Program)");
        iplButton.addActionListener(e -> loadProgram());
        panel.add(iplButton);

        JButton loadParagraphButton = new JButton("Load Paragraph File");
        loadParagraphButton.addActionListener(e -> loadParagraphFile());
        panel.add(loadParagraphButton);

        stepButton = new JButton("Single Step");
        stepButton.addActionListener(e -> singleStep());
        panel.add(stepButton);

        runButton = new JButton("Run");
        runButton.addActionListener(e -> runAsync());
        panel.add(runButton);

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> reset());
        panel.add(resetButton);

        JButton clearConsoleButton = new JButton("Clear Console");
        clearConsoleButton.addActionListener(e -> clearConsole());
        panel.add(clearConsoleButton);

        return panel;
    }

    private void setRunControls(boolean running) {
        runInProgress = running;
        iplButton.setEnabled(!running);
        stepButton.setEnabled(!running);
        runButton.setEnabled(!running);
    }

    private void promptForParagraphFile() {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Select paragraph .txt file for card reader input");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();

        try {
            simulator.loadTextFileIntoCardReader(selected.getAbsolutePath());
            JOptionPane.showMessageDialog(
                    this,
                    "Paragraph text file loaded into card reader input:\n" + selected.getName());
            updateDisplays();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error loading paragraph text file: " + ex.getMessage());
        }
    }

    private void loadParagraphFile() {
        JFileChooser chooser = new JFileChooser(".");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();

        try {
            simulator.loadTextFileIntoCardReader(selected.getAbsolutePath());
            JOptionPane.showMessageDialog(
                    this,
                    "Paragraph file loaded into card reader input:\n" + selected.getName());
            updateDisplays();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error loading paragraph file: " + ex.getMessage());
        }
    }

    private void loadProgram() {
        JFileChooser chooser = new JFileChooser(".");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();
        String name = selected.getName().toLowerCase(Locale.ROOT);

        try {
            simulator.reset();

            if (name.endsWith(".asm")) {
                Assembler asm = new Assembler();

                File listing = new File(selected.getParentFile(), "Listing.txt");
                File load = new File(selected.getParentFile(), "Load.txt");

                Assembler.AssemblerResult res = asm.assemble(selected, listing, load);
                if (!res.success) {
                    String msg = "Assembly failed:\n" + String.join("\n", res.errors);
                    JOptionPane.showMessageDialog(this, msg);
                    return;
                }

                simulator.loadProgramFromFile(load.getAbsolutePath());

                Integer entry = firstInstructionAddress(res.lines);
                if (entry != null) {
                    simulator.setPC(entry);
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Assembled + loaded.\n"
                                + "Listing: " + listing.getName() + "\n"
                                + "Load: " + load.getName() + "\n"
                                + "PC set to: " + String.format("%04X", simulator.getCPU().getPC()));
            } else {
                simulator.loadProgramFromFile(selected.getAbsolutePath());

                JOptionPane.showMessageDialog(
                        this,
                        "Loaded load-file.\nPC set to first loaded address: "
                                + String.format("%04X", simulator.getCPU().getPC()));
            }

            updateDisplays();

            if (name.contains("program2") || name.contains("prog2")) {
                promptForParagraphFile();
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading program: " + ex.getMessage());
        }
    }

    private static Integer firstInstructionAddress(List<Assembler.AsmLine> lines) {
        for (Assembler.AsmLine al : lines) {
            if (al.address == null || al.op == null) {
                continue;
            }

            String op = al.op.toUpperCase(Locale.ROOT);
            if (op.equals("LOC") || op.equals("DATA")) {
                continue;
            }

            return al.address;
        }

        for (Assembler.AsmLine al : lines) {
            if (al.address != null) {
                return al.address;
            }
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

    private void runAsync() {
        if (runInProgress) {
            return;
        }

        setRunControls(true);
        refreshTimer.start();

        runWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                simulator.run(RUN_MAX_STEPS);
                return null;
            }

            @Override
            protected void done() {
                refreshTimer.stop();
                updateDisplays();
                setRunControls(false);

                JOptionPane.showMessageDialog(
                        GUI.this,
                        simulator.getCPU().isHalted()
                                ? "Halted"
                                : "Stopped (max steps reached)");
            }
        };

        runWorker.execute();
    }

    private void reset() {
        simulator.reset();
        clearConsole();
        updateDisplays();
    }

    private void sendInput() {
        String input = consoleInput.getText();
        if (input == null || input.isEmpty()) {
            return;
        }

        simulator.appendConsoleInput(input + "\n");
        consoleInput.setText("");
        updateDisplays();
    }

    private void clearConsole() {
        consoleOutput.setText("");
        simulator.clearConsoleOutput();
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

        int cc = cpu.getCC();
        String ccStr = Integer.toBinaryString(0x10 | cc).substring(1) + " (";
        if (cpu.getOverflow())
            ccStr += "O";
        if (cpu.getUnderflow())
            ccStr += "U";
        if (cpu.getDivZero())
            ccStr += "D";
        if (cpu.getEqualOrNot())
            ccStr += "E";
        if (cc == 0)
            ccStr += "-";
        ccStr += ")";
        ccDisplay.setText(ccStr);

        mfrDisplay.setText(String.format("%X", cpu.getMFR()));
        consoleOutput.setText(simulator.getConsoleOutput());

        updateCacheDisplay();

        try {
            memAtMARDisplay.setText(String.format("%04X", simulator.getMemoryAtMAR()));
        } catch (Exception e) {
            memAtMARDisplay.setText("----");
        }
    }

    private void updateCacheDisplay() {
        Cache cache = simulator.getCache();
        CacheLine[] lines = cache.getLines();

        StringBuilder sb = new StringBuilder();
        sb.append("Idx  V   Addr   Data   FIFO\n");
        sb.append("--------------------------------\n");

        for (int i = 0; i < lines.length; i++) {
            CacheLine line = lines[i];
            sb.append(String.format(
                    "%2d   %d   %04X   %04X   %d%n",
                    i,
                    line.valid ? 1 : 0,
                    line.address & 0xFFFF,
                    line.data & 0xFFFF,
                    line.fifoOrder));
        }

        sb.append("\n");
        sb.append(cache.getLastAccessSummary());
        cacheDisplay.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUI().setVisible(true));
    }
}