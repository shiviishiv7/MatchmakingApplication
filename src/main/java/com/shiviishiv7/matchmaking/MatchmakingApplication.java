package com.shiviishiv7.matchmaking;

import com.shiviishiv7.matchmaking.scheduler.PostMatchingScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableJpaAuditing
public class MatchmakingApplication extends SpringBootServletInitializer  implements CommandLineRunner {
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(MatchmakingApplication.class);
	}
	public static void main(String[] args) {
		SpringApplication.run(MatchmakingApplication.class, args);
	}

	@Autowired
	PostMatchingScheduler postMatchingScheduler;

	@Override
	public void run(String... args) throws Exception {
	//	postMatchingScheduler.findAndScoreMatches();
	}
}