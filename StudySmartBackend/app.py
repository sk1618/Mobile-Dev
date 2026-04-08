from flask import Flask, request, jsonify
from pypdf import PdfReader
from dotenv import load_dotenv
import requests
import io
import os

load_dotenv()

app = Flask(__name__)

OPENROUTER_API_KEY = os.getenv("OPENROUTER_API_KEY", "").strip()
OPENROUTER_MODEL = os.getenv("OPENROUTER_MODEL", "openrouter/free").strip()
OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"


def call_openrouter(system_prompt: str, user_prompt: str, max_tokens: int = 700, temperature: float = 0.7) -> str:
    if not OPENROUTER_API_KEY:
        raise RuntimeError("Missing OPENROUTER_API_KEY in .env")

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {OPENROUTER_API_KEY}",
        "HTTP-Referer": "http://localhost",
        "X-Title": "StudySmart"
    }

    payload = {
        "model": OPENROUTER_MODEL,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ],
        "temperature": temperature,
        "max_tokens": max_tokens
    }

    response = requests.post(OPENROUTER_URL, headers=headers, json=payload, timeout=60)
    response.raise_for_status()

    data = response.json()
    return data["choices"][0]["message"]["content"].strip()


@app.route("/ask-study", methods=["POST"])
def ask_study():
    try:
        data = request.get_json(force=True)
        prompt = data.get("prompt", "").strip()

        if not prompt:
            return jsonify({"error": "Prompt is required"}), 400

        system_prompt = (
            "You are StudySmart AI, a helpful academic study assistant for university students. "
            "Help with studying, roadmaps, quiz questions, revision plans, summaries, and explanations. "
            "Be clear, supportive, and practical. "
            "If the user asks to be quizzed, actually ask quiz questions. "
            "If the user asks for a roadmap, provide a structured roadmap. "
            "If the user asks a normal study question, answer it directly and helpfully."
        )

        answer = call_openrouter(system_prompt, prompt, max_tokens=800, temperature=0.6)
        return jsonify({"answer": answer})

    except requests.HTTPError as e:
        return jsonify({"error": f"OpenRouter API HTTP error: {str(e)}"}), 502
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/generate-study-plan", methods=["POST"])
def generate_study_plan():
    try:
        data = request.get_json(force=True)

        subject = data.get("subject", "").strip()
        exam_date = data.get("exam_date", "").strip()
        difficulty = data.get("difficulty", "").strip()
        topics_left = data.get("topics_left", "").strip()
        confidence = data.get("confidence", "").strip()

        if not subject or not exam_date:
            return jsonify({"error": "Subject and exam date are required"}), 400

        system_prompt = (
            "You are StudySmart AI, an expert academic planning assistant. "
            "Generate a personalized day-by-day study plan for a student. "
            "Be practical, structured, and concise. "
            "The plan must be easy to follow and should include daily tasks, revision days, practice days, and final review. "
            "Format clearly using Day 1, Day 2, etc."
        )

        user_prompt = (
            f"Create a smart personalized study plan.\n\n"
            f"Subject: {subject}\n"
            f"Exam Date: {exam_date}\n"
            f"Difficulty: {difficulty}\n"
            f"Topics or chapters left: {topics_left}\n"
            f"Confidence level: {confidence}\n\n"
            f"Make the plan realistic, student-friendly, and clearly structured day by day."
        )

        plan = call_openrouter(system_prompt, user_prompt, max_tokens=1000, temperature=0.6)
        return jsonify({"plan": plan})

    except requests.HTTPError as e:
        return jsonify({"error": f"OpenRouter API HTTP error: {str(e)}"}), 502
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/summarize-pdf", methods=["POST"])
def summarize_pdf():
    if "file" not in request.files:
        return jsonify({"error": "No file uploaded"}), 400

    uploaded_file = request.files["file"]

    if uploaded_file.filename == "":
        return jsonify({"error": "Empty file name"}), 400

    if not uploaded_file.filename.lower().endswith(".pdf"):
        return jsonify({"error": "Only PDF files are allowed"}), 400

    try:
        pdf_bytes = uploaded_file.read()
        reader = PdfReader(io.BytesIO(pdf_bytes))

        extracted_text = ""
        for page in reader.pages:
            page_text = page.extract_text()
            if page_text:
                extracted_text += page_text + "\n"

        extracted_text = " ".join(extracted_text.split())

        if not extracted_text:
            return jsonify({"summary": "No readable text was found in this PDF."})

        if len(extracted_text) > 12000:
            extracted_text = extracted_text[:12000]

        system_prompt = (
            "You are StudySmart AI. Summarize lecture notes clearly for a student. "
            "Give a concise but useful summary with: "
            "1) main topic, 2) key ideas, 3) important definitions or formulas if present, "
            "4) short final takeaway."
        )

        user_prompt = f"Summarize this lecture PDF text:\n\n{extracted_text}"
        summary = call_openrouter(system_prompt, user_prompt, max_tokens=900, temperature=0.4)

        return jsonify({"summary": summary})

    except requests.HTTPError as e:
        return jsonify({"error": f"OpenRouter API HTTP error: {str(e)}"}), 502
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/", methods=["GET"])
def home():
    return "StudySmart Backend is running with OpenRouter free AI."


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
