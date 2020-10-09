package kr.webgori.lolien.discord.bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import kr.webgori.lolien.discord.bot.component.AuthenticationComponent;
import kr.webgori.lolien.discord.bot.jwt.JwtAuthenticationFilter;
import kr.webgori.lolien.discord.bot.jwt.JwtLoginFilter;
import kr.webgori.lolien.discord.bot.jwt.TokenAuthenticationService;
import kr.webgori.lolien.discord.bot.repository.user.UserRepository;
import kr.webgori.lolien.discord.bot.spring.AuthenticationProviderImpl;
import kr.webgori.lolien.discord.bot.spring.CustomAccessDeniedHandler;
import kr.webgori.lolien.discord.bot.spring.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
@RequiredArgsConstructor
@Configuration
@Order(SecurityProperties.BASIC_AUTH_ORDER)
@EnableGlobalMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  private final TokenAuthenticationService tokenService;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final RequestMappingHandlerMapping requestMappingHandlerMapping;
  private final AuthenticationComponent authenticationComponent;

  @Override
  protected AuthenticationManager authenticationManager() {
    return new ProviderManager(Collections
        .singletonList(new AuthenticationProviderImpl(userRepository, passwordEncoder,
            authenticationComponent)));
  }

  /**
   * corsConfigurationSource.
   * @return CorsConfigurationSource
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowCredentials(true);
    configuration.addAllowedOrigin("*");
    configuration.addAllowedHeader("*");
    configuration.addAllowedMethod("*");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  @Override
  public void configure(WebSecurity web) {
    web
        .ignoring()
        .antMatchers("/v3/api-docs/**", "/swagger-ui/**")
        .antMatchers("/v*/users/register", "/v*/users/access-token", "/v*/users/logout")
        .antMatchers("/v*/summoners/**", "/v*/custom-game/**", "/v*/leagues/**");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.headers().cacheControl();

    http
        .csrf()
        .disable()
        .exceptionHandling()
        .authenticationEntryPoint(customAuthenticationEntryPoint)
        .accessDeniedHandler(customAccessDeniedHandler())
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .antMatchers("/**")
        .hasAuthority("USER")
        .anyRequest()
        .authenticated()
        .and()
        .httpBasic().disable()
        .addFilterBefore(new JwtLoginFilter("/v1/users/login", authenticationManager(),
            tokenService, objectMapper), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new JwtAuthenticationFilter(tokenService),
            UsernamePasswordAuthenticationFilter.class);
  }

  @Bean
  public CustomAccessDeniedHandler customAccessDeniedHandler() {
    return new CustomAccessDeniedHandler(requestMappingHandlerMapping);
  }
}
