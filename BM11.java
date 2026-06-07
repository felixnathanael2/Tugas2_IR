import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class BM11 {
    private InvertedIndex index;
    private Stemmer stemmer;
    
    // Konstanta k pada rentang 1 <= k < 2
    private double k;

    /**
     * Constructor untuk BM11
     * @param index Inverted Index yang sudah dibangun
     * @param k Konstanta penyeimbang frekuensi (contoh: 1.5)
     */
    public BM11(InvertedIndex index, double k) {
        this.index = index;
        this.stemmer = new PorterStemmer(); 
        this.k = k;
    }

    /**
     * Menghitung skor relevansi Skenario 1 (Tanpa Relevance Judgements)
     */
    public HashMap<Integer, Double> calculateScoresScenario1(String rawQuery) {
        HashMap<Integer, Double> documentScores = new HashMap<>();
        Set<String> queryTerms = preprocessQuery(rawQuery);
        
        double N = index.totalDocuments; 

        for (String term : queryTerms) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            if (postings == null || postings.isEmpty()) continue;
            
            double Nt = postings.size();
            
            // wt berdasarkan BIM Skenario 1
            double wt = Math.log10((0.5 * N) / Nt); 
            
            for (InvertedIndex.Posting post : postings) {
                int docId = post.docID;
                double currentScore = documentScores.getOrDefault(docId, 0.0);
                
                // ft,D = frekuensi mentah term t pada dokumen D
                double ftD = post.freq;
                
                int ld = index.getDocumentLength(docId);
                int lavg = index.getDocumentAvgLength();
                double relevanceScore = (ftD * (k + 1) * wt) / (ftD + (k * ld / lavg));
                
                documentScores.put(docId, currentScore + relevanceScore);
            }
        }
        
        return documentScores;
    }

    public HashMap<Integer, Double> calculateScoresScenario2(String rawQuery, Set<Integer> relevantDocs) {
        HashMap<Integer, Double> documentScores = new HashMap<>();
        Set<String> queryTerms = preprocessQuery(rawQuery);
        
        double N = index.totalDocuments; 
        double R = relevantDocs.size(); 

        for (String term : queryTerms) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            if (postings == null || postings.isEmpty()) continue;
            
            double Nt = postings.size();
            
            double rt = 0;
            for (InvertedIndex.Posting post : postings) {
                if (relevantDocs.contains(post.docID)) {
                    rt++;
                }
            }
            
            double numerator = (rt + 0.5) * (N - R + 1);
            double denominator = (R + 1) * (Nt - rt + 0.5);
            double wt = Math.log10(numerator / denominator); 
            
            for (InvertedIndex.Posting post : postings) {
                int docId = post.docID;
                double currentScore = documentScores.getOrDefault(docId, 0.0);
                
                // ft,D = frekuensi mentah term t pada dokumen D
                double ftD = post.freq;
                
                int ld = index.getDocumentLength(docId);
                int lavg = index.getDocumentAvgLength();
                double relevanceScore = (ftD * (k + 1) * wt) / (ftD + (k * ld / lavg));
                
                documentScores.put(docId, currentScore + relevanceScore);
            }
        }
        
        return documentScores;
    }

    /**
     * Fungsi helper untuk memproses query menjadi himpunan unik
     */
    private Set<String> preprocessQuery(String rawQuery) {
        Set<String> validTerms = new HashSet<>();
        String[] terms = rawQuery.split("[\\W_]+"); 
        
        for (String t : terms) {
            String clean = t.toLowerCase();
            if (clean.isEmpty() || index.stopWords.contains(clean)) continue;
            validTerms.add(stemmer.stem(clean)); 
        }
        
        return validTerms;
    }
}