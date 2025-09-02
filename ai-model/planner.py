class PortfolioPlanner:
    """
    사용자의 재무 정보를 입력받아 맞춤형 비상금과 포트폴리오 비율을 추천하는 클래스.
    """
    def __init__(
        self,
        monthly_income: int, # 월 수익
        risk_tolerance: str, # 투자 성향
        investment_period: int, # 투자 기간
        investment_goal: str, # 투자 목표
        target_return: int # 목표 수익(%)
    ):
        """
        클래스 초기화
        """
        self.monthly_income = monthly_income
        self.risk_tolerance = risk_tolerance
        self.investment_period = investment_period
        self.investment_goal = investment_goal
        self.target_return = target_return
        
        # 결과를 저장할 변수
        self.emergency_fund = 0 # 비상금
        self.portfolio_ratio = "" # 포트폴리오 비율

    def _calculate_emergency_fund(self): # 만원 단위
        income = self.monthly_income
        if 0 <= income <= 200:
            self.emergency_fund = 500
        elif 201 <= income <= 300:
            self.emergency_fund = 800
        elif 301 <= income <= 500:
            self.emergency_fund = 1500
        else:
            self.emergency_fund = 3000
            
    def _recommend_portfolio_ratio(self):
        total_score = 0

        """
        투자 성향 종류
        1. 안정형 : 0
        2. 중립형 : 20
        3. 공격투자형 : 40
        """
        # 1. 투자 성향 점수
        if self.risk_tolerance == "중립형":
            total_score += 20
        elif self.risk_tolerance == "공격투자형":
            total_score += 40

        """
        투자 기간 종류
        1. 1년 이하 : 0
        2. 1~3년 : 10
        3. 3~5년 : 20
        4. 5년 이상 : 30
        """
        # 2. 투자 기간 점수 (년 단위)
        if 1 <= self.investment_period < 3:
            total_score += 10
        elif 3 <= self.investment_period < 5:
            total_score += 20
        elif self.investment_period >= 5:
            total_score += 30

        """
        투자 목표 종류
        1. 여행, 자기계발 : 0
        2. 차량 구매 : 10
        3. 결혼 : 20
        4. 내집 마련 : 30
        """
        # 3. 투자 목표 점수
        if self.investment_goal == "차량 구매":
            total_score += 10
        elif self.investment_goal in ["결혼", "내집 마련"]:
            total_score += 20


        """
        목표 수익 종류
        1. 5% 미만 : 0
        2. 5~10% : 10
        3. 10~15% : 20
        4. 15% 이상 : 30
        """   
        # 4. 목표 수익 점수
        if 5 <= self.target_return < 10:
            total_score += 10
        elif 10 <= self.target_return < 15:
            total_score += 20
        elif self.target_return >= 15:
            total_score += 30

        # 최종 점수 기반으로 포트폴리오 결정
        if 0 <= total_score <= 30:
            self.portfolio_ratio = "안정형 (예적금 80% / 투자 20%)"
        elif 31 <= total_score <= 60:
            self.portfolio_ratio = "안정 추구형 (예적금 60% / 투자 40%)"
        elif 61 <= total_score <= 90:
            self.portfolio_ratio = "위험 중립형 (예적금 40% / 투자 60%)"
        else:
            self.portfolio_ratio = "공격 투자형 (예적금 20% / 투자 80%)"

    def get_recommendation(self) -> dict:
        # 내부 함수들을 순서대로 호출
        self._calculate_emergency_fund()
        self._recommend_portfolio_ratio()
        
        # 계산된 결과를 딕셔너리에 담아 반환
        return {
            "emergency_fund": self.emergency_fund,
            "portfolio_ratio": self.portfolio_ratio
        }

# 사용 예시
if __name__ == "__main__":
    # 사용자의 정보로 planner 객체 생성
    user_planner = PortfolioPlanner(
        monthly_income=450, # 450만원
        risk_tolerance="중립형",
        investment_period=4, # 4년
        investment_goal="차량 구매",
        target_return=12 # 12%
    )
    
    # 한번에 두 가지 결과 얻기
    recommendation = user_planner.get_recommendation()
    
    print("--- 사용자 1 맞춤 분석 결과 ---")
    print(f"추천 비상금: {recommendation['emergency_fund']:,}만원")
    print(f"추천 포트폴리오: {recommendation['portfolio_ratio']}")