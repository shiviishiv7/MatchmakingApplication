package com.shiviishiv7.matchmaking.testing2;

import lombok.Data;

import java.util.List;
@Data
public class ResumeAnalysisResultVO {
    private int matchScore;
    private List<MissingKeywordVO> missingKeywords;
    private List<ImprovementSuggestionVO> improvements;
}

