package kr.webgori.lolien.discord.bot.component;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.UserPosition;
import kr.webgori.lolien.discord.bot.entity.user.ClienUser;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.entity.user.UserRole;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.UserPositionRepository;
import kr.webgori.lolien.discord.bot.repository.user.ClienUserRepository;
import kr.webgori.lolien.discord.bot.repository.user.UserRepository;
import kr.webgori.lolien.discord.bot.repository.user.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserTransactionComponent {
  private final UserRepository userRepository;
  private final UserRoleRepository userRoleRepository;
  private final ClienUserRepository clienUserRepository;
  private final LolienSummonerRepository lolienSummonerRepository;
  private final LeagueRepository leagueRepository;
  private final UserPositionRepository userPositionRepository;

  /**
   * 회원가입.
   * @param user user
   * @param userRole userRole
   * @param clienUser clienUser
   * @param lolienSummoner lolienSummoner
   * @param leagues leagues
   */
  @Transactional
  public void register(User user, UserRole userRole, ClienUser clienUser,
                       LolienSummoner lolienSummoner, List<League> leagues,
                       List<UserPosition> userPositions) {
    clienUserRepository.save(clienUser);
    lolienSummonerRepository.save(lolienSummoner);
    leagueRepository.saveAll(leagues);
    userRepository.save(user);
    userRoleRepository.save(userRole);
    userPositionRepository.saveAll(userPositions);
  }

  /**
   * 회원 탈퇴.
   * @param user user
   * @param userRole userRole
   * @param clienUser clienUser
   * @param lolienSummoner lolienSummoner
   * @param leagues leagues
   */
  @Transactional
  public void deleteUser(User user, UserRole userRole, ClienUser clienUser,
                         LolienSummoner lolienSummoner, List<League> leagues) {
    userRoleRepository.delete(userRole);
    userRepository.delete(user);
    leagueRepository.deleteAll(leagues);
    lolienSummonerRepository.delete(lolienSummoner);
    clienUserRepository.delete(clienUser);
  }

  /**
   * 회원 정보 수정.
   * @param user user
   * @param userPositions userPositions
   */
  @Transactional
  public void alterUser(User user, List<UserPosition> userPositions) {
    LolienSummoner lolienSummoner = user.getLolienSummoner();
    leagueRepository.deleteByLolienSummoner(lolienSummoner);

    userRepository.save(user);

    userPositionRepository.deleteByLolienSummoner(lolienSummoner);
    userPositionRepository.saveAll(userPositions);
  }

  @Transactional
  public void leaveUser(User user) {
    userRepository.save(user);
  }

  @Transactional
  public void generateTempPassword(User user) {
    userRepository.save(user);
  }
}
