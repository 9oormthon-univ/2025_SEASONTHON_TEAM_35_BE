import numpy as np
import pandas as pd
import riskfolio as rp
import os


class PortfolioRecommender:
    """
    CSV(prices_3y.csv)의 Close만 사용하여 수익률 계산 → CVaR 효율적 경계선으로 가중치 산출.
    """

    def __init__(self, assets=None, lookback_years=3, rf=0.0, csv_path=None):
        self.rf = rf
        self.lookback_years = lookback_years
        self.assets = sorted(assets or ["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"])

        # 1) CSV 로드(멀티헤더) → Close 전부 추출
        csv_path = os.path.join(os.path.dirname(__file__), "prices_3y.csv")
        data = pd.read_csv(csv_path, header=[0, 1])

        close = data['Close']
        close = close.ffill()
        close = close.bfill()

        # 2) 요청 자산만 사용(교집합), 전부 NaN 컬럼 제거
        cols = [t for t in self.assets if t in close.columns]
        if not cols:
            raise ValueError("요청한 assets가 CSV에 없습니다.")
        self.prices = close.loc[:, cols].copy()
        self.prices = self.prices.dropna(axis=1, how="all")
        if self.prices.shape[1] < 1:
            raise ValueError("사용 가능한 가격 컬럼이 없습니다.")

        # 3) 일간 수익률
        self.returns = (
            self.prices.pct_change()
            .replace([np.inf, -np.inf], np.nan)
            .dropna(how="any")
        )

    def _risk_contribution_share(self, Y: pd.DataFrame, w_series: pd.Series):
        cov = Y.cov()
        wv = w_series.reindex(cov.index).fillna(0.0).values
        sigma_w = cov.values @ wv
        rc = wv * sigma_w
        share = rc / rc.sum() if rc.sum() != 0 else rc
        return pd.Series(share, index=cov.index).sort_values(ascending=False)

    def recommend(self, risk_level=3, points=10, Y: pd.DataFrame | None = None, rm_by_level=None):
        """
        반환: {
          "annual_return": float,
          "annual_vol": float,
          "sharpe": float|None,
          "max_drawdown": float,
          "weights": {ticker: float}
        }
        """
        Y = Y if Y is not None else self.returns

        rm_map = rm_by_level or {1: "CVaR", 2: "CVaR", 3: "CVaR", 4: "CVaR", 5: "CVaR"}
        rm = rm_map.get(int(risk_level), "CVaR")

        idx_map = {1: 0, 2: 2, 3: 5, 4: 8, 5: 10}
        idx = max(0, min(points - 1, idx_map.get(int(risk_level), 5)))

        port = rp.Portfolio(returns=Y)
        port.assets_stats(method_mu="hist", method_cov="hist")
        frontier = port.efficient_frontier(model="Classic", rm=rm, points=points, rf=self.rf, hist=True)
        w = frontier.iloc[:, idx].copy()

        ret = (Y @ w).rename("ret")
        ann_ret = float(ret.mean() * 252)
        ann_vol = float(ret.std() * np.sqrt(252))
        sharpe = (ann_ret - float(self.rf)) / ann_vol if ann_vol > 0 else None

        cum = (1 + ret).cumprod()
        mdd = float((cum / cum.cummax() - 1).min())

        weights = {k: float(v) for k, v in w.sort_index().items()}
        return {
            "annual_return": ann_ret,
            "annual_vol": ann_vol,
            "sharpe": None if sharpe is None else float(sharpe),
            "max_drawdown": mdd,
            "weights": weights,
        }

    def summary_text(self, w: pd.Series, metrics: dict, top_n_weight=3, top_n_rc=2):
        Y = self.returns
        w_top = w.sort_values(ascending=False).head(top_n_weight)
        rc_top = self._risk_contribution_share(Y, w).head(top_n_rc)

        lines = []
        lines.append(f"- 예상 연수익률: {metrics['annual_return']:.2%}")
        lines.append(f"- 예상 연변동성: {metrics['annual_vol']:.2%}")
        lines.append(f"- 샤프지수(무위험 {self.rf:.2%}): {metrics['sharpe']:.2f}" if metrics["sharpe"] is not None else "- 샤프지수: 계산 불가")
        lines.append(f"- 과거 구간 기준 최대낙폭: {metrics['max_drawdown']:.2%}")
        lines.append("- 비중 상위:")
        for k, v in w_top.items():
            lines.append(f"  · {k}: {v:.2%}")
        lines.append("- 변동성 기여 상위:")
        for k, v in rc_top.items():
            lines.append(f"  · {k}: {v:.2%}")
        return "\n".join(lines)

    def get_current_price(self):
        cur = self.prices.tail(5).ffill().tail(1)
        return cur

    def get_current_price_change(self):
        cur2 = self.prices.tail(2).ffill()
        chg = cur2.pct_change().iloc[-1] * 100.0
        return chg


if __name__ == "__main__":
    rec = PortfolioRecommender(assets=["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"], rf=0.0, csv_path="prices_3y.csv")
    result = rec.recommend(risk_level=3, points=10)

    print(f"예상 연수익률: {result['annual_return']:.2%}")
    print(f"예상 연변동성: {result['annual_vol']:.2%}")
    print(f"샤프지수: {result['sharpe']:.2f}" if result["sharpe"] is not None else "샤프지수: 계산 불가")
    print(f"최대낙폭: {result['max_drawdown']:.2%}")
    print("종목별 포트폴리오 비율:")
    for t, w in result["weights"].items():
        print(f"  - {t}: {w:.2%}")

    print(rec.get_current_price())
    print(rec.get_current_price_change())