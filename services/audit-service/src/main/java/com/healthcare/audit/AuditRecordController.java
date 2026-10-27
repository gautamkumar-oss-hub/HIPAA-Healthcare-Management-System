package com.healthcare.audit;

import com.healthcare.shared.crypto.FieldEncryption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditRecordController {
    private final AuditRecordRepository records;
    private final FieldEncryption encryption;

    public AuditRecordController(AuditRecordRepository records, FieldEncryption encryption) {
        this.records = records;
        this.encryption = encryption;
    }

    @GetMapping
    public Page<AuditRecord.AuditRecordView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Invalid pagination");
        }
        Page<AuditRecord> results = records.findAllByOrderByOccurredAtDesc(
                PageRequest.of(page, Math.min(size, 100)));
        return results.map(record -> record.toView(encryption));
    }
}
