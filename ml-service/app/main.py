
# Path helps us locate files and folders.
from pathlib import Path

# Literal restricts a field to specific allowed values, such as 0 or 1.
from typing import Literal

# joblib loads the trained machine learning model from a file.
import joblib

# FastAPI creates REST APIs.
# HTTPException returns an HTTP error response.
from fastapi import FastAPI, HTTPException

# BaseModel defines the structure of API request and response data.
# Field adds validation rules to individual fields.
from pydantic import BaseModel, Field


# --------------------------------------------------
# 1. DEFINE THE MODEL FILE PATH
# --------------------------------------------------

# Get the directory containing this Python file's parent folder.
# Example:
# Project/
# ├── models/
# │   └── fraud_model.joblib
# └── backend/
#     └── main.py
#
# If main.py is inside Project/backend/app/main.py,
# parent.parent points to Project/backend.
BASE_DIR = Path(__file__).resolve().parent.parent

# Build the path to the saved machine learning model.
MODEL_PATH = BASE_DIR / "models" / "fraud_model.joblib"


# --------------------------------------------------
# 2. CREATE THE FASTAPI APPLICATION
# --------------------------------------------------

# Create the API application.
# These details appear in the automatic API documentation.
app = FastAPI(
    title="Banking Fraud Detection API",
    version="1.0.0",
    description="Demo fraud risk prediction using synthetic training data.",
)


# --------------------------------------------------
# 3. LOAD THE TRAINED MACHINE LEARNING MODEL
# --------------------------------------------------

# Load the model artifact saved by the training script.
# The file contains a dictionary with the model and feature names.
artifact = joblib.load(MODEL_PATH)

# Extract the trained Random Forest model.
model = artifact["model"]

# Extract the feature names and their expected order.
FEATURES = artifact["features"]


# --------------------------------------------------
# 4. VALIDATE THE INPUT TRANSACTION
# --------------------------------------------------

# This class defines which transaction details the API accepts.
# FastAPI uses it to validate incoming JSON automatically.
class TransactionFeatures(BaseModel):

    # Amount must be greater than 0 and at most 100 million.
    amount: float = Field(gt=0, le=100_000_000)

    # Transaction hour must be between 0 and 23.
    # Example: 14 means 2 PM.
    hour: int = Field(ge=0, le=23)

    # Number of transactions in the previous hour.
    # The accepted range is 0 to 1000.
    transactions_last_hour: int = Field(ge=0, le=1000)

    # 0 means an existing/known device; 1 means a new device.
    # Only 0 or 1 is accepted.
    new_device: Literal[0, 1]

    # 0 means domestic; 1 means international.
    # Only 0 or 1 is accepted.
    international: Literal[0, 1]


# --------------------------------------------------
# 5. DEFINE THE API RESPONSE FORMAT
# --------------------------------------------------

# This class defines the JSON structure returned by /predict.
class FraudPrediction(BaseModel):

    # Estimated fraud probability expressed as a percentage.
    risk_score: float

    # Risk category: LOW, MEDIUM, or HIGH.
    risk_level: str

    # True means the transaction is recommended for review.
    review_recommended: bool

    # Identifies the model used for this prediction.
    model_type: str


# --------------------------------------------------
# 6. CREATE A HEALTH-CHECK ENDPOINT
# --------------------------------------------------

# GET /health checks whether the API is running.
@app.get("/health")
def health():

    # Return the API status and whether the model is loaded.
    return {
        "status": "UP",
        "model_loaded": model is not None,
    }


# --------------------------------------------------
# 7. CREATE THE FRAUD PREDICTION ENDPOINT
# --------------------------------------------------

# POST /predict accepts transaction details as JSON.
# response_model validates the response structure.
@app.post("/predict", response_model=FraudPrediction)
def predict(transaction: TransactionFeatures):

    try:
        # Convert the incoming transaction into one row of features.
        # The order must match the order used during model training.
        row = [[
            transaction.amount,
            transaction.hour,
            transaction.transactions_last_hour,
            transaction.new_device,
            transaction.international,
        ]]

        # Ask the trained model for probabilities.
        # [0] selects the first transaction.
        # [1] selects the probability of the fraud class (class 1).
        fraud_probability = float(
            model.predict_proba(row)[0][1]
        )

        # Convert the probability into a percentage.
        # Example: 0.82 becomes 82.0%.
        risk_score = round(fraud_probability * 100, 2)

        # Assign a risk category using example thresholds.
        if risk_score >= 70:
            risk_level = "HIGH"

        elif risk_score >= 30:
            risk_level = "MEDIUM"

        else:
            risk_level = "LOW"

        # Return the result as a validated JSON response.
        return FraudPrediction(
            risk_score=risk_score,
            risk_level=risk_level,

            # Recommend review only when the category is HIGH.
            review_recommended=(risk_level == "HIGH"),

            # Identify this as the demonstration model.
            model_type="synthetic_demo_random_forest",
        )

    except Exception as exc:
        # Return HTTP 500 if an unexpected prediction error occurs.
        # Keep internal exception details out of the API response.
        raise HTTPException(
            status_code=500,
            detail="Fraud prediction failed",
        ) from exc
