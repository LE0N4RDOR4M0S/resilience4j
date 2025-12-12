package com.leonardoramos.resilience4j;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResilienceIntegrationTest {
    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();
    @Autowired
    private TestRestTemplate testRestTemplate;
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Redireciona a URL do serviço para o WireMock dinâmico
        registry.add("inventory.service.url", wireMockServer::baseUrl);
        // Ajusta configs para teste (janelas menores)
        registry.add("resilience4j.circuitbreaker.instances.inventoryService.slidingWindowSize", () -> "5");
        registry.add("resilience4j.circuitbreaker.instances.inventoryService.minimumNumberOfCalls", () -> "5");
    }
    @Test
    void testCircuitBreakerOpensAndFallbackIsCalled() {
        System.out.println("🔴 INICIANDO TESTE DE CAOS COM WIREMOCK 🔴");

        // 1. Simular Erro 500 no serviço externo
        wireMockServer.stubFor(WireMock.get(WireMock.urlMatching("/api/.*"))
                .willReturn(WireMock.aResponse().withStatus(500)));

        System.out.println("Step 1: WireMock configurado para retornar HTTP 500 em todas as chamadas.");

        // 2. Disparar chamadas para atingir o threshold
        System.out.println("Step 2: Disparando 5 chamadas para forçar erros...");
        for (int i = 1; i <= 5; i++) {
            String response = testRestTemplate.getForObject("/api/products/123/async", String.class);
            assertThat(response).contains("Fallback");
            System.out.println("   Tentativa " + i + ": Falhou (WireMock 500) -> Resposta da API: " + response);
        }

        // 3. A 6ª chamada deve ser bloqueada pelo Circuit Breaker
        System.out.println("Step 3: Resetando WireMock. A próxima chamada NÃO deve bater na rede.");
        wireMockServer.resetRequests();

        String responseOpen = testRestTemplate.getForObject("/api/products/123/async", String.class);
        assertThat(responseOpen).contains("Fallback");

        System.out.println("Step 4: Chamada 6 realizada (Circuito Aberto) -> Resposta da API: " + responseOpen);

        // 4. Verificação
        wireMockServer.verify(0, WireMock.getRequestedFor(WireMock.urlMatching("/api/.*")));
        System.out.println("✅ SUCESSO: O WireMock confirmou que recebeu 0 requisições na fase de Circuito Aberto!");
    }
}
