package kr.webgori.lolien.discord.bot.util;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommonUtil {
  public static void sendMessage(TextChannel textChannel, String message) {
    textChannel.sendMessage(message).queue();
  }

  public static void sendMessageAfter(TextChannel textChannel, String message, long delay) {
    textChannel.sendMessage(message).queueAfter(delay, TimeUnit.SECONDS);
  }

  public static void sendErrorMessage(TextChannel textChannel, String message, Color color) {
    MessageEmbed messageEmbed = new EmbedBuilder()
            .setColor(color)
            .setFooter(message, null)
            .build();

    textChannel.sendMessage(messageEmbed).queue();
  }

  public static String numberToRomanNumeral(String number) {
    switch (number) {
      case "1":
        return "I";
      case "2":
        return "II";
      case "3":
        return "III";
      case "4":
        return "IV";
      default:
        throw new IllegalArgumentException("can not covert number");
    }
  }
}