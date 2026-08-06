package com.bonae.logistics.user.domain.entity;

import com.bonae.logistics.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "p_users")
public class User extends BaseEntity {
    @Id
    @Column(name = "user_id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(name = "username", unique = true, length = 10, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slack_id", nullable = false, length = 100)
    private String slackId;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder.Default
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "affiliation_name", nullable = false, length = 100)
    private String affiliationName;

    @Column(name = "hub_id")
    private UUID hubId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "approved_by", length = 10)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
