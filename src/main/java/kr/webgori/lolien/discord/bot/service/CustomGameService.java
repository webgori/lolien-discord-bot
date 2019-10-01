package kr.webgori.lolien.discord.bot.service;

import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;
import kr.webgori.lolien.discord.bot.response.CustomGamesResponse;

public interface CustomGameService {
  void addResult(CustomGameAddResultRequest customGameAddResultRequest);

  CustomGamesResponse getCustomGames();
}