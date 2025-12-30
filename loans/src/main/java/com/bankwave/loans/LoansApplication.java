package com.bankwave.loans;

import com.bankwave.loans.dto.LoansContactInfoDto;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl")
@EnableConfigurationProperties(value = {LoansContactInfoDto.class})
@OpenAPIDefinition(
		info = @Info(
				title = "Loans microservice REST API Documentation",
				description = "BankWave Loans microservice REST API Documentation",
				version = "v1",
				contact = @Contact(
						name = "Themba Ngobeni",
						email = "thembatman0@gmail.com",
						url = "https://github.com/thembatman0"
				),
				license = @License(
						name = "Apache 2.0",
						url = "https://github.com/thembatman0"
				)
		),
		externalDocs = @ExternalDocumentation(
				description = "BankWave Loans microservice REST API Documentation",
				url = "https://github.com/thembatman0"
		)
)
public class LoansApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoansApplication.class, args);
	}
}
