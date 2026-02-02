import requests
import pandas as pd

from sklearn.model_selection import train_test_split
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

BASE_URL = "http://localhost:8080"
DATASET_URL = f"{BASE_URL}/ml/dataset?monthsBack=24&stepDays=5"

JWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjaGFyYmVseW1uQGdtYWlsLmNvbSIsImlhdCI6MTc3MDAyMDk0OSwiZXhwIjoxNzcwMTA3MzQ5fQ.OIbuaTrwrI5GEMXXVZtnmDl15f_d028_s-z6SF_QXqo"
HEADERS = {
    "Authorization": f"Bearer {JWT}"
}

FEATURES = [
    "dayRatio",
    "expensesSoFar",
    "avgDailyExpense",
    "maxExpenseSoFar",
    "nexpenseTx",
]

MODEL = Pipeline([
    ("scaler", StandardScaler()),
    ("ridge", Ridge(alpha=1.0))
])

def fetch_dataset() -> pd.DataFrame:
    r = requests.get(DATASET_URL, headers=HEADERS)
    r.raise_for_status()
    return pd.DataFrame(r.json())

def train_and_predict(df: pd.DataFrame):

    df["dayRatio"] = pd.to_numeric(df["day"], errors="coerce") / pd.to_numeric(df["daysInMonth"], errors="coerce")

    for col in FEATURES + ["finalMonthExpenses"]:
        df[col] = pd.to_numeric(df[col], errors="coerce")

    train_df = df[
        (df["finalMonthExpenses"] > 0) &
        (df["nexpenseTx"] > 0)
    ].copy()

    if train_df.shape[0] < 5:
        print("❌ Pas assez de données exploitables")
        return

    train_df["remaining"] = (
        train_df["finalMonthExpenses"] - train_df["expensesSoFar"]
    )

    X = train_df[FEATURES]
    y = train_df["remaining"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.3, random_state=42
    )

    MODEL.fit(X_train, y_train)

    preds = MODEL.predict(X_test)
    mae = mean_absolute_error(y_test, preds)
    print(f"MAE (reste à dépenser) ≈ {mae:,.0f} LBP")

    cur_df = df[df["finalMonthExpenses"].isna()].copy()
    if cur_df.empty:
        print("❌ Mois courant introuvable")
        return

    latest = cur_df.sort_values(["year", "month", "day"]).iloc[-1]
    X_latest = latest[FEATURES].to_frame().T

    pred_remaining = max(0, MODEL.predict(X_latest)[0])
    pred_final = latest["expensesSoFar"] + pred_remaining

    print("\n--- PREDICTION MOIS COURANT ---")
    print(f"Mois: {int(latest['month'])}/{int(latest['year'])}")
    print(f"Jour snapshot: {int(latest['day'])}")
    print(f"Dépenses so far: {latest['expensesSoFar']:,.0f} LBP")
    print(f"Revenus so far:  {latest['revenuesSoFar']:,.0f} LBP")
    print(f"➡️ Prédiction fin de mois: {pred_final:,.0f} LBP")

    coef = pd.Series(
        MODEL.named_steps["ridge"].coef_,
        index=FEATURES
    ).sort_values(key=abs, ascending=False)

    print("\nTop coefficients:")
    print(coef)

def main():
    df = fetch_dataset()
    print(f"Lignes dataset: {len(df)}")
    train_and_predict(df)

if __name__ == "__main__":
    main()
