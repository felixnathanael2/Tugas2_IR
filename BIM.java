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
        
        // Membersihkan query dari tanda baca dan mengambil kata-kata dasarnya yang unik
        Set<String> queryTerms = preprocessQuery(rawQuery);
        
        // N = Total seluruh dokumen di corpus
        double N = index.totalDocuments; 

        for (String term : queryTerms) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            
            // Jika kata tersebut sama sekali tidak ada di corpus dokumen mana pun, lewati
            if (postings == null || postings.isEmpty()) continue;
            
            // Nt = Document Frequency (jumlah dokumen yang mengandung kata ini minimal 1 kali)
            double Nt = postings.size();
            
            // Menghitung bobot kata (wt) menggunakan rumus probabilitas BIM standar.
            // Kata yang dianggap "langka" (Nt kecil) akan mendapat bobot logaritma yang lebih tinggi.
            double wt = Math.log10((0.5 * N) / Nt); 
            
            for (InvertedIndex.Posting post : postings) {
                int docId = post.docID;
                
                // Ambil skor sementara dari dokumen ini, jika belum pernah dihitung, default 0.0
                double currentScore = documentScores.getOrDefault(docId, 0.0);
                
                // Menambahkan bobot kata (wt) ke total skor dokumen
                documentScores.put(docId, currentScore + wt);
            }
        }
        
        // Mengembalikan daftar dokumen beserta skor relevansinya
        return documentScores;
    }

     /**
     * Skenario 2: Menghitung skor relevansi dengan bantuan data dokumen yang sudah dipastikan relevan.
     * @param rawQuery Query pencarian
     * @param relevantDocs Himpunan docID yang sudah diketahui relevan (dari qrels)
     */
    public HashMap<Integer, Double> calculateScoresScenario2(String rawQuery, Set<Integer> relevantDocs) {
        HashMap<Integer, Double> documentScores = new HashMap<>();
        
        // Membersihkan query menjadi himpunan kata dasar unik
        Set<String> queryTerms = preprocessQuery(rawQuery);
        
        // N = Total seluruh dokumen di corpus
        double N = index.totalDocuments; 
        
        // R = Total dokumen yang sudah dinilai relevan oleh sistem/user
        double R = relevantDocs.size(); 

        for (String term : queryTerms) {
            ArrayList<InvertedIndex.Posting> postings = index.getPostings(term);
            
            // Jika kata tersebut sama sekali tidak ada di corpus dokumen mana pun, lewati
            if (postings == null || postings.isEmpty()) continue;
            
            // Nt = Document Frequency (jumlah dokumen yang mengandung term t)
            double Nt = postings.size();
            
            // Menghitung rt: Berapa banyak dari dokumen relevan (R) yang mengandung kata ini minimal 1 kali
            double rt = 0;
            for (InvertedIndex.Posting post : postings) {
                if (relevantDocs.contains(post.docID)) {
                    rt++;
                }
            }
            
            // Menghitung pembilang (numerator) untuk rumus BIM dengan Smoothing (ditambah 0.5)
            double numerator = (rt + 0.5) * (N - R + 1);
            // Menghitung penyebut (denominator) untuk rumus BIM
            double denominator = (R + 1) * (Nt - rt + 0.5);
            // wt = Logaritma basis 10 dari numerator dibagi denominator
            double wt = Math.log10(numerator / denominator); 
            
            for (InvertedIndex.Posting post : postings) {
                int docId = post.docID;
                double currentScore = documentScores.getOrDefault(docId, 0.0);
                // Menambahkan bobot kata (wt) ke total skor dokumen
                documentScores.put(docId, currentScore + wt);
            }
        }
        // Mengembalikan daftar dokumen beserta skor relevansinya
        return documentScores;
    }

    /**
     * Memproses teks input pengguna menjadi sekumpulan kata yang bersih dan siap dicari
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