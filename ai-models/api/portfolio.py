import numpy as np
import pandas as pd
import yfinance as yf
import datetime as dt
import riskfolio as rp

data = pd.read_csv("prices_3y.csv", header=[0, 1])
close = data['Close']
close = close.ffill()
close = close.bfill()

class PortfolioRecommender:
    """
    간단한 포트폴리오 추천 클래스
    - 자산 가격을 가져와 일간 수익률을 계산
    - 성향별로 효율적 경계선에서 가중치 선택
    - 성과/위험 지표와 상위 비중/변동성 기여를 요약
    """

    def __init__(self, assets=None, lookback_years=3, rf=0.0):
        self.assets = assets or ["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"]
        self.assets = sorted(self.assets)
        self.rf = rf
        self.lookback_years = lookback_years
        # 날짜 범위
        now = dt.datetime.now() - dt.timedelta(days=1)
        start = now - dt.timedelta(days=1 * self.lookback_years)
        self.now = now
        self.start = start.strftime("%Y-%m-%d")

        self.prices = close
        self.returns = close.pct_change().dropna()

    def _risk_contribution_share(self, Y, w_series):
        cov = Y.cov()
        w_aligned = w_series.reindex(cov.index).fillna(0.0).values
        sigma_w = cov.values @ w_aligned
        rc = w_aligned * sigma_w
        rc_share = rc / rc.sum() if rc.sum() != 0 else rc
        return pd.Series(rc_share, index=cov.index).sort_values(ascending=False)

    def recommend(self, risk_level=3, points=10, Y=None, rm_by_level=None):
        """
        반환: {
        "annual_return": float,   # 연수익률
        "annual_vol": float,      # 연변동성
        "sharpe": float|None,     # 샤프지수 (분모 0이면 None)
        "max_drawdown": float,    # 최대낙폭
        "weights": {ticker: float}# 종목별 비율(0~1)
        }
        """
        Y = Y if Y is not None else self.returns

        rm_map = rm_by_level or {1: 'CVaR', 2: 'CVaR', 3: 'CVaR', 4: 'CVaR', 5: 'CVaR'}
        rm = rm_map.get(int(risk_level), 'CVaR')

        idx_map = {1: 0, 2: 2, 3: 5, 4: 8, 5: 10}
        idx = max(0, min(points - 1, idx_map.get(int(risk_level), 5)))

        port = rp.Portfolio(returns=Y)
        port.assets_stats(method_mu='hist', method_cov='hist')
        frontier = port.efficient_frontier(model='Classic', rm=rm, points=points, rf=self.rf, hist=True)
        w = frontier.iloc[:, idx].copy()

        ret = (Y @ w).rename('ret')
        ann_ret = float(ret.mean() * 252)
        ann_vol = float(ret.std() * np.sqrt(252))
        sharpe = (ann_ret - float(self.rf)) / ann_vol if ann_vol > 0 else None

        cum = (1 + ret).cumprod()
        dd = cum / cum.cummax() - 1
        mdd = float(dd.min())

        weights = {k: float(v) for k, v in w.sort_index().items()}

        return {
            "annual_return": ann_ret,
            "annual_vol": ann_vol,
            "sharpe": None if sharpe is None else float(sharpe),
            "max_drawdown": mdd,
            "weights": weights,
        }

    def summary_text(self, w, metrics, top_n_weight=3, top_n_rc=2):
        w_top = w.sort_values(ascending=False).head(top_n_weight)
        rc = self._risk_contribution_share(self.returns if self.returns is not None else self.returns, w)
        rc_top = rc.head(top_n_rc)

        lines = []
        lines.append(f"- 예상 연수익률: {metrics['annual_return']:.2%}")
        lines.append(f"- 예상 연변동성: {metrics['annual_vol']:.2%}")
        lines.append(f"- 샤프지수(무위험 {self.rf:.2%}): {metrics['sharpe']:.2f}")
        lines.append(f"- 과거 구간 기준 최대낙폭: {metrics['max_drawdown']:.2%}")
        lines.append("- 비중 상위:")
        for k, v in w_top.items():
            lines.append(f"  · {k}: {v:.2%}")
        lines.append("- 변동성 기여 상위:")
        for k, v in rc_top.items():
            lines.append(f"  · {k}: {v:.2%}")
        return "\n".join(lines)
    
    # 최근 가격 불러오기
    def get_current_price(self):
        self.current_price = self.prices.iloc[-5:] # 공휴일 방지 넉넉히
        self.current_price = self.current_price.ffill() # 공휴일 방지
        return self.current_price.iloc[-2:]
    
    # 전일 대비 오늘 가격 상승률
    def get_current_price_change(self):
        self.current_price_change = self.current_price.iloc[-2:].pct_change()
        return self.current_price_change.iloc[1] * 100
    

if __name__ == "__main__":
    rec = PortfolioRecommender(assets=["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"], lookback_years=1, rf=0.0)
    result = rec.recommend(risk_level=3, points=10)

    print(f"예상 연수익률: {result['annual_return']:.2%}")
    print(f"예상 연변동성: {result['annual_vol']:.2%}")
    print(f"샤프지수: {result['sharpe']:.2f}")
    print(f"최대낙폭: {result['max_drawdown']:.2%}")
    print("종목별 포트폴리오 비율:")
    for t, w in result["weights"].items():
        print(f"  - {t}: {w:.2%}")
    
    get_current_price = rec.get_current_price()
    print(get_current_price)    

    get_current_price_change = rec.get_current_price_change()
    print(get_current_price_change)