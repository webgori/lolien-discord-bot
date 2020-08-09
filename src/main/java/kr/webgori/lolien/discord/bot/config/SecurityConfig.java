package kr.webgori.lolien.discord.bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import kr.webgori.lolien.discord.bot.jwt.JwtAuthenticationFilter;
import kr.webgori.lolien.discord.bot.jwt.JwtLoginFilter;
import kr.webgori.lolien.discord.bot.jwt.TokenAuthenticationService;
import kr.webgori.lolien.discord.bot.service.LolienService;
import kr.webgori.lolien.discord.bot.spring.AuthenticationProviderImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Slf4j
@RequiredArgsConstructor
@Configuration
@Order(SecurityProperties.BASIC_AUTH_ORDER)
@EnableGlobalMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  private final TokenAuthenticationService tokenService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final LolienService lolienService;
  private final ObjectMapper objectMapper;

  @Override
  protected AuthenticationManager authenticationManager() {
    return new ProviderManager(Collections
        .singletonList(new AuthenticationProviderImpl(redisTemplate, lolienService, objectMapper)));
  }

  @Bean
  public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.addAllowedOrigin("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return new CorsFilter(source);
  }

  @Override
  public void configure(WebSecurity web) {
    web
        .ignoring()
        .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "v1/custom-game/result");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http.headers().cacheControl();

    http
        .csrf()
        .disable()
        .authorizeRequests()
        .antMatchers("/**")
        .hasAuthority("ROLE_USER")
        .anyRequest()
        .authenticated()
        .and()
        .httpBasic().disable()
        .addFilterBefore(new JwtLoginFilter("/v1/users/login", authenticationManager(),
            tokenService, lolienService, objectMapper), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new JwtAuthenticationFilter(tokenService),
            UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling();
  }
}
