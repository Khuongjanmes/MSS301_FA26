package com.fudn.orderservice;

import com.fudn.orderservice.stub.InventoryStubs;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceApplicationTests {

    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    static WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

    @LocalServerPort
    private Integer port;

    @DynamicPropertySource
    static void inventoryProperties(DynamicPropertyRegistry registry) {
        registry.add("inventory.url", wireMockServer::baseUrl);
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        wireMockServer.resetAll();
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    static {
        mySQLContainer.start();
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @Test
    void shouldSubmitOrder() {
        String submitOrderJson = """
                {
                     "skuCode": "iphone_15",
                     "price": 1000,
                     "quantity": 1
                }
                """;

        InventoryStubs.stubInventoryCall("iphone_15", 1);

        var responseBodyString = RestAssured.given()
                .contentType("application/json")
                .body(submitOrderJson)
                .when()
                .post("/api/order")
                .then()
                .log().all()
                .statusCode(201)
                .extract().body().asString();

        assertThat(responseBodyString, Matchers.is("Order Placed Successfully"));
    }
}
