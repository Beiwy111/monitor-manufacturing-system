import base64
from io import BytesIO
from pathlib import Path

from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image
from ultralytics import YOLO


ROOT = Path(__file__).resolve().parent
model = YOLO(ROOT / 'models' / 'production' / 'best.pt')

app = FastAPI(title='Computer Monitor Appearance Inspection')


@app.get('/health')
def health():
    return {'ok': True, 'task': model.task, 'classes': model.names}


@app.post('/predict-json')
async def predict_json(file: UploadFile = File(...)):
    try:
        image = Image.open(BytesIO(await file.read())).convert('RGB')
    except Exception as exc:
        raise HTTPException(status_code=400, detail='Invalid image file') from exc

    result = model.predict(image, save=False, verbose=False)[0]
    detections = []
    polygons = result.masks.xy if result.masks is not None else []

    for index, box in enumerate(result.boxes):
        class_id = int(box.cls.cpu().item())
        detections.append({
            'classId': class_id,
            'className': model.names[class_id],
            'confidence': round(float(box.conf.cpu().item()), 4),
            'box': [round(float(value), 2) for value in box.xyxy[0].cpu().tolist()],
            'polygon': [[round(float(x), 2), round(float(y), 2)] for x, y in polygons[index]] if index < len(polygons) else [],
        })

    rendered = Image.fromarray(result.plot()[..., ::-1])
    output = BytesIO()
    rendered.save(output, format='PNG')

    return {
        'defect': bool(detections),
        'defectType': 'SCRATCH' if detections else None,
        'count': len(detections),
        'maxConfidence': max((item['confidence'] for item in detections), default=0),
        'detections': detections,
        'imageWidth': image.width,
        'imageHeight': image.height,
        'resultImage': f"data:image/png;base64,{base64.b64encode(output.getvalue()).decode('ascii')}",
        'message': f'Detected {len(detections)} scratch area(s).' if detections else 'No scratch detected.',
    }
