package kr.webgori.lolien.discord.bot.service;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getEndDateOfYear;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getStartDateOfYear;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.component.LeagueComponent;
import kr.webgori.lolien.discord.bot.dto.league.LeagueDto;
import kr.webgori.lolien.discord.bot.dto.league.SummonerForParticipationDto;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeague;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueRepository;
import kr.webgori.lolien.discord.bot.request.LeagueAddRequest;
import kr.webgori.lolien.discord.bot.request.LeagueAddResultRequest;
import kr.webgori.lolien.discord.bot.response.league.LeagueResponse;
import kr.webgori.lolien.discord.bot.response.league.SummonerForParticipationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeagueService {
  private final LolienLeagueRepository lolienLeagueRepository;
  private final LeagueComponent leagueComponent;
  private final LolienLeagueMatchRepository lolienLeagueMatchRepository;
  private final CustomGameService customGameService;

  /**
   * getLeagues.
   * @return Leagues
   */
  @Transactional(readOnly = true)
  public LeagueResponse getLeagues() {
    List<LolienLeague> lolienLeagues = lolienLeagueRepository.findAll();
    List<LeagueDto> leagues = Lists.newArrayList();

    for (LolienLeague lolienLeague : lolienLeagues) {
      int idx = lolienLeague.getIdx();
      String title = lolienLeague.getTitle();
      LocalDateTime createdDate = lolienLeague.getCreatedDate();

      LeagueDto leagueResponse = LeagueDto
          .builder()
          .idx(idx)
          .title(title)
          .createdDate(createdDate)
          .build();

      leagues.add(leagueResponse);
    }

    return LeagueResponse
        .builder()
        .leagues(leagues)
        .build();
  }

  /**
   * addLeague.
   * @param leagueAddRequest leagueAddRequest
   */
  @Transactional
  public void addLeague(LeagueAddRequest leagueAddRequest) {
    String title = leagueAddRequest.getTitle();
    LolienLeague lolienLeague = LolienLeague.builder().title(title).build();
    lolienLeagueRepository.save(lolienLeague);
  }

  @Transactional
  public void deleteLeague(int leagueIdx) {
    lolienLeagueRepository.deleteById(leagueIdx);
  }

  /**
   * addLeagueResult.
   * @param leagueAddResultRequest leagueAddResultRequest
   */
  @Transactional
  public void addLeagueResult(LeagueAddResultRequest leagueAddResultRequest) {
    long matchId = leagueAddResultRequest.getMatchId();

    boolean existsByGameId = lolienLeagueMatchRepository.existsByGameId(matchId);

    if (existsByGameId) {
      throw new IllegalArgumentException("이미 등록된 리그 결과 입니다.");
    }

    String entriesString = leagueAddResultRequest.getEntries();

    String[] entries = entriesString.split(",");

    if (entries.length != 10) {
      throw new IllegalArgumentException("게임 참여 인원이 잘못 되었습니다.");
    }

    int leagueIdx = leagueAddResultRequest.getLeagueIdx();

    leagueComponent.addResult(leagueIdx, matchId, entries);
  }

  /**
   * getSummonersForParticipation.
   * @return SummonerForParticipationResponse
   */
  @Transactional(readOnly = true)
  public SummonerForParticipationResponse getSummonersForParticipation() {
    LocalDate startDateOfYear = getStartDateOfYear();
    LocalDate endDateOfYear = getEndDateOfYear();

    List<LolienMatch> lolienMatches = customGameService
        .getLolienMatches(startDateOfYear, endDateOfYear);

    List<SummonerForParticipationDto> summonersForParticipationDto =
        getSummonersForParticipationDto(lolienMatches);

    return SummonerForParticipationResponse
        .builder()
        .summoners(summonersForParticipationDto)
        .build();
  }

  private List<SummonerForParticipationDto> getSummonersForParticipationDto(
      List<LolienMatch> lolienMatches) {

    List<SummonerForParticipationDto> summonersForParticipationDto = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        String summonerName = participant.getLolienSummoner().getSummonerName();

        SummonerForParticipationDto summonerForParticipationDto = summonersForParticipationDto
            .stream()
            .filter(mb -> mb.getSummonerName().equals(summonerName))
            .findFirst()
            .orElse(null);

        if (Objects.isNull(summonerForParticipationDto)) {
          summonerForParticipationDto = SummonerForParticipationDto
              .builder()
              .summonerName(summonerName)
              .numberOfParticipation(1)
              .build();

          summonersForParticipationDto.add(summonerForParticipationDto);
        } else {
          summonerForParticipationDto.increaseNumberOfParticipation();
        }
      }
    }

    return summonersForParticipationDto
        .stream()
        .filter(s -> s.getNumberOfParticipation() >= 10)
        .collect(Collectors.toList());
  }
}
