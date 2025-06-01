package org.example.movielens;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class MoviesGenreJoin {

    public static class MoviesMapper extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // movies.dat: MovieID::Title::Genres
            String[] tokens = value.toString().split("::");
            if (tokens.length >= 3) {
                context.write(new Text(tokens[0]), new Text("M::" + tokens[2]));
            }
        }
    }

    public static class RatingsUsersMapper extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // Output from previous: MovieID::AgeGroup::Rating
            String[] tokens = value.toString().split("::");
            if (tokens.length >= 3) {
                context.write(new Text(tokens[0]), new Text("RU::" + tokens[1] + "::" + tokens[2]));
            }
        }
    }

    public static class JoinReducer extends Reducer<Text, Text, Text, FloatWritable> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String genres = null;
            List<String[]> ageRatings = new ArrayList<>();

            for (Text val : values) {
                String v = val.toString();
                if (v.startsWith("M::")) {
                    genres = v.substring(3);
                } else if (v.startsWith("RU::")) {
                    String[] parts = v.substring(4).split("::");
                    ageRatings.add(parts);
                }
            }

            if (genres != null) {
                for (String[] ar : ageRatings) {
                    String ageGroup = ar[0];
                    float rating;
                    try {
                        rating = Float.parseFloat(ar[1]);
                    } catch (Exception e) {
                        continue;
                    }
                    for (String genre : genres.split("\\|")) {
                        context.write(new Text(genre + "::" + ageGroup), new FloatWritable(rating));
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: MoviesGenreJoin <ratings-users input> <movies input> <output>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "MoviesGenreJoin");
        job.setJarByClass(MoviesGenreJoin.class);

        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, RatingsUsersMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, MoviesMapper.class);

	job.setMapOutputKeyClass(Text.class);
	job.setMapOutputValueClass(Text.class);
        job.setReducerClass(JoinReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FloatWritable.class);

        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
