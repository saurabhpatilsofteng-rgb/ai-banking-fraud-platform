
package com.example.bankingbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bankingbackend.entity.Transaction;
import com.example.bankingbackend.entity.TransactionStatus;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    @Query("""
        SELECT t
        FROM Transaction t
        JOIN t.fromAccount fa
        JOIN fa.user fu
        LEFT JOIN t.toAccount ta
        LEFT JOIN ta.user tu
        WHERE fu.email = :email
           OR tu.email = :email
        ORDER BY t.createdAt DESC
        """)
    List<Transaction> findVisibleToUser(
            @Param("email") String email
    );
@Query("""
    SELECT t
    FROM Transaction t
    JOIN t.fromAccount fa
    JOIN fa.user fu
    LEFT JOIN t.toAccount ta
    LEFT JOIN ta.user tu
    WHERE t.id = :transactionId
      AND (fu.email = :email OR tu.email = :email)
    """)
Optional<Transaction> findVisibleTransaction(
    @Param("transactionId") Long transactionId,
    @Param("email") String email
);


    List<Transaction> findByStatus(TransactionStatus status);
}
