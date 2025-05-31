package com.example.annotationPlatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //.csrf(AbstractHttpConfigurer::disable)  // Désactivé pour Thymeleaf forms, activez-le dans un environnement de production
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                                .requestMatchers("/login", "/error").permitAll()
                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                .requestMatchers("/annotator/**").hasRole("ANNOTATOR")
                                .anyRequest().authenticated()
                )
                .formLogin(form ->
                        form
                                .loginPage("/login")
                                .defaultSuccessUrl("/dashboard", true)
                                .failureUrl("/login?error=true")
                                .permitAll()
                )
                .logout(logout ->
                        logout
                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                .logoutSuccessUrl("/login?logout=true")
                                .invalidateHttpSession(true)
                                .deleteCookies("JSESSIONID")
                                .permitAll()
                )
                .rememberMe(remember ->
                        remember
                                .key("uniqueAndSecretKey")
                                .tokenValiditySeconds(86400) // 24 heures
                )
                .sessionManagement(session ->
                        session
                                .maximumSessions(1)  // Un utilisateur ne peut avoir qu'une session active
                                .expiredUrl("/login?expired=true")
                ).csrf(csrf -> csrf
                        // S'assurer que le CSRF est activé pour toutes les requêtes sauf /logout si nécessaire
                        .ignoringRequestMatchers("/logout")
                );;

        return http.build();
    }
}