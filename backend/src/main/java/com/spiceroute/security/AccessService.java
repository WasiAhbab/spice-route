package com.spiceroute.security;

import com.spiceroute.management.BranchRepository;
import com.spiceroute.management.DiningTableRepository;
import com.spiceroute.management.RestaurantRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessService {

    private final RestaurantRepository restaurants;
    private final BranchRepository branches;
    private final DiningTableRepository tables;

    public AccessService(RestaurantRepository restaurants, BranchRepository branches,
                         DiningTableRepository tables) {
        this.restaurants = restaurants;
        this.branches = branches;
        this.tables = tables;
    }

    public void owner(AuthService.Actor actor) {
        if (!"OWNER".equals(actor.role())) throw forbidden();
    }

    public void admin(AuthService.Actor actor) {
        if (!"ADMIN".equals(actor.role())) throw forbidden();
    }

    public void restaurant(AuthService.Actor actor, UUID id) {
        owner(actor);
        if (id == null || !restaurants.existsByIdAndOwnerIdAndActiveTrueAndArchivedFalse(id, actor.id())) {
            throw forbidden();
        }
    }

    public void branch(AuthService.Actor actor, UUID id) {
        if (id == null || !java.util.Set.of("OWNER", "MANAGER").contains(actor.role())
                || branches.countAccessible(id, actor.id()) == 0) {
            throw forbidden();
        }
    }

    public void table(AuthService.Actor actor, UUID id) {
        if (id == null) throw forbidden();
        branch(actor, tables.findBranchId(id).orElseThrow(this::forbidden));
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }
}
