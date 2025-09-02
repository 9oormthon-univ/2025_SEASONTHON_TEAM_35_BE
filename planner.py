class PortfolioPlanner:
    """
    사용자의 재무 정보를 입력받아 비상금과 4개 버킷(현금/예금/적금/투자) 비율을 추천하는 클래스.
    단위: 만원
    """
    def __init__(
        self,
        current_asset: int, # 현재 자산
        monthly_income: int, # 월 수익
        risk_tolerance: str, # 투자 성향
        investment_period: int, # 투자 기간(년)
        investment_goal: str, # 투자 목표
        target_return: int, # 목표 수익(%)
        cash_ratio: float = 0.10 # 현금 비중(기본 10%)
    ):
        self.current_asset = current_asset
        self.monthly_income = monthly_income
        self.risk_tolerance = risk_tolerance
        self.investment_period = investment_period
        self.investment_goal = investment_goal
        self.target_return = target_return
        self.cash_ratio = max(0.0, min(0.5, cash_ratio))

        self.emergency_fund = 0
        self.allocation_ratio = {}  # {'cash','deposit','savings','investment'}

    def _calculate_emergency_fund(self):  # 만원 단위(간단 규칙 유지)
        income = self.monthly_income
        if 0 <= income <= 200:
            self.emergency_fund = 500
        elif 201 <= income <= 300:
            self.emergency_fund = 800
        elif 301 <= income <= 500:
            self.emergency_fund = 1500
        else:
            self.emergency_fund = 3000

    def _total_score(self) -> int:
        total_score = 0
        # 1) 투자 성향
        if self.risk_tolerance == "안정형":
            total_score += 0
        elif self.risk_tolerance == "중립형":
            total_score += 20
        elif self.risk_tolerance == "공격투자형":
            total_score += 40
        # 2) 투자 기간
        if 1 <= self.investment_period < 3:
            total_score += 10
        elif 3 <= self.investment_period < 5:
            total_score += 20
        elif self.investment_period >= 5:
            total_score += 30
        # 3) 투자 목표
        if self.investment_goal == "차량 구매":
            total_score += 10
        elif self.investment_goal in ["결혼", "내집 마련"]:
            total_score += 20
        # 4) 목표 수익
        if 5 <= self.target_return < 10:
            total_score += 10
        elif 10 <= self.target_return < 15:
            total_score += 20
        elif self.target_return >= 15:
            total_score += 30
        return total_score

    def _safe_invest_split(self, total_score: int):
        # 예적금(안전자산) vs 투자 큰 비율
        if 0 <= total_score <= 30:
            safe, invest = 0.80, 0.20
        elif 31 <= total_score <= 60:
            safe, invest = 0.60, 0.40
        elif 61 <= total_score <= 90:
            safe, invest = 0.40, 0.60
        else:
            safe, invest = 0.20, 0.80
        return safe, invest

    def _deposit_savings_split(self, safe_ratio: float):
        # 예적금 비율 분배
        monthly_income = max(1, self.monthly_income)  # 0 나눗셈 방지
        runway = self.current_asset / monthly_income  # 몇 개월치 여유인가

        if runway >= 6:
            savings_share = 0.30  # 적금
        elif runway >= 3:
            savings_share = 0.50
        elif runway >= 1:
            savings_share = 0.65
        else:
            savings_share = 0.80

        deposit_share = 1.0 - savings_share
        return safe_ratio * deposit_share, safe_ratio * savings_share

    def _recommend_allocation(self):
        total_score = self._total_score()

        # 1) 현금 고정
        cash = self.cash_ratio

        # 2) 나머지(1 - cash)를 안전/투자로 분할
        safe_w, invest_w = self._safe_invest_split(total_score)
        residual = max(0.0, 1.0 - cash)
        safe_ratio = residual * safe_w
        invest_ratio = residual * invest_w

        # 3) 안전자산 내부 예금/적금 분할
        deposit_ratio, savings_ratio = self._deposit_savings_split(safe_ratio)

        # 4) 정규화(수치 안전)
        total = cash + deposit_ratio + savings_ratio + invest_ratio
        if total > 0:
            cash /= total
            deposit_ratio /= total
            savings_ratio /= total
            invest_ratio /= total

        self.allocation_ratio = {
            "cash": cash,
            "deposit": deposit_ratio,
            "savings": savings_ratio,
            "investment": invest_ratio
        }

    def get_recommendation(self) -> dict:
        self._calculate_emergency_fund()
        self._recommend_allocation()
        return {
            "emergency_fund": self.emergency_fund,
            "allocation_ratio": self.allocation_ratio  # 0~1 비율
        }


# 사용 예시
if __name__ == "__main__":
    user_planner = PortfolioPlanner(
        current_asset=300,  # 현 자산
        monthly_income=300,  # 월 수입
        risk_tolerance="중립형", # 안정형, 중립형, 공격투자형
        investment_period=3,  # 투자 기간(년)
        investment_goal="차량 구매", # 여행, 자기계발, 차량 구매, 결혼, 내집 마련
        target_return=5  # 목표 수익(%)
    )
    recommendation = user_planner.get_recommendation()
    print("--- 사용자 1 맞춤 분석 결과 ---")
    print(f"추천 비상금: {recommendation['emergency_fund']:,}만원")
    alloc = recommendation["allocation_ratio"]
    print(
        "추천 비율(%)  현금: {0:.0f}% / 예금: {1:.0f}% / 적금: {2:.0f}% / 투자: {3:.0f}%".format(
            alloc["cash"] * 100,
            alloc["deposit"] * 100,
            alloc["savings"] * 100,
            alloc["investment"] * 100
        )
    )