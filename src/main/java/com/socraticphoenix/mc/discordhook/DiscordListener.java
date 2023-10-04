package com.socraticphoenix.mc.discordhook;

import com.socraticphoenix.mc.discordhook.service.DiscordMessageService;
import com.socraticphoenix.mc.discordhook.service.DiscordNameService;
import com.socraticphoenix.mc.discordhook.service.ServerMessageService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.io.IOException;

public class DiscordListener extends ListenerAdapter {
    private Config config;
    private DiscordNameService discordNameService;
    private ServerMessageService serverMessageService;
    private DiscordMessageService discordMessageService;

    public DiscordListener(Config config, DiscordNameService discordNameService, ServerMessageService serverMessageService, DiscordMessageService discordMessageService) {
        this.config = config;
        this.discordNameService = discordNameService;
        this.serverMessageService = serverMessageService;
        this.discordMessageService = discordMessageService;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        User user = event.getAuthor();

        if (event.getChannel().getId().equals(this.config.chatChannelId)) {
            this.serverMessageService.sendMessage(user.getName(), message.getContentDisplay());
        } else if (event.getChannel().getId().equals(this.config.consoleChannelId)) {
            this.discordMessageService.resetConsole();
            this.serverMessageService.sendCommand(message.getContentRaw());
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() != null && event.getMember() != null) {
            if (event.getName().equals("socraticmc")) {
                if (event.getSubcommandName().equals("setname")) {
                    String userName = event.getUser().getName();
                    String mcName = event.getOption("minecraft_name").getAsString();

                    this.discordNameService.put(userName, mcName);
                    event.reply(MarkdownSanitizer.escape("Set minecraft username for " + userName + " to " + mcName)).queue();
                    this.saveNameService();
                } else if (event.getSubcommandName().equals("setothername")) {
                    if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                        String userName = event.getOption("discord_user").getAsUser().getName();
                        String mcName = event.getOption("minecraft_name").getAsString();

                        this.discordNameService.put(userName, mcName);
                        event.reply(MarkdownSanitizer.escape("Set minecraft username for " + userName + " to " + mcName)).queue();
                        this.saveNameService();
                    } else {
                        event.reply("You must be an admin to use this command").queue();
                    }
                }
            }
        }
    }

    private void saveNameService() {
        new Thread(() -> {
            try {
                this.discordNameService.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
