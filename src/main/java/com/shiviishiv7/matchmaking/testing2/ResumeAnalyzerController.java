package com.shiviishiv7.matchmaking.testing2;


import com.shiviishiv7.matchmaking.common.security.MatchmakingSecurityUtility;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/resume-analyzer")
@Slf4j
@Tag(name = "Resume Analyzer", description = "AI-powered resume vs job description analysis")
public class ResumeAnalyzerController {

    @Autowired
    private IResumeAnalyzerProcessor resumeAnalyzerProcessor;

    @Autowired
    private MatchmakingSecurityUtility securityUtility;

    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Analyze resume against a job description using AI")
    public ResponseEntity<BaseVO> analyze(
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam("resumeFile") MultipartFile resumeFile
    ) throws Exception {

        String sub = securityUtility.getAuthenticatedUserSub();
        log.info("Resume analysis request received for sub: {}", sub);

        if (jobDescription == null || jobDescription.isBlank()) {
            BaseVO error = new BaseVO();
//            error.setSuccess(false);
            error.setMessage("Job description must not be empty.");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        if (resumeFile == null || resumeFile.isEmpty()) {
            BaseVO error = new BaseVO();
//            error.setSuccess(false);
            error.setMessage("Resume PDF file must not be empty.");
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }

        BaseVO response = resumeAnalyzerProcessor.analyze(jobDescription, resumeFile);
        log.info("Resume analysis completed for sub: {}", sub);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
