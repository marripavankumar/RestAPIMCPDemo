package com.restmcp.demo;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RestApiIntegrationTest {

    static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {
    };

    @LocalServerPort
    int port;

    RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.builder().baseUrl("http://localhost:" + port + "/api/v1").build();
    }

    @Test
    void createTransferSummarizeAndGuardedDelete() {
        var ops = http.post().uri("/departments").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("code", "ops", "name", "Operations", "location", "Chennai"))
                .retrieve().toEntity(JSON);
        assertThat(ops.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(ops.getHeaders().getLocation()).isNotNull();
        Number opsId = (Number) ops.getBody().get("id");
        assertThat(ops.getBody()).containsEntry("code", "OPS");

        var hire = http.post().uri("/employees").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("firstName", "Lena", "lastName", "Paul", "email", "lena.paul@example.com",
                        "jobTitle", "Ops Lead", "salary", 120000, "hireDate", "2026-01-05",
                        "departmentId", opsId))
                .retrieve().body(JSON);
        Number hireId = (Number) hire.get("id");

        Number finId = (Number) http.get().uri("/departments").retrieve()
                .body(new ParameterizedTypeReference<java.util.List<Map<String, Object>>>() {
                }).stream().filter(d -> "FIN".equals(d.get("code"))).findFirst().orElseThrow().get("id");
        var finBefore = http.get().uri("/departments/{id}/summary", finId).retrieve().body(JSON);

        var moved = http.put().uri("/employees/{id}/department/{d}", hireId, finId).retrieve().body(JSON);
        assertThat(((Map<?, ?>) moved.get("department")).get("code")).isEqualTo("FIN");

        var finAfter = http.get().uri("/departments/{id}/summary", finId).retrieve().body(JSON);
        assertThat(((Number) finAfter.get("headcount")).longValue())
                .isEqualTo(((Number) finBefore.get("headcount")).longValue() + 1);

        assertThatThrownBy(() -> http.delete().uri("/departments/{id}", finId).retrieve().toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.Conflict.class)
                .hasMessageContaining("Transfer them before deleting");

        var deleted = http.delete().uri("/departments/{id}", opsId).retrieve().toBodilessEntity();
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void searchIsPagedAndFiltered() {
        var page = http.get().uri("/employees?name=rao&size=5").retrieve().body(JSON);

        assertThat(page).containsEntry("totalElements", 1).containsEntry("size", 5);
    }

    @Test
    void unknownEmployeeIsProblemDetail() {
        assertThatThrownBy(() -> http.get().uri("/employees/999999").retrieve().body(JSON))
                .isInstanceOf(HttpClientErrorException.NotFound.class)
                .hasMessageContaining("Employee 999999 not found");
    }
}
