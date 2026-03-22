package src;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public final class TraceLogger {
    private PrintWriter out;

    public TraceLogger(String filename) throws IOException {
        out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
    }

    public void log(String msg) {
        out.println(msg);
        out.flush();
    }

    public void close() {
        if (out != null) {
            out.close();
        }
    }
}