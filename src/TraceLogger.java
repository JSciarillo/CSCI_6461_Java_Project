package src;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TraceLogger {
    private final PrintWriter out;
    private boolean enabled = true;

    public TraceLogger(String filename) throws IOException {
        out = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)));
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public synchronized void log(String category, String message) {
        if (!enabled) return;

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        String line = "[" + timestamp + "] [" + category + "] " + message;
        out.println(line);
        out.flush();
    }

    public synchronized void close() {
        out.flush();
        out.close();
    }
}
