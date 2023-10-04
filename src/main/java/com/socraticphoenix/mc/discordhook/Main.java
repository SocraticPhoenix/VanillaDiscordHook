package com.socraticphoenix.mc.discordhook;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.socraticphoenix.mc.discordhook.service.DiscordMessageService;
import com.socraticphoenix.mc.discordhook.service.DiscordNameService;
import com.socraticphoenix.mc.discordhook.service.ServerMessageService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    public static final Gson GSON = new Gson();

    public static void main(String[] args) {
        Config config;
        try {
            config = GSON.fromJson(Files.newBufferedReader(Paths.get("./conf.json")), Config.class);
        } catch (IOException | JsonParseException e) {
            System.err.println("FAILED TO LOAD CONFIG FILE");
            e.printStackTrace();
            return;
        }

        try {
            Path names = Paths.get("./names.json");
            DiscordNameService nameService;
            if (!Files.exists(names)) {
                nameService = new DiscordNameService();
                nameService.save();
            } else {
                nameService = GSON.fromJson(Files.newBufferedReader(names), DiscordNameService.class);
            }
            ServerMessageService serverMessageService = new ServerMessageService(config, nameService);
            DiscordMessageService discordMessageService = new DiscordMessageService(config);
            DiscordListener listener = new DiscordListener(config, nameService, serverMessageService, discordMessageService);

            System.out.println("Created services, loading JDA...");

            JDA jda = JDABuilder.createDefault(config.botToken)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .setActivity(Activity.playing("Minecraft"))
                    .addEventListeners(listener)
                    .build();
            jda.awaitReady();

            System.out.println("JDA Loaded, initializing services...");

            LogWatcher logWatcher = new LogWatcher(config, discordMessageService);

            discordMessageService.setJda(jda);
            serverMessageService.init();
            discordMessageService.init();
            logWatcher.init();

            CommandHandler.updateCommands(jda);

            discordMessageService.sendChatMessage("Server started!");
            System.out.println("Waiting for input");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String line = scanner.nextLine();
                if (line.equals("stop")) {
                    break;
                }
            }
            System.out.println("Shutting down");
            discordMessageService.sendConsoleMessage("Server stopped!");
            discordMessageService.sendChatMessage("Server stopped!");

            logWatcher.close();
            discordMessageService.close();
            serverMessageService.close();
            jda.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
