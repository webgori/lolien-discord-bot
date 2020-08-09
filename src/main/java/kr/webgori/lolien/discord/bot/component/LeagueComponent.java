package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getMatch;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeague;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueMatch;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueParticipant;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueParticipantStats;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueTeamBans;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueTeamStats;
import kr.webgori.lolien.discord.bot.exception.LeagueNotFoundException;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueRepository;
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
@RequiredArgsConstructor
@Component
public class LeagueComponent {
  private final LolienLeagueRepository lolienLeagueRepository;
  private final LolienSummonerRepository lolienSummonerRepository;
  private final LolienLeagueMatchRepository lolienLeagueMatchRepository;

  /**
   * addResult.
   *
   * @param leagueIdx leagueIdx
   * @param matchId   matchId
   * @param entries   entries
   */
  public void addResult(int leagueIdx, long matchId, String[] entries) {
    LolienLeague lolienLeague = lolienLeagueRepository
        .findById(leagueIdx)
        .orElseThrow(() -> new LeagueNotFoundException("존재하지 않는 리그 입니다."));

    for (String summonerName : entries) {
      boolean hasSummonerName = lolienSummonerRepository.existsBySummonerName(summonerName);

      if (!hasSummonerName) {
        String errorMessage = String.format(
            "Discord에서 \"!소환사 등록 %s\" 명령어로 소환사 등록을 먼저 해주시기 바랍니다.",
            summonerName);

        throw new IllegalArgumentException(errorMessage);
      }
    }

    Match match = getMatch(matchId);

    Set<LolienLeagueParticipant> lolienLeagueParticipantSet = Sets.newHashSet();
    Set<LolienLeagueTeamStats> lolienLeagueTeamStatsSet = Sets.newHashSet();

    LolienLeagueMatch lolienLeagueMatch = LolienLeagueMatch
        .builder()
        .lolienLeague(lolienLeague)
        .participants(lolienLeagueParticipantSet)
        .teams(lolienLeagueTeamStatsSet)
        .build();

    BeanUtils.copyProperties(match, lolienLeagueMatch);

    List<Participant> participants = match.getParticipants();

    for (int i = 0; i < participants.size(); i++) {
      Participant participant = participants.get(i);
      ParticipantStats stats = participant.getStats();

      LolienLeagueParticipantStats lolienLeagueParticipantStats = LolienLeagueParticipantStats
          .builder()
          .build();

      BeanUtils.copyProperties(stats, lolienLeagueParticipantStats);

      String summonerName = entries[i];
      LolienSummoner bySummonerName = lolienSummonerRepository
          .findBySummonerName(summonerName);

      LolienLeagueParticipant lolienLeagueParticipant = LolienLeagueParticipant
          .builder()
          .match(lolienLeagueMatch)
          .stats(lolienLeagueParticipantStats)
          .lolienSummoner(bySummonerName)
          .build();

      BeanUtils.copyProperties(participant, lolienLeagueParticipant);

      lolienLeagueParticipantStats.setParticipant(lolienLeagueParticipant);

      lolienLeagueParticipantSet.add(lolienLeagueParticipant);
    }

    List<TeamStats> teams = match.getTeams();
    List<LolienLeagueTeamBans> lolienLeagueTeamBansList = Lists.newArrayList();

    for (TeamStats teamStats : teams) {
      LolienLeagueTeamStats lolienLeagueTeamStats = LolienLeagueTeamStats
          .builder()
          .match(lolienLeagueMatch)
          .bans(lolienLeagueTeamBansList)
          .build();

      BeanUtils.copyProperties(teamStats, lolienLeagueTeamStats);

      List<TeamBans> bans = teamStats.getBans();

      for (TeamBans teamBans : bans) {
        int championId = teamBans.getChampionId();
        int pickTurn = teamBans.getPickTurn();

        LolienLeagueTeamBans lolienLeagueTeamBans = LolienLeagueTeamBans
            .builder()
            .teamStats(lolienLeagueTeamStats)
            .championId(championId)
            .pickTurn(pickTurn)
            .build();

        lolienLeagueTeamBansList.add(lolienLeagueTeamBans);
      }

      lolienLeagueTeamStatsSet.add(lolienLeagueTeamStats);
    }

    lolienLeagueMatchRepository.save(lolienLeagueMatch);

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