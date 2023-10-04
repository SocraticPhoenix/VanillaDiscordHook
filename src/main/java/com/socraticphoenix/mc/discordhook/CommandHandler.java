package com.socraticphoenix.mc.discordhook;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class CommandHandler {

    public static void updateCommands(JDA jda) {
        jda.updateCommands().addCommands(
                Commands.slash("socraticmc", "Base command for SocraticMC bot")
                        .addSubcommands(
                                new SubcommandData("setname", "Sets your minecraft username")
                                        .addOption(OptionType.STRING, "minecraft_name", "Your minecraft username", true),
                                new SubcommandData("setothername", "Sets the minecraft username name of a server member")
                                        .addOption(OptionType.STRING, "minecraft_name", "The minecraft username", true)
                                        .addOption(OptionType.USER, "discord_user", "The user to set the name for", true)
                        )
        ).queue();
    }

}
