import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class BIM {
    private InvertedIndex index;
    private Stemmer stemmer;

    public BIM(InvertedIndex index) {
        this.index = index;
        // Menggunakan stemmer yang sama agar token query matching dengan token di index
        this.stemmer = new PorterStemmer(); 
    }

    /**
     * Menghitung skor relevansi dengan BIM Skenario 1 (tanpa Relevance Judgements)
     * @param rawQuery Query pencarian
     * @return HashMap berisi docID dan skor relevansinya
     */ 
    public HashMap<Integer, Double> calculateScoresScenario1(String rawQuery) {
        HashMap<Integer, Double> documentScores = new HashMap<>();
        
        // Menggunakan Set untuk mencegah duplikasi term pada query
        Set<String> queryTerms = preprocessQuery(rawQuery);
        
        // Mengambil Total Dokumen (N) dari Inverted Index
        double N = index.totalDocuments; 

        // IMPLEMENTASI ASUMSI 2: 
        // HANYA memproses term unik yang ada di dalam query.
        // Term di luar query diabaikan karena probabilitasnya dianggap sama (log(1) = 0).
        for (String term : queryTerms) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            
            // Jika kata tersebut sama sekali tidak ada di corpus dokumen mana pun, lewati
            if (postings == null || postings.isEmpty()) continue;
            
            // Nt = Document Frequency (jumlah dokumen yang mengandung kata ini minimal 1 kali)
            double Nt = postings.size();
            
            // Menghitung bobot term (wt) menggunakan Skenario 1 BIM
            double wt = Math.log10((0.5 * N) / Nt); 
            
            // IMPLEMENTASI ASUMSI 1:
            // Karena antar term diasumsikan independen, probabilitas kemunculan bersama 
            // mereka cukup dijumlahkan saja di dalam ruang logaritma.
            for (InvertedIndex.Posting post : postings) {
                int docId = post.docID;
                
                // Ambil skor sementara dari dokumen ini, jika belum pernah dihitung, mulai dari 0.0
                double currentScore = documentScores.getOrDefault(docId, 0.0);
                
                documentScores.put(docId, currentScore + wt);
            }
        }
        
        return documentScores;
    }

    /**
     * Menghitung skor relevansi dengan BIM Skenario 2 (dengan Relevance Judgements)
     * @param rawQuery Query pencarian
     * @param relevantDocs Himpunan docID yang sudah diketahui relevan (dari qrels)
     */
    public HashMap<Integer, Double> calculateScoresScenario2(String rawQuery, Set<Integer> relevantDocs) {
        HashMap<Integer, Double> documentScores = new HashMap<>();
        
        // Menggunakan Set untuk mencegah duplikasi term pada query
        Set<String> queryTerms = preprocessQuery(rawQuery);
        
        // N = Total seluruh dokumen di corpus
        double N = index.totalDocuments; 
        
        // R = Total dokumen yang relevan dengan query ini
        double R = relevantDocs.size(); 

        // IMPLEMENTASI ASUMSI 2: 
        // HANYA memproses term unik yang ada di dalam query.
        // Term di luar query diabaikan karena probabilitasnya dianggap sama (log(1) = 0).
        for (String term : queryTerms) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            
            if (postings == null || postings.isEmpty()) continue;
            
            // Nt = Document Frequency (jumlah dokumen yang mengandung term t)
            double Nt = postings.size();
            
            // Cari nilai rt (berapa banyak dokumen di 'postings' yang JUGA ada di 'relevantDocs')
            double rt = 0;
            for (InvertedIndex.Posting post : postings) {
                if (relevantDocs.contains(post.docID)) {
                    rt++;
                }
            }
            
            // Menghitung bobot term (wt) menggunakan rumus BIM Skenario 2 (Smoothing)
            double numerator = (rt + 0.5) * (N - R + 1);
            double denominator = (R + 1) * (Nt - rt + 0.5);
            double wt = Math.log10(numerator / denominator); 
            
            // IMPLEMENTASI ASUMSI 1:
            // Karena antar term diasumsikan independen, probabilitas kemunculan bersama 
            // mereka cukup dijumlahkan saja di dalam ruang logaritma.
            for (InvertedIndex.Posting post : postings) {
                int docId = post.docID;
                double currentScore = documentScores.getOrDefault(docId, 0.0);
                documentScores.put(docId, currentScore + wt);
            }
        }
        
        return documentScores;
    }

    /**
     * Fungsi helper untuk memproses input query mentah menjadi himpunan term unik (Set) yang valid
     */
    private Set<String> preprocessQuery(String rawQuery) {
        // Menggunakan HashSet agar kata yang terduplikasi secara otomatis disaring menjadi satu
        Set<String> validTerms = new HashSet<>();
        
        // Memecah kata menggunakan delimiter yang identik dengan InvertedIndex.java
        String[] terms = rawQuery.split("[\\W_]+"); 
        
        for (String t : terms) {
            String clean = t.toLowerCase();
            if (clean.isEmpty() || index.stopWords.contains(clean)) continue;
            // HashSet secara otomatis menolak item jika sudah ada di dalamnya
            validTerms.add(stemmer.stem(clean)); 
        }
        
        return validTerms;
    }
}