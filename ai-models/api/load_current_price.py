import pandas as pd, yfinance as yf, datetime

def load_current_price():
    day = datetime.datetime.now().day
    month = datetime.datetime.now().month
    start=str((pd.Timestamp.utcnow().normalize()-pd.Timedelta(days=3)).date())
    end=str((pd.Timestamp.utcnow().normalize()-pd.Timedelta(days=1)).date())
    return yf.download(["SPY","QQQM","277630.KS","272910.KS","IMTB"], start=start, end=end, interval="1d", auto_adjust=True, threads=False).to_csv(f"prices_{month}_{day}.csv")