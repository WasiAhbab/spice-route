package com.spiceroute.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SummaryValidatorTest {
    private final SummaryValidator validator = new SummaryValidator();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void countsOnlyDistinctValidCommentIds() throws Exception {
        var input = json.readTree("""
            {"sentiment":"mixed","topics":[{"title":"Service","kind":"complaint",
            "suggestion":"Improve service","ids":[0,0,1,-1,99,"0"]}]}
            """);
        var report = validator.validate(input, List.of("Slow service", "Long wait", "Great food"));
        assertEquals(2, report.topics().getFirst().count());
        assertEquals(List.of("Slow service", "Long wait"), report.topics().getFirst().examples());
    }

    @Test
    void rejectsBlankReports() throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> validator.validate(json.readTree("{\"sentiment\":\"\",\"topics\":[]}"), List.of("Good food")));
    }
}
