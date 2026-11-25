package com.spiceroute.security;

import com.spiceroute.management.BranchRepository;
import com.spiceroute.management.DiningTableRepository;
import com.spiceroute.management.RestaurantRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccessServiceTest {
    private final RestaurantRepository restaurants = mock(RestaurantRepository.class);
    private final BranchRepository branches = mock(BranchRepository.class);
    private final DiningTableRepository tables = mock(DiningTableRepository.class);
    private final AccessService access = new AccessService(restaurants, branches, tables);

    @Test
    void rejectsUnassignedManager() {
        var actor = new AuthService.Actor(UUID.randomUUID(), "MANAGER", "Manager");
        var branch = UUID.randomUUID();
        assertThrows(ResponseStatusException.class, () -> access.branch(actor, branch));
    }

    @Test
    void permitsAssignedManager() {
        var actor = new AuthService.Actor(UUID.randomUUID(), "MANAGER", "Manager");
        var branch = UUID.randomUUID();
        when(branches.countAccessible(branch, actor.id())).thenReturn(1L);
        assertDoesNotThrow(() -> access.branch(actor, branch));
    }

    @Test
    void managerCannotPerformOwnerOperations() {
        var actor = new AuthService.Actor(UUID.randomUUID(), "MANAGER", "Manager");
        assertThrows(ResponseStatusException.class, () -> access.owner(actor));
    }
}
