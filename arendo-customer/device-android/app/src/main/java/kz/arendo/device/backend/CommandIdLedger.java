package kz.arendo.device.backend;

import android.util.AtomicFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashSet;

/** Durable bounded deduplication ledger; IDs are persisted before a command can touch hardware. */
public final class CommandIdLedger {
    private static final int MAX_IDS = 512;

    private final AtomicFile file;
    private final LinkedHashSet<String> commandIds = new LinkedHashSet<>();

    public CommandIdLedger(File directory) throws IOException {
        if (directory == null) {
            throw new IllegalArgumentException("Storage directory is required");
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Cannot create command ledger directory");
        }
        file = new AtomicFile(new File(directory, "processed-command-ids.txt"));
        load();
    }

    /** Returns false for duplicates. A true result is durable before this method returns. */
    public synchronized boolean markIfNew(String commandId) throws IOException {
        if (commandId == null || commandId.trim().isEmpty() || commandId.length() > 200
                || commandId.indexOf('\n') >= 0 || commandId.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Invalid command ID");
        }
        if (commandIds.contains(commandId)) {
            return false;
        }
        commandIds.add(commandId);
        while (commandIds.size() > MAX_IDS) {
            Iterator<String> iterator = commandIds.iterator();
            iterator.next();
            iterator.remove();
        }
        persist();
        return true;
    }

    private void load() throws IOException {
        try (FileInputStream input = file.openRead();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && line.length() <= 200) {
                    commandIds.add(line);
                }
            }
        } catch (java.io.FileNotFoundException ignored) {
            // First application run: no commands have been processed yet.
        }
        while (commandIds.size() > MAX_IDS) {
            Iterator<String> iterator = commandIds.iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private void persist() throws IOException {
        FileOutputStream output = file.startWrite();
        Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        try {
            for (String commandId : commandIds) {
                writer.write(commandId);
                writer.write('\n');
            }
            writer.flush();
            file.finishWrite(output);
        } catch (IOException error) {
            file.failWrite(output);
            throw error;
        }
    }
}
