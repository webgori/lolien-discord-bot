package kr.webgori.lolien.discord.bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;
import kr.webgori.lolien.discord.bot.spring.CustomLocalDateTimeDeserializer;
import kr.webgori.lolien.discord.bot.spring.CustomLocalDateTimeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RootConfig {
  private static final String YML_PROPERTY_PATH_WINDOWS = "c:\\config.yml";
  private static final String YML_PROPERTY_PATH_LINUX = "/usr/local/tomcat/conf/config.yml";

  /**
   * webMvcConfigurer.
   *
   * @return webMvcConfigurer
   */
  @Bean
  public WebMvcConfigurer webMvcConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedHeaders("*")
            .allowedMethods("*")
            .allowCredentials(false)
            .maxAge(3600);
      }
    };
  }

  /**
   * restTemplate.
   *
   * @return RestTemplate
   */
  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getMessageConverters()
        .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    return restTemplate;
  }

  /**
   * objectMapper.
   * @return objectMapper
   */
  @Bean
  public ObjectMapper objectMapper() {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
    javaTimeModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());

    return Jackson2ObjectMapperBuilder
        .json()
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .modules(javaTimeModule, new Jdk8Module())
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * properties.
   * @return PropertySourcesPlaceholderConfigurer
   */
  @Bean
  public PropertySourcesPlaceholderConfigurer properties() {
    YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
    String ymlPropertyFilePath = getYmlPropertyFilePath();
    FileSystemResource fileSystemResource = new FileSystemResource(ymlPropertyFilePath);
    yamlPropertiesFactoryBean.setResources(fileSystemResource);

    Properties properties = yamlPropertiesFactoryBean.getObject();

    if (Objects.isNull(properties)) {
      throw new IllegalArgumentException("");
    }

    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setProperties(properties);

    return configurer;
  }

  private String getYmlPropertyFilePath() {
    String osName = System.getProperty("os.name");

    if (osName.startsWith("Windows")) {
      return YML_PROPERTY_PATH_WINDOWS;
    } else {
      return YML_PROPERTY_PATH_LINUX;
    }
  }

  @Bean
  public JavaMailSender getJavaMailSender(
      @Value("${spring.mail.host}") String mailHost,
      @Value("${spring.mail.port}") int mailPort,
      @Value("${spring.mail.username}") String mailUsername,
      @Value("${spring.mail.password}") String mailPassword,
      @Value("${spring.mail.properties.mail.smtp.auth}") boolean smtpAuth,
      @Value("${spring.mail.properties.mail.smtp.starttls.enable}") boolean starttlsEnable) {

    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(mailHost);
    mailSender.setPort(mailPort);

    mailSender.setUsername(mailUsername);
    mailSender.setPassword(mailPassword);

    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", smtpAuth);
    props.put("mail.smtp.starttls.enable", starttlsEnable);
    props.put("mail.debug", "true");

    return mailSender;
  }
}
