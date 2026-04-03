from flask import Flask, request, jsonify
from pypdf import PdfReader
import io

app = Flask(__name__)


def generate_roadmap(topic):
    topic_lower = topic.lower()

    if "chemistry" in topic_lower:
        roadmap = (
            f"1) Review the core concepts of {topic} for 25 minutes.\n\n"
            f"2) Focus on reaction mechanisms for 20 minutes.\n\n"
            f"3) Solve 5 practice questions.\n\n"
            f"4) Take a 10-minute break.\n\n"
            f"5) Summarize key formulas and reactions."
        )

    elif "history" in topic_lower:
        roadmap = (
            f"1) Review timeline and major events of {topic}.\n\n"
            f"2) Highlight causes and consequences.\n\n"
            f"3) Write a short paragraph summary.\n\n"
            f"4) Take a 10-minute break.\n\n"
            f"5) Practice one possible exam question."
        )

    elif "math" in topic_lower or "calculus" in topic_lower:
        roadmap = (
            f"1) Review formulas and concepts of {topic}.\n\n"
            f"2) Solve 3 guided examples.\n\n"
            f"3) Solve 3 independent questions.\n\n"
            f"4) Take a 10-minute break.\n\n"
            f"5) Review mistakes."
        )

    else:
        roadmap = (
            f"1) Study {topic} for 25 minutes.\n\n"
            f"2) Break it into subtopics.\n\n"
            f"3) Practice questions.\n\n"
            f"4) Take a short break.\n\n"
            f"5) Summarize what you learned."
        )

    return roadmap


def summarize_text(text):
    text = " ".join(text.split())
    if not text:
        return "No readable text was found in this PDF."

    if len(text) > 2500:
        text = text[:2500]

    sentences = text.split(". ")
    short_sentences = [s.strip() for s in sentences[:6] if s.strip()]

    if not short_sentences:
        return text[:600]

    summary = "Lecture Summary:\n\n"
    for i, sentence in enumerate(short_sentences[:5], start=1):
        summary += f"{i}) {sentence.strip()}"
        if not sentence.endswith("."):
            summary += "."
        summary += "\n\n"

    summary += "Key takeaway: review the main definitions, examples, and repeated concepts from this lecture."
    return summary


@app.route('/generate-roadmap', methods=['POST'])
def roadmap():
    data = request.get_json()
    topic = data.get("topic", "")
    result = generate_roadmap(topic)
    return jsonify({"roadmap": result})


@app.route('/summarize-pdf', methods=['POST'])
def summarize_pdf():
    if 'file' not in request.files:
        return jsonify({"error": "No file uploaded"}), 400

    uploaded_file = request.files['file']

    if uploaded_file.filename == '':
        return jsonify({"error": "Empty file name"}), 400

    if not uploaded_file.filename.lower().endswith('.pdf'):
        return jsonify({"error": "Only PDF files are allowed"}), 400

    try:
        pdf_bytes = uploaded_file.read()
        reader = PdfReader(io.BytesIO(pdf_bytes))

        extracted_text = ""
        for page in reader.pages:
            page_text = page.extract_text()
            if page_text:
                extracted_text += page_text + "\n"

        summary = summarize_text(extracted_text)

        return jsonify({
            "summary": summary
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)