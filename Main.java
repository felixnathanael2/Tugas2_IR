import java.util.*;

public class Main {

    // Declare yang akan diprint 
    static class HasilEvaluasi {
        double precision;
        double recall;
        double pAt1, pAt3, pAt5, pAt10;
        double[] interpolasi11Titik; 
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // 1. Bangun inverted index dari folder corpus 
        System.out.println("Membangun Inverted Index...");
        InvertedIndex index = new InvertedIndex();
        System.out.println("Total dokumen di corpus: " + index.totalDocuments);

        if (index.totalDocuments == 0) {
            System.out.println("Error: Tidak ada dokumen yang terbaca. Cek folder corpus.");
            return;
        }

        // 2. Inisialisasi model-model yang akan digunakan
        BIM bimModel = new BIM(index);
        double k = 1.5;
        double b = 0.75;
        TwoPoisson tpModel = new TwoPoisson(index, k);
        BM25 bm25Model = new BM25(index, k, b);
        BM11 bm11Model = new BM11(index, k);

        // 3. Pilih model 
        System.out.println("Pilih model : ");
        System.out.println("1. Binary Independence Model (BIM)");
        System.out.println("2. Two Poisson Model");
        System.out.println("3. BM11 Model");
        System.out.println("4. BM25 Model");
        int pilihanModel = sc.nextInt();
        sc.nextLine();

        String namaModel;
        switch (pilihanModel) {
            case 1:
                namaModel = "BIM";
                break;
            case 2:
                namaModel = "Two Poisson";
                break;
            case 3:
                namaModel = "BM11";
                break;
            case 4:
                namaModel = "BM25";
                break;
            default:
                System.out.println("Pilihan tidak valid. Default ke BIM.");
                pilihanModel = 1;
                namaModel = "BIM";
        }

        final int JUMLAH_QUERY = 5;

        List<HasilEvaluasi> hasilSemuaQuerySkenario1 = new ArrayList<>();
        List<HasilEvaluasi> hasilSemuaQuerySkenario2 = new ArrayList<>();

        // 4. Input query dan groundtruth, lalu lakukan evaluasi untuk setiap query
        for (int q = 1; q <= JUMLAH_QUERY; q++) {
            System.out.println("\n=============================================");
            System.out.println("QUERY KE-" + q + " DARI " + JUMLAH_QUERY);
            System.out.println("=============================================");

            System.out.println("Masukkan Query : ");
            String query = sc.nextLine();

            System.out.println(
                    "Masukkan ID dokumen relevan untuk query ini (pisahkan dengan koma, contoh: 12, 14, 102, 285):");
            String baris = sc.nextLine();
            Set<Integer> relevantDocs = new HashSet<>();
            if (!baris.trim().isEmpty()) {
                for (String token : baris.trim().split(",")) {
                    relevantDocs.add(Integer.parseInt(token.trim()));
                }
            }

            HashMap<Integer, Double> scores;

            switch (pilihanModel) {
                case 2:
                    scores = tpModel.calculateScoresScenario2(query, relevantDocs);
                    break;
                case 3:
                    scores = bm11Model.calculateScoresScenario2(query, relevantDocs);
                    break;
                case 4:
                    scores = bm25Model.calculateScoresScenario2(query, relevantDocs);
                    break;
                default:
                    scores = bimModel.calculateScoresScenario2(query, relevantDocs);
            }

            System.out.println("\n--- " + namaModel + " ---");
            HasilEvaluasi hasil1 = evaluationMetrics(scores, relevantDocs, index.totalDocuments);
            if (hasil1 != null)
                hasilSemuaQuerySkenario1.add(hasil1);
        }

        // 5. Print 11 point average setelah semua query selesai dievaluasi
        System.out.println("\n=============================================");
        System.out.println(
                "11-POINT AVERAGE PRECISION (RATA-RATA " + JUMLAH_QUERY + " QUERY) - " + namaModel);
        System.out.println("==============================================");
        cetak11PointRataRata(hasilSemuaQuerySkenario1);
    }

    // Fungsi untuk menghitung dan print hasil evaluasi 
    public static HasilEvaluasi evaluationMetrics(HashMap<Integer, Double> scores, Set<Integer> groundTruth,
            int totalDocs) {
        if (scores == null || scores.isEmpty()) {
            System.out.println("Tidak ada dokumen yang cocok dengan query tersebut.");
            return null;
        }

        // Untuk mengurutkan hasil dokumen yang ditemukan 
        List<Map.Entry<Integer, Double>> list = new ArrayList<>(scores.entrySet());
        list.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        int rank = 1;
        for (Map.Entry<Integer, Double> entry : list) {
            System.out.printf("Rank %d | DocID: %d | Score: %.4f\n", rank, entry.getKey(), entry.getValue());
            rank++;
            // Batasi dokumen menjadi 10 
            if (rank > 10)
                break;
        }

        int totalRelevanDiKunci = groundTruth.size();

        List<Double> catatanRecallAsli = new ArrayList<>();
        List<Double> catatanPrecisionAsli = new ArrayList<>();
        double pAt1 = 0, pAt3 = 0, pAt5 = 0, pAt10 = 0;

        int batasRank = Math.min(list.size(), 10);

        int tp = 0, fp = 0, fn = 0, tn = 0;

        // Hitung nilai precision at k (1, 3, 5, 10)
        for (int i = 1; i <= batasRank; i++) {
            int docId = list.get(i - 1).getKey();

            if (groundTruth.contains(docId)) {
                tp++;
                if (totalRelevanDiKunci > 0) {
                    catatanRecallAsli.add((double) tp / totalRelevanDiKunci);
                    catatanPrecisionAsli.add((double) tp / i);
                }
            }

            if (i == 1)
                pAt1 = (double) tp / 1;
            if (i == 3)
                pAt3 = (double) tp / 3;
            if (i == 5)
                pAt5 = (double) tp / 5;
            if (i == 10)
                pAt10 = (double) tp / 10;
        }

        // Hitung nilai false positive, false negative, dan true negative 
        fp = batasRank - tp;
        fn = totalRelevanDiKunci - tp;
        tn = totalDocs - (tp + fp + fn);

        // Hitung nilai precision dan recall
        double precision = (tp + fp == 0) ? 0.0 : (double) tp / (tp + fp);
        double recall = (tp + fn == 0) ? 0.0 : (double) tp / (tp + fn);

        // Hitung nilai 11 point interpolasi untuk masing-masing query 
        double[] sebelasTitikRecall = { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
        double[] hasilPrecisionInterpolasi = new double[11];

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
        }

        System.out.println("---------------------------------------------");
        System.out.println("HASIL EVALUASI:");
        System.out.printf("    [TP: %d | FP: %d | FN: %d | TN: %d]\n", tp, fp, fn, tn);
        System.out.println("    -----------------------------------------");
        System.out.printf("    Precision Score: %.2f\n", precision);
        System.out.printf("    Recall Score: %.2f\n", recall);
        System.out.println("    -----------------------------------------");
        System.out.printf("    Precision @1  : %.2f\n", pAt1);
        System.out.printf("    Precision @3  : %.2f\n", pAt3);
        System.out.printf("    Precision @5  : %.2f\n", pAt5);
        System.out.printf("    Precision @10 : %.2f\n", pAt10);
        System.out.println("=============================================");

        HasilEvaluasi hasil = new HasilEvaluasi();
        hasil.precision = precision;
        hasil.recall = recall;
        hasil.pAt1 = pAt1;
        hasil.pAt3 = pAt3;
        hasil.pAt5 = pAt5;
        hasil.pAt10 = pAt10;
        hasil.interpolasi11Titik = hasilPrecisionInterpolasi;
        return hasil;
    }

    // Fungsi untuk melalukan print 11 point average untuk seluruh query 
    public static void cetak11PointRataRata(List<HasilEvaluasi> hasilSemuaQuery) {
        if (hasilSemuaQuery.isEmpty()) {
            System.out.println("Tidak ada hasil query yang valid untuk dirata-rata.");
            return;
        }

        int n = hasilSemuaQuery.size();
        double[] sumInterpolasi = new double[11];

        for (HasilEvaluasi h : hasilSemuaQuery) {
            for (int i = 0; i < 11; i++) {
                sumInterpolasi[i] += h.interpolasi11Titik[i];
            }
        }

        double[] sebelasTitikRecall = { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
        double[] rataRataInterpolasi = new double[11];
        double totalRataRataInterpolasi = 0.0;
        for (int i = 0; i < 11; i++) {
            rataRataInterpolasi[i] = sumInterpolasi[i] / n;
            totalRataRataInterpolasi += rataRataInterpolasi[i];
        }
        double rataRata11PointSemuaQuery = totalRataRataInterpolasi / 11;

        System.out.println("Jumlah query yang dirata-rata: " + n);
        System.out.println("---------------------------------------------");
        for (int i = 0; i < 11; i++) {
            System.out.printf("    Recall=%.1f -> Precision=%.2f\n", sebelasTitikRecall[i],
                    rataRataInterpolasi[i]);
        }
        System.out.println("---------------------------------------------");
        System.out.printf("    11-Point Average Precision: %.2f\n", rataRata11PointSemuaQuery);
        System.out.println("=============================================");
    }
}