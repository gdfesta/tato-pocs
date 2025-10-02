-- Initial schema for Form Sentiment Analysis application
-- Table to store form submissions and sentiment analysis results
CREATE TABLE form_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    review_text TEXT NOT NULL,
    sentiment VARCHAR(20) CHECK (sentiment IN ('Positive', 'Negative', 'Neutral')),
    raw_llm_response TEXT, -- Store the full LLM response for debugging
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for common queries
CREATE INDEX idx_form_submissions_created_at ON form_submissions(created_at);
CREATE INDEX idx_form_submissions_sentiment ON form_submissions(sentiment);