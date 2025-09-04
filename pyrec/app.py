from fastapi import FastAPI
from pydantic import BaseModel
import numpy as np, pandas as pd, riskfolio as rp, yfinance as yf
import datetime as dt

app = FastAPI()

class RecRequest(BaseModel):
    assets: list[str] = ["SPY","QQQM","277630.KS","272910.KS","IMTB"]
    rf: float = 0.0
    points: int = 10
    risk_level: int = 3
    use_csv: bool = True
    lookback_years: int = 3

def load_prices(assets, years, use_csv=True):
    if use_csv:
        df = pd.read_csv("prices_3y.csv", header=[0,1])
        close = df['Close'].ffill().bfill()
        cols = [c for c in close.columns if c in assets]
        return close[cols]
    data = yf.download(assets, period=f"{years}y", interval="1d", auto_adjust=True)["Close"]
    if isinstance(data, pd.Series): data = data.to_frame()
    return data.ffill().bfill()

def optimize(Y, rf, points, level):
    rm = "CVaR"
    idx = max(0, min(points-1, {1:0,2:2,3:5,4:8,5:points-1}.get(int(level),5)))
    port = rp.Portfolio(returns=Y)
    port.assets_stats(method_mu='hist', method_cov='hist')
    frontier = port.efficient_frontier(model='Classic', rm=rm, points=points, rf=rf, hist=True)
    w = frontier.iloc[:, idx].copy()

    ret = (Y @ w).rename("ret")
    ann_ret = float(ret.mean()*252); ann_vol = float(ret.std()*np.sqrt(252))
    sharpe = None if ann_vol==0 else float((ann_ret-rf)/ann_vol)
    cum = (1+ret).cumprod(); mdd = float((cum/cum.cummax()-1).min())

    return {
      "annual_return": ann_ret,
      "annual_vol": ann_vol,
      "sharpe": sharpe,
      "max_drawdown": mdd,
      "weights": {k:float(v) for k,v in w.sort_index().items()}
    }

@app.post("/recommend")
def recommend(req: RecRequest):
    prices = load_prices(req.assets, req.lookback_years, req.use_csv)
    returns = prices.pct_change().dropna()
    metrics = optimize(returns, req.rf, req.points, req.risk_level)
    tail = prices.tail(2).ffill()
    day_change = (tail.iloc[-1]/tail.iloc[-2]-1.0).to_dict()
    as_of = str(tail.index[-1])
    return {"metrics":metrics, "last_prices":tail.iloc[-1].to_dict(), "day_change_pct":day_change, "as_of": as_of}
