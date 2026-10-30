
package com.example.bankingbackend.controller;

import com.example.bankingbackend.controller.dto.FraudAssessmentResponse;
import com.example.bankingbackend.service.FraudAssessmentService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fraud")
public class FraudController {

    private final FraudAssessmentService fraudAssessmentService;

    public FraudController(
            FraudAssessmentService fraudAssessmentService) {
        this.fraudAssessmentService = fraudAssessmentService;
    }

    @PostMapping("/transactions/{transactionId}/assess")
    public FraudAssessmentResponse assessTransaction(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            throw new IllegalStateException(
                    "Authenticated user was not provided");
        }

        System.out.println("Authenticated user: " + user.getUsername());
        System.out.println("Transaction ID: " + transactionId);

        return fraudAssessmentService.assessTransaction(
                transactionId,
                user.getUsername()
        );
    }

    @GetMapping("/my-assessments")
    public List<FraudAssessmentResponse> getMyAssessments(
            @AuthenticationPrincipal UserDetails user) {

        return fraudAssessmentService.getMyAssessments(
                user.getUsername());
    }

    @GetMapping("/review-queue")
    @PreAuthorize("hasRole('FRAUD_REVIEWER') or hasRole('ADMIN')")
    public List<FraudAssessmentResponse> getReviewQueue() {
        return fraudAssessmentService.getReviewQueue();
    }
}
