package kr.webgori.lolien.discord.bot.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kr.webgori.lolien.discord.bot.dto.ChampDto;
import kr.webgori.lolien.discord.bot.dto.ChampsDto;
import kr.webgori.lolien.discord.bot.dto.DataDragonVersionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Component
public class RiotComponent {
  private static final String REDIS_CHAMP_CLIENT_VERSION_KEY = "lolien-discord-bot:champ-%s";

  private final RestTemplate restTemplate;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private final Gson gson;

  /**
   * getDataDragonVersions.
   * @return Data DragonVersion List
   */
  public List<DataDragonVersionDto> getDataDragonVersions() {
    DataDragonVersionDto[] dataDragonVersionDtoArray = Optional.ofNullable(restTemplate
        .getForObject("https://ddragon.leagueoflegends.com/api/versions.json",
            DataDragonVersionDto[].class)).orElseGet(() -> new DataDragonVersionDto[0]);

    List<DataDragonVersionDto> dataDragonVersionDtoList = Arrays.asList(dataDragonVersionDtoArray);

    if (dataDragonVersionDtoList.isEmpty()) {
      throw new IllegalStateException("riot client version api result is empty");
    }

    return dataDragonVersionDtoList;
  }

  private DataDragonVersionDto getLatestDataDragonVersion() {
    return getDataDragonVersions().get(0);
  }

  /**
   * getCloseDataDragonVersion.
   * @param gameVersion gameVersion
   * @param dataDragonVersionsDto dataDragonVersionsDto
   * @return closeDataDragonVersion
   */
  public String getCloseDataDragonVersion(String gameVersion,
                                           List<DataDragonVersionDto> dataDragonVersionsDto) {
    String regexVersion = String.format("%s.%s",
        gameVersion.split("\\.")[0],
        gameVersion.split("\\.")[1]);

    Pattern pattern = Pattern.compile("(" + regexVersion + ").+");
    String dataDragonVersion = "";

    for (DataDragonVersionDto dataDragonVersionDto : dataDragonVersionsDto) {
      String version = dataDragonVersionDto.getVersion();
      Matcher matcher = pattern.matcher(version);

      while (matcher.find()) {
        dataDragonVersion = matcher.group();
      }
    }

    if (dataDragonVersion.isEmpty()) {
      throw new IllegalArgumentException();
    }

    return dataDragonVersion;
  }

  private JsonObject getChampionJsonObject(String clientVersion) {
    String responseBody = Optional.ofNullable(restTemplate
        .getForObject("http://ddragon.leagueoflegends.com/cdn/{client-version}/data/ko_KR/champion.json",
            String.class, clientVersion))
        .orElseThrow(() -> new IllegalStateException("riot champions api result is empty"));

    return gson.fromJson(responseBody, JsonObject.class);
  }

  private void cachingChampionName(String clientVersion, ChampsDto champsDto) {
    String redisChampClientVersionKey = String.format(REDIS_CHAMP_CLIENT_VERSION_KEY,
        clientVersion);

    redisTemplate.opsForValue().set(redisChampClientVersionKey, champsDto);
  }

  private String getChampionNameFromRiotApi(String clientVersion, int champId) {
    JsonObject championsJsonObject = getChampionJsonObject(clientVersion);
    JsonObject data = championsJsonObject.getAsJsonObject("data");
    Set<String> championsName = data.keySet();
    List<ChampDto> champDtoList = Lists.newArrayList();

    for (String championName : championsName) {
      JsonObject championJsonObject = data.getAsJsonObject(championName);
      int championId = championJsonObject.get("key").getAsInt();
      String championKoreanName = championJsonObject.get("name").getAsString();

      ChampDto champDto = ChampDto
          .builder()
          .key(championId)
          .name(championKoreanName)
          .build();

      champDtoList.add(champDto);
    }

    ChampsDto champsDto = ChampsDto
        .builder()
        .champs(champDtoList)
        .build();

    cachingChampionName(clientVersion, champsDto);

    return getChampionNameFromCache(clientVersion, champId);
  }

  private String getChampionNameFromCache(String clientVersion, int champId) {
    String redisChampClientVersionKey = String.format(REDIS_CHAMP_CLIENT_VERSION_KEY,
        clientVersion);

    Object obj = redisTemplate.opsForValue().get(redisChampClientVersionKey);
    ChampsDto champsDto = objectMapper.convertValue(obj, ChampsDto.class);
    List<ChampDto> champs = champsDto.getChamps();

    return champs
        .stream()
        .filter(c -> c.getKey() == champId)
        .findFirst().orElseThrow(IllegalStateException::new)
        .getName();
  }

  String getChampionNameByChampId(int champId) {
    DataDragonVersionDto latestDataDragonVersionDto = getLatestDataDragonVersion();
    String clientVersion = latestDataDragonVersionDto.getVersion();

    String redisChampClientVersionKey = String.format(REDIS_CHAMP_CLIENT_VERSION_KEY,
        clientVersion);

    ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();
    boolean hasKey = Optional
        .ofNullable(opsForValue.getOperations().hasKey(redisChampClientVersionKey))
        .orElse(false);

    if (hasKey) {
      return getChampionNameFromCache(clientVersion, champId);
    }

    return getChampionNameFromRiotApi(clientVersion, champId);
  }

  /**
   * getChampionUrl.
   * @param dataDragonVersion dataDragonVersion
   * @param championId championId
   * @return championUrl
   */
  public String getChampionUrl(String dataDragonVersion, int championId) {
    String championImageFilename = getChampionImageFilename(dataDragonVersion, championId);
    return String
        .format("http://ddragon.leagueoflegends.com/cdn/%s/img/champion/%s",
            dataDragonVersion, championImageFilename);
  }

  private String getChampionImageFilename(String dataDragonVersion, int championId) {
    JsonObject championsJsonObject = getChampionJsonObject(dataDragonVersion);
    JsonObject data = championsJsonObject.getAsJsonObject("data");
    Set<String> championsName = data.keySet();

    String championImageFilename = "";

    for (String championName : championsName) {
      JsonObject championJsonObject = data.getAsJsonObject(championName);
      int key = championJsonObject.get("key").getAsInt();

      if (championId == key) {
        JsonObject image = championJsonObject.get("image").getAsJsonObject();
        championImageFilename = image.get("full").getAsString();
      }
    }

    if (championImageFilename.isEmpty()) {
      throw new IllegalArgumentException();
    }

    return championImageFilename;
  }

  /**
   * getSpellUrl.
   * @param dataDragonVersion dataDragonVersion
   * @param spellId spellId
   * @return spellUrl
   */
  public String getSpellUrl(String dataDragonVersion, int spellId) {
    String spellImageFilename = getSpellImageFilename(dataDragonVersion, spellId);
    return String
        .format("http://ddragon.leagueoflegends.com/cdn/%s/img/spell/%s",
            dataDragonVersion, spellImageFilename);
  }

  private String getSpellImageFilename(String dataDragonVersion, int spellId) {
    JsonObject summonerJsonObject = getSummonerJsonObject(dataDragonVersion);
    JsonObject data = summonerJsonObject.getAsJsonObject("data");
    Set<String> spellsName = data.keySet();

    String spellImageFilename = "";

    for (String spellName : spellsName) {
      JsonObject spellJsonObject = data.getAsJsonObject(spellName);
      int key = spellJsonObject.get("key").getAsInt();

      if (spellId == key) {
        JsonObject image = spellJsonObject.get("image").getAsJsonObject();
        spellImageFilename = image.get("full").getAsString();
      }
    }

    if (spellImageFilename.isEmpty()) {
      throw new IllegalArgumentException();
    }

    return spellImageFilename;
  }

  private JsonObject getSummonerJsonObject(String dataDragonVersion) {
    String responseBody = Optional.ofNullable(restTemplate
        .getForObject("http://ddragon.leagueoflegends.com/cdn/{data-dragon-version}/data/ko_KR/summoner.json",
            String.class, dataDragonVersion))
        .orElseThrow(() -> new IllegalStateException("riot champions api result is empty"));

    return gson.fromJson(responseBody, JsonObject.class);
  }

  /**
   * getItemUrl.
   * @param dataDragonVersion dataDragonVersion
   * @param itemId itemId
   * @return itemUrl
   */
  public String getItemUrl(String dataDragonVersion, int itemId) {
    String itemImageFilename = getItemImageFilename(dataDragonVersion, itemId);
    return String
        .format("http://ddragon.leagueoflegends.com/cdn/%s/img/item/%s",
            dataDragonVersion, itemImageFilename);
  }

  private String getItemImageFilename(String dataDragonVersion, int itemId) {
    JsonObject itemsJsonObject = getItemJsonObject(dataDragonVersion);
    JsonObject data = itemsJsonObject.getAsJsonObject("data");
    String key = String.valueOf(itemId);
    boolean hasKey = data.has(key);

    if (!hasKey) {
      throw new IllegalArgumentException();
    }

    JsonObject itemJsonObject = data.get(key).getAsJsonObject();
    JsonObject image = itemJsonObject.get("image").getAsJsonObject();
    return image.get("full").getAsString();
  }

  private JsonObject getItemJsonObject(String dataDragonVersion) {
    String responseBody = Optional.ofNullable(restTemplate
        .getForObject("http://ddragon.leagueoflegends.com/cdn/{data-dragon-version}/data/ko_KR/item.json",
            String.class, dataDragonVersion))
        .orElseThrow(() -> new IllegalStateException("riot champions api result is empty"));

    return gson.fromJson(responseBody, JsonObject.class);
  }
}
