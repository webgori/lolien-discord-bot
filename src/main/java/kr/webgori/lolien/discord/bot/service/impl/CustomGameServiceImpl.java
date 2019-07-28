package kr.webgori.lolien.discord.bot.service.impl;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendErrorMessage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Color;
import java.util.List;
import kr.webgori.lolien.discord.bot.component.CustomGameComponent;
import kr.webgori.lolien.discord.bot.repository.LoLienMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LoLienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.request.CustomGameAddLeagueResultRequest;
import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;
import kr.webgori.lolien.discord.bot.service.CustomGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.TextChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Service
public class CustomGameServiceImpl implements CustomGameService {
  private final JDA jda;
  private final LoLienMatchRepository loLienMatchRepository;
  private final CustomGameComponent customGameComponent;
  private final LoLienLeagueMatchRepository loLienLeagueMatchRepository;

  @Value("${riot.api.key}")
  private String riotApiKey;

  @Transactional
  @Override
  public void addResult(CustomGameAddResultRequest customGameAddResultRequest) {
    long matchId = customGameAddResultRequest.getMatchId();
    String entriesString = customGameAddResultRequest.getEntries();

    boolean existsByGameId = loLienMatchRepository.existsByGameId(matchId);

    List<TextChannel> textChannels = jda.getTextChannelsByName("내전-결과-data", false);
    TextChannel textChannel = textChannels.get(0);

    if (existsByGameId) {
      sendErrorMessage(textChannel, "이미 등록된 내전 결과 입니다.", Color.RED);
      return;
    }

    String[] entries = entriesString.split(",");

    if (entries.length != 10) {
      customGameComponent.sendAddResultSyntax(textChannel);
      return;
    }

    customGameComponent.addResult(textChannel, matchId, entries);
  }

  @Transactional
  @Override
  public void addLeagueResult(CustomGameAddLeagueResultRequest customGameAddLeagueResultRequest) {
    long matchId = customGameAddLeagueResultRequest.getMatchId();
    String entriesString = customGameAddLeagueResultRequest.getEntries();

    boolean existsByGameId = loLienLeagueMatchRepository.existsByGameId(matchId);

    List<TextChannel> textChannels = jda.getTextChannelsByName("내전-결과-data", false);
    TextChannel textChannel = textChannels.get(0);

    if (existsByGameId) {
      sendErrorMessage(textChannel, "이미 등록된 내전 결과 입니다.", Color.RED);
      return;
    }

    String[] entries = entriesString.split(",");

    if (entries.length != 10) {
      customGameComponent.sendAddResultSyntax(textChannel);
      return;
    }

    customGameComponent.addResult(textChannel, matchId, entries);
  }
}