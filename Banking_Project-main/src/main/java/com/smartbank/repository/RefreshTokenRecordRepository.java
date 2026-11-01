
package com.smartbank.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.smartbank.entity.RefreshTokenRecord;
import java.util.List;

@Repository
public interface RefreshTokenRecordRepository
        extends JpaRepository<RefreshTokenRecord, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r
            from RefreshTokenRecord r
            where r.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenRecord> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash);

    @Modifying
    @Query("""
            update RefreshTokenRecord r
            set r.revokedAt = CURRENT_TIMESTAMP
            where r.user.id = :userId and r.revokedAt is null
            """)
    int revokeAllActiveTokensForUser(@Param("userId") Long userId);

    List<RefreshTokenRecord> findByUserId(Long userId);
}