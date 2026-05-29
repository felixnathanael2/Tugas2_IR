import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class TwoPoisson {
    private InvertedIndex index;
    private Stemmer stemmer;
    
    // Konstanta k pada rentang 1 <= k < 2
    private double k; 

    /**
     * Constructor untuk Two Poisson
     * @param index Inverted Index yang sudah dibangun
     * @param k Konstanta penyeimbang frekuensi (contoh: 1.5)
     */
    public TwoPoisson(InvertedIndex index, double k) {
        this.index = index;
        this.stemmer = new PorterStemmer(); 
        this.k = k;
    }

    /**
     * Menghitung skor relevansi Skenario 1 (Tanpa Relevance Judgements)
     */
    public HashMap<Integer, Double> calculateScores(String rawQuery) {
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
                
                // Menghitung bobot dengan rumus Two Poisson
                double poissonScore = (ftD * (k + 1) * wt) / (ftD + k);
                
                documentScores.put(docId, currentScore + poissonScore);
            }
        }
        
        return documentScores;
    }

    /**
     * Menghitung skor relevansi Skenario 2 (Dengan Relevance Judgements)
     */
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
            
            // wt berdasarkan BIM Skenario 2 (dengan Smoothing)
            double numerator = (rt + 0.5) * (N - R + 1);
            double denominator = (R + 1) * (Nt - rt + 0.5);
            double wt = Math.log10(numerator / denominator); 
            
            for (InvertedIndex.Posting post : postings) {
                int docId = post.docID;
                double currentScore = documentScores.getOrDefault(docId, 0.0);
                
                // ft,D = frekuensi mentah term t pada dokumen D
                double ftD = post.freq;
                
                // Menghitung bobot dengan rumus Two Poisson
                double poissonScore = (ftD * (k + 1) * wt) / (ftD + k);
                
                documentScores.put(docId, currentScore + poissonScore);
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