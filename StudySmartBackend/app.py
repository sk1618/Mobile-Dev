from flask import Flask, request, jsonify

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


@app.route('/generate-roadmap', methods=['POST'])
def roadmap():
    data = request.get_json()

    topic = data.get("topic", "")

    result = generate_roadmap(topic)

    return jsonify({
        "roadmap": result
    })


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)