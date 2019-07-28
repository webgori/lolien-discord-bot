package kr.webgori.lolien.discord.bot.service;

import kr.webgori.lolien.discord.bot.request.CustomGameAddLeagueResultRequest;
import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;

public interface CustomGameService {
  void addResult(CustomGameAddResultRequest customGameAddResultRequest);

  void addLeagueResult(CustomGameAddLeagueResultRequest customGameAddLeagueResultRequest);
}