package com.shiviishiv7.matchmaking;

import com.shiviishiv7.matchmaking.common.enums.Gender;
import com.shiviishiv7.matchmaking.common.enums.UserStatus;
import com.shiviishiv7.matchmaking.provider.implementation.UserRepository;
import com.shiviishiv7.matchmaking.provider.model.Company;
import com.shiviishiv7.matchmaking.provider.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;


@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
public class MatchmakingApplication extends SpringBootServletInitializer  {
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(MatchmakingApplication.class);
	}
	public static void main(String[] args) {
		SpringApplication.run(MatchmakingApplication.class, args);
	}
}