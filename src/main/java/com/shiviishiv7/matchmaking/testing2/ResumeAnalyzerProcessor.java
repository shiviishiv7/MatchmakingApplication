package com.shiviishiv7.matchmaking.testing2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ResumeAnalyzerProcessor implements IResumeAnalyzerProcessor {

    @Value("${anthropic.api.key}")
    private String anthropicApiKey;

    @Value("${anthropic.api.model:claude-sonnet-4-6}")
    private String anthropicModel;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION  = "2023-06-01";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BaseVO analyze(String jobDescription, MultipartFile resumeFile) throws Exception {

        // 1. Extract text from PDF
        String resumeText = extractTextFromPdf(resumeFile);
        log.info("Extracted {} characters from resume PDF", resumeText.length());

        // 2. Call Claude API
        ResumeAnalysisResultVO result = callClaudeForAnalysis(jobDescription, resumeText);

        // 3. Build response
        return new BaseVO(200, "Analysis completed successfully.", null, result);
    }

    // ─── PDF Text Extraction ────────────────────────────────────────────────────

    private String extractTextFromPdf(MultipartFile resumeFile) throws Exception {
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(resumeFile.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Could not extract text from the uploaded PDF. " +
                        "Please ensure it is a text-based PDF, not a scanned image.");
            }
            return text.trim();
        }
    }

    // ─── Claude API Call ────────────────────────────────────────────────────────

    private ResumeAnalysisResultVO callClaudeForAnalysis(String jobDescription, String resumeText) throws Exception {

        String prompt = buildPrompt(jobDescription, resumeText);

        String requestBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
            put("model", anthropicModel);
            put("max_tokens", 1500);
            put("messages", List.of(new java.util.HashMap<>() {{
                put("role", "user");
                put("content", prompt);
            }}));
        }});

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ANTHROPIC_API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", anthropicApiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            log.error("Claude API error: status={} body={}", httpResponse.statusCode(), httpResponse.body());
            throw new RuntimeException("AI analysis failed. Please try again later.");
        }

        return parseClaudeResponse(httpResponse.body());
    }

    // ─── Prompt Construction ────────────────────────────────────────────────────

    private String buildPrompt(String jobDescription, String resumeText) {
        return """
                You are an expert technical recruiter and resume coach. Analyze the resume against the job description below.

                Respond ONLY with a valid JSON object — no preamble, no markdown, no explanation. Use exactly this structure:

                {
                  "matchScore": <integer 0-100>,
                  "missingKeywords": [
                    { "keyword": "<skill/tool/qualification>", "category": "<Skill|Tool|Certification|Experience>" },
                    ... (5 to 10 items)
                  ],
                  "improvements": [
                    { "index": 1, "suggestion": "<concrete actionable suggestion>", "jdReference": "<exact phrase from JD this addresses>" },
                    { "index": 2, "suggestion": "<concrete actionable suggestion>", "jdReference": "<exact phrase from JD this addresses>" },
                    { "index": 3, "suggestion": "<concrete actionable suggestion>", "jdReference": "<exact phrase from JD this addresses>" }
                  ]
                }

                Scoring criteria:
                - Skills match (40%%): Does the resume have the required technical skills?
                - Experience level (30%%): Does the candidate's experience align with the role requirements?
                - Keywords (20%%): Are JD-specific keywords present in the resume?
                - Role requirements (10%%): Education, certifications, domain knowledge.

                Rules:
                - missingKeywords must list items present in the JD but absent or weak in the resume.
                - Each improvement must be specific and actionable — reference an exact phrase from the JD.
                - Do not give generic advice. Be precise.

                ---
                JOB DESCRIPTION:
                %s

                ---
                RESUME:
                %s
                """.formatted(jobDescription, resumeText);
    }

    // ─── Response Parsing ───────────────────────────────────────────────────────

    private ResumeAnalysisResultVO parseClaudeResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String rawContent = root.path("content").get(0).path("text").asText();

        // Strip markdown code fences if Claude wraps the JSON
        String cleaned = rawContent
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        JsonNode json = objectMapper.readTree(cleaned);

        ResumeAnalysisResultVO result = new ResumeAnalysisResultVO();
        result.setMatchScore(json.path("matchScore").asInt());

        // Parse missing keywords
        List<MissingKeywordVO> keywords = new ArrayList<>();
        for (JsonNode kw : json.path("missingKeywords")) {
            MissingKeywordVO vo = new MissingKeywordVO();
            vo.setKeyword(kw.path("keyword").asText());
            vo.setCategory(kw.path("category").asText());
            keywords.add(vo);
        }
        result.setMissingKeywords(keywords);

        // Parse improvements
        List<ImprovementSuggestionVO> improvements = new ArrayList<>();
        for (JsonNode imp : json.path("improvements")) {
            ImprovementSuggestionVO vo = new ImprovementSuggestionVO();
            vo.setIndex(imp.path("index").asInt());
            vo.setSuggestion(imp.path("suggestion").asText());
            vo.setJdReference(imp.path("jdReference").asText());
            improvements.add(vo);
        }
        result.setImprovements(improvements);

        return result;
    }
}