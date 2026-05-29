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
        TwoPoisson tpModel = new TwoPoisson(index, k);

        // 3. Persiapan Query dan Relevance Judgements
        // GANTI string ini dengan kata yang kamu tahu PASTI ada di dalam teks corpus milikmu
        System.out.println("Masukkan Query : ");
        // String query = "two dimensional"; 
        String query = sc.nextLine(); 
        
        
        // Simulasi Skenario 2: Anggaplah dari file qrels, kita tahu bahwa
        // Dokumen dengan ID 1, 3, dan 5 adalah dokumen yang relevan untuk query di atas
        Set<Integer> relevantDocs = new HashSet<>(Arrays.asList(1, 3, 5));

        System.out.println("\n=== PENGETESAN QUERY: '" + query + "' ===");

        // --- TEST BIM ---
        System.out.println("\n--- 1. Hasil BIM (Skenario 1 - Tanpa Relevance Judgements) ---");
        HashMap<Integer, Double> bimScores1 = bimModel.calculateScoresScenario1(query);
        printRankedScores(bimScores1);

        System.out.println("\n--- 2. Hasil BIM (Skenario 2 - Dengan Relevance Judgements) ---");
        HashMap<Integer, Double> bimScores2 = bimModel.calculateScoresScenario2(query, relevantDocs);
        printRankedScores(bimScores2);

        // --- TEST TWO POISSON ---
        System.out.println("\n--- 3. Hasil Two Poisson (Skenario 1) ---");
        HashMap<Integer, Double> tpScores1 = tpModel.calculateScores(query);
        printRankedScores(tpScores1);

        System.out.println("\n--- 4. Hasil Two Poisson (Skenario 2) ---");
        HashMap<Integer, Double> tpScores2 = tpModel.calculateScoresScenario2(query, relevantDocs);
        printRankedScores(tpScores2);
    }

    /**
     * Helper method untuk mengurutkan HashMap berdasarkan skor tertinggi ke terendah,
     * lalu mencetaknya ke terminal. Karena inti dari IR adalah "Ranking".
     */
    public static void printRankedScores(HashMap<Integer, Double> scores) {
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
            if (rank > 10) break; 
        }
    }
}

/*
Query: Two Dimensional
1. Hasil BIM (Skor Seragam)
Hasil 1 dan Hasil 2 Semua dokumen di peringkat 10 besar mendapatkan skor yang sama persis (0.2840 untuk Skenario 1, dan 0.1587 untuk Skenario 2).
Faktor Penentu: BIM hanya peduli apakah sebuah term itu ada atau tidak di dalam dokumen.  
Analisis: Dokumen 2, 4, hingga 15 semuanya terdeteksi mengandung kata "two" dan/atau "dimensional". Karena BIM tidak memedulikan frekuensi, dokumen yang menyebutkan kata tersebut 1 kali mendapatkan skor yang sama persis dengan dokumen yang menyebutkannya 50 kali.  Perbedaan Skenario 1 vs 2: Skor Skenario 2 lebih kecil (0.1587) karena menggunakan rumus smoothing dari relevance judgements yang kamu masukkan di kode testing. Rumus probabilitasnya menjadi lebih ketat dan realistis.  

2. Hasil Two Poisson (Skor Bervariasi)Sekarang perhatikan Hasil 3 dan Hasil 4. 
Di sinilah letak keunggulan model Two Poisson. Skor tidak lagi seragam, melainkan bervariasi secara dinamis.
Faktor Penentu: Two Poisson memasukkan faktor Term Frequency ($f_{t,D}$). Model ini mengasumsikan frekuensi kata mengikuti distribusi Poisson, 
di mana kata dari query muncul lebih sering pada dokumen yang relevan.  Analisis: Dokumen 8 dan 80 melesat ke peringkat atas (skor 0.5461) karena kata "two" atau "dimensional" muncul secara berulang-ulang di dalamnya. 
Sementara itu, Dokumen 4 dan 5 merosot ke bawah karena frekuensi kemunculan kata tersebut jauh lebih sedikit (skornya kembali ke 0.2840, yang mengindikasikan kata tersebut mungkin hanya muncul 1 kali sehingga tidak mendapat dorongan pengali yang besar).
2. Evaluasi Kualitas RankingBerdasarkan data di atas, kita bisa menarik kesimpulan mengenai kualitas kedua model:Model BIM (Probabilistik): Model ini cukup baik dalam menemukan dokumen yang mengandung kata kunci. Namun, ia lemah dalam membedakan kualitas dokumen. Sebuah artikel penelitian yang membahas topik tersebut secara mendalam (misal: 50 kali penyebutan) akan mendapatkan peringkat yang sama dengan artikel yang hanya menyebutkan kata kunci tersebut satu kali di judul. Ini membuatnya kurang ideal untuk riset akademik yang mendalam.Model Two Poisson: Model ini jauh lebih unggul dalam konteks ini. Ia mampu memberikan peringkat yang lebih "cerdas" karena memperhitungkan frekuensi kata (term frequency). Dokumen yang secara konsisten membahas topik query akan mendapatkan skor tinggi, sementara dokumen yang hanya menyebutkan kata kunci secara sporadis akan mendapatkan skor rendah. Hasil ini lebih mencerminkan relevansi semantik (makna) daripada sekadar pencocokan leksikal (kata). 

*/