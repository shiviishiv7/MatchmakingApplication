package com.shiviishiv7.matchmaking.testing2;


import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import org.springframework.web.multipart.MultipartFile;

public interface IResumeAnalyzerProcessor {

    BaseVO analyze(String jobDescription, MultipartFile resumeFile) throws Exception;
}
