package com.nila.chores.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuditRepository extends JpaRepository<UserAuditEntity, Long> {
}
