package com.spiceroute.feedback;

import java.util.UUID;
import java.time.Duration;
import jakarta.servlet.http.HttpServletRequest;
import com.spiceroute.common.RateLimiter;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/feedback")
public class FeedbackController {

  private final FeedbackService service;
  private final RateLimiter rateLimiter;

  public FeedbackController(FeedbackService service, RateLimiter rateLimiter) {
    this.service = service;
    this.rateLimiter = rateLimiter;
  }

  @GetMapping("/{token}")
  Object get(@PathVariable UUID token) {
    return service.lookup(token);
  }

  @PostMapping("/{token}")
  Object submit(
    @PathVariable UUID token,
    @RequestBody FeedbackService.Submission submission,
    HttpServletRequest request
  ) {
    rateLimiter.enforce("feedback-network", request.getRemoteAddr(), 300, Duration.ofHours(1));
    rateLimiter.enforce("feedback-table", token.toString(), 30, Duration.ofHours(1));
    return service.submit(token, submission);
  }
}
