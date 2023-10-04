package com.socraticphoenix.mc.discordhook.service;

import com.socraticphoenix.mc.discordhook.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DiscordMessageService {
    private Config config;
    private JDA jda;

    public DiscordMessageService(Config config) {
        this.config = config;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    public void init() {
        System.out.println("init DiscordMessageService");
        Thread consoleWorker = new Thread(() -> {
            while (this.running.get()) {
                try {
                    if (!this.consoleMessages.isEmpty()) {
                        this.doSendConsoleMessage(this.consoleMessages.poll());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            try {
                while (!this.consoleMessages.isEmpty()) {
                    this.doSendConsoleMessage(this.consoleMessages.poll());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "discord-console-worker");

        Thread chatWorker = new Thread(() -> {
            while (this.running.get()) {
                try {
                    if (!this.chatMessages.isEmpty()) {
                        this.doSendChatMessage(this.chatMessages.poll());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            try {
                while (!this.chatMessages.isEmpty()) {
                    this.doSendChatMessage(this.chatMessages.poll());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "discord-chat-worker");

        consoleWorker.start();
        chatWorker.start();
    }

    public void close() {
        this.running.set(false);
    }

    public void sendConsoleMessage(String line) {
        consoleMessages.add(line);
    }

    public void sendChatMessage(String line) {
        chatMessages.add(line);
    }

    private Deque<String> chatMessages = new ConcurrentLinkedDeque<>();
    private Deque<String> consoleMessages = new ConcurrentLinkedDeque<>();
    private AtomicBoolean running = new AtomicBoolean(true);

    private AtomicLong previousConsoleMessage = new AtomicLong(0);
    private StringBuilder consoleMessageContent = new StringBuilder();

    public void resetConsole() {
        this.previousConsoleMessage.set(0);
    }

    private void doSendConsoleMessage(String line) {
        TextChannel channel = this.jda.getTextChannelById(this.config.consoleChannelId);
        if (channel != null) {
            String escaped = MarkdownSanitizer.escape(line) + "\n";
            if (this.previousConsoleMessage.get() == 0 ||
                    this.consoleMessageContent.length() + escaped.length() + 10 >= Message.MAX_CONTENT_LENGTH) {
                this.consoleMessageContent.setLength(0);
                this.consoleMessageContent.append(escaped);
                Message message = channel.sendMessage("```\n" + escaped + "```")
                        .complete();
                this.previousConsoleMessage.set(message.getIdLong());
            } else {
                try {
                    this.consoleMessageContent.append(escaped);
                    channel.editMessageById(this.previousConsoleMessage.get(), "```\n" + this.consoleMessageContent + "```")
                            .complete();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    this.previousConsoleMessage = null;
                    this.doSendConsoleMessage(line);
                }
            }
        }
    }

    private void doSendChatMessage(String line) {
        TextChannel channel = this.jda.getTextChannelById(this.config.chatChannelId);
        if (channel != null) {
            channel.sendMessage(MarkdownSanitizer.escape(line)).queue();
        }
    }

}
