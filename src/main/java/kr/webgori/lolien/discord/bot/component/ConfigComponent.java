package kr.webgori.lolien.discord.bot.component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import kr.webgori.lolien.discord.bot.exception.PasswordFileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ConfigComponent implements InitializingBean {
  private static String JSON_STRING;
  public static String DATA_SOURCE_URL;
  public static String DATA_SOURCE_USERNAME;
  public static String DATA_SOURCE_PASSWORD;
  static String CHALLONGE_API_KEY;
  static String CHALLONGE_USERNAME;
  public static String RIOT_API_KEY;
  public static String DISCORD_TOKEN;
  public static String REDIS_HOST;
  public static String REDIS_PASSWORD;

  private final Gson gson;

  @Value("${config.file.path.windows}")
  private String jsonConfigFilePathWindows;

  @Value("${config.file.path.linux}")
  private String jsonConfigFilePathLinux;

  @Value("${spring.profiles.active}")
  private String profile;

  @Override
  public void afterPropertiesSet() throws Exception {
    getJsonString();
    getDataSourceUrl();
    getDataSourceUsername();
    getDataSourcePassword();
    getChallongeUsername();
    getChallongeApiKey();
    getRiotApiKey();
    getDiscordToken();
    getRedisHost();
    getRedisPassword();
  }

  private void getJsonString() {
    String filePath = "";

    try {
      String os = System.getProperty("os.name");

      if (os.startsWith("Windows")) {
        filePath = String.format(jsonConfigFilePathWindows, profile);
      } else {
        filePath = String.format(jsonConfigFilePathLinux, profile);
      }

      FileInputStream fileInputStream = new FileInputStream(filePath);
      JSON_STRING = IOUtils.toString(fileInputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("", e);
      throw new PasswordFileNotFoundException("invalid " + filePath + " path");
    }
  }

  private void getDataSourceUrl() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    DATA_SOURCE_URL = jsonObject
        .get("dataSource")
        .getAsJsonObject()
        .get("url")
        .getAsString();
  }

  private void getDataSourceUsername() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    DATA_SOURCE_USERNAME = jsonObject
        .get("dataSource")
        .getAsJsonObject()
        .get("username")
        .getAsString();
  }

  private void getDataSourcePassword() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    DATA_SOURCE_PASSWORD = jsonObject
        .get("dataSource")
        .getAsJsonObject()
        .get("password")
        .getAsString();
  }

  private void getChallongeUsername() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    CHALLONGE_USERNAME = jsonObject.get("challonge")
        .getAsJsonObject()
        .get("username")
        .getAsString();
  }

  private void getChallongeApiKey() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    CHALLONGE_API_KEY = jsonObject
        .get("challonge")
        .getAsJsonObject()
        .get("apiKey")
        .getAsString();
  }

  private void getRiotApiKey() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    RIOT_API_KEY = jsonObject
        .get("riotApiKey")
        .getAsString();
  }

  private void getDiscordToken() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    DISCORD_TOKEN = jsonObject
        .get("discordToken")
        .getAsString();
  }

  private void getRedisHost() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    REDIS_HOST = jsonObject
        .get("redis")
        .getAsJsonObject()
        .get("host")
        .getAsString();
  }

  private void getRedisPassword() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    REDIS_PASSWORD = jsonObject
        .get("redis")
        .getAsJsonObject()
        .get("password")
        .getAsString();
  }
}