package com.ar.crm2.adapter.out.persistence.agent.memory;

import com.ar.crm2.model.agent.enums.DurableMemoryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DurableMemoryRepository extends JpaRepository<DurableMemoryEntity, String> {
    Optional<DurableMemoryEntity> findByOwnerIdAndId(String ownerId, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select memory from DurableMemoryEntity memory where memory.ownerId = :ownerId and memory.id = :id")
    Optional<DurableMemoryEntity> findByOwnerIdAndIdForUpdate(
            @Param("ownerId") String ownerId,
            @Param("id") String id);

    List<DurableMemoryEntity> findByOwnerIdAndStatusAndExpiresAtAfterOrderByCreatedAtAscIdAsc(
            String ownerId, DurableMemoryStatus status, LocalDateTime now);

    List<DurableMemoryEntity> findByOwnerIdAndStatusAndExpiresAtIsNullOrderByCreatedAtAscIdAsc(
            String ownerId, DurableMemoryStatus status);

    @Modifying
    @Query("delete from DurableMemoryEntity memory where "
            + "(memory.status = :activeStatus and memory.expiresAt is not null and memory.expiresAt <= :boundary) "
            + "or (memory.status = :deletedStatus and memory.deletedAt < :boundary)")
    void deleteExpiredAndDeletedBefore(
            @Param("boundary") LocalDateTime boundary,
            @Param("activeStatus") DurableMemoryStatus activeStatus,
            @Param("deletedStatus") DurableMemoryStatus deletedStatus);
}
