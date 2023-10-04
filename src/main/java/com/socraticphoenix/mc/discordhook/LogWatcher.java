package com.socraticphoenix.mc.discordhook;

import com.socraticphoenix.mc.discordhook.service.DiscordMessageService;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class LogWatcher implements FileListener {
    private Config config;
    private DiscordMessageService messageService;

    private Pattern logPrefix;
    private Pattern[] chatPatterns;

    public LogWatcher(Config config, DiscordMessageService messageService) {
        this.config = config;
        this.messageService = messageService;
        this.logPrefix = Pattern.compile(config.logPrefix);
        this.chatPatterns = new Pattern[config.chatPatterns.length];
        for (int i = 0; i < config.chatPatterns.length; i++) {
            this.chatPatterns[i] = Pattern.compile(config.logPrefix + config.chatPatterns[i]);
        }
    }

    private Path target;
    private DefaultFileMonitor monitor;

    public void init() throws IOException {
        System.out.println("init LogWatcher");
        this.target = Paths.get(config.serverPath).resolve("logs").resolve("latest.log");
        if (Files.exists(this.target)) {
            readLinesFromIndex().forEach(this::handleNewLine);
        } else {
            System.err.println("failed to find log at " + this.target);
        }

        DefaultFileMonitor monitor = new DefaultFileMonitor(this);
        this.monitor = monitor;
        FileObject fileObject = VFS.getManager().toFileObject(this.target.toFile());
        monitor.addFile(fileObject);
        monitor.start();
    }

    public void close() {
        this.monitor.stop();
    }

    private void handleNewLine(String line) {
        this.messageService.sendConsoleMessage(line);
        for (Pattern chatPattern : this.chatPatterns) {
            if (chatPattern.matcher(line).matches()) {
                this.messageService.sendChatMessage(removeLogPrefix(line));
                break;
            }
        }
    }

    private long fileIndex = 0;

    @Override
    public void fileChanged(FileChangeEvent event) throws Exception {
        readLinesFromIndex().forEach(this::handleNewLine);
    }

    @Override
    public void fileCreated(FileChangeEvent event) throws IOException {
        readLinesFromIndex().forEach(this::handleNewLine);
    }

    @Override
    public void fileDeleted(FileChangeEvent event) {
        this.fileIndex = 0;
    }

    private static final Pattern NEW_LINE = Pattern.compile("\\R");

    private String removeLogPrefix(String str) {
        String[] parts = this.logPrefix.split(str, 2);
        return parts.length == 2 ? parts[1] : "";
    }

    private List<String> readLinesFromIndex() throws IOException {
        String content = readFromIndex();
        if (content == null) return Collections.emptyList();

        List<String> parts = new ArrayList<>();
        Collections.addAll(parts, NEW_LINE.split(content));
        return parts;
    }

    private String readFromIndex() throws IOException {
        if (!Files.exists(this.target)) return null;

        FileInputStream fis = new FileInputStream(this.target.toFile());
        fis.skip(this.fileIndex);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int len;
        while ((len = fis.read(buffer)) >= 0) {
            this.fileIndex += len;
            bos.write(buffer, 0, len);
        }
        fis.close();

        return bos.toString(StandardCharsets.UTF_8);
    }
}
