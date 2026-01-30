import requests
import pandas as pd

from sklearn.model_selection import train_test_split
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

BASE_URL = "http://localhost:8080"
DATASET_URL = f"{BASE_URL}/ml/dataset?monthsBack=2&stepDays=5"

JWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjaGFyYmVseW1uQGdtYWlsLmNvbSIsImlhdCI6MTc2OTE3MTM1OSwiZXhwIjoxNzY5MjU3NzU5fQ.uudMjAlLqbyHkZLapWZLrTUIUiQxA4y6Uo049a0BwNI"

HEADERS = {
    "Authorization": f"Bearer {JWT}"
}

model = Pipeline([
    ("scaler", StandardScaler()),
    ("ridge", Ridge(alpha=1.0))
])

def fetch_dataset() -> pd.DataFrame:
    r = requests.get(DATASET_URL, headers=HEADERS)
    r.raise_for_status()
    data = r.json()
    df = pd.DataFrame(data)
    return df

def train_and_predict(df: pd.DataFrame):
    features = [
        "day",
        "expensesSoFar",
        "avgDailyExpense",
        "maxExpenseSoFar",
        "nexpenseTx",
    ]

    for col in features + ["finalMonthExpenses"]:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors="coerce")

    train_df = df[(df["finalMonthExpenses"] > 0) &(df["nexpenseTx"] > 0)].copy()

    if len(train_df) < 5:
        print("Pas assez de données pour entraîner un modèle (il faut au moins ~5 lignes avec target).")
        print("Augmente stepDays=1 ou attends d'avoir plus de jours/mois.")
        return
    
    x = train_df[features]
    y = train_df["finalMonthExpenses"]

    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.3, random_state=42)

    model = Ridge(alpha=1.0)
    model.fit(x_train, y_train)

    preds = model.predict(x_test)
    mae = mean_absolute_error(y_test, preds)
    print(f"MAE (erreur moyenne absolue) ≈ {mae:,.0f} LBP")

    cur_df = df[df["finalMonthExpenses"].isna()].copy()
    if cur_df.empty:
        print("Aucune ligne à prédire (mois courant introuvable).")
        return
    
    cur_df = cur_df.sort_values(["year","month","day"])
    latest = cur_df.iloc[-1]
    x_latest = latest[features].to_frame().T

    pred_final = model.predict(x_latest)[0]
    pred_final = max(0,pred_final)

    print("\n--- PREDICTION MOIS COURANT ---")
    print(f"Mois: {int(latest['month'])}/{int(latest['year'])}  Jour snapshot: {int(latest['day'])}")
    print(f"Dépenses so far: {latest['expensesSoFar']:,.0f} LBP")
    print(f"Revenus so far:  {latest['revenuesSoFar']:,.0f} LBP")
    print(f"Prediction dépenses fin de mois: {pred_final:,.0f} LBP")

    coef = pd.Series(model.coef_, index=features).sort_values(key=abs, ascending=False)
    print("\nTop coefficients (Ridge):")
    print(coef.head(5))

def main():
    df = fetch_dataset()
    print(f"Lignes dataset: {len(df)}")
    print(df.head(3))
    train_and_predict(df)

if __name__ == "__main__":
    main()