package com.spiceroute.management;

import com.spiceroute.security.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manage")
public class ManagementController {

  private final ManagementService service;

  public ManagementController(ManagementService service) {
    this.service = service;
  }

  @GetMapping
  Object overview(@AuthenticationPrincipal AuthService.Actor actor) {
    return service.overview(actor);
  }

  @PostMapping("/restaurants")
  Object restaurant(
    @AuthenticationPrincipal AuthService.Actor actor,
    @RequestBody ManagementService.Entry e
  ) {
    return service.restaurant(actor, e);
  }

  @PostMapping("/branches")
  Object branch(
    @AuthenticationPrincipal AuthService.Actor actor,
    @RequestBody ManagementService.Entry e
  ) {
    return service.branch(actor, e);
  }

  @PostMapping("/tables")
  Object table(
    @AuthenticationPrincipal AuthService.Actor actor,
    @RequestBody ManagementService.Entry e
  ) {
    return service.table(actor, e);
  }

  @GetMapping("/tables/{id}")
  Object tables(
    @AuthenticationPrincipal AuthService.Actor actor,
    @PathVariable UUID id
  ) {
    return service.tables(actor, id);
  }

  @PatchMapping("/tables/{id}")
  Object toggle(
    @AuthenticationPrincipal AuthService.Actor actor,
    @PathVariable UUID id,
    @RequestBody(required = false) ManagementService.Update update
  ) {
    if (update == null) service.toggle(actor, id);
    else service.updateTable(actor, id, update);
    return Map.of("ok", true);
  }

  @PostMapping("/managers")
  Object manager(
    @AuthenticationPrincipal AuthService.Actor actor,
    @RequestBody ManagementService.Entry e
  ) {
    return service.manager(actor, e);
  }

  @GetMapping("/managers/{id}")
  Object managers(
    @AuthenticationPrincipal AuthService.Actor actor,
    @PathVariable UUID id
  ) {
    return service.managers(actor, id);
  }

  @GetMapping("/admin")
  Object admin(@AuthenticationPrincipal AuthService.Actor actor) {
    return service.admin(actor);
  }

  @PatchMapping("/admin/{type}/{id}")
  Object adminToggle(
    @AuthenticationPrincipal AuthService.Actor actor,
    @PathVariable String type,
    @PathVariable UUID id
  ) {
    service.adminToggle(actor, type, id);
    return Map.of("ok", true);
  }
  @PatchMapping("/restaurants/{id}")
  Object renameRestaurant(@AuthenticationPrincipal AuthService.Actor actor, @PathVariable UUID id,
                          @RequestBody ManagementService.Update entry) {
    service.updateRestaurant(actor, id, entry);
    return Map.of("ok", true);
  }

  @PatchMapping("/branches/{id}")
  Object updateBranch(@AuthenticationPrincipal AuthService.Actor actor, @PathVariable UUID id,
                      @RequestBody ManagementService.Update entry) {
    service.changeBranch(actor, id, entry);
    return Map.of("ok", true);
  }

  @PatchMapping("/restaurants/{id}/branding")
  Object branding(@AuthenticationPrincipal AuthService.Actor actor, @PathVariable UUID id,
                  @RequestBody ManagementService.Branding entry) {
    service.customizeFeedback(actor, id, entry);
    return Map.of("ok", true);
  }

  @GetMapping("/directory")
  Object directory(@AuthenticationPrincipal AuthService.Actor actor) {
    return service.directory(actor);
  }

  @PostMapping("/tables/{id}/rotate-qr")
  Object rotateQr(@AuthenticationPrincipal AuthService.Actor actor, @PathVariable UUID id) {
    service.rotateQr(actor, id);
    return Map.of("ok", true);
  }

  @PatchMapping("/managers/{id}")
  Object updateManager(@AuthenticationPrincipal AuthService.Actor actor, @PathVariable UUID id,
                       @RequestBody ManagementService.Update update) {
    service.updateManager(actor, id, update);
    return Map.of("ok", true);
  }

  @DeleteMapping("/managers/{managerId}/branches/{branchId}")
  Object removeManager(@AuthenticationPrincipal AuthService.Actor actor, @PathVariable UUID managerId,
                       @PathVariable UUID branchId) {
    service.removeAssignment(actor, managerId, branchId);
    return Map.of("ok", true);
  }

  @PostMapping("/managers/{managerId}/branches/{branchId}")
  Object assignManager(@AuthenticationPrincipal AuthService.Actor actor, @PathVariable UUID managerId,
                       @PathVariable UUID branchId) {
    service.assignManager(actor, managerId, branchId);
    return Map.of("ok", true);
  }
}
