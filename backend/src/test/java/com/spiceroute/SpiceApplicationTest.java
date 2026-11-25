package com.spiceroute;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.spiceroute.security.AuthService;
import com.spiceroute.management.ManagementService;
import com.spiceroute.management.DiningTableRepository;
import com.spiceroute.feedback.FeedbackService;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SpiceApplicationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired AuthService auth;
    @Autowired ManagementService management;
    @Autowired DiningTableRepository tables;
    @Autowired FeedbackService feedback;

    @Test
    void migrationsAndRepositoriesLoad() {}

    @Test
    void archiveAndRestorePreserveQrAndRejectForeignOwner() {
        UUID ownerId = auth.createUser(new AuthService.Credentials(
            UUID.randomUUID() + "@example.test", "ExamplePass123!", "Owner"), "OWNER");
        var owner = new AuthService.Actor(ownerId, "OWNER", "Owner");
        var foreign = new AuthService.Actor(UUID.randomUUID(), "OWNER", "Other");
        UUID restaurant = id(management.restaurant(owner,
            new ManagementService.Entry("Restaurant", null, null, null, null, null)));
        UUID branch = id(management.branch(owner,
            new ManagementService.Entry("Branch", "Address", restaurant, null, null, null)));
        UUID table = id(management.table(owner,
            new ManagementService.Entry("Table 1", null, null, branch, null, null)));
        UUID token = tables.findById(table).orElseThrow().getToken();
        assertThrows(ResponseStatusException.class, () -> management.changeBranch(foreign, branch,
            new ManagementService.Update("Foreign rename", null, null, null)));
        management.changeBranch(owner, branch, new ManagementService.Update(null, null, null, true));
        assertThrows(ResponseStatusException.class, () -> feedback.lookup(token));
        management.changeBranch(owner, branch, new ManagementService.Update("Renamed", null, null, false));
        assertEquals("Renamed", feedback.lookup(token).get("branch"));
        management.updateRestaurant(owner, restaurant, new ManagementService.Update(null, null, null, true));
        assertThrows(ResponseStatusException.class, () -> feedback.lookup(token));
        management.updateRestaurant(owner, restaurant, new ManagementService.Update(null, null, null, false));
        assertEquals(table, feedback.lookup(token).get("id"));
        management.rotateQr(owner, table);
        assertThrows(ResponseStatusException.class, () -> feedback.lookup(token));
        assertEquals(table, feedback.lookup(tables.findById(table).orElseThrow().getToken()).get("id"));
    }

    private UUID id(Object response) {
        return (UUID) ((Map<?, ?>) response).get("id");
    }
}
