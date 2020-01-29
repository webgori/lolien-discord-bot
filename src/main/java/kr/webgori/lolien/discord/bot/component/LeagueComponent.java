package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getMatch;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeague;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeagueMatch;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeagueParticipant;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeagueParticipantStats;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeagueTeamBans;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeagueTeamStats;
import kr.webgori.lolien.discord.bot.exception.LeagueNotFoundException;
import kr.webgori.lolien.discord.bot.repository.LoLienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.league.LoLienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LoLienLeagueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.match.dto.ParticipantStats;
import net.rithms.riot.api.endpoints.match.dto.TeamBans;
import net.rithms.riot.api.endpoints.match.dto.TeamStats;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Component
public class LeagueComponent {
  private final LoLienLeagueRepository loLienLeagueRepository;
  private final LoLienSummonerRepository loLienSummonerRepository;
  private final LoLienLeagueMatchRepository loLienLeagueMatchRepository;

  /**
   * addResult.
   *
   * @param leagueIdx leagueIdx
   * @param matchId   matchId
   * @param entries   entries
   */
  public void addResult(int leagueIdx, long matchId, String[] entries) {
    LoLienLeague loLienLeague = loLienLeagueRepository
        .findById(leagueIdx)
        .orElseThrow(() -> new LeagueNotFoundException("존재하지 않는 리그 입니다."));

    for (String summonerName : entries) {
      boolean hasSummonerName = loLienSummonerRepository.existsBySummonerName(summonerName);

      if (!hasSummonerName) {
        String errorMessage = String.format(
            "Discord에서 \"!소환사 등록 %s\" 명령어로 소환사 등록을 먼저 해주시기 바랍니다.",
            summonerName);

        throw new IllegalArgumentException(errorMessage);
      }
    }

    Match match = getMatch(matchId);

    Set<LoLienLeagueParticipant> loLienLeagueParticipantSet = Sets.newHashSet();
    Set<LoLienLeagueTeamStats> loLienLeagueTeamStatsSet = Sets.newHashSet();

    LoLienLeagueMatch loLienLeagueMatch = LoLienLeagueMatch
        .builder()
        .lolienLeague(loLienLeague)
        .participants(loLienLeagueParticipantSet)
        .teams(loLienLeagueTeamStatsSet)
        .build();

    BeanUtils.copyProperties(match, loLienLeagueMatch);

    List<Participant> participants = match.getParticipants();

    for (int i = 0; i < participants.size(); i++) {
      Participant participant = participants.get(i);
      ParticipantStats stats = participant.getStats();

      LoLienLeagueParticipantStats loLienLeagueParticipantStats = LoLienLeagueParticipantStats
          .builder()
          .build();

      BeanUtils.copyProperties(stats, loLienLeagueParticipantStats);

      String summonerName = entries[i];
      LoLienSummoner bySummonerName = loLienSummonerRepository
          .findBySummonerName(summonerName);

      LoLienLeagueParticipant loLienLeagueParticipant = LoLienLeagueParticipant
          .builder()
          .match(loLienLeagueMatch)
          .stats(loLienLeagueParticipantStats)
          .loLienSummoner(bySummonerName)
          .build();

      BeanUtils.copyProperties(participant, loLienLeagueParticipant);

      loLienLeagueParticipantStats.setParticipant(loLienLeagueParticipant);

      loLienLeagueParticipantSet.add(loLienLeagueParticipant);
    }

    List<TeamStats> teams = match.getTeams();
    List<LoLienLeagueTeamBans> loLienLeagueTeamBansList = Lists.newArrayList();

    for (TeamStats teamStats : teams) {
      LoLienLeagueTeamStats loLienLeagueTeamStats = LoLienLeagueTeamStats
          .builder()
          .match(loLienLeagueMatch)
          .bans(loLienLeagueTeamBansList)
          .build();

      BeanUtils.copyProperties(teamStats, loLienLeagueTeamStats);

      List<TeamBans> bans = teamStats.getBans();

      for (TeamBans teamBans : bans) {
        int championId = teamBans.getChampionId();
        int pickTurn = teamBans.getPickTurn();

        LoLienLeagueTeamBans loLienLeagueTeamBans = LoLienLeagueTeamBans
            .builder()
            .teamStats(loLienLeagueTeamStats)
            .championId(championId)
            .pickTurn(pickTurn)
            .build();

        loLienLeagueTeamBansList.add(loLienLeagueTeamBans);
      }

      loLienLeagueTeamStatsSet.add(loLienLeagueTeamStats);
    }

    loLienLeagueMatchRepository.save(loLienLeagueMatch);

    /*for (String summonerName : entries) {
      HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();
      boolean hasHashKey = opsForHash.hasKey(REDIS_MOST_CHAMPS_KEY, summonerName);
      if (hasHashKey) {
        opsForHash.delete(REDIS_MOST_CHAMPS_KEY, summonerName);
      }
      getMostChamp(summonerName, 3);
    }*/
  }
}