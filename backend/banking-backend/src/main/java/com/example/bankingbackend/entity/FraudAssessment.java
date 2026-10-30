package com.example.bankingbackend.entity;




import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fraud_assessments")
public class FraudAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private double riskScore;

    @Column(nullable = false)
    private String riskLevel;

    @Column(nullable = false)
    private boolean reviewRecommended;

    @Column(nullable = false)
    private String modelType;

    @Column(nullable = false)
    private LocalDateTime assessedAt = LocalDateTime.now();

    public FraudAssessment() {}

    public Long getId() { return id; }

    public Transaction getTransaction() { return transaction; }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public double getRiskScore() { return riskScore; }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() { return riskLevel; }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isReviewRecommended() {
        return reviewRecommended;
    }

    public void setReviewRecommended(boolean reviewRecommended) {
        this.reviewRecommended = reviewRecommended;
    }

    public String getModelType() { return modelType; }

    public void setModelType(String modelType) {
        this.modelType = modelType;
    }

    public LocalDateTime getAssessedAt() { return assessedAt; }
}

