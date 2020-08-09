package kr.webgori.lolien.discord.bot.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kr.webgori.lolien.discord.bot.dto.ChampDto;
import kr.webgori.lolien.discord.bot.dto.ChampsDto;
import kr.webgori.lolien.discord.bot.dto.ClientVersionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChampComponent {
  private static final String REDIS_CHAMP_CLIENT_VERSION_KEY = "lolien-discord-bot:champ-%s";

  private final RestTemplate restTemplate;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private final Gson gson;

  private ClientVersionDto getLatestClientVersion() {
    ClientVersionDto[] clientVersionDtoArray = Optional.ofNullable(restTemplate
        .getForObject("https://ddragon.leagueoflegends.com/api/versions.json",
            ClientVersionDto[].class)).orElseGet(() -> new ClientVersionDto[0]);

    List<ClientVersionDto> clientVersionDtoList = Arrays.asList(clientVersionDtoArray);

    if (clientVersionDtoList.isEmpty()) {
      throw new IllegalStateException("riot client version api result is empty");
    }

    return clientVersionDtoList.get(0);
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
    ClientVersionDto latestClientVersionDto = getLatestClientVersion();
    String clientVersion = latestClientVersionDto.getVersion();

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
}
