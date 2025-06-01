package org.example.movielens;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class GenreAgeAvg {

    public static class AvgMapper extends Mapper<LongWritable, Text, Text, FloatWritable> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // Input: Genre::AgeGroup<TAB>Rating
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

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: GenreAgeAvg <input> <output>");
            System.exit(2);
        }
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "GenreAgeAvg");
        job.setJarByClass(GenreAgeAvg.class);

	job.setMapperClass(AvgMapper.class);
        job.setReducerClass(AvgReducer.class);

        job.setMapOutputKeyClass(Text.class);
	job.setMapOutputValueClass(FloatWritable.class); 
	job.setOutputKeyClass(Text.class);               
	job.setOutputValueClass(FloatWritable.class);    
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
