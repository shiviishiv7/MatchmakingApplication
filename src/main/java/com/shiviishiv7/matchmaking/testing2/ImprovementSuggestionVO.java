// ─── ResumeAnalysisResultVO.java ────────────────────────────────────────────
package com.shiviishiv7.matchmaking.testing2;


import lombok.Data;
@Data
public class ImprovementSuggestionVO {
    private int index;
    private String suggestion;
    private String jdReference; // exact phrase from JD this suggestion addresses
}
