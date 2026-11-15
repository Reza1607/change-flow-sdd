package com.changeflow.changerequest.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ChangeRequestRepository extends JpaRepository<ChangeRequestEntity,UUID> {}
