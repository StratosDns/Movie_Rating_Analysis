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

public class RatingsUsersJoin {

    public static class RatingsMapper extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // ratings.dat: UserID::MovieID::Rating::Timestamp
            String[] tokens = value.toString().split("::");
            if (tokens.length >= 3) {
                context.write(new Text(tokens[0]), new Text("R::" + tokens[1] + "::" + tokens[2]));
            }
        }
    }

    public static class UsersMapper extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // users.dat: UserID::Gender::Age::Occupation::Zip-code
            String[] tokens = value.toString().split("::");
            if (tokens.length >= 3) {
                context.write(new Text(tokens[0]), new Text("U::" + tokens[2]));
            }
        }
    }

    public static class JoinReducer extends Reducer<Text, Text, NullWritable, Text> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            String ageGroup = null;
            List<String> ratings = new ArrayList<>();

            for (Text val : values) {
                String v = val.toString();
                if (v.startsWith("U::")) {
                    ageGroup = v.split("::")[1];
                } else if (v.startsWith("R::")) {
                    ratings.add(v);
                }
            }

            if (ageGroup != null) {
                for (String r : ratings) {
                    String[] parts = r.split("::");
                    // Output: MovieID::AgeGroup::Rating
                    context.write(NullWritable.get(), new Text(parts[1] + "::" + ageGroup + "::" + parts[2]));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: RatingsUsersJoin <ratings input> <users input> <output>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "RatingsUsersJoin");
        job.setJarByClass(RatingsUsersJoin.class);

        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, RatingsMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, UsersMapper.class);

        job.setReducerClass(JoinReducer.class);

	job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

	job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);

        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
