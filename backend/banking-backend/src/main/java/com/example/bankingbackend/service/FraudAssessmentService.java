
package com.example.bankingbackend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import com.example.bankingbackend.controller.dto.FraudAssessmentResponse;
import com.example.bankingbackend.controller.dto.FraudPredictionRequest;
import com.example.bankingbackend.controller.dto.FraudPredictionResponse;
import com.example.bankingbackend.entity.FraudAssessment;
import com.example.bankingbackend.entity.Transaction;
import com.example.bankingbackend.repository.FraudAssessmentRepository;
import com.example.bankingbackend.repository.TransactionRepository;

@Service
public class FraudAssessmentService {

    private final TransactionRepository transactionRepository;
    private final FraudAssessmentRepository assessmentRepository;
    private final RestTemplate restTemplate;
    private final String fraudServiceUrl;

    public FraudAssessmentService(
            TransactionRepository transactionRepository,
            FraudAssessmentRepository assessmentRepository,
            RestTemplate fraudRestTemplate,
            @Value("${app.fraud-service.base-url}") String fraudServiceUrl) {

        this.transactionRepository = transactionRepository;
        this.assessmentRepository = assessmentRepository;
        this.restTemplate = fraudRestTemplate;
        this.fraudServiceUrl = fraudServiceUrl;
    }

    @Transactional
    public FraudAssessmentResponse assessTransaction(
            Long transactionId,
            String email) {

        Transaction transaction = transactionRepository
                .findVisibleTransaction(transactionId, email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Transaction not found"));

        FraudPredictionRequest request = new FraudPredictionRequest(
                transaction.getAmount().doubleValue(),
                transaction.getCreatedAt().getHour(),
                0, // transactionsLastHour: demo value
                0, // newDevice: demo value
                0  // international: demo value
        );

        FraudPredictionResponse prediction;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<FraudPredictionRequest> entity =
                    new HttpEntity<>(request, headers);

            System.out.println("Fraud request DTO: " + request);
            System.out.println("Fraud service URL: "
                    + fraudServiceUrl + "/predict");

            prediction = restTemplate.postForObject(
                    fraudServiceUrl + "/predict",
                    entity,
                    FraudPredictionResponse.class
            );

        } catch (RestClientException ex) {

            if (ex instanceof HttpStatusCodeException httpEx) {
                System.err.println("FastAPI status: "
                        + httpEx.getStatusCode());
                System.err.println("FastAPI error body: "
                        + httpEx.getResponseBodyAsString());
            } else {
                System.err.println("Fraud service error: "
                        + ex.getMessage());
            }

            throw new IllegalStateException(
                    "Fraud prediction service request failed", ex);
        }

        if (prediction == null
                || !Double.isFinite(prediction.risk_score())
                || prediction.risk_score() < 0
                || prediction.risk_score() > 100
                || prediction.risk_level() == null
                || prediction.model_type() == null) {

            throw new IllegalStateException(
                    "Invalid response from fraud prediction service");
        }

        FraudAssessment assessment = new FraudAssessment();
        assessment.setTransaction(transaction);
        assessment.setRiskScore(prediction.risk_score());
        assessment.setRiskLevel(prediction.risk_level());
        assessment.setReviewRecommended(
                prediction.review_recommended());
        assessment.setModelType(prediction.model_type());

        FraudAssessment saved = assessmentRepository.save(assessment);

        return new FraudAssessmentResponse(
                saved.getId(),
                transaction.getId(),
                saved.getRiskScore(),
                saved.getRiskLevel(),
                saved.isReviewRecommended(),
                saved.getModelType(),
                saved.getAssessedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<FraudAssessmentResponse> getMyAssessments(String email) {
        return assessmentRepository.findMyAssessments(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FraudAssessmentResponse> getReviewQueue() {
        return assessmentRepository
                .findByReviewRecommendedTrueOrderByAssessedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FraudAssessmentResponse toResponse(
            FraudAssessment assessment) {

        return new FraudAssessmentResponse(
                assessment.getId(),
                assessment.getTransaction().getId(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                assessment.isReviewRecommended(),
                assessment.getModelType(),
                assessment.getAssessedAt()
        );
    }
}
