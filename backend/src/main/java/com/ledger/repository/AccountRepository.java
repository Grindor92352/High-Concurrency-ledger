package com.ledger.repository;

import com.ledger.entity.Account;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Eagerly fetches user details along with the account to prevent LazyInitializationException.
     */
    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Account> findById(Long id);

    @EntityGraph(attributePaths = "user")
    Optional<Account> findByAccountNumber(String accountNumber);

    @EntityGraph(attributePaths = "user")
    List<Account> findByUser_Username(String username);

    @Override
    @EntityGraph(attributePaths = "user")
    List<Account> findAll();

    /**
     * Locks the account row using DB-level SELECT ... FOR UPDATE (PESSIMISTIC_WRITE).
     * Prevents race conditions and double spending during concurrent transactions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
