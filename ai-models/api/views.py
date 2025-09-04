import os
import json
import time

from django.http import JsonResponse, HttpResponseBadRequest, HttpResponse
from django.views.decorators.http import require_POST, require_GET
from django.views.decorators.csrf import csrf_exempt

# 실제 사용하는 파일명에 맞춰 수정하세요: .portfolio / .portfolio1 / .portfolio2
from .portfolio import PortfolioRecommender

CSV_PATH = os.path.join(os.path.dirname(__file__), "prices_3y.csv")


def _parse_json(body: bytes):
    try:
        return json.loads(body.decode("utf-8"))
    except Exception:
        return None


@csrf_exempt
@require_POST
def recommend(request):
    data = _parse_json(request.body)
    if data is None:
        return HttpResponseBadRequest(JsonResponse({"error": "invalid_json"}).content)

    assets = data.get("assets") or ["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"]
    lookback_years = int(data.get("lookback_years", 3))
    risk_level = int(data.get("risk_level", 3))
    rf = float(data.get("rf", 0.0))
    points = int(data.get("points", 10))

    t0 = time.time()
    try:
        rec = PortfolioRecommender(
            assets=assets, lookback_years=lookback_years, rf=rf, csv_path=CSV_PATH
        )
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


@csrf_exempt
@require_POST
def current_price(request):
    data = _parse_json(request.body)
    if data is None:
        return HttpResponseBadRequest(JsonResponse({"error": "invalid_json"}).content)

    assets = data.get("assets") or ["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"]
    lookback_years = int(data.get("lookback_years", 3))
    rf = float(data.get("rf", 0.0))

    try:
        rec = PortfolioRecommender(
            assets=assets, lookback_years=lookback_years, rf=rf, csv_path=CSV_PATH
        )
        df = rec.get_current_price()  # DataFrame (마지막 1행만 의미)
        return JsonResponse({"prices": df.iloc[-1].to_dict()})
    except Exception as e:
        return HttpResponseBadRequest(JsonResponse({"error": str(e)}).content)


@csrf_exempt
@require_POST
def price_change(request):
    data = _parse_json(request.body)
    if data is None:
        return HttpResponseBadRequest(JsonResponse({"error": "invalid_json"}).content)

    assets = data.get("assets") or ["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"]
    lookback_years = int(data.get("lookback_years", 3))
    rf = float(data.get("rf", 0.0))

    try:
        rec = PortfolioRecommender(
            assets=assets, lookback_years=lookback_years, rf=rf, csv_path=CSV_PATH
        )
        changes = rec.get_current_price_change()  # Series(등락률 %)
        return JsonResponse({"changes": {k: float(v) for k, v in changes.items()}})
    except Exception as e:
        return HttpResponseBadRequest(JsonResponse({"error": str(e)}).content)


@csrf_exempt
@require_GET
def healthz(_request):
    return HttpResponse("ok")