import numpy as np
import pandas as pd
import yfinance as yf
import datetime as dt
import riskfolio as rp


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
        # 날짜 범위
        now = dt.datetime.now() - dt.timedelta(days=1)
        start = now - dt.timedelta(days=365 * lookback_years)
        self.now = now
        self.start = start.strftime("%Y-%m-%d")

        self.prices = None
        self.returns = None

    def load_prices(self):
        data = yf.download(self.assets, start=self.start, end=self.now)
        data = data.loc[:, ('Close', slice(None))]
        data.columns = self.assets
        self.prices = data
        return self.prices

    def get_returns(self):
        if self.prices is None:
            self.load_prices()
        self.returns = self.prices.pct_change().dropna()
        return self.returns

    def _risk_contribution_share(self, Y, w_series):
        cov = Y.cov()
        w_aligned = w_series.reindex(cov.index).fillna(0.0).values
        sigma_w = cov.values @ w_aligned
        rc = w_aligned * sigma_w
        rc_share = rc / rc.sum() if rc.sum() != 0 else rc
        return pd.Series(rc_share, index=cov.index).sort_values(ascending=False)

    def recommend(self, risk_level=3, points=10, Y=None, rm_by_level=None):
        """
        - risk_level: 1(보수)~5(공격)
        - points: 효율적 경계선 분해 개수
        - Y: 수익률 DF(없으면 내부에서 계산)
        - rm_by_level: {1:'CVaR', 2:'MV', ...} 커스텀 매핑 가능
        """
        Y = Y if Y is not None else self.get_returns()

        # 1) 성향→리스크측도/프론티어 위치
        rm_map = rm_by_level or {1: 'CVaR', 2: 'CVaR', 3: 'CVaR', 4: 'CVaR', 5: 'CVaR'}
        rm = rm_map.get(int(risk_level), 'CVaR')

        idx_map = {1: 0, 2: 2, 3: 5, 4: 8, 5: 10}
        idx = max(0, min(points - 1, idx_map.get(int(risk_level), 5)))

        # 2) 효율적 경계선
        port = rp.Portfolio(returns=Y)
        port.assets_stats(method_mu='hist', method_cov='hist')
        frontier = port.efficient_frontier(model='Classic', rm=rm, points=points, rf=self.rf, hist=True)
        w = frontier.iloc[:, idx].copy()

        # 3) 지표(연환산)
        ret = (Y @ w).rename('ret')
        ann_ret = ret.mean() * 252
        ann_vol = ret.std() * np.sqrt(252)
        sharpe = (ann_ret - self.rf) / ann_vol if ann_vol > 0 else np.nan

        cum = (1 + ret).cumprod()
        dd = cum / cum.cummax() - 1
        mdd = dd.min()

        # 4) 변동성 기여 비중
        rc_share = self._risk_contribution_share(Y, w)

        # 5) 요약 텍스트 출력(그래프 라벨은 사용 안 함)
        print(f"[추천 포트폴리오] 위험 성향 {risk_level}")
        print(f"- 예상 연수익률: {ann_ret:.2%}")
        print(f"- 예상 연변동성: {ann_vol:.2%}")
        print(f"- 샤프지수(무위험 {self.rf:.2%}): {sharpe:.2f}")
        print(f"- 과거 구간 기준 최대낙폭: {mdd:.2%}")

        top_w = w.sort_values(ascending=False).head(3)
        print("- 비중 상위:")
        for k, v in top_w.items():
            print(f"  · {k}: {v:.2%}")

        top_rc = rc_share.head(2)
        print("- 변동성 기여 상위:")
        for k, v in top_rc.items():
            print(f"  · {k}: {v:.2%}")

        metrics = {
            "risk_level": risk_level,
            "rm": rm,
            "annual_return": ann_ret,
            "annual_vol": ann_vol,
            "sharpe": sharpe,
            "max_drawdown": mdd
        }
        return w, metrics

    def summary_text(self, w, metrics, top_n_weight=3, top_n_rc=2):
        w_top = w.sort_values(ascending=False).head(top_n_weight)
        rc = self._risk_contribution_share(self.returns if self.returns is not None else self.get_returns(), w)
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


if __name__ == "__main__":
    rec = PortfolioRecommender(assets=["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"], lookback_years=3, rf=0.0)
    w, m = rec.recommend(risk_level=4, points=10)
    s = rec.summary_text(w, m)
    print("\n[요약]\n" + s)