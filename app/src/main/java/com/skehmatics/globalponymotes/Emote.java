package com.skehmatics.globalponymotes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Emote{

    public final int start;
    public final int end;
    public final int localEnd;
    public final CharSequence name;
    public String quote;

    public Emote (String matchedString, int mStart, int mEnd){
        start = mStart;
        end = mEnd;
        localEnd = matchedString.length()-1;

        int subStringEmoteNameEnd;

        if (matchedString.contains("-") && matchedString.indexOf('-') < localEnd) {
            subStringEmoteNameEnd = matchedString.indexOf('-') - 1;
        } else if (matchedString.contains(" ") && matchedString.indexOf(' ') < localEnd) {
            subStringEmoteNameEnd = matchedString.indexOf(' ');
        } else if (matchedString.contains("\"") && matchedString.indexOf("\"") < localEnd) {
            subStringEmoteNameEnd = matchedString.indexOf("\"") - 1;
        } else {
            subStringEmoteNameEnd = localEnd;
        }
        name = matchedString.subSequence(4, subStringEmoteNameEnd);

        Matcher m = Pattern.compile("\"[^\"\\\\\\r\\n]*(?:\\\\.[^\"\\\\\\r\\n]*)*\"").matcher(matchedString);
        while (m.find()){
            quote = m.group();
        }
    }

    public boolean hasQuote() {
        return quote != null;
    }
}
