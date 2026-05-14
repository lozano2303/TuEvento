import io as _io
from fastapi import FastAPI, File, UploadFile, HTTPException
from PIL import Image
import opennsfw2

app = FastAPI(title="NSFW Classifier", version="1.0.0")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/classify")
async def classify(file: UploadFile = File(...)):
    content_type = file.content_type or ""
    if not content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Only image/* content types are supported")

    raw = await file.read()
    if not raw:
        raise HTTPException(status_code=400, detail="Empty file")

    try:
        Image.open(_io.BytesIO(raw)).convert("RGB")
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid or corrupted image")

    try:
        image_bytes_io = _io.BytesIO(raw)
        nsfw_score = opennsfw2.predict_image(image_bytes_io)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Model inference failed: {exc}")

    return {
        "nsfw_score": round(float(nsfw_score), 4),
        "flagged": nsfw_score >= 0.6,
    }
