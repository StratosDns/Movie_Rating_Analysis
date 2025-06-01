# MovieLens Hadoop MapReduce Pipeline Report

## Overview

This report documents the file storage locations, Hadoop execution commands, and a detailed explanation of the Java code for each step in your MovieLens MapReduce workflow.

---

## 1. Input Files

| File                  | Description                  | Storage Location         |
|-----------------------|-----------------------------|-------------------------|
| `ratings.dat`         | User-movie ratings          | `/a4/ratings.dat` (HDFS)|
| `users.dat`           | User demographic data       | `/a4/users.dat` (HDFS)  |
| `movies.dat`          | Movie metadata (genres, etc)| `/a4/movies.dat` (HDFS) |

---

## 2. MapReduce Jobs and Intermediate Files

### a. Step 1: RatingsUsersJoin

- **Purpose:** Joins ratings with users to associate each rating with the user's age group.
- **Input:** `/a4/ratings.dat`, `/a4/users.dat`
- **Output:** `/a4/step1_output/` (HDFS)
- **Execution:**
  ```bash
  hadoop fs -rm -r /a4/step1_output
  hadoop jar target/MovieLensJob-1.0-SNAPSHOT.jar org.example.movielens.RatingsUsersJoin /a4/ratings.dat /a4/users.dat /a4/step1_output
  ```
- **Output example line:**  
  ```
  1193::25	5.0
  ```
  (where "1193" is MovieID, "25" is age group, "5.0" is rating)

#### Java Code Details

- **Mapper:** Reads lines from ratings and users, tags them, emits MovieID as key.
- **Reducer:** Receives MovieID, joins user info to rating, emits MovieID::AgeGroup as key and rating as value.

---

### b. Step 2: MoviesGenreJoin

- **Purpose:** Joins the previous output with movies to associate each (genre, age group) with a rating.
- **Input:** `/a4/step1_output/`, `/a4/movies.dat`
- **Output:** `/a4/step2_output/` (HDFS)
- **Execution:**
  ```bash
  hadoop fs -rm -r /a4/step2_output
  hadoop jar target/MovieLensJob-1.0-SNAPSHOT.jar org.example.movielens.MoviesGenreJoin /a4/step1_output /a4/movies.dat /a4/step2_output
  ```
- **Output example line:**  
  ```
  Comedy::25	4.0
  ```
  (where "Comedy" is genre, "25" is age group, "4.0" is rating)

#### Java Code Details

- **Mapper:** Reads joined (MovieID::AgeGroup, rating) and movies, emits Genre::AgeGroup as key and rating as value.
- **Reducer:** Receives Genre::AgeGroup, collects ratings, emits Genre::AgeGroup and each rating.

---

### c. Step 3: GenreAgeAvg

- **Purpose:** Calculates the average rating for every (genre, age group) pair.
- **Input:** `/a4/step2_output/`
- **Output:** `/a4/final_output/` (HDFS)
- **Execution:**
  ```bash
  hadoop fs -rm -r /a4/final_output
  hadoop jar target/MovieLensJob-1.0-SNAPSHOT.jar org.example.movielens.GenreAgeAvg /a4/step2_output /a4/final_output
  ```
- **Output example line:**  
  ```
  Comedy::25	3.49
  ```
  (average rating for Comedy genre, users age 25)

#### Java Code Details

**Main points for GenreAgeAvg.java:**
- **Mapper:**  
  - Input: `Genre::AgeGroup<TAB>Rating`  
  - Splits each line by tab, emits `Text` key (`Genre::AgeGroup`) and `FloatWritable` value (rating).
- **Reducer:**  
  - Receives each unique key and a list of ratings.
  - Sums ratings and counts, emits key with average rating.
- **Main Method:**  
  - Sets the Mapper and Reducer classes.
  - Sets input/output key and value types.
  - Specifies input/output HDFS paths.

---

## 3. Output Files

| Step           | Output Directory     | File Names (HDFS)                      | How to Download Locally          |
|----------------|---------------------|----------------------------------------|----------------------------------|
| Step 1 Output  | `/a4/step1_output/` | `part-r-00000`, ...                    | `hadoop fs -get /a4/step1_output .` |
| Step 2 Output  | `/a4/step2_output/` | `part-r-00000`, ...                    | `hadoop fs -get /a4/step2_output .` |
| Final Output   | `/a4/final_output/` | `part-r-00000`, ...                    | `hadoop fs -get /a4/final_output .` |

---

## 4. How Each Java Code Works (Detailed)

### a. Mapper and Reducer Structure

- **Mapper:**  
  Extends `org.apache.hadoop.mapreduce.Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>`.  
  Reads a line, parses the necessary fields, emits a key-value pair.

- **Reducer:**  
  Extends `org.apache.hadoop.mapreduce.Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT>`.  
  Receives all values for a given key, aggregates or processes them, emits the result.

### b. GenreAgeAvg.java (Final Step) Example

```java
public static class AvgMapper extends Mapper<LongWritable, Text, Text, FloatWritable> {
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        String[] tokens = line.split("\\t");
        if (tokens.length == 2) {
            String keyStr = tokens[0];
            try {
                float rating = Float.parseFloat(tokens[1]);
                context.write(new Text(keyStr), new FloatWritable(rating));
            } catch (Exception e) {
                // skip line
            }
        }
    }
}

public static class AvgReducer extends Reducer<Text, FloatWritable, Text, FloatWritable> {
    public void reduce(Text key, Iterable<FloatWritable> values, Context context) throws IOException, InterruptedException {
        float sum = 0;
        int count = 0;
        for (FloatWritable val : values) {
            sum += val.get();
            count++;
        }
        if (count != 0) {
            context.write(key, new FloatWritable(sum / count));
        }
    }
}
```

- **Main method** sets up the job with:
  - Input/output paths
  - Mapper/Reducer classes
  - Output key/value types

### c. Execution Flow

1. **Mapper** reads each input line, parses fields, emits a composite key and value.
2. **Shuffle and Sort:** Hadoop groups all values for the same key.
3. **Reducer** processes values for each key, aggregates as required (e.g., computes average).
4. **Output** is written to the specified HDFS output directory.

---

## 5. Accessing Results

- To view results directly:
  ```bash
  hadoop fs -cat /a4/final_output/part-r-00000 | head
  ```
- To download results to your local file system:
  ```bash
  hadoop fs -get /a4/final_output .
  ```
  Results will be in `final_output/part-r-00000` in your current local directory.

---

## 6. Notes

- All intermediate and final outputs are stored in HDFS; use `hadoop fs -get` to copy them locally.
- Always remove existing output directories before running a job to avoid errors.
- Each Java file must be recompiled and repackaged (e.g., with Maven) after changes.

---

## 7. References

- [Hadoop MapReduce Tutorial](https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html)
- [MovieLens Dataset Documentation](https://grouplens.org/datasets/movielens/)
