# Tugas 2_IR - Information Retrieval

Implementasi model-model probabilistik untuk Information Retrieval (IR) menggunakan Java.

Struktur File

- **InvertedIndex.java** - Membangun inverted index dari corpus dokumen
- **BIM.java** - Binary Independence Model
- **TwoPoisson.java** - Two Poisson Model
- **BM11.java** - BM11 Model
- **BM25.java** - BM25 Model
- **Main.java** - Program utama untuk menjalankan dan mengevaluasi model
- **PorterStemmer.java** - Implementasi Porter Stemmer untuk stemming
- **Stemmer.java** - Interface untuk stemmer
- **stopwords.txt** - Daftar stopword (kata yang diabaikan)
- **corpus/** - Folder berisi 100 dokumen teks (doc_1.txt sampai doc_100.txt)
- **cran/** - File Cranfield collection (cran.all.1400, cran.qry, cranqrel)

Cara Menjalankan

Prasyarat
- Java JDK 8 atau lebih baru
- Pastikan struktur folder sesuai (corpus/, stopwords.txt, dll di direktori yang sama)

Menjalankan Program
javac *.java
java Main

Langkah-langkah Penggunaan
1. Program akan membangun inverted index dari folder `corpus/`
2. Pilih model yang ingin digunakan (1-4):
   - 1: BIM (Binary Independence Model)
   - 2: Two Poisson
   - 3: BM11
   - 4: BM25
3. Masukkan query pencarian
4. Masukkan ID dokumen yang relevan (ground truth) untuk evaluasi, pisahkan dengan koma
5. Program akan menampilkan:
   - Ranking 10 dokumen teratas dengan skor relevansi
   - Metrik evaluasi: Precision, Recall, Precision@K (K=1,3,5,10)
6. Ulangi untuk query berikutnya (total 5 query)
7. Program akan menampilkan 11-point average precision dari semua query

Penjelasan Model dan Formula
1. BIM (Binary Independence Model)

Model dasar yang mengasumsikan kemunculan term dalam dokumen bersifat independen.

**Skenario 1 (Tanpa Relevance Judgements):**
wt = log₁₀(0.5 × N / Nt)

**Skenario 2 (Dengan Relevance Judgements):**
wt = log₁₀[((rt + 0.5) × (N - R + 1)) / ((R + 1) × (Nt - rt + 0.5))]

**Keterangan:**
- `wt` = bobot term
- `N` = total dokumen dalam corpus
- `Nt` = jumlah dokumen yang mengandung term t (document frequency)
- `R` = jumlah dokumen yang relevan
- `rt` = jumlah dokumen relevan yang mengandung term t

**Skor Dokumen:**
Skor(D) = Σ wt untuk semua term query yang ada di dokumen D

2. Two Poisson Model
Pengembangan dari BIM yang menambahkan normalisasi frekuensi term menggunakan fungsi saturasi. Term yang muncul berulang kali tidak memberikan kontribusi yang proporsional.

**Formula Skor Term:**
Skor_term = (ft,D × (k + 1) × wt) / (ft,D + k)

**Keterangan:**
- `ft,D` = frekuensi mentah term t di dokumen D (raw term frequency)
- `k` = konstanta penyeimbang (1 ≤ k < 2), default: 1.5
- `wt` = bobot term dari BIM (Skenario 1 atau 2)

3. BM11 Model
Model yang menambahkan normalisasi panjang dokumen ke dalam Two Poisson. Dokumen yang lebih panjang cenderung memiliki frekuensi term lebih tinggi, sehingga perlu dinormalisasi.

**Formula Skor Term:**
Skor_term = (ft,D × (k + 1) × wt) / (ft,D + k × (ld / lavg))

**Keterangan:**
- `ft,D` = frekuensi mentah term t di dokumen D
- `ld` = panjang dokumen D (jumlah term valid)
- `lavg` = rata-rata panjang dokumen dalam corpus
- `k` = konstanta penyeimbang (1 ≤ k < 2), default: 1.5
- `wt` = bobot term dari BIM

4. BM25 (Okapi BM25)
Model akan menggabungkan normalisasi Two Poisson dengan parameter kontrol panjang dokumen yang lebih fleksibel.

**Formula Skor Term:**
Skor_term = (ft,D × (k + 1) × wt) / (ft,D + k × [(1 - b) + b × (ld / lavg)])

**Keterangan:**
- `ft,D` = frekuensi mentah term t di dokumen D
- `ld` = panjang dokumen D
- `lavg` = rata-rata panjang dokumen
- `k` = konstanta penyeimbang frekuensi (1 ≤ k < 2), default: 1.5
- `b` = parameter kontrol panjang dokumen (0 ≤ b ≤ 1), default: 0.75
- `wt` = bobot term dari BIM

Metrik Evaluasi
Program menghitung metrik-metrik berikut:
1. **Precision** = TP / (TP + FP)
2. **Recall** = TP / (TP + FN)
3. **Precision@K** = Precision pada top-K dokumen (K = 1, 3, 5, 10)
4. **11-Point Average Precision** = Rata-rata precision pada 11 level recall (0.0, 0.1, ..., 1.0)