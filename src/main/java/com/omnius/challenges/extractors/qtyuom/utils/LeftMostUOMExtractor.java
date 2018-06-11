package com.omnius.challenges.extractors.qtyuom.utils;

import com.omnius.challenges.extractors.qtyuom.QtyUomExtractor;
import com.omnius.challenges.extractors.qtyuom.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements {@link QtyUomExtractor} identifying as <strong>the most relevant UOM</strong> the leftmost UOM found in the articleDescription.
 * The array contains the list of valid UOMs. The algorithm search for the leftmost occurence of UOM[i], if there are no occurrences then tries UOM[i+1].
 *
 * Example
 * <ul>
 * <li>article description: "black steel bar 35 mm 77 stck"</li>
 * <li>QTY: "77" (and NOT "35")</li>
 * <li>UOM: "stck" (and not "mm" since "stck" has an higher priority as UOM )</li>
 * </ul>
 *
 * @author <a href="mailto:damiano@searchink.com">Damiano Giampaoli</a>
 * @since 4 May 2018
 */
public class LeftMostUOMExtractor implements QtyUomExtractor {

    /**
     * Array of valid UOM to match. the elements with lower index in the array has higher priority
     */
    public static String[] UOM = {"stk", "stk.", "stck", "stück", "stg", "stg.", "st", "st.", "stange", "stange(n)", "tafel", "tfl", "taf", "mtr", "meter", "qm", "kg", "lfm", "mm", "m"};

    private static final Logger log = LoggerFactory.getLogger(LeftMostUOMExtractor.class);

    public LeftMostUOMExtractor() {}

    @Override
    public Pair<String, String> extract(String articleDescription) {
        try {
            if (articleDescription != null && !articleDescription.isEmpty()) {
                articleDescription = articleDescription.replaceAll("\\s", "  ");
                Pattern pattern = getPattern();
                Matcher m = pattern.matcher(articleDescription);
                String pairs = "";
                int priority = UOM.length;
                String quantity = "";
                String measure = "";
                while (m.find()) {
                    String tempPairs = m.group().trim();
                    if (tempPairs.startsWith("("))
                        tempPairs = tempPairs.substring(1, tempPairs.length());
                    String tempQuantity = tempPairs.replaceAll("\\p{IsAlphabetic}+(\\.)?(\\(n\\))?", "");
                    String tempMeasure = tempPairs.replaceAll(tempQuantity, "").toLowerCase();
                    int tempPriority = getPriority(tempMeasure);
                    if (tempPriority < priority) {
                        priority = tempPriority;
                        pairs = tempPairs;
                        quantity = tempQuantity;
                        measure = tempMeasure;
                    }
                }
                log.debug(" Article Description:" + articleDescription + " Measure:" + measure + " Quantitiy:" + quantity);
                if (pairs != null && !pairs.isEmpty() && !pairs.equalsIgnoreCase(""))
                    return new Pair<String, String>(quantity.replaceAll(" ", ""), measure);
            }
        }catch (Exception e){
            log.error("Exract Error:"+e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public Pair<Double, String> extractAsDouble(String articleDescription) {
        Pair<String,String> pair = extract(articleDescription);

        if (pair != null)
            //it could be better returning BigDecimal instead of Double because of Bit restriction.
            return new Pair<Double, String>(getDouble(pair.getFirst()),pair.getSecond());

        return null;
    }

    public Double getDouble(String doubleStrIn) {
        try {
            if (doubleStrIn.matches("\\d{1,3}((,\\d{3})+)?(\\.\\d+)"))
                return Double.parseDouble(doubleStrIn.replaceAll(",", ""));
            else if (doubleStrIn.matches("\\d{1,3}((\\.\\d{3})+)?(,\\d+)"))
                return Double.parseDouble((doubleStrIn.replaceAll("\\.", "").replaceAll(",", ".")));
            else if (doubleStrIn.matches("\\d+(,)\\d+"))
                return Double.parseDouble(doubleStrIn.replaceAll(",", "."));
            else {
                return Double.parseDouble(doubleStrIn);
            }
        }catch (Exception e){
            log.error("Parsing Error:"+e.getLocalizedMessage());
            return null;
        }
    }

    public int getPriority(String measure) {
        for(int i = 0; i<UOM.length ; i++){
            if(UOM[i].equals(measure)){
                return i;
            }
        }
        return UOM.length-1;
    }

    public Pattern getPattern() {
        String regex = "(?i)";
        regex = regex + UOM[0];
        for (int i = 1; i < UOM.length; i++) {
            regex = regex + "|"+(UOM[i].replace(".", "\\.").replace("(", "\\(").replace(")", "\\)"));
        }

        Pattern pattern = Pattern.compile("((((\\()|\\s|^|^$)\\d{1,3}((\\s{2}\\d{3})+)?(((\\s{2}(,|\\.)\\s{2})|(,|\\.))\\d+)?\\s{2}(?:"+regex+"))"
                + "|(((\\()|\\s|^|^$)\\d{1,3}((((\\s{2},\\s{2})|(,))\\d{3})+)?(((\\s{2}\\.\\s{2})|(\\.))\\d+)?\\s{2}(?:"+regex+"))"
                + "|(((\\()|\\s|^|^$)\\d{1,3}((((\\s{2}\\.\\s{2})|(\\.))\\d{3})+)?(((\\s{2},\\s{2})|(,))\\d+)?\\s{2}(?:"+regex+"))"
                + "|(((\\()|\\s|^|^$)\\d{4,}(((\\s{2}(,|\\.)\\s{2})|(,|\\.))\\d+)?\\s{2}(?:"+regex+")))($|\\s|^$|([^a-zA-Z0-9_\\(\\)]))");
        return pattern;
    }
}
