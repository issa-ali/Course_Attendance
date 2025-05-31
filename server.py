import base64
import datetime
import json
import os
import re
from io import BytesIO
from flask import Flask, request, jsonify
import numpy as np
from PIL import Image, ImageEnhance
import cv2
import csv
from insightface.app import FaceAnalysis

app = Flask(__name__)

# Path to the encodings file
ENCODINGS_FILE = os.path.join('encodings', 'encodings.json')
ATTENDANCE_DIR = 'attendance_records'

# Configuration
FACE_MATCHING_THRESHOLD = 0.6  # Higher is more strict (typical range 0.5-0.7)
MIN_FACE_SIZE = 100  # Minimum face size in pixels for reliable recognition

# Initialize InsightFace
face_app = FaceAnalysis(name='buffalo_l', providers=['CUDAExecutionProvider', 'CPUExecutionProvider'])
face_app.prepare(ctx_id=0, det_size=(640, 640))

def sanitize_filename(name):
    """Convert course name to safe filename"""
    return re.sub(r'[^\w\s-]', '', name).strip().replace(' ', '_')

def load_known_faces():
    try:
        with open(ENCODINGS_FILE, 'r') as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return []

def save_known_faces(encodings):
    os.makedirs('encodings', exist_ok=True)
    with open(ENCODINGS_FILE, 'w') as f:
        json.dump(encodings, f)

def preprocess_image(image):
    """Enhance image quality for better face recognition"""
    # Convert to numpy array for OpenCV processing
    img_array = np.array(image)
    
    # Convert to grayscale for histogram equalization
    if len(img_array.shape) == 3:
        gray = cv2.cvtColor(img_array, cv2.COLOR_RGB2GRAY)
    else:
        gray = img_array
    
    # Apply CLAHE (Contrast Limited Adaptive Histogram Equalization)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    enhanced_gray = clahe.apply(gray)
    
    # Convert back to color if original was color
    if len(img_array.shape) == 3:
        enhanced = cv2.cvtColor(enhanced_gray, cv2.COLOR_GRAY2RGB)
    else:
        enhanced = enhanced_gray
    
    # Convert back to PIL Image
    enhanced_image = Image.fromarray(enhanced)
    
    # Enhance contrast and sharpness
    enhancer = ImageEnhance.Contrast(enhanced_image)
    enhanced_image = enhancer.enhance(1.2)
    
    enhancer = ImageEnhance.Sharpness(enhanced_image)
    enhanced_image = enhancer.enhance(1.5)
    
    return enhanced_image

def decode_image(base64_data):
    try:
        img_data = base64.b64decode(base64_data)
        img = Image.open(BytesIO(img_data))
        
        # Check image size and warn if too small
        width, height = img.size
        if width < 640 or height < 480:
            print(f"Warning: Image resolution is low ({width}x{height}). Recognition may be less accurate.")
        
        # Preprocess the image
        img = preprocess_image(img)
        return img
    except Exception as e:
        print("Error decoding image:", e)
        return None

def ensure_attendance_dir():
    """Ensure attendance directory exists"""
    os.makedirs(ATTENDANCE_DIR, exist_ok=True)

def log_attendance(student_name, student_id, course_name):
    """Log attendance with only: Name, ID, Hour, Weekday, Date"""
    ensure_attendance_dir()
    
    now = datetime.datetime.now()
    
    # Formatting time and date (removed 24h time)
    hour_am_pm = now.strftime("%I:%M %p")        # 12-hour + AM/PM (e.g., 02:30 PM)
    weekday = now.strftime("%A")                  # Full weekday (e.g., Monday)
    date_mmddyyyy = now.strftime("%d/%m/%Y")     # MM/DD/YYYY (e.g., 11/04/2024)
    
    safe_course_name = sanitize_filename(course_name)
    attendance_file = os.path.join(ATTENDANCE_DIR, f"attendance_{safe_course_name}.csv")
    
    file_exists = os.path.isfile(attendance_file)
    
    with open(attendance_file, 'a', newline='') as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow(["Name", "ID", "Hour", "Weekday", "Date"])
        writer.writerow([student_name, student_id, hour_am_pm, weekday, date_mmddyyyy])
    
    return attendance_file

@app.route('/register', methods=['POST'])
def register():
    data = request.json
    student_name = data.get('name')
    student_id = data.get('id')
    image_data = data.get('selfie')

    if not all([student_name, student_id, image_data]):
        return jsonify({"error": "Missing required fields"}), 400

    image = decode_image(image_data)
    if not image:
        return jsonify({"error": "Invalid image data"}), 400

    image = image.convert("RGB")
    image_array = np.array(image)

    try:
        # Detect faces using InsightFace
        faces = face_app.get(image_array)
        
        if not faces:
            return jsonify({"error": "No face found in the image"}), 400
            
        # Get the largest face
        face = max(faces, key=lambda x: (x.bbox[2]-x.bbox[0])*(x.bbox[3]-x.bbox[1]))
        
        # Check if face is large enough
        bbox = face.bbox
        face_height = bbox[3] - bbox[1]
        face_width = bbox[2] - bbox[0]
        if face_height < MIN_FACE_SIZE or face_width < MIN_FACE_SIZE:
            return jsonify({"error": f"Face is too small for reliable registration. Please get closer to the camera."}), 400
        
        # Get the embedding
        embedding = face.embedding.tolist()
    except Exception as e:
        print("Error during face registration:", e)
        return jsonify({"error": "Error processing face image"}), 400

    known_encodings = load_known_faces()
    
    # Check if student ID already exists
    if any(entry['id'] == student_id for entry in known_encodings):
        return jsonify({"error": "Student ID already exists"}), 400

    # Add new entry
    known_encodings.append({
        "id": student_id,
        "name": student_name,
        "embedding": embedding
    })

    save_known_faces(known_encodings)
    return jsonify({"message": "Student registered successfully"}), 200

@app.route('/recognize', methods=['POST'])
def recognize():
    data = request.json
    image_data = data.get('image')
    course_name = data.get('courseName', 'Unknown_Course')
    course_index = data.get('courseIndex', -1)

    if not image_data:
        return jsonify({"error": "Missing image data"}), 400

    image = decode_image(image_data)
    if not image:
        return jsonify({"error": "Invalid image data"}), 400

    image = image.convert("RGB")
    image_array = np.array(image)

    try:
        # Detect faces using InsightFace
        faces = face_app.get(image_array)
        
        if not faces:
            return jsonify({"error": "No face found in the image"}), 400
            
        # Get the largest face
        face = max(faces, key=lambda x: (x.bbox[2]-x.bbox[0])*(x.bbox[3]-x.bbox[1]))
        unknown_embedding = face.embedding
    except Exception as e:
        print("Error during face recognition:", e)
        return jsonify({"error": "Error processing face image"}), 400

    known_encodings = load_known_faces()
    if not known_encodings:
        return jsonify({"error": "No registered faces found"}), 400

    # Convert known embeddings to numpy arrays
    known_face_embeddings = [np.array(entry["embedding"]) for entry in known_encodings]
    known_face_names = [entry["name"] for entry in known_encodings]
    known_face_ids = [entry["id"] for entry in known_encodings]

    # Calculate similarity scores
    similarities = []
    for emb in known_face_embeddings:
        # Cosine similarity
        sim = np.dot(unknown_embedding, emb) / (np.linalg.norm(unknown_embedding) * np.linalg.norm(emb))
        similarities.append(sim)
    
    # Find the best match (highest similarity)
    best_match_index = np.argmax(similarities)
    best_similarity = similarities[best_match_index]
    
    if best_similarity >= FACE_MATCHING_THRESHOLD:
        attendance_file = log_attendance(
            known_face_names[best_match_index], 
            known_face_ids[best_match_index], 
            course_name
        )
        return jsonify({
            "message": "Attendance confirmed",
            "name": known_face_names[best_match_index],
            "id": known_face_ids[best_match_index],
            "course": course_name,
            "courseIndex": course_index,
            "attendanceFile": os.path.basename(attendance_file),
            "confidence": float(best_similarity)  # Similarity score as confidence
        }), 200

    return jsonify({"error": "Face not recognized"}), 400

@app.route('/get_attendance/<course_name>', methods=['GET'])
def get_attendance(course_name):
    """Endpoint to retrieve attendance records for a course"""
    try:
        safe_course_name = sanitize_filename(course_name)
        attendance_file = os.path.join(ATTENDANCE_DIR, f"attendance_{safe_course_name}.csv")
        
        if not os.path.exists(attendance_file):
            return jsonify({"error": "No attendance records found for this course"}), 404
            
        records = []
        with open(attendance_file, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                records.append(row)
        
        return jsonify({
            "course": course_name,
            "records": records,
            "count": len(records)
        }), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    os.makedirs('encodings', exist_ok=True)
    os.makedirs(ATTENDANCE_DIR, exist_ok=True)
    
    app.run(host='192.168.192.39', port=5000, debug=True)