package kr.webgori.lolien.discord.bot.service.impl;

import kr.webgori.lolien.discord.bot.exception.SummonerNotFoundException;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.service.SummonerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class SummonerServiceImpl implements SummonerService {
  private final LolienSummonerRepository lolienSummonerRepository;

  @Override
  public void existSummonerBySummonerName(String summonerName) {
    boolean existsBySummonerName = lolienSummonerRepository.existsBySummonerName(summonerName);

    if (!existsBySummonerName) {
      throw new SummonerNotFoundException("");
    }
  }
}
