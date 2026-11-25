package com.spiceroute.management;

import com.spiceroute.security.AccessService;
import com.spiceroute.security.AuthService;
import com.spiceroute.security.UserAccount;
import com.spiceroute.security.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ManagementService {

    private final RestaurantRepository restaurants;
    private final BranchRepository branches;
    private final DiningTableRepository tables;
    private final BranchAssignmentRepository assignments;
    private final UserRepository users;
    private final AccessService access;
    private final AuthService auth;

    public ManagementService(RestaurantRepository restaurants, BranchRepository branches,
                             DiningTableRepository tables, BranchAssignmentRepository assignments,
                             UserRepository users, AccessService access, AuthService auth) {
        this.restaurants = restaurants;
        this.branches = branches;
        this.tables = tables;
        this.assignments = assignments;
        this.users = users;
        this.access = access;
        this.auth = auth;
    }

    public record Entry(String name, String address, UUID restaurantId, UUID branchId,
                        String email, String password) {}

    public record Update(String name, String address, Boolean active, Boolean archived) {}

    public Object directory(AuthService.Actor actor) {
        access.owner(actor);
        return Map.of("restaurants", restaurants.findByOwnerIdOrderByNameAsc(actor.id()).stream()
                .map(this::restaurantView).toList(),
            "branches", branches.findByRestaurant_OwnerIdOrderByNameAsc(actor.id()).stream()
                .map(this::branchView).toList());
    }

    private Restaurant ownedRestaurant(AuthService.Actor actor, UUID id) {
        access.owner(actor);
        Restaurant restaurant = restaurants.findById(id).orElseThrow(this::notFound);
        if (!restaurant.getOwnerId().equals(actor.id())) throw notFound();
        return restaurant;
    }

    private Branch ownedBranch(AuthService.Actor actor, UUID id) {
        access.owner(actor);
        Branch branch = branches.findById(id).orElseThrow(this::notFound);
        if (!branch.getRestaurant().getOwnerId().equals(actor.id())) throw notFound();
        return branch;
    }

    @Transactional
    public void updateRestaurant(AuthService.Actor actor, UUID id, Update update) {
        Restaurant restaurant = ownedRestaurant(actor, id);
        if (update.name() != null) restaurant.rename(name(update.name()));
        if (update.archived() != null) restaurant.setArchived(update.archived());
    }

    public record Branding(String welcomeMessage, String thankYouMessage, Boolean collectContact) {}

    @Transactional
    public void customizeFeedback(AuthService.Actor actor, UUID restaurantId, Branding branding) {
        Restaurant restaurant = ownedRestaurant(actor, restaurantId);
        if (branding.collectContact() == null) throw new IllegalArgumentException("Choose a contact preference");
        restaurant.customizeFeedback(guestMessage(branding.welcomeMessage()),
            guestMessage(branding.thankYouMessage()), branding.collectContact());
    }

    private String guestMessage(String value) {
        if (value == null) return "";
        if (value.strip().length() > 240) throw new IllegalArgumentException("Guest messages must be at most 240 characters");
        return value.strip();
    }

    @Transactional
    public void changeBranch(AuthService.Actor actor, UUID id, Update update) {
        Branch branch = ownedBranch(actor, id);
        if (update.name() != null || update.address() != null) {
            branch.update(update.name() == null ? branch.getName() : name(update.name()),
                update.address() == null ? branch.getAddress() : address(update.address()));
        }
        if (update.archived() != null) branch.setArchived(update.archived());
    }

    @Transactional
    public void updateTable(AuthService.Actor actor, UUID id, Update update) {
        access.table(actor, id);
        DiningTable table = tables.findById(id).orElseThrow(this::notFound);
        if (update.name() != null) table.rename(name(update.name()));
        if (update.active() != null) table.setActive(update.active());
    }

    @Transactional
    public void rotateQr(AuthService.Actor actor, UUID id) {
        access.table(actor, id);
        tables.findById(id).orElseThrow(this::notFound).rotateToken();
    }

    private UserAccount ownedManager(AuthService.Actor actor, UUID id) {
        access.owner(actor);
        UserAccount manager = users.lockById(id).orElseThrow(this::notFound);
        var assigned = assignments.findBranchIds(id);
        if (!"MANAGER".equals(manager.getRole()) || assigned.isEmpty()
                || !branches.findOwnedBranchIds(actor.id()).containsAll(assigned)) throw notFound();
        return manager;
    }

    @Transactional
    public void updateManager(AuthService.Actor actor, UUID id, Update update) {
        UserAccount manager = ownedManager(actor, id);
        if (update.name() != null) manager.rename(name(update.name()));
        if (update.active() != null) {
            manager.setOwnerSuspended(!update.active());
            if (!update.active()) auth.revokeSessions(id);
        }
    }

    @Transactional
    public void removeAssignment(AuthService.Actor actor, UUID managerId, UUID branchId) {
        ownedManager(actor, managerId);
        ownedBranch(actor, branchId);
        var assigned = assignments.findBranchIds(managerId);
        if (!assigned.contains(branchId)) throw notFound();
        if (assigned.size() == 1) throw new IllegalArgumentException(
            "Assign another branch first, or suspend this manager instead.");
        assignments.deleteById(new BranchAssignment.Key(managerId, branchId));
    }

    public Object overview(AuthService.Actor actor) {
        return Map.of(
            "restaurants", restaurants.findAccessible(actor.id()).stream().map(this::restaurantView).toList(),
            "branches", branches.findAccessible(actor.id()).stream().map(this::branchView).toList()
        );
    }

    @Transactional
    public Object restaurant(AuthService.Actor actor, Entry entry) {
        access.owner(actor);
        Restaurant restaurant = new Restaurant(UUID.randomUUID(), name(entry.name()), actor.id());
        return Map.of("id", restaurants.save(restaurant).getId());
    }

    @Transactional
    public Object branch(AuthService.Actor actor, Entry entry) {
        access.restaurant(actor, entry.restaurantId());
        Restaurant restaurant = restaurants.getReferenceById(entry.restaurantId());
        Branch branch = new Branch(UUID.randomUUID(), restaurant, name(entry.name()), address(entry.address()));
        return Map.of("id", branches.save(branch).getId());
    }

    @Transactional
    public Object table(AuthService.Actor actor, Entry entry) {
        access.branch(actor, entry.branchId());
        DiningTable table = new DiningTable(UUID.randomUUID(), branches.getReferenceById(entry.branchId()),
            name(entry.name()), UUID.randomUUID());
        return Map.of("id", tables.save(table).getId());
    }

    public Object tables(AuthService.Actor actor, UUID branchId) {
        access.branch(actor, branchId);
        return tables.findByBranch_IdOrderByLabelAsc(branchId).stream().map(this::tableView).toList();
    }

    @Transactional
    public void toggle(AuthService.Actor actor, UUID tableId) {
        access.table(actor, tableId);
        tables.findById(tableId).orElseThrow(this::notFound).toggleActive();
    }

    @Transactional
    public Object manager(AuthService.Actor actor, Entry entry) {
        access.owner(actor);
        access.branch(actor, entry.branchId());
        UUID userId = auth.createUser(new AuthService.Credentials(entry.email(), entry.password(), entry.name()), "MANAGER");
        assignments.save(new BranchAssignment(userId, entry.branchId()));
        return Map.of("id", userId);
    }

    public Object managers(AuthService.Actor actor, UUID branchId) {
        access.owner(actor);
        access.branch(actor, branchId);
        return users.findBranchManagers(branchId).stream().map(this::managerView).toList();
    }

    @Transactional
    public void assignManager(AuthService.Actor actor, UUID managerId, UUID branchId) {
        access.owner(actor);
        access.branch(actor, branchId);
        ownedManager(actor, managerId);
        assignments.save(new BranchAssignment(managerId, branchId));
    }

    public Object admin(AuthService.Actor actor) {
        access.admin(actor);
        var accounts = users.findAllByOrderByNameAsc();
        var emailById = new java.util.HashMap<UUID, String>();
        accounts.forEach(user -> emailById.put(user.getId(), user.getEmail()));
        return Map.of(
            "restaurants", restaurants.findAllByOrderByNameAsc().stream().map(restaurant -> Map.of(
                "id", restaurant.getId(), "name", restaurant.getName(), "active", restaurant.isActive(),
                "email", emailById.getOrDefault(restaurant.getOwnerId(), "")
            )).toList(),
            "users", accounts.stream().map(this::userView).toList()
        );
    }

    @Transactional
    public void adminToggle(AuthService.Actor actor, String type, UUID id) {
        access.admin(actor);
        if ("restaurants".equals(type)) {
            restaurants.findById(id).orElseThrow(this::notFound).toggleActive();
        } else if ("users".equals(type) && !id.equals(actor.id())) {
            users.findById(id).orElseThrow(this::notFound).toggleActive();
        } else {
            throw new IllegalArgumentException("Invalid target");
        }
    }

    private Map<String, Object> restaurantView(Restaurant restaurant) {
        return Map.of("id", restaurant.getId(), "name", restaurant.getName(), "active", restaurant.isActive(), "archived", restaurant.isArchived(),
            "welcome_message", restaurant.getWelcomeMessage(), "thank_you_message", restaurant.getThankYouMessage(),
            "collect_contact", restaurant.isCollectContact());
    }

    private Map<String, Object> branchView(Branch branch) {
        return Map.of("id", branch.getId(), "name", branch.getName(), "address", branch.getAddress(),
            "restaurant_id", branch.getRestaurant().getId(), "archived", branch.isArchived());
    }

    private Map<String, Object> tableView(DiningTable table) {
        return Map.of("id", table.getId(), "label", table.getLabel(), "token", table.getToken(),
            "active", table.isActive(), "branch_id", table.getBranch().getId());
    }

    private Map<String, Object> managerView(UserAccount user) {
        return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(),
            "active", !user.isOwnerSuspended(), "platform_active", user.isActive(), "role", user.getRole());
    }

    private Map<String, Object> userView(UserAccount user) {
        return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(),
            "active", user.isActive(), "role", user.getRole());
    }

    private String name(String value) {
        if (value == null || value.isBlank() || value.length() > 120) {
            throw new IllegalArgumentException("Enter a name up to 120 characters");
        }
        return value.trim();
    }

    private String address(String value) {
        if (value == null) return "";
        if (value.length() > 300) throw new IllegalArgumentException("Address is too long");
        return value.trim();
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    }
}
