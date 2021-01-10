package kr.webgori.lolien.discord.bot.quartz;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@Slf4j
@Component
class UpdateTierJob implements Job {
  @Override
  public void execute(JobExecutionContext context) {

  }
}
