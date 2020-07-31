package kr.webgori.lolien.discord.bot.quartz;

import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class QuartzConfig {
  /**
   * jobADetails.
   *
   * @return JobDetail
   */
  @Bean
  public JobDetail updateTierJobADetails() {
    return JobBuilder
            .newJob(UpdateTierJob.class)
            .withIdentity("UpdateTierJob")
            .storeDurably()
            .build();
  }

  /**
   * jobATrigger.
   *
   * @param updateTierJobADetails jobADetails
   * @return Trigger
   */
  @Bean
  public Trigger updateTierJobATrigger(JobDetail updateTierJobADetails) {
    return TriggerBuilder
            .newTrigger()
            .forJob(updateTierJobADetails)
            .withIdentity("UpdateTierTrigger")
            .withSchedule(
                    CronScheduleBuilder.cronSchedule("0 0 * ? * *"))
            .build();
  }
}