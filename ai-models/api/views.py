import json, time
from django.http import JsonResponse, HttpResponseBadRequest, HttpResponse
from django.views.decorators.http import require_POST, require_GET

# 최상단에 있는 portfolio.py의 PortfolioRecommender 사용
# (파일이 레포 루트에 있으므로, 파이썬 경로상 현재 작업 디렉터리 기준 import 가능)
from portfolio import PortfolioRecommender  # 클래스가 존재한다고 가정

@require_POST
def recommend(request):
    try:
        data = json.loads(request.body.decode("utf-8"))
    except Exception:
        return HttpResponseBadRequest(JsonResponse({"error": "invalid_json"}).content)

    assets = data.get("assets") or ["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"]
    lookback_years = int(data.get("lookback_years", 3))
    risk_level = int(data.get("risk_level", 3))
    rf = float(data.get("rf", 0.0))
    points = int(data.get("points", 10))

    t0 = time.time()
    try:
        rec = PortfolioRecommender(assets=assets, lookback_years=lookback_years, rf=rf)
        result = rec.recommend(risk_level=risk_level, points=points)
        resp = {
            "annual_return": float(result["annual_return"]),
            "annual_vol": float(result["annual_vol"]),
            "sharpe": None if result["sharpe"] is None else float(result["sharpe"]),
            "max_drawdown": float(result["max_drawdown"]),
            "weights": {k: float(v) for k, v in result["weights"].items()},
            "elapsed_ms": int((time.time() - t0) * 1000),
        }
        return JsonResponse(resp, json_dumps_params={"ensure_ascii": False})
    except Exception as e:
        return HttpResponseBadRequest(JsonResponse({"error": str(e)}).content)

@require_GET
def healthz(_request):
    return HttpResponse("ok")