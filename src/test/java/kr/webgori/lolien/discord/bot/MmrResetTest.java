package kr.webgori.lolien.discord.bot;

import java.util.List;
import kr.webgori.lolien.discord.bot.component.CustomGameComponent;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.LolienTierMmr;
import kr.webgori.lolien.discord.bot.repository.LolienMatchRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.LolienTierMmrRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class MmrResetTest {
  @Autowired
  private LolienSummonerRepository lolienSummonerRepository;

  @Autowired
  private LolienTierMmrRepository lolienTierMmrRepository;

  @Autowired
  private LolienMatchRepository lolienMatchRepository;

  @Autowired
  private CustomGameComponent customGameComponent;

  @Test
  public void resetMmr() {
    List<LolienSummoner> lolienSummoners = lolienSummonerRepository.findAll();

    for (LolienSummoner lolienSummoner : lolienSummoners) {
      League league = lolienSummoner
          .getLeagues()
          .stream()
          .filter(l -> l.getSeason().equals("S10"))
          .findFirst()
          .orElseGet(() -> League.builder().season("S10").tier("UNRANKED").build());

      String tier = league.getTier();
      LolienTierMmr mmrFromTable = getMmrFromTable(tier);
      int mmr = mmrFromTable.getMmr();
      lolienSummoner.setMmr(mmr);
    }

    lolienSummonerRepository.saveAll(lolienSummoners);
  }

  private LolienTierMmr getMmrFromTable(String tier) {
    return lolienTierMmrRepository.findByTier(tier);
  }

  @Test
  public void applyCustomGameMmr() {
    List<LolienMatch> matches = lolienMatchRepository.findAll();
    for (LolienMatch match : matches) {
      customGameComponent.addResultMmr(match);
    }
  }
}
