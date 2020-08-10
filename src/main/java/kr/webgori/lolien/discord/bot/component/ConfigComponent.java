package kr.webgori.lolien.discord.bot.component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
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
  private static String DATA_SOURCE_URL;
  private static String DATA_SOURCE_USERNAME;
  private static String DATA_SOURCE_PASSWORD;
  private static String RIOT_API_KEY;
  private static String DISCORD_TOKEN;
  private static String REDIS_HOST;
  private static String REDIS_PASSWORD;
  private static String JWT_SECRET_KEY;

  private final Gson gson;

  @Value("${config.file.path.windows}")
  private String jsonConfigFilePathWindows;

  @Value("${config.file.path.linux}")
  private String jsonConfigFilePathLinux;

  @Value("${spring.profiles.active}")
  private String profile;

  public static String getDataSourceUrl() {
    return DATA_SOURCE_URL;
  }

  private static void setDataSourceUrl(String dataSourceUrl) {
    DATA_SOURCE_URL = dataSourceUrl;
  }

  public static String getDataSourceUsername() {
    return DATA_SOURCE_USERNAME;
  }

  private static void setDataSourceUsername(String dataSourceUsername) {
    DATA_SOURCE_USERNAME = dataSourceUsername;
  }

  public static String getDataSourcePassword() {
    return DATA_SOURCE_PASSWORD;
  }

  private static void setDataSourcePassword(String dataSourcePassword) {
    DATA_SOURCE_PASSWORD = dataSourcePassword;
  }

  public static String getRiotApiKey() {
    return RIOT_API_KEY;
  }

  private static void setRiotApiKey(String riotApiKey) {
    RIOT_API_KEY = riotApiKey;
  }

  public static String getDiscordToken() {
    return DISCORD_TOKEN;
  }

  private static void setDiscordToken(String discordToken) {
    DISCORD_TOKEN = discordToken;
  }

  public String getJwtSecretKey() {
    return JWT_SECRET_KEY;
  }

  private static void setJwtSecretKey(String jwtSecretKey) {
    JWT_SECRET_KEY = jwtSecretKey;
  }

  /**
   * getRedisHost.
   *
   * @return redis host
   */
  public String getRedisHost() {
    if (Objects.isNull(REDIS_HOST)) {
      getRedisHostFromConfig();
    }

    return REDIS_HOST;
  }

  private static void setRedisHost(String redisHost) {
    REDIS_HOST = redisHost;
  }

  /**
   * getRedisPassword.
   *
   * @return redis password
   */
  public String getRedisPassword() {
    if (Objects.isNull(REDIS_PASSWORD)) {
      getRedisPasswordFromConfig();
    }

    return REDIS_PASSWORD;
  }

  private static void setRedisPassword(String redisPassword) {
    REDIS_PASSWORD = redisPassword;
  }

  @Override
  public void afterPropertiesSet() {
    getJsonStringFromConfig();
    getDataSourceUrlFromConfig();
    getDataSourceUsernameFromConfig();
    getDataSourcePasswordFromConfig();
    getRiotApiKeyFromConfig();
    getDiscordTokenFromConfig();
    getRedisHostFromConfig();
    getRedisPasswordFromConfig();
  }

  private void getJsonStringFromConfig() {
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

  private void getDataSourceUrlFromConfig() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    String dataSourceUrl = jsonObject
        .get("dataSource")
        .getAsJsonObject()
        .get("url")
        .getAsString();

    setDataSourceUrl(dataSourceUrl);
  }

  private void getDataSourceUsernameFromConfig() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    String dataSourceUsername = jsonObject
        .get("dataSource")
        .getAsJsonObject()
        .get("username")
        .getAsString();

    setDataSourceUsername(dataSourceUsername);
  }

  private void getDataSourcePasswordFromConfig() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    String dataSourcePassword = jsonObject
        .get("dataSource")
        .getAsJsonObject()
        .get("password")
        .getAsString();

    setDataSourcePassword(dataSourcePassword);
  }

  private void getRiotApiKeyFromConfig() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    String riotApiKey = jsonObject
        .get("riotApiKey")
        .getAsString();

    setRiotApiKey(riotApiKey);
  }

  private void getDiscordTokenFromConfig() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    String discordToken = jsonObject
        .get("discordToken")
        .getAsString();

    setDiscordToken(discordToken);
  }

  private void getRedisHostFromConfig() {
    if (Objects.isNull(JSON_STRING)) {
      getJsonStringFromConfig();
    }

    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    String redisHost = jsonObject
        .get("redis")
        .getAsJsonObject()
        .get("host")
        .getAsString();

    setRedisHost(redisHost);
  }

  private void getRedisPasswordFromConfig() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    String redisPassword = jsonObject
        .get("redis")
        .getAsJsonObject()
        .get("password")
        .getAsString();

    setRedisPassword(redisPassword);
  }

  /**
   * getJwtSecretKeyFromConfig.
   */
  public void getJwtSecretKeyFromConfig() {
    JsonObject jsonObject = gson.fromJson(JSON_STRING, JsonObject.class);
    String jwtSecretKey = jsonObject
        .get("jwtSecretKey")
        .getAsString();

    setJwtSecretKey(jwtSecretKey);
  }
}
