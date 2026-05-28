// import java.util.function.Predicate;

public class PorterStemmer implements Stemmer {
    // m
    private int m(String word) {
        int i, m = 0, len = word.length();
        boolean isC = true;
        for(i = 0; i < len; i++){
            if(!isVocal(word, i)){ // C
                if(isC == false) m++; // VC
                isC = true;
            } else { // V, mulai ngitung VC brp kali
                isC = false;
            }
        }
        return m;
    }

    // *S, gituan mah pake endsWith aja

    // *v* (mengandung vokal)
    private boolean _v_(String word){
        int i, len = word.length();
        for(i = 0; i < len; i++){
            if(isVocal(word, i)) return true;
        }
        return false;
    }

    // *d (diakhiri konsonan ganda yg sama)
    private boolean _d(String word){
        int len = word.length();
        return (len >= 2) && !isVocal(word, len - 1) && (word.charAt(len -  1) == word.charAt(len - 2)); 
    }

    // *o (diakhiri cvc, c terakhir bukan W, X, Y)
    private boolean _o(String word){
        int len = word.length();
        return (len >= 3) && !isVocal(word, len - 3) && isVocal(word, len - 2) && !isVocal(word, len - 1) && 
               word.charAt(len - 1) != 'w' && word.charAt(len - 1) != 'x' && word.charAt(len - 1) != 'y'; 
    }

    @Override
    public String stem(String word) {
        if(word.length() <= 2) return word;
        // STEP 1, 2, 3, 4, 5
        return step5b(step5a(step4(step3(step2(step1c(step1b(step1a(word))))))));
    }

    private boolean isAIUEO(String word, int idx){
        return word.charAt(idx) == 'a' || word.charAt(idx) == 'i' || word.charAt(idx) == 'u' || word.charAt(idx) == 'e' || word.charAt(idx) == 'o';
    }

    private boolean isVocal(String word, int idx){ // isConsonant tinggal !isVocal
        // kalo Y, cek didahului konsonan atau engga
        boolean vocalY = false;
        if(idx > 0 && word.charAt(idx) == 'y'){ // 'y' gausah isAIUEO
            if(!isVocal(word, idx - 1)){ // jadi rekursif njir
                vocalY = true;
            }
        }

        return isAIUEO(word, idx) || vocalY;
    }

    // helper STEP
    // https://www.javapro.academy/understanding-java-util-function-predicate-in-java/
    // private String replace(String word, String oldSuffix, String newSuffix, Predicate<String> condition){
    //     if(word.endsWith(oldSuffix)){
    //         String stem = word.substring(0, word.length() - oldSuffix.length());
    //         if(condition.test(stem)){
    //             return stem + newSuffix;
    //         }
    //     }
    //     return word;
    // }
    // ribet gajadi dipake

    // https://stackoverflow.com/questions/12310978/check-if-string-ends-with-certain-pattern
    private String step1a(String word){
        int len = word.length();
        // endsWith dipake di awal biar gausah ngecek length segala (sama juga buat step lain)
        if(word.endsWith("sses")) return word.substring(0, len - 4) + "ss"; // SSES -> SS
        else if (word.endsWith("ies")) return word.substring(0, len - 3) + "i"; // IES -> I
        else if (word.endsWith("ss")) return word; // tetep harus pake biar ga masuk kondisi 4 (S -> )
        else if (word.endsWith("s")) return word.substring(0, len - 1); // S -> 
        return word;
    }
    private String step1b(String word){
        int len = word.length();
        boolean more = false;

        if(word.endsWith("eed")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 0) word = stem + "ee";
        } else if(word.endsWith("ed")){
            String stem = word.substring(0, len - 2);
            if(_v_(stem)) {
                word = stem;
                more = true;
            }
        } else if(word.endsWith("ing")){
            String stem = word.substring(0, len - 3);
            if(_v_(stem)) {
                word = stem;
                more = true;
            }
        }

        if(more){
            len = word.length();
            if(word.endsWith("at") || word.endsWith("bl") || word.endsWith("iz")) word += "e";
            else if(_d(word) && !(word.endsWith("l") || word.endsWith("s") || word.endsWith("z"))) word = word.substring(0, len - 1);
            // HOPPING -> HOPP -> HOP
            else if(m(word) == 1 && _o(word)) word += "e";
        }

        return word;
    }
    private String step1c(String word){
        int len = word.length();
        if(word.endsWith("y")){
            String stem = word.substring(0, len - 1);
            if(_v_(stem)) word = stem + "i";
        }
        return word;
    }

    private String step2(String word){
        int len = word.length();

        if(word.endsWith("ational")){
            String stem = word.substring(0, len - 7);
            if(m(stem) > 0) word = stem + "ate";
        } else if(word.endsWith("tional")){
            String stem = word.substring(0, len - 6);
            if(m(stem) > 0) word = stem + "tion";
        } else if(word.endsWith("enci")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 0) word = stem + "ence";
        } else if(word.endsWith("anci")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 0) word = stem + "ance";
        } else if(word.endsWith("izer")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 0) word = stem + "ize";
        } else if(word.endsWith("abli")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 0) word = stem + "able";
        } else if(word.endsWith("alli")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 0) word = stem + "al";
        } else if(word.endsWith("entli")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem + "ent";
        } else if(word.endsWith("eli")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 0) word = stem + "e";
        } else if(word.endsWith("ousli")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem + "ous";
        } else if(word.endsWith("ization")){
            String stem = word.substring(0, len - 7);
            if(m(stem) > 0) word = stem + "ize";
        } else if(word.endsWith("ation")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem + "ate";
        } else if(word.endsWith("ator")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 0) word = stem + "ate";
        } else if(word.endsWith("alism")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem + "al";
        } else if(word.endsWith("iveness")){
            String stem = word.substring(0, len - 7);
            if(m(stem) > 0) word = stem + "ive";
        } else if(word.endsWith("fulness")){
            String stem = word.substring(0, len - 7);
            if(m(stem) > 0) word = stem + "ful";
        } else if(word.endsWith("ousness")){
            String stem = word.substring(0, len - 7);
            if(m(stem) > 0) word = stem + "ous";
        } else if(word.endsWith("aliti")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem + "al";
        } else if(word.endsWith("iviti")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem + "ive";
        } else if(word.endsWith("biliti")){
            String stem = word.substring(0, len - 6);
            if(m(stem) > 0) word = stem + "ble";
        }

        return word;
    }

    private String step3(String word){
        int len = word.length();

        if(word.endsWith("icate")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem + "ic";
        } else if(word.endsWith("ative")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem;
        } else if(word.endsWith("alize")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem + "al";
        } else if(word.endsWith("iciti")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 0) word = stem + "ic";
        } else if(word.endsWith("ical")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 0) word = stem + "ic";
        // di ppt ada ousness -> ous padahal udah ada di step 2
        // kalo di internet ga ada ousness -> ous di step 3, jadi gw ga tulis  
        } else if(word.endsWith("ful")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 0) word = stem;
        } else if(word.endsWith("ness")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 0) word = stem;
        }

        return word;
    }

    private String step4(String word){
        int len = word.length();

        if(word.endsWith("al")){
            String stem = word.substring(0, len - 2);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ance")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ence")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("er")){
            String stem = word.substring(0, len - 2);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ic")){
            String stem = word.substring(0, len - 2);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("able")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ible")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ant")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ement")){
            String stem = word.substring(0, len - 5);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ment")){
            String stem = word.substring(0, len - 4);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ent")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ion")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 1 && (stem.endsWith("s") || stem.endsWith("t"))) word = stem;
        } else if(word.endsWith("ou")){
            String stem = word.substring(0, len - 2);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ism")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ate")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 1) word = stem; //
        } else if(word.endsWith("iti")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ous")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ive")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 1) word = stem;
        } else if(word.endsWith("ize")){
            String stem = word.substring(0, len - 3);
            if(m(stem) > 1) word = stem;
        }

        return word;
    }

    private String step5a(String word){
        int len = word.length();

        if(word.endsWith("e")){
            String stem = word.substring(0, len - 1);
            if(m(stem) > 1) word = stem;
            else if(m(stem) == 1 && !_o(stem)) word = stem;
        }

        return word;
    }
    private String step5b(String word){
        int len = word.length();

        if(m(word) > 1 && _d(word) && word.endsWith("l")) word = word.substring(0, len - 1);

        return word;
    }

    // debug
    // public static void main(String[] args) {
    //     Stemmer ps = new PorterStemmer();
    //     String text = "Such an analysis can reveal features that are not easily visible from the variations in the individual genes and can lead to a picture of expression that is more biologically transparent and accessible to interpretation";
    //     for(String word : text.split(" ")){
    //         System.out.print(ps.stem(word) + " ");
    //     }

    //     // Such an analysi can reveal featur that ar not easili visibl from the variat in the individu gene and can lead to a pictur of express that i more biolog transpar and access to interpret
    //     // is mestinya ga jadi i
    //     // ternyata stem cuman kalo length > 2 (https://tartarus.org/martin/PorterStemmer/)
    // }
}
