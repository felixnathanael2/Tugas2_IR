import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class TwoPoisson {
    private InvertedIndex index;
    private Stemmer stemmer;
    
    //Parameter k berfungsi untuk mengatur seberapa besar pengaruh Term Frequency (TF).
    private double k; 

    /**
     * @param index Inverted Index
     * @param k Konstanta penyeimbang frekuensi (rentang 1 <= k < 2)
     */
    public TwoPoisson(InvertedIndex index, double k) {
        this.index = index;
        this.stemmer = new PorterStemmer(); 
        this.k = k; 
    }

    /**
     * Skenario 1: Pencarian awal tanpa Relevance Judgements
     */
    public HashMap<Integer, Double> calculateScoresScenario1(String rawQuery) {
        HashMap<Integer, Double> documentScores = new HashMap<>();
        Set<String> queryTerms = preprocessQuery(rawQuery);
        
        // N = Total seluruh dokumen di corpus
        double N = index.totalDocuments; 

        for (String term : queryTerms) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            if (postings == null || postings.isEmpty()) continue;
            
            // Nt = Document Frequency (jumlah dokumen yang mengandung term t)
            double Nt = postings.size();
            
            // wt berdasarkan BIM Skenario 1
            double wt = Math.log10((0.5 * N) / Nt); 
            
            for (InvertedIndex.Posting post : postings) {
                int docId = post.docID;
                double currentScore = documentScores.getOrDefault(docId, 0.0);
                
                // frekuensi kemunculan (ftD)
                // Semakin sering kata muncul (ftD besar), nilainya semakin tinggi. Sebaliknya, semakin jarang kata muncul (ftD kecil), nilainya semakin kecil.
                double ftD = post.freq;
                
                // Menghitung bobot dengan rumus Two Poisson
                double poissonScore = (ftD * (k + 1) * wt) / (ftD + k);
                
                documentScores.put(docId, currentScore + poissonScore);
            }
        }
        
        return documentScores;
    }

    /**
     * Skenario 2: Pencarian dengan memanfaatkan Relevance Judgements (Feedback Dokumen Relevan)
     */
    public HashMap<Integer, Double> calculateScoresScenario2(String rawQuery, Set<Integer> relevantDocs) {
        HashMap<Integer, Double> documentScores = new HashMap<>();
        Set<String> queryTerms = preprocessQuery(rawQuery);
        
        // Total dokumen di corpus
        double N = index.totalDocuments;
        // Total dokumen di corpus
        double R = relevantDocs.size(); 

        for (String term : queryTerms) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            if (postings == null || postings.isEmpty()) continue;
            
            // Jumlah dokumen yang mengandung kata ini
            double Nt = postings.size();
            
            // Menghitung rt (jumlah dokumen relevan yang mengandung kata ini)
            double rt = 0;
            for (InvertedIndex.Posting post : postings) {
                if (relevantDocs.contains(post.docID)) {
                    rt++;
                }
            }
            
            // Menghitung bobot kata (wt) dasar menggunakan rumus smoothing relevance judgements
            double numerator = (rt + 0.5) * (N - R + 1);
            double denominator = (R + 1) * (Nt - rt + 0.5);
            double wt = Math.log10(numerator / denominator); 
            
            for (InvertedIndex.Posting post : postings) {
                int docId = post.docID;
                double currentScore = documentScores.getOrDefault(docId, 0.0);
                
                // ft,D = frekuensi mentah term t pada dokumen D
                double ftD = post.freq;
                
                // Menghitung bobot dengan rumus Two Poisson, wt
                double poissonScore = (ftD * (k + 1) * wt) / (ftD + k);
                
                documentScores.put(docId, currentScore + poissonScore);
            }
        }
        
        return documentScores;
    }

    /**
     * Memproses teks input pengguna menjadi sekumpulan kata unik (Set) yang siap diproses
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