package com.spiceroute.management;

import com.spiceroute.security.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final QrService qr;

    public QrController(QrService qr) {
            this.qr = qr;
    }

    @GetMapping(value = "/table/{id}", produces = "image/png")
    byte[] table(@AuthenticationPrincipal AuthService.Actor actor, @PathVariable UUID id) throws Exception {
        return qr.table(actor, id);
    }

    @GetMapping(value = "/branch/{id}", produces = "application/pdf")
    ResponseEntity<byte[]> branch(@AuthenticationPrincipal AuthService.Actor actor, @PathVariable UUID id) throws Exception {
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=table-codes.pdf")
            .body(qr.branch(actor, id));
    }
}
