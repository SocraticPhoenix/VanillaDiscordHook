package com.socraticphoenix.mc.discordhook.service;

import com.socraticphoenix.mc.discordhook.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiscordNameService {
    private Map<String, String> names = new LinkedHashMap<>();

    public String get(String discordName) {
        return this.names.get(discordName);
    }

    public void put(String discordName, String mcName) {
        this.names.put(discordName, mcName);
    }

    public void save() throws IOException {
        Files.writeString(Paths.get("./names.json"), Main.GSON.toJson(this));
    }

}
