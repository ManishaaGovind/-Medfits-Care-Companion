# Root Dockerfile so pushing this repo to GitHub deploys the MedFits Care Companion WEBSITE directly
FROM node:20-alpine AS frontend-builder
WORKDIR /frontend
COPY medfits-care-companion/frontend/package*.json ./
RUN npm install
COPY medfits-care-companion/frontend/ ./
RUN npm run build

FROM python:3.11-slim
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY medfits-care-companion/backend/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY medfits-care-companion/backend/app ./app
COPY --from=frontend-builder /frontend/dist ./static

RUN mkdir -p /app/uploads

EXPOSE 8080

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8080}"]
