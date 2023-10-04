package com.socraticphoenix.mc.discordhook.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.socraticphoenix.mc.discordhook.Config;
import com.socraticphoenix.mc.discordhook.Main;

import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class ServerMessageService {
    private Config config;
    private DiscordNameService nameService;

    public ServerMessageService(Config config, DiscordNameService nameService) {
        this.config = config;
        this.nameService = nameService;
    }

    private Deque<String> serverCommands = new LinkedBlockingDeque<>();
    private AtomicBoolean running = new AtomicBoolean(true);

    public void init() {
        System.out.println("init ServerMessageService");
        new Thread(() -> {
            while (this.running.get()) {
                try {
                    if (!this.serverCommands.isEmpty()) {
                        this.doSendCommand(this.serverCommands.poll());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            try {
                while (!this.serverCommands.isEmpty()) {
                    this.doSendCommand(this.serverCommands.poll());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "mc-server-worker").start();
    }

    public void close() {
        this.running.set(false);
    }

    public void sendCommand(String command) {
        this.serverCommands.add(command);
    }

    public void sendMessage(String discordName, String content) {
        this.sendCommand("tellraw @a " + createMessage(this.nameService.get(discordName), discordName, content));
    }

    private void doSendCommand(String command) throws IOException, InterruptedException {
        System.out.println("Sending command " + command);
        for (String[] broadcastPart : this.config.broadcastCommand) {
            ProcessBuilder builder = new ProcessBuilder(
                    Stream.of(broadcastPart).map(s -> s.replace("%command%", command)).toList()
            ).inheritIO();

            Process process = builder.start();
            process.waitFor();
        }
    }

    private String createMessage(String mcName, String discordName, String content) {
        JsonArray tellraw = new JsonArray();
        JsonObject userName = new JsonObject();
        JsonObject msgContent = new JsonObject();
        tellraw.add(userName);
        tellraw.add(msgContent);

        if (mcName != null) {
            userName.addProperty("text", "[Discord] <" + mcName + "> ");
            JsonObject hover = new JsonObject();
            userName.add("hoverEvent", hover);
            hover.addProperty("action", "show_text");
            hover.addProperty("value", discordName + " on Discord");
        } else {
            userName.addProperty("text", "[Discord] <" + discordName + "> ");
        }

        msgContent.addProperty("text", content);

        return Main.GSON.toJson(tellraw);
    }
}
