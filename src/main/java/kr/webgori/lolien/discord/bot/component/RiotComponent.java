package kr.webgori.lolien.discord.bot.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
   *
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
   *
   * @param gameVersion           gameVersion
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
      dataDragonVersion = dataDragonVersionsDto
          .stream()
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("not found data dragon version"))
          .getVersion();
    }

    return dataDragonVersion;
  }

  /**
   * getChampionJsonObject.
   *
   * @param dataDragonVersion dataDragonVersion
   * @return championJsonObject
   */
  public JsonObject getChampionJsonObject(String dataDragonVersion) {
    String responseBody = Optional.ofNullable(restTemplate
        .getForObject("https://ddragon.leagueoflegends"
                + ".com/cdn/{data-dragon-version}/data/ko_KR/champion.json",
            String.class, dataDragonVersion))
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
   * getChampionNameByChampId.
   * @param champs champs
   * @param champId champId
   * @return 챔피언 이름
   */
  public String getChampionNameByChampId(List<ChampDto> champs, int champId) {
    return champs
        .stream()
        .filter(c -> c.getKey() == champId)
        .findFirst().orElseThrow(IllegalStateException::new)
        .getName();
  }

  /**
   * getChampionNames.
   * @return 챔피언 이름 목록
   */
  public List<ChampDto> getChampionNames() {
    DataDragonVersionDto latestDataDragonVersionDto = getLatestDataDragonVersion();
    String clientVersion = latestDataDragonVersionDto.getVersion();

    String redisChampClientVersionKey = String.format(REDIS_CHAMP_CLIENT_VERSION_KEY,
        clientVersion);

    ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();
    boolean hasKey = Optional
        .ofNullable(opsForValue.getOperations().hasKey(redisChampClientVersionKey))
        .orElse(false);

    if (hasKey) {
      return getChampionNamesFromCache(clientVersion);
    }

    return getChampionNamesFromRiotApi(clientVersion);
  }

  private List<ChampDto> getChampionNamesFromCache(String clientVersion) {
    String redisChampClientVersionKey = String.format(REDIS_CHAMP_CLIENT_VERSION_KEY,
        clientVersion);

    Object obj = redisTemplate.opsForValue().get(redisChampClientVersionKey);
    ChampsDto champsDto = objectMapper.convertValue(obj, ChampsDto.class);
    return champsDto.getChamps();
  }

  private List<ChampDto> getChampionNamesFromRiotApi(String clientVersion) {
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

    return champDtoList;
  }

  /**
   * getChampionUrl.
   *
   * @param dataDragonVersion dataDragonVersion
   * @param championId        championId
   * @return championUrl
   */
  public String getChampionUrl(JsonObject championsJsonObject, String dataDragonVersion,
                               int championId) {
    String championImageFilename = getChampionImageFilename(championsJsonObject, championId);
    return String
        .format("https://ddragon.leagueoflegends.com/cdn/%s/img/champion/%s",
            dataDragonVersion, championImageFilename);
  }

  private String getChampionImageFilename(JsonObject championsJsonObject, int championId) {
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
   * getChampionName.
   *
   * @param championId        championId
   * @return championName
   */
  public String getChampionName(JsonObject championsJsonObject, int championId) {
    JsonObject data = championsJsonObject.getAsJsonObject("data");
    Set<String> championsName = data.keySet();

    String championKoreanName = "";

    for (String championName : championsName) {
      JsonObject championJsonObject = data.getAsJsonObject(championName);
      int key = championJsonObject.get("key").getAsInt();

      if (championId == key) {
        championKoreanName = championJsonObject.get("name").getAsString();
      }
    }

    if (championKoreanName.isEmpty()) {
      throw new IllegalArgumentException();
    }

    return championKoreanName;
  }

  /**
   * getSpellUrl.
   *
   * @param dataDragonVersion dataDragonVersion
   * @param spellId           spellId
   * @return spellUrl
   */
  public String getSpellUrl(JsonObject summonerJsonObject, String dataDragonVersion, int spellId) {
    String spellImageFilename = getSpellImageFilename(summonerJsonObject, spellId);
    return String
        .format("https://ddragon.leagueoflegends.com/cdn/%s/img/spell/%s",
            dataDragonVersion, spellImageFilename);
  }

  private String getSpellImageFilename(JsonObject summonerJsonObject, int spellId) {
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

  /**
   * getSpellName.
   *
   * @param spellId           spellId
   * @return spellName
   */
  public String getSpellName(JsonObject summonerJsonObject, int spellId) {
    JsonObject data = summonerJsonObject.getAsJsonObject("data");
    Set<String> spellsName = data.keySet();

    String spellKoreanName = "";

    for (String spellName : spellsName) {
      JsonObject spellJsonObject = data.getAsJsonObject(spellName);
      int key = spellJsonObject.get("key").getAsInt();

      if (spellId == key) {
        spellKoreanName = spellJsonObject.get("name").getAsString();
      }
    }

    if (spellKoreanName.isEmpty()) {
      throw new IllegalArgumentException();
    }

    return spellKoreanName;
  }

  /**
   * getSpellDescription.
   *
   * @param spellId           spellId
   * @return spellDescription
   */
  public String getSpellDescription(JsonObject summonerJsonObject, int spellId) {
    JsonObject data = summonerJsonObject.getAsJsonObject("data");
    Set<String> spellsName = data.keySet();

    String spellKoreanDescription = "";

    for (String spellName : spellsName) {
      JsonObject spellJsonObject = data.getAsJsonObject(spellName);
      int key = spellJsonObject.get("key").getAsInt();

      if (spellId == key) {
        spellKoreanDescription = spellJsonObject.get("description").getAsString();
      }
    }

    if (spellKoreanDescription.isEmpty()) {
      throw new IllegalArgumentException();
    }

    return spellKoreanDescription;
  }

  /**
   * getSummonerJsonObject.
   *
   * @param dataDragonVersion dataDragonVersion
   * @return summonerJsonObject
   */
  public JsonObject getSummonerJsonObject(String dataDragonVersion) {
    String responseBody = Optional.ofNullable(restTemplate
        .getForObject("https://ddragon.leagueoflegends"
                + ".com/cdn/{data-dragon-version}/data/ko_KR/summoner.json",
            String.class, dataDragonVersion))
        .orElseThrow(() -> new IllegalStateException("riot champions api result is empty"));

    return gson.fromJson(responseBody, JsonObject.class);
  }

  /**
   * getItemUrl.
   *
   * @param dataDragonVersion dataDragonVersion
   * @param itemId            itemId
   * @return itemUrl
   */
  public String getItemUrl(JsonObject itemsJsonObject, String dataDragonVersion, int itemId) {
    if (itemId == 0) {
      return "";
    }

    String itemImageFilename = getItemImageFilename(itemsJsonObject, itemId);
    return String
        .format("https://ddragon.leagueoflegends.com/cdn/%s/img/item/%s",
            dataDragonVersion, itemImageFilename);
  }

  private String getItemImageFilename(JsonObject itemsJsonObject, int itemId) {
    JsonObject itemJsonObject = getItemJsonObject(itemsJsonObject, itemId);
    JsonObject image = itemJsonObject.get("image").getAsJsonObject();
    return image.get("full").getAsString();
  }

  /**
   * getItemName.
   *
   * @param itemId            itemId
   * @return itemName
   */
  public String getItemName(JsonObject itemsJsonObject, int itemId) {
    if (itemId == 0) {
      return "";
    }

    JsonObject itemJsonObject = getItemJsonObject(itemsJsonObject, itemId);
    return itemJsonObject.get("name").getAsString();
  }

  /**
   * getItemDescription.
   *
   * @param itemId            itemId
   * @return itemDescription
   */
  public String getItemDescription(JsonObject itemsJsonObject, int itemId) {
    if (itemId == 0) {
      return "";
    }

    JsonObject itemJsonObject = getItemJsonObject(itemsJsonObject, itemId);
    return itemJsonObject.get("description").getAsString();
  }

  private JsonObject getItemJsonObject(JsonObject itemsJsonObject, int itemId) {
    JsonObject data = itemsJsonObject.getAsJsonObject("data");
    String key = String.valueOf(itemId);
    boolean hasKey = data.has(key);

    if (!hasKey) {
      throw new IllegalArgumentException();
    }

    return data.get(key).getAsJsonObject();
  }

  /**
   * getItemJsonObject.
   *
   * @param dataDragonVersion dataDragonVersion
   * @return itemJsonObject
   */
  public JsonObject getItemJsonObject(String dataDragonVersion) {
    String responseBody = Optional.ofNullable(restTemplate
        .getForObject("https://ddragon.leagueoflegends"
                + ".com/cdn/{data-dragon-version}/data/ko_KR/item.json",
            String.class, dataDragonVersion))
        .orElseThrow(() -> new IllegalStateException("riot champions api result is empty"));

    return gson.fromJson(responseBody, JsonObject.class);
  }

  private JsonObject getRuneJsonArray(JsonArray runesJsonArray, int runeId) {
    for (JsonElement jsonElement : runesJsonArray) {
      JsonObject jsonObject = jsonElement.getAsJsonObject();
      int id = jsonObject.get("id").getAsInt();

      if (id == runeId) {
        return getPrimaryRuneJsonObject(jsonObject, runeId);
      }

      JsonArray slots = jsonObject.get("slots").getAsJsonArray();

      for (JsonElement slot : slots) {
        JsonObject slotJsonObject = slot.getAsJsonObject();
        JsonArray runes = slotJsonObject.get("runes").getAsJsonArray();

        for (JsonElement run : runes) {
          JsonObject runJsonObject = run.getAsJsonObject();
          int slotId = runJsonObject.get("id").getAsInt();

          if (slotId == runeId) {
            return runJsonObject;
          }
        }
      }
    }

    throw new IllegalArgumentException("존재하지 않는 룬 정보입니다.");
  }

  /**
   * getRuneJsonObject.
   *
   * @param dataDragonVersion dataDragonVersion
   * @return runeJsonObject
   */
  public JsonArray getRuneJsonArray(String dataDragonVersion) {
    String responseBody = Optional.ofNullable(restTemplate
        .getForObject("https://ddragon.leagueoflegends"
                + ".com/cdn/{data-dragon-version}/data/ko_KR/runesReforged.json",
            String.class, dataDragonVersion))
        .orElseThrow(() -> new IllegalStateException("riot champions api result is empty"));

    return gson.fromJson(responseBody, JsonArray.class);
  }

  private JsonObject getPrimaryRuneJsonObject(JsonObject jsonObject, int runeId) {
    String primaryRuneDescription = getPrimaryRuneDescription(runeId);
    jsonObject.addProperty("longDesc", primaryRuneDescription);

    return jsonObject;
  }

  private String getPrimaryRuneDescription(int runeId) {
    switch (runeId) {
      case 8000:
        return "전설이 되어라!<br>정밀은 기본 공격이나 지속적인 피해를 강화할 수 있는 빌드입니다.";
      case 8100:
        return "먹잇감을 사냥하고 처치하라!"
            + "<br>지배는 강력한 피해를 주거나 대상으로 접근을 용이하게 해줄 수 있는 빌드입니다.";
      case 8200:
        return "파괴여, 오라!"
            + "<br>마법은 스킬을 강화하거나 자원을 효율적으로 관리할 수 있는 빌드입니다.";
      case 8300:
        return "가소로운 필멸자들은 물렀거라!"
            + "<br>영감은 정해진 규칙에서 벗어나 창의적으로 플레이할 수 있는 빌드입니다.";
      case 8400:
        return "끝까지 살아있어라!"
            + "<br>결의는 내구력과 군중 제어에 대한 빌드입니다.";
      default:
        return "";
    }
  }

  /**
   * getRuneUrl.
   *
   * @param runeId            runeId
   * @return runeUrl
   */
  public String getRuneUrl(JsonArray runesJsonArray, int runeId) {
    if (runeId == 0) {
      return "";
    }

    String runeImageFilename = getRuneImageFilename(runesJsonArray, runeId);
    return String
        .format("https://ddragon.leagueoflegends.com/cdn/img/%s", runeImageFilename);
  }

  private String getRuneImageFilename(JsonArray runesJsonArray, int runeId) {
    JsonObject runeJsonObject = getRuneJsonArray(runesJsonArray, runeId);
    return runeJsonObject.get("icon").getAsString();
  }

  /**
   * getRuneName.
   *
   * @param runeId            runeId
   * @return runeName
   */
  public String getRuneName(JsonArray runesJsonArray, int runeId) {
    if (runeId == 0) {
      return "";
    }

    JsonObject runeJsonObject = getRuneJsonArray(runesJsonArray, runeId);
    return runeJsonObject.get("name").getAsString();
  }

  /**
   * getRuneDescription.
   *
   * @param runeId            runeId
   * @return runeDescription
   */
  public String getRuneDescription(JsonArray itemsJsonArray,
                                   int runeId) {
    if (runeId == 0) {
      return "";
    }

    JsonObject runeJsonObject = getRuneJsonArray(itemsJsonArray, runeId);
    return runeJsonObject.get("longDesc").getAsString();
  }
}
