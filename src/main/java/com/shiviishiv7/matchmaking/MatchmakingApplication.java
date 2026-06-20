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
public class MatchmakingApplication extends SpringBootServletInitializer implements CommandLineRunner {
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(MatchmakingApplication.class);
	}
	public static void main(String[] args) {
		SpringApplication.run(MatchmakingApplication.class, args);
	}

	@Autowired
	UserRepository repository;

	@Override
	public void run(String... args) throws Exception {
		User user = new User();
		user.setCognitoSub("51638dda-a0b1-70e4-2b7c-9d30746999f7");
		user.setEmail("shiviishiv7@gmail.com");
		user.setFirstName("Shiv");
		user.setLastName("User");
//		user.setBio("Test User");
		user.setGender(Gender.MALE);
//		user.setIndustry("Software");
//		user.setTimezone("Asia/Kolkata");
		Company c = new Company();
//		c.setId(String.randomUUID());
//		user.setCompanyId(String.randomUUID().toString());
		user.setStatus(UserStatus.MATCHED);
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
//		this.repository.save(user);
		 user = new User();
		user.setCognitoSub("c1739d4a-a0a1-70c2-dce1-59e4a8a60a35");
		user.setEmail("peneno4903@dosbee.com");
		user.setFirstName("abc");
		user.setLastName("User");
//		user.setBio("Test User");
		user.setGender(Gender.MALE);
//		user.setIndustry("Software");
//		user.setTimezone("Asia/Kolkata");
		 c = new Company();
//		c.setId(String.randomUUID());
//		user.setCompanyId(String.randomUUID().toString());
		user.setStatus(UserStatus.MATCHED);
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
//		this.repository.save(user);
	}
}