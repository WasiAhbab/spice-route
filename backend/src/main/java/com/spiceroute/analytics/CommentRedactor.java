package com.spiceroute.analytics;

import com.spiceroute.feedback.FeedbackView;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CommentRedactor {

    private static final Pattern EMAIL = Pattern.compile("(?i)[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("[+\\p{Nd}][\\p{Nd} .()-]{6,}\\p{Nd}");

    public String redact(FeedbackView feedback) {
        String comment = feedback.comment();
        if (comment == null) return "";
        comment = removeLiteral(comment, feedback.customerName());
        comment = removeLiteral(comment, feedback.phone());
        return PHONE.matcher(EMAIL.matcher(comment).replaceAll("[redacted]")).replaceAll("[redacted]").strip();
    }

    private String removeLiteral(String comment, String value) {
        if (value == null || value.isBlank()) return comment;
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
            .matcher(comment).replaceAll("[redacted]");
    }

    public boolean meaningful(String comment) {
        String text = comment.replace("[redacted]", "").strip();
        return text.length() >= 4 && text.codePoints().filter(Character::isLetter).count() >= 3;
    }
}
