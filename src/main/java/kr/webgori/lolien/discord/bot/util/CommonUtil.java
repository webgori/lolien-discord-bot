package kr.webgori.lolien.discord.bot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import kr.webgori.lolien.discord.bot.spring.CustomLocalDateTimeDeserializer;
import kr.webgori.lolien.discord.bot.spring.CustomLocalDateTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
public class CommonUtil {
  public static void sendMessage(TextChannel textChannel, String message) {
    textChannel.sendMessage(message).queue();
  }

  public static void sendMessageAfter(TextChannel textChannel, String message, long delay) {
    textChannel.sendMessage(message).queueAfter(delay, TimeUnit.SECONDS);
  }

  /**
   * sendErrorMessage.
   *
   * @param textChannel textChannel
   * @param message     message
   * @param color       color
   */
  public static void sendErrorMessage(TextChannel textChannel, String message, Color color) {
    MessageEmbed messageEmbed = new EmbedBuilder()
        .setColor(color)
        .setFooter(message, null)
        .build();

    textChannel.sendMessage(messageEmbed).queue();
  }

  /**
   * numberToRomanNumeral.
   *
   * @param number number
   * @return String
   */
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

  /**
   * 객체를 Json 문자열로 반환.
   *
   * @param o 객체
   * @return Json
   */
  public static String objectToJsonString(Object o) {
    String json = "{}";

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
    javaTimeModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());

    objectMapper.registerModule(javaTimeModule);

    try {
      json = objectMapper.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      logger.error("", e);
    }

    return json;
  }

  /**
   * getTournamentCreatedDate.
   *
   * @return String
   */
  public static String getTournamentCreatedDate() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime dateTime = LocalDateTime.now();
    return dateTime.format(formatter);
  }

  public static int getCurrentMonth() {
    return LocalDate.now().getMonthValue();
  }
}