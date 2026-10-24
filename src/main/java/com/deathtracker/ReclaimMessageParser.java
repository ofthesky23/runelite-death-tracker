package com.deathtracker;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only accepts explicit payment text about a death reclaim, never generic coin changes. */
final class ReclaimMessageParser
{
    private static final Pattern PAID = Pattern.compile("(?i)\\byou\\s+(?:have\\s+)?(?:paid|pay|were\\s+charged)\\s+([0-9][0-9,]*)\\s+(?:coins?|gp)\\b");

    private ReclaimMessageParser() {}

    static Long actualFee(String message)
    {
        if (message == null)
        {
            return null;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (!(lower.contains("reclaim") || lower.contains("retriev") || lower.contains("grave") || lower.contains("death")))
        {
            return null;
        }
        // A quote or offered fee is not evidence of a completed payment.
        if (lower.contains("would") || lower.contains("will cost") || lower.contains("can pay") || lower.contains("fee would"))
        {
            return null;
        }
        Matcher paid = PAID.matcher(message);
        if (!paid.find())
        {
            return null;
        }
        try
        {
            return Long.parseLong(paid.group(1).replace(",", ""));
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}
