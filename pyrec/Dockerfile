FROM python:3.11-slim
WORKDIR /app

# 과학패키지 런타임 의존성
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 libglib2.0-0 && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# FastAPI 소스 경로에 맞게 복사 (main.py가 pyrec/app/main.py 라면)
COPY . .
# 필요하면 CSV나 데이터 파일도 여기서 복사
COPY prices_3y.csv ./app/prices_3y.csv

EXPOSE 8001
CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8001"]
