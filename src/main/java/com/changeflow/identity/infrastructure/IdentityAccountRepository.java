package com.changeflow.identity.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface IdentityAccountRepository extends JpaRepository<IdentityAccountEntity,UUID> {
    Optional<IdentityAccountEntity> findByUsername(String username);
}
