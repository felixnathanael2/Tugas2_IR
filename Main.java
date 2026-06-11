import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // 1. Inisialisasi Inverted Index 
        // Pastikan folder "corpus" dan file "stopwords.txt" berada di direktori yang tepat
        System.out.println("Membangun Inverted Index...");
        InvertedIndex index = new InvertedIndex();
        System.out.println("Total dokumen di corpus: " + index.totalDocuments);

        // Pastikan indeks tidak kosong sebelum lanjut
        if (index.totalDocuments == 0) {
            System.out.println("Error: Tidak ada dokumen yang terbaca. Cek folder corpus.");
            return;
        }

        // 2. Inisialisasi Model BIM dan Two Poisson
        BIM bimModel = new BIM(index);
        double k = 1.5; // Konstanta untuk Two Poisson (sesuai ppt)
        double b = 0.75; // Konstanta untuk BM25 (sesuai ppt)
        TwoPoisson tpModel = new TwoPoisson(index, k);
        BM25 bm25Model = new BM25(index, k, b);
        BM11 bm11Model = new BM11(index, k);

        // 3. Persiapan Query dan Relevance Judgements
        System.out.println("Masukkan Query : ");
        // String query = "what similarity laws must be obeyed when constructing aeroelastic models of heated high speed aircraft ."; 
        String query = sc.nextLine(); 
        
        // Simulasi Skenario 2: Anggaplah dari file qrels, kita tahu bahwa
        // Dokumen dengan ID di bawah ini merupakan dokumen relevan untuk query di atas 
        Set<Integer> relevantDocs = new HashSet<>(Arrays.asList(
            12, 14, 15, 51, 52, 102, 184, 202, 285, 380, 390, 391, 
            442, 497, 643, 746, 856, 857, 858, 859, 864, 877, 948, 658
        ));

        System.out.println("\n=== PENGETESAN QUERY: '" + query + "' ===");

        // --- TEST BIM ---
        System.out.println("\n--- 1. Hasil BIM (Skenario 1 - Tanpa Relevance Judgements) ---");
        HashMap<Integer, Double> bimScores1 = bimModel.calculateScoresScenario1(query);
        evaluationMetrics(bimScores1, relevantDocs, index.totalDocuments);

        System.out.println("\n--- 2. Hasil BIM (Skenario 2 - Dengan Relevance Judgements) ---");
        HashMap<Integer, Double> bimScores2 = bimModel.calculateScoresScenario2(query, relevantDocs);
        evaluationMetrics(bimScores2, relevantDocs, index.totalDocuments);

        // --- TEST TWO POISSON ---
        System.out.println("\n--- 3. Hasil Two Poisson (Skenario 1) ---");
        HashMap<Integer, Double> tpScores1 = tpModel.calculateScoresScenario1(query);
        evaluationMetrics(tpScores1, relevantDocs, index.totalDocuments);

        System.out.println("\n--- 4. Hasil Two Poisson (Skenario 2) ---");
        HashMap<Integer, Double> tpScores2 = tpModel.calculateScoresScenario2(query, relevantDocs);
        evaluationMetrics(tpScores2, relevantDocs, index.totalDocuments);
        
        System.out.println("\n--- 5. Hasil BM11 (Skenario 1) ---");
        HashMap<Integer, Double> bm11Scores1 = bm11Model.calculateScoresScenario1(query);
        evaluationMetrics(bm11Scores1, relevantDocs, index.totalDocuments);

        System.out.println("\n--- 6. Hasil BM11 (Skenario 2) ---");
        HashMap<Integer, Double> bm11Scores2 = bm11Model.calculateScoresScenario2(query, relevantDocs);
        evaluationMetrics(bm11Scores2, relevantDocs, index.totalDocuments);
        
        System.out.println("\n--- 7. Hasil BM25 (Skenario 1) ---");
        HashMap<Integer, Double> bm25Scores1 = bm25Model.calculateScoresScenario1(query);
        evaluationMetrics(bm25Scores1, relevantDocs, index.totalDocuments);

        System.out.println("\n--- 8. Hasil BM25 (Skenario 2) ---");
        HashMap<Integer, Double> bm25Scores2 = bm25Model.calculateScoresScenario2(query, relevantDocs);
        evaluationMetrics(bm25Scores2, relevantDocs, index.totalDocuments);
    }
    
    public static void evaluationMetrics(HashMap<Integer, Double> scores, Set<Integer> groundTruth, int totalDocs) {
        if (scores.isEmpty()) {
            System.out.println("Tidak ada dokumen yang cocok dengan query tersebut.");
            return;
        }

        // Convert Map ke List agar bisa diurutkan (sorting)
        List<Map.Entry<Integer, Double>> list = new ArrayList<>(scores.entrySet());

        // Urutkan secara descending (skor tertinggi ke terendah)
        list.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        // Tampilkan hasil (maksimal top 10 agar terminal tidak terlalu penuh)
        int rank = 1;
        for (Map.Entry<Integer, Double> entry : list) {
            System.out.printf("Rank %d | DocID: %d | Score: %.4f\n", rank, entry.getKey(), entry.getValue());
            rank++;
            if (rank > 10)
                break;
        }

        int totalRelevanDiKunci = groundTruth.size(); 

        // Set nilai K untuk evaluasi top-k 
        int daftarK[] = { 1, 3, 5, 10 };

        List<Double> catatanRecallAsli = new ArrayList<>();
        List<Double> catatanPrecisionAsli = new ArrayList<>();
        double pAt1 = 0, pAt3 = 0, pAt5 = 0, pAt10 = 0;

        int batasRank = Math.min(list.size(), 10);

        int tp = 0; //True Positives
        int fp = 0; //False Positives  
        int fn = 0; //False Negatives
        int tn = 0; //True Negatives

        //Hitung true positive dan false positive 
        for (int i = 1; i <= batasRank; i++) {
            int docId = list.get(i - 1).getKey();

            if (groundTruth.contains(docId)) {
                tp++; // Dokumen di Top 10 beneran relevan
                catatanRecallAsli.add((double) tp / totalRelevanDiKunci);
                catatanPrecisionAsli.add((double) tp / i);
            }

            // Hitung Precision@K
            if (i == 1)
                pAt1 = (double) tp / 1;
            if (i == 3)
                pAt3 = (double) tp / 3;
            if (i == 5)
                pAt5 = (double) tp / 5;
            if (i == 10)
                pAt10 = (double) tp / 10;
        }
        
        fp = batasRank - tp; // Total dokumen tampil dikurang yang benar
        fn = totalRelevanDiKunci - tp; // Total kunci jawaban dikurang yang berhasil ketemu
        tn = totalDocs - (tp + fp + fn); // Sisa dokumen bersih di corpus yang aman

        double precision = (tp + fp == 0) ? 0.0 : (double) tp / (tp + fp);
        double recall = (tp + fn == 0) ? 0.0 : (double) tp / (tp + fn);
        
        //Hitung 11-point average precision
        double[] sebelasTitikRecall = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        double[] hasilPrecisionInterpolasi = new double[11];
        double totalP11Point = 0.0;

        for (int i = 0; i < 11; i++) {
            double rTarget = sebelasTitikRecall[i];
            double nilaiMaksimalKanan = 0.0;
            for (int j = 0; j < catatanRecallAsli.size(); j++) {
                if (catatanRecallAsli.get(j) >= rTarget) {
                    if (catatanPrecisionAsli.get(j) > nilaiMaksimalKanan) {
                        nilaiMaksimalKanan = catatanPrecisionAsli.get(j);
                    }
                }
            }
            hasilPrecisionInterpolasi[i] = nilaiMaksimalKanan;
            totalP11Point += nilaiMaksimalKanan;
        }
        double rataRata11Point = totalP11Point / 11;

        System.out.println("---------------------------------------------");
        System.out.println("HASIL EVALUASI:");
        System.out.printf("    [TP: %d | FP: %d | FN: %d | TN: %d]\n", tp, fp, fn, tn);
        System.out.println("    -----------------------------------------");
        System.out.printf("    Precision Score: %.2f%%\n", precision * 100);
        System.out.printf("    Recall Score: %.2f%%\n", recall * 100);
        System.out.println("    -----------------------------------------");
        System.out.printf("    Precision @1  : %.2f%%\n", pAt1 * 100);
        System.out.printf("    Precision @3  : %.2f%%\n", pAt3 * 100);
        System.out.printf("    Precision @5  : %.2f%%\n", pAt5 * 100);
        System.out.printf("    Precision @10 : %.2f%%\n", pAt10 * 100);
        System.out.println("    -----------------------------------------");
        System.out.printf("    11-Point Average Precision: %.2f%%\n", rataRata11Point * 100);
        System.out.println("=============================================");
    }
}