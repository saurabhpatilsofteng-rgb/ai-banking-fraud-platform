
# Path helps us work with file and folder paths.
from pathlib import Path

# joblib saves our trained machine learning model into a file.
import joblib

# pandas reads and processes CSV data in table format.
import pandas as pd

# RandomForestClassifier is the machine learning algorithm.
from sklearn.ensemble import RandomForestClassifier

# Splits the dataset into training data and testing data.
from sklearn.model_selection import train_test_split

# Measures model performance using a report and ROC-AUC score.
from sklearn.metrics import classification_report, roc_auc_score


# --------------------------------------------------
# 1. DEFINE PROJECT FILE PATHS
# --------------------------------------------------

# Get the absolute path of the folder containing this Python file.
# Example: C:\ai-banking-fraud-platform\backend
BASE_DIR = Path(__file__).resolve().parent

# Path to the CSV file containing transaction data.
# The CSV should be in the same folder as train_model.py.
DATA_PATH = BASE_DIR / "historical_transactions_demo.csv"

# Folder where the trained model will be stored.
MODEL_DIR = BASE_DIR / "models"

# Final path of the saved machine learning model.
MODEL_PATH = MODEL_DIR / "fraud_model.joblib"


# --------------------------------------------------
# 2. DEFINE INPUT FEATURES AND TARGET
# --------------------------------------------------

# Features are the transaction details used by the model
# to learn whether a transaction might be fraudulent.
FEATURES = [
    "amount",                  # Transaction amount
    "hour",                    # Hour when transaction occurred
    "transactions_last_hour",  # Number of recent transactions
    "new_device",              # 1 = new device, 0 = known device
    "international",           # 1 = international, 0 = domestic
]

# Target is the answer the model must learn to predict.
# 0 = legitimate transaction
# 1 = fraudulent transaction
TARGET = "is_fraud"


# --------------------------------------------------
# 3. LOAD AND VALIDATE THE CSV DATA
# --------------------------------------------------

def load_data():
    """Read the CSV and check whether its data is valid."""

    # Check whether the CSV file exists.
    if not DATA_PATH.exists():
        # Stop execution and display an error if the file is missing.
        raise FileNotFoundError(
            f"CSV file not found: {DATA_PATH}"
        )

    # Read the CSV file into a pandas DataFrame (a table).
    df = pd.read_csv(DATA_PATH)

    # Combine all required input columns and the target column.
    required_columns = FEATURES + [TARGET]

    # Find required columns that are missing from the CSV.
    missing_columns = [
        col for col in required_columns
        if col not in df.columns
    ]

    # Stop if any required column is missing.
    if missing_columns:
        raise ValueError(
            f"Missing CSV columns: {missing_columns}"
        )

    # Convert required columns into numeric values.
    # Invalid text becomes NaN (a missing/invalid value).
    df[required_columns] = df[required_columns].apply(
        pd.to_numeric, errors="coerce"
    )

    # Check whether any required value is missing or invalid.
    if df[required_columns].isnull().any().any():
        raise ValueError(
            "CSV contains missing or non-numeric values."
        )

    # Make sure the fraud label contains only 0 or 1.
    if not df[TARGET].isin([0, 1]).all():
        raise ValueError(
            "is_fraud must contain only 0 or 1."
        )

    # Confirm the dataset contains both fraud and legitimate examples.
    # nunique() counts the number of distinct target values.
    if df[TARGET].nunique() != 2:
        raise ValueError(
            "Dataset must contain both fraud and "
            "legitimate transactions."
        )

    # Check that these two features contain only 0 or 1.
    for col in ["new_device", "international"]:
        if not df[col].isin([0, 1]).all():
            raise ValueError(
                f"{col} must contain only 0 or 1."
            )

    # X contains the input features used to make predictions.
    X = df[FEATURES]

    # y contains the correct answers the model learns to predict.
    # astype(int) converts the target values to integers.
    y = df[TARGET].astype(int)

    # Return both the input data and the target labels.
    return X, y


# --------------------------------------------------
# 4. MAIN PROGRAM
# --------------------------------------------------

def main():

    # Create the models folder if it does not exist.
    # exist_ok=True prevents an error if the folder already exists.
    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    # Call load_data() to read and validate the dataset.
    # X = transaction details; y = fraud labels.
    X, y = load_data()

    # Display basic information about the dataset.
    print("Dataset loaded successfully.")
    print("Total transactions:", len(X))

    # Count how many legitimate (0) and fraud (1) records exist.
    print("\nClass distribution:")
    print(y.value_counts().sort_index())

    # --------------------------------------------------
    # 5. SPLIT DATA INTO TRAINING AND TESTING SETS
    # --------------------------------------------------

    # Training data teaches the model.
    # Testing data checks how the model performs on unseen records.
    X_train, X_test, y_train, y_test = train_test_split(
        X,                  # Input features
        y,                  # Correct labels
        test_size=0.2,      # Use 20% for testing, 80% for training
        random_state=42,    # Makes the split repeatable
        stratify=y,         # Preserves the class proportions
    )

    # Show how many records are in each set.
    print("\nTraining rows:", len(X_train))
    print("Testing rows:", len(X_test))

    # --------------------------------------------------
    # 6. CREATE THE RANDOM FOREST MODEL
    # --------------------------------------------------

    # Random Forest combines many decision trees to make predictions.
    model = RandomForestClassifier(
        n_estimators=150,       # Build 150 decision trees
        max_depth=10,           # Limit each tree's depth
        class_weight="balanced",# Give more weight to less common classes
        random_state=42,        # Makes training more repeatable
    )

    # --------------------------------------------------
    # 7. TRAIN THE MODEL
    # --------------------------------------------------

    # Teach the model using training features and correct labels.
    model.fit(X_train, y_train)

    # Training has finished.
    print("\nModel training completed.")

    # --------------------------------------------------
    # 8. PREDICT FRAUD ON TEST DATA
    # --------------------------------------------------

    # Predict a class for each test transaction:
    # 0 = legitimate, 1 = fraud.
    predictions = model.predict(X_test)

    # Get the model's estimated probability of fraud for each record.
    # [:, 1] selects the probability for class 1 (fraud).
    probabilities = model.predict_proba(X_test)[:, 1]

    # --------------------------------------------------
    # 9. EVALUATE MODEL PERFORMANCE
    # --------------------------------------------------

    # Print precision, recall, F1-score, and support for each class.
    print("\nClassification Report:")
    print(
        classification_report(
            y_test,                   # Actual answers
            predictions,              # Model predictions
            labels=[0, 1],             # Evaluate both classes
            target_names=["Legitimate", "Fraud"],
            zero_division=0,           # Avoid division-by-zero warnings
        )
    )

    # ROC-AUC needs both classes in the test data.
    if y_test.nunique() == 2:

        # Measure how well the model ranks fraud above legitimate cases.
        # A score closer to 1 is generally better.
        auc = roc_auc_score(y_test, probabilities)

        # Display the score rounded to four decimal places.
        print("ROC-AUC:", round(auc, 4))

    else:
        # The score cannot be calculated when test data has one class.
        print("ROC-AUC unavailable: test set has one class.")

    # --------------------------------------------------
    # 10. SAVE THE TRAINED MODEL
    # --------------------------------------------------

    # Save the model and its feature names into one file.
    # Another Python program or API can load this file later.
    joblib.dump(
        {
            "model": model,      # Trained Random Forest model
            "features": FEATURES # Required feature names and order
        },
        MODEL_PATH,              # Where the file will be saved
    )

    # Display the location of the saved model.
    print(f"\nModel saved successfully: {MODEL_PATH}")


# Run main() only when this file is executed directly.
# Importing this file from another Python file will not automatically
# start the training process.
if __name__ == "__main__":
    main()
