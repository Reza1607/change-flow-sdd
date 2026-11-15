package com.changeflow.identity.infrastructure;

import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name="identity_account")
class IdentityAccountEntity {
    @Id @Column(name="actor_id",nullable=false,updatable=false) UUID actorId;
    @Column(nullable=false,unique=true,columnDefinition="text") String username;
    @Column(name="password_hash",nullable=false,columnDefinition="text") String passwordHash;
    @Column(nullable=false) boolean enabled;
    @Column(nullable=false) boolean requester;
    @Column(nullable=false) boolean reviewer;
    @Version @Column(name="access_version",nullable=false) long accessVersion;
    protected IdentityAccountEntity() {}
}
