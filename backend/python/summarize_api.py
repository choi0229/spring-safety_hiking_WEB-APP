from transformers import BartForConditionalGeneration, PreTrainedTokenizerFast
from fastapi import FastAPI
from pydantic import BaseModel
import statistics
import re

app = FastAPI()

# KoBART 모델과 토크나이저 로드
tokenizer = PreTrainedTokenizerFast.from_pretrained('hyunwoongko/kobart')
model = BartForConditionalGeneration.from_pretrained('hyunwoongko/kobart')

# 리뷰 데이터 모델
class ReviewData(BaseModel):
    course_name: str
    reviews: list  # 리뷰 텍스트 리스트
    ratings: list  # 별점 리스트

def select_connector(sentence1, sentence2):
    if "하지만" in sentence1 or "그러나" in sentence1:
        return "하지만"
    elif "또한" in sentence1 or "그리고" in sentence1:
        return "또한" if "또한" in sentence1 else "그리고"
    elif "따라서" in sentence2 or "그래서" in sentence2:
        return "따라서"
    else:
        return "그리고"

def clean_summary_text(text):
    # 반복 패턴 제거
    text = re.sub(r'(좋아요,?)+', '좋아요', text)
    text = re.sub(r'(.)\1{2,}', r'\1', text)
    text = re.sub(r'\b(\w+)\s+\1\b', r'\1', text)
    text = re.sub(r'(요\.|습니다\.|네요\.|다\.)+', r'\1', text)

    # 문장 분리 및 필터링
    sentences = re.split(r'(?<=[.!?])\s+', text)
    filtered_sentences = [sentence.strip() for sentence in sentences if len(sentence.strip()) > 10]

    # 중복 문장 제거 및 연결
    unique_sentences = list(dict.fromkeys(filtered_sentences))
    if len(unique_sentences) > 1:
        connector = select_connector(unique_sentences[0], unique_sentences[-1])
        final_summary = f"{unique_sentences[0]} {connector} {unique_sentences[-1]}"
    else:
        final_summary = " ".join(unique_sentences).strip()

    # 중복 연결어 "그리고" 및 "또한" 제거
    final_summary = re.sub(r'\b(그리고|또한)\s+\1\b', r'\1', final_summary)


    # 끝맺음 처리
    final_summary = re.sub(r'\.+$', '.', final_summary)
    if not final_summary.endswith(('.', '!', '?')):
        final_summary += '.'

    return final_summary

def summarize_text(text):
    try:
        input_ids = tokenizer.encode(text, return_tensors='pt', truncation=True, max_length=512)
        summary_ids = model.generate(
            input_ids, 
            max_length=30,
            num_beams=3, 
            early_stopping=True, 
            no_repeat_ngram_size=3,
            length_penalty=2.0,
            min_length=20
        )
        summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
        return clean_summary_text(summary)
    except Exception as e:
        print(f"요약 과정에서 에러 발생: {str(e)}")
        return "요약 실패"

@app.post("/summarize")
def summarize_reviews(data: ReviewData):
    summarized_reviews = [summarize_text(review) for review in data.reviews]
    final_summary = clean_summary_text(" ".join(set(summarized_reviews)))
    average_rating = statistics.mean(data.ratings)

    return {
        "course_name": data.course_name,
        "summary": final_summary,
        "averageRating": round(average_rating, 2),
        "reviewCount": len(data.reviews)
    }

@app.get("/")
def read_root():
    return {"message": "Welcome to the summarization API"}
