# 🖼️ Fake Image Detection System

A Java-based system to detect whether an image is **Authentic or Tampered (Fake)** using:

- Image forensics (pixel-level features)
- Machine Learning (Logistic Regression)
- Similarity search (Image Hashing)
- Metadata analysis

---

## Features

- Extracts multiple forensic features:
  - Entropy (image randomness)
  - Laplacian (sharpness)
  - Edge Density
  - RGB Mean & Variance
  - JPEG Blockiness
  - Metadata presence

- Trains a Logistic Regression model from scratch
- Predicts:
  - Authentic  
  - Tampered (Fake)
- Shows:
  - Confidence score
  - Top contributing features (Explainability)
  - Closest similar image (via hashing)

---

## Project Structure
```
FakeImageDetector/
│
├── Main.java
├── DatasetGenerator.java
├── FeatureExtractor.java
├── LogisticRegression.java
├── ImageHash.java
├── DBHelper.java
├── Explainability.java
├── SimSearch.java
│
├── model.csv # Trained mode
├── README.md
```

---

## Dependencies

Download these JAR files and place them in the project root:

- metadata-extractor-2.19.0.jar  
- sqlite-jdbc-3.51.3.0.jar  
- xmpcore-6.1.11.jar  

---

## Compilation

```bash
javac -cp ".;metadata-extractor-2.19.0.jar;sqlite-jdbc-3.51.3.0.jar;xmpcore-6.1.11.jar" *.java
```
---

## Usage 
1. Create 2 folders au/ and tp/
2. Place images in respective folder
3. Train model:
   ```bash
   java -cp ".;metadata-extractor-2.19.0.jar;sqlite-jdbc-3.51.3.0.jar;xmpcore-6.1.11.jar" Main train au tp
   ```
   To generate csv data, train model, save model.csv, index images in database
4. Predict image:
   ```bash
   java -cp ".;metadata-extractor-2.19.0.jar;sqlite-jdbc-3.51.3.0.jar;xmpcore-6.1.11.jar" Main predict test.jpg
   ```
   replace test.jpg with image you want to test

---

## Contributors:
Team Idli - Anjana and Rachana :)
