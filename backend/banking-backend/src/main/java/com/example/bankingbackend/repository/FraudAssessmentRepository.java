
package com.example.bankingbackend.repository;

import com.example.bankingbackend.entity.FraudAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FraudAssessmentRepository
        extends JpaRepository<FraudAssessment, Long> {

    @Query("""
        SELECT fa
        FROM FraudAssessment fa
        JOIN fa.transaction t
        JOIN t.fromAccount faAccount
        JOIN faAccount.user fromUser
        LEFT JOIN t.toAccount ta
        LEFT JOIN ta.user toUser
        WHERE fromUser.email = :email
           OR toUser.email = :email
        ORDER BY fa.assessedAt DESC
        """)
    List<FraudAssessment> findMyAssessments(
            @Param("email") String email);

    List<FraudAssessment>
        findByReviewRecommendedTrueOrderByAssessedAtDesc();
}
