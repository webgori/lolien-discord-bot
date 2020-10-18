package kr.webgori.lolien.discord.bot.component;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.user.ClienUser;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.entity.user.UserRole;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.user.ClienUserRepository;
import kr.webgori.lolien.discord.bot.repository.user.UserRepository;
import kr.webgori.lolien.discord.bot.repository.user.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserTransactionComponent {
  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final ClienUserRepository clienUserRepository;
  private final LolienSummonerRepository lolienSummonerRepository;
  private final LeagueRepository leagueRepository;

  /**
   * 회원가입.
   * @param user user
   * @param userRole userRole
   * @param clienUser clienUser
   * @param lolienSummoner lolienSummoner
   * @param leagues leagues
   */
  public void register(User user, UserRole userRole, ClienUser clienUser,
                       LolienSummoner lolienSummoner, List<League> leagues) {
    userRepository.save(user);
    userRoleRepository.save(userRole);
    clienUserRepository.save(clienUser);
    lolienSummonerRepository.save(lolienSummoner);
    leagueRepository.saveAll(leagues);
  }
}
