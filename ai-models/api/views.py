import json, time
from django.http import JsonResponse, HttpResponseBadRequest, HttpResponse
from django.views.decorators.http import require_POST, require_GET

# api/portfolio.py 기준 상대 임포트
from .portfolio import PortfolioRecommender


def _parse_json(body: bytes):
    try:
        return json.loads(body.decode("utf-8"))
    except Exception:
        return None


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


@require_POST
def current_price(request):
    data = _parse_json(request.body)
    if data is None:
        return HttpResponseBadRequest(JsonResponse({"error": "invalid_json"}).content)

    assets = data.get("assets") or ["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"]
    lookback_years = int(data.get("lookback_years", 3))
    rf = float(data.get("rf", 0.0))
    days = int(data.get("days", 5))

    try:
        rec = PortfolioRecommender(assets=assets, lookback_years=lookback_years, rf=rf)
        prices = rec.get_current_price(days=days, verbose=False)
        return JsonResponse({"prices": prices})
    except Exception as e:
        return HttpResponseBadRequest(JsonResponse({"error": str(e)}).content)


@require_POST
def price_change(request):
    data = _parse_json(request.body)
    if data is None:
        return HttpResponseBadRequest(JsonResponse({"error": "invalid_json"}).content)

    assets = data.get("assets") or ["SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"]
    lookback_years = int(data.get("lookback_years", 3))
    rf = float(data.get("rf", 0.0))
    periods = int(data.get("periods", 1))  # 전일 대비: 1
    days = int(data.get("days", 6))

    try:
        rec = PortfolioRecommender(assets=assets, lookback_years=lookback_years, rf=rf)
        changes = rec.get_current_price_change(periods=periods, days=days, verbose=False)
        return JsonResponse({"changes": changes})
    except Exception as e:
        return HttpResponseBadRequest(JsonResponse({"error": str(e)}).content)


@require_GET
def healthz(_request):
    return HttpResponse("ok")