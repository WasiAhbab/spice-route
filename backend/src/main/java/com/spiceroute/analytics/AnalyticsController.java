package com.spiceroute.analytics;

import com.spiceroute.security.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

  private final AnalyticsService analytics;
  private final SummaryService summaries;

  public AnalyticsController(
    AnalyticsService analytics,
    SummaryService summaries
  ) {
    this.analytics = analytics;
    this.summaries = summaries;
  }

  @GetMapping("/feedback")
  Object feedback(
    @AuthenticationPrincipal AuthService.Actor actor,
    @RequestParam UUID branch,
    @RequestParam LocalDate from,
    @RequestParam LocalDate to,
    @RequestParam(required = false) Integer rating,
    @RequestParam(required = false) UUID table
  ) {
    return analytics.feedback(actor, branch, from, to, rating, table);
  }

  @PatchMapping("/feedback/{id}/review")
  Object review(@AuthenticationPrincipal AuthService.Actor actor, @RequestParam UUID branch,
                @PathVariable UUID id, @RequestBody AnalyticsService.Review entry) {
    analytics.review(actor, branch, id, entry);
    return java.util.Map.of("ok", true);
  }

  @GetMapping("/comparison")
  Object compare(@AuthenticationPrincipal AuthService.Actor actor) {
    return analytics.comparison(actor);
  }

  @PostMapping("/summary")
  Object summary(
    @AuthenticationPrincipal AuthService.Actor actor,
    @RequestParam UUID branch,
    @RequestParam LocalDate from,
    @RequestParam LocalDate to
  ) throws Exception {
    return summaries.generate(actor, branch, from, to);
  }
}
