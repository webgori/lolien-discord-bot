package kr.webgori.lolien.discord.bot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.awt.Color;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.component.ConfigComponent;
import kr.webgori.lolien.discord.bot.spring.CustomLocalDateTimeDeserializer;
import kr.webgori.lolien.discord.bot.spring.CustomLocalDateTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.constant.Platform;

@Slf4j
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
        throw new IllegalArgumentException("can not covert number " + number);
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

  private static void throwRiotApiException(RiotApiException e) {
    int errorCode = e.getErrorCode();
    if (errorCode == RiotApiException.FORBIDDEN) {
      String message =
          "Riot API Key가 만료되어 기능이 정상적으로 작동하지 않습니다. 개발자에게 알려주세요.";
      throw new IllegalArgumentException(message);
    } else {
      logger.error("{}", e.getMessage());
      throw new IllegalArgumentException("riotApiException");
    }
  }

  /**
   * matchId로 match 정보 조회.
   * @param matchId matchId
   * @return Match
   */
  public static Match getMatch(long matchId) {
    String riotApiKey = ConfigComponent.getRiotApiKey();

    try {
      ApiConfig config;
      config = new ApiConfig().setKey(riotApiKey);
      RiotApi riotApi = new RiotApi(config);
      return riotApi.getMatch(Platform.KR, matchId);
    } catch (RiotApiException e) {
      throwRiotApiException(e);
    }

    throw new IllegalStateException();
  }

  /**
   * op.gg의 시즌을 형식에 맞게 변경.
   * @param season season
   * @return formatted season
   */
  public static String getSeasonFormat(String season) {
    String[] s = season.split("S");
    int seasonNumber = Integer.parseInt(s[1]);
    return seasonNumber <= 9 ? "S0" + seasonNumber : season;
  }

  /**
   * LocalDateTime to String.
   * @param localDateTime localDateTime
   * @return String
   */
  public static String localDateTimeToString(LocalDateTime localDateTime) {
    return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  /**
   * String to LocalDateTime.
   * @param string string
   * @return LocalDateTime
   */
  public static LocalDateTime stringToLocalDateTime(String string) {
    return LocalDateTime.parse(string, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  /**
   * stringArrayToStringList.
   * @param stringArray stringArray
   * @return string list
   */
  public static List<String> stringArrayToStringList(String[] stringArray) {
    return Arrays
        .stream(stringArray)
        .collect(Collectors.toList());
  }

  /**
   * getTimestamp.
   * @param localDateTime localDateTime
   * @return timestamp
   */
  public static long localDateTimeToTimestamp(LocalDateTime localDateTime) {
    return Timestamp.valueOf(localDateTime).getTime();
  }

  /**
   * getTimestamp.
   * @return timestamp
   */
  public static long localDateTimeToTimestamp() {
    LocalDateTime now = LocalDateTime.now();
    return localDateTimeToTimestamp(now);
  }

  /**
   * localDateToTimestamp.
   * @param localDate localDate
   * @return timestamp
   */
  public static long localDateToTimestamp(LocalDate localDate) {
    return Timestamp.valueOf(localDate.atStartOfDay()).getTime();
  }

  /**
   * timestampToLocalDateTime.
   * @param timestamp timestamp
   * @return LocalDateTime
   */
  public static LocalDateTime timestampToLocalDateTime(long timestamp) {
    return LocalDateTime
        .ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId());
  }

  /**
   * getStartDateOfMonth.
   * @return LocalDate
   */
  public static LocalDate getStartDateOfMonth() {
    return LocalDate.now().withDayOfMonth(1);
  }

  /**
   * getStartDateOfPrevMonth.
   * @return LocalDate
   */
  public static LocalDate getStartDateOfPrevMonth() {
    return LocalDate.now().minusMonths(1).withDayOfMonth(1);
  }

  /**
   * getStartDateOfYear.
   * @return LocalDate
   */
  public static LocalDate getStartDateOfYear() {
    return LocalDate.now().withDayOfYear(1);
  }

  /**
   * getEndDateOfMonth.
   * @return LocalDate
   */
  public static LocalDate getEndDateOfMonth() {
    return LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);
  }

  /**
   * getEndDateOfPrevMonth.
   * @return LocalDate
   */
  public static LocalDate getEndDateOfPrevMonth() {
    return LocalDate.now().withDayOfMonth(1).minusDays(1);
  }

  /**
   * getEndDateOfPrevMonth.
   * @return LocalDate
   */
  public static LocalDate getEndDateOfYear() {
    return LocalDate.now().withDayOfYear(1);
  }
}
