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
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RootConfig {
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
  public static PropertySourcesPlaceholderConfigurer properties() {
    YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
    FileSystemResource fileSystemResource = new FileSystemResource("c:\\application-dev.yml");
    yamlPropertiesFactoryBean.setResources(fileSystemResource);

    Properties properties = yamlPropertiesFactoryBean.getObject();

    if (Objects.isNull(properties)) {
      throw new IllegalArgumentException("");
    }

    PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
    configurer.setProperties(properties);

    return configurer;
  }
}
