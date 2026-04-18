package com.retailpulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String status;

    private String details;

    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    protected AuditLogEntity() {}

    public AuditLogEntity(String actor, String action, String status, String ipAddress, LocalDateTime timestamp) {
        this.actor = actor;
        this.action = action;
        this.status = status;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }
}
