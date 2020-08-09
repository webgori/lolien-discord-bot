package kr.webgori.lolien.discord.bot.service;

import javax.servlet.http.HttpServletResponse;
import kr.webgori.lolien.discord.bot.request.LolienUserAddSummonerRequest;
import kr.webgori.lolien.discord.bot.spring.AuthenticationTokenImpl;

public interface LolienService {
  void checkLogin(String id, String password);

  void login(AuthenticationTokenImpl authTokenImpl, HttpServletResponse response);

  void addSummoner(LolienUserAddSummonerRequest request);

  void deleteSummoner();
}
