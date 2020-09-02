package kr.webgori.lolien.discord.bot.service;

import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;
import kr.webgori.lolien.discord.bot.response.CustomGamesResponse;
import kr.webgori.lolien.discord.bot.response.CustomGamesStatisticsResponse;

public interface CustomGameService {
  void addResult(CustomGameAddResultRequest customGameAddResultRequest);

  CustomGamesResponse getCustomGames(int page, int size);

  CustomGamesResponse getCustomGamesBySummoner(String summonerName, int page, int size);

  CustomGamesStatisticsResponse getStatistics();
}
