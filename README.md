# Movie Rating Analysis with Hadoop MapReduce

This project analyzes movie ratings by age group and genre using Hadoop MapReduce. It processes the [MovieLens Dataset](https://grouplens.org/datasets/movielens/) and computes the average rating for each genre and age group combination.

---

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Setup Instructions (Windows/WSL)](#setup-instructions-windowswsl)
  - [1. Install Java](#1-install-java)
  - [2. Install Maven](#2-install-maven)
  - [3. Install Hadoop](#3-install-hadoop)
  - [4. Set Up Hadoop (HDFS)](#4-set-up-hadoop-hdfs)
  - [5. Get and Build the Project](#5-get-and-build-the-project)
  - [6. Prepare Input Files](#6-prepare-input-files)
  - [7. Upload Input Files to HDFS](#7-upload-input-files-to-hdfs)
- [Run the MapReduce Pipeline](#run-the-mapreduce-pipeline)
- [Accessing the Results](#accessing-the-results)
- [Troubleshooting](#troubleshooting)
- [References](#references)

---

## Features

- **End-to-end Hadoop MapReduce pipeline**
- **Joins MovieLens ratings, user demographics, and movie genres**
- **Calculates average ratings by genre and age group**
- **Step-by-step instructions for first-time users**

---

## Prerequisites

- Windows PC with [WSL (Windows Subsystem for Linux)](https://learn.microsoft.com/en-us/windows/wsl/install)
- Internet connection
- [MovieLens Dataset](https://grouplens.org/datasets/movielens/) files: `ratings.dat`, `users.dat`, `movies.dat`

---

## Project Structure

```
Movie_Rating_Analysis/
├── src/main/java/org/example/movielens/
│   ├── RatingsUsersJoin.java
│   ├── MoviesGenreJoin.java
│   └── GenreAgeAvg.java
├── README.md
├── pom.xml
└── ...
```

---

## Setup Instructions (Windows/WSL)

### 1. Install Java

In your WSL terminal:
```bash
sudo apt update
sudo apt install openjdk-11-jdk -y
java -version
```
You should see a version output like `openjdk version "11..."`.

---

### 2. Install Maven

```bash
sudo apt install maven -y
mvn -version
```
You should see Maven's version information.

---

### 3. Install Hadoop

1. **Download Hadoop:**
   ```bash
   wget https://downloads.apache.org/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
   tar -xzf hadoop-3.3.6.tar.gz
   sudo mv hadoop-3.3.6 /usr/local/hadoop
   ```

2. **Set Environment Variables:**  
   Add the following lines to the end of your `~/.bashrc` file:
   ```bash
   export HADOOP_HOME=/usr/local/hadoop
   export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
   export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
   ```
   Then run:
   ```bash
   source ~/.bashrc
   ```

3. **Check Installation:**
   ```bash
   hadoop version
   ```
   You should see Hadoop’s version info.

---

### 4. Set Up Hadoop (HDFS)

1. **Format HDFS (run once):**
   ```bash
   hdfs namenode -format
   ```

2. **Start Hadoop:**
   ```bash
   start-dfs.sh
   start-yarn.sh
   ```

3. **Create HDFS Directory for Project:**
   ```bash
   hadoop fs -mkdir /a4
   ```

---

### 5. Get and Build the Project

1. **Clone the repository:**
   ```bash
   git clone https://github.com/StratosDns/Movie_Rating_Analysis.git
   cd Movie_Rating_Analysis
   ```

2. **Build with Maven:**
   ```bash
   mvn clean package
   ```
   The compiled JAR will appear as `target/MovieLensJob-1.0-SNAPSHOT.jar`.

---

### 6. Prepare Input Files

#### **Download MovieLens Data**

- Download the dataset from [MovieLens]([https://grouplens.org/datasets/movielens/](https://files.grouplens.org/datasets/movielens/ml-1m.zip )).
- Extract `ratings.dat`, `users.dat`, and `movies.dat` on your Windows system.

#### **Move Input Files from Windows to WSL**

1. Open Windows File Explorer and go to the folder containing your `.dat` files.
2. In another Explorer window, go to:  
   `\\wsl$\Ubuntu\home\<your-wsl-username>\`
3. Drag and drop the `.dat` files into your WSL home directory.

*Alternatively, you can use the `cp` command in WSL if your Windows drives are mounted at `/mnt/c/`:*
```bash
cp /mnt/c/Users/<YourWindowsUser>/Downloads/ratings.dat ~/
cp /mnt/c/Users/<YourWindowsUser>/Downloads/users.dat ~/
cp /mnt/c/Users/<YourWindowsUser>/Downloads/movies.dat ~/
```

---

### 7. Upload Input Files to HDFS

```bash
hadoop fs -put ~/ratings.dat /a4/
hadoop fs -put ~/users.dat /a4/
hadoop fs -put ~/movies.dat /a4/
```
Check upload:
```bash
hadoop fs -ls /a4/
```

---

## Run the MapReduce Pipeline

Run each step in order. Remove output directories before each run to avoid errors.

### Step 1: Join Ratings with Users

```bash
hadoop fs -rm -r /a4/step1_output
hadoop jar target/MovieLensJob-1.0-SNAPSHOT.jar org.example.movielens.RatingsUsersJoin /a4/ratings.dat /a4/users.dat /a4/step1_output
```

### Step 2: Join with Movies (Genres)

```bash
hadoop fs -rm -r /a4/step2_output
hadoop jar target/MovieLensJob-1.0-SNAPSHOT.jar org.example.movielens.MoviesGenreJoin /a4/step1_output /a4/movies.dat /a4/step2_output
```

### Step 3: Calculate Average Ratings by Genre and Age Group

```bash
hadoop fs -rm -r /a4/final_output
hadoop jar target/MovieLensJob-1.0-SNAPSHOT.jar org.example.movielens.GenreAgeAvg /a4/step2_output /a4/final_output
```

---

## Accessing the Results

- **To preview results in the terminal:**
  ```bash
  hadoop fs -cat /a4/final_output/part-r-00000 | head
  ```

- **To download the results to your WSL home:**
  ```bash
  hadoop fs -get /a4/final_output .
  ls final_output/
  cat final_output/part-r-00000
  ```

- **Move results from WSL back to Windows:**  
  Use the File Explorer path `\\wsl$\Ubuntu\home\<your-wsl-username>\final_output\` to copy results to Windows.

---

## Troubleshooting

- **If a job fails:** Check the error logs printed in the terminal, or use Hadoop’s web UI (usually at `localhost:9870` or `localhost:8088`) for more details.
- **Remember to remove previous output directories** with `hadoop fs -rm -r <output_dir>` before each run.
- **If you change the Java code,** re-run `mvn clean package` before running jobs again.

---

## References

- [Hadoop Official Documentation](https://hadoop.apache.org/docs/stable/)
- [MovieLens Dataset Documentation](https://grouplens.org/datasets/movielens/)
- [WSL Documentation](https://learn.microsoft.com/en-us/windows/wsl/)

---

**Project by [StratosDns](https://github.com/StratosDns) — Contributions welcome!**
