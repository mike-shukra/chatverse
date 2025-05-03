package com.example.chatverse;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(servers = {
		@Server(url = "http://chatverse.local:8888", description = "Development server (via port-forward/NodePort)"),
		// Можно добавить другие серверы, например, для продакшена
		// @Server(url = "https://prod.chatverse.com", description = "Production server")
})
public class ChatverseApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatverseApplication.class, args);
	}

}
