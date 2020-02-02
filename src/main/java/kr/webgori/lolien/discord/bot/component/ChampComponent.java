package kr.webgori.lolien.discord.bot.component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kr.webgori.lolien.discord.bot.dto.ClientVersionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChampComponent {
  private static final String REDIS_CHAMP_CLIENT_VERSION_KEY = "lolien-discord-bot:champ-%s";

  private final RestTemplate restTemplate;
  private final RedisTemplate<String, Object> redisTemplate;
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

  private void cachingChampionName(String clientVersion, int champId, String championName) {
    HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
    String redisChampClientVersionKey = String.format(REDIS_CHAMP_CLIENT_VERSION_KEY,
        clientVersion);
    hashOperations.put(redisChampClientVersionKey, String.valueOf(champId), championName);
  }

  private String getChampionNameFromRiotApi(String clientVersion, int champId) {
    JsonObject championsJsonObject = getChampionJsonObject(clientVersion);
    JsonObject data = championsJsonObject.getAsJsonObject("data");
    Set<String> championsName = data.keySet();

    for (String championName : championsName) {
      JsonObject championJsonObject = data.getAsJsonObject(championName);
      int championId = championJsonObject.get("key").getAsInt();

      if (championId == champId) {
        String championKoreanName = championJsonObject.get("name").getAsString();
        cachingChampionName(clientVersion, champId, championKoreanName);
        return championKoreanName;
      }
    }

    throw new IllegalStateException("can not find champion name form riot champions api");
  }

  String getChampionNameByChampId(int champId) {
    ClientVersionDto latestClientVersionDto = getLatestClientVersion();
    String clientVersion = latestClientVersionDto.getVersion();

    String redisChampClientVersionKey = String.format(REDIS_CHAMP_CLIENT_VERSION_KEY,
        clientVersion);

    HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

    return (String) Optional
        .ofNullable(hashOperations.get(redisChampClientVersionKey, String.valueOf(champId)))
        .orElseGet(() -> getChampionNameFromRiotApi(clientVersion, champId));
  }
}
