import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.Task;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class SONMapReduce {

    private final static IntWritable one = new IntWritable(1);

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();


        Path pathofinput = new Path(args[0]);
        Path pathoftemp = new Path(args[1]);
        Path pathofoutput = new Path(args[2]);
        int NumOfLines =  Integer.parseInt(args[3]);
        float support = Float.parseFloat(args[4]);

        conf.set("tempfilepath", args[1]);
        conf.setFloat("support", support);
        conf.setInt("NumOfLines",NumOfLines);
        Job job = Job.getInstance(conf, "SONMapReduce");
        job.setJarByClass(SONMapReduce.class);

        job.setMapperClass(MapperTokenizerOne.class);
        job.setReducerClass(ReducerSumOne.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setInputFormatClass(LinesInputFormat.class);


        FileInputFormat.addInputPath(job, pathofinput);
        FileOutputFormat.setOutputPath(job, pathoftemp);
        job.waitForCompletion(true);

        long totalNum = job.getCounters().findCounter(Task.Counter.MAP_INPUT_RECORDS).getValue();


        conf.setLong("TOTALNUM", totalNum*NumOfLines);



        Path freqpath = new Path(args[1]+"/"+"part-r-00000");
        Job jobtwo = Job.getInstance(conf, "SONMapReduceTwo");
        jobtwo.setJarByClass(SONMapReduce.class);
        jobtwo.addCacheFile(freqpath.toUri());
        jobtwo.setMapperClass(MapperTokenizerTwo.class);
        jobtwo.setReducerClass(ReducerSumTwo.class);
        jobtwo.setOutputKeyClass(Text.class);
        jobtwo.setOutputValueClass(IntWritable.class);

        jobtwo.setInputFormatClass(LinesInputFormat.class);
        FileInputFormat.addInputPath(jobtwo, pathofinput);
        FileOutputFormat.setOutputPath(jobtwo, pathofoutput);
        jobtwo.waitForCompletion(true);
    }

    public static class MapperTokenizerOne
            extends Mapper<Object, Text, Text, IntWritable>{

        private double minimumSupport;

        //@Override
        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            minimumSupport = conf.getFloat("support",0.01f);
        }

        //@Override
        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {

            String input = value.toString();
            String output = null;

            Apriori algo = new Apriori();

            Itemsets result = algo.algoExecution(minimumSupport, input, output);

            List<String> results = result.getItemsets();

            for (int i =0; i<results.size();i++){
                context.write(new Text(results.get(i)), one);
            }
        }
    }

    public static class ReducerSumOne
            extends Reducer<Text,IntWritable,Text,IntWritable> {

        public void reduce(Text key, Iterable<IntWritable> values,
                           Context context
        ) throws IOException, InterruptedException {

            context.write(key, one);
        }
    }

    public static class MapperTokenizerTwo
            extends Mapper<Object, Text, Text, IntWritable>{

        private final Map<String,Integer > itemsMapping = new HashMap<>();

        @Override
        public void setup(Context context) throws IOException {

            Configuration conf = context.getConfiguration();
            URI[] FrqSetsURI = Job.getInstance(conf).getCacheFiles();
            Path FrqSetsPath = new Path(FrqSetsURI[0].getPath());

            String FrqSetsFileName = FrqSetsPath.getName();
            FileReader freader = new FileReader(FrqSetsFileName);
            BufferedReader bufferedreader = new BufferedReader(freader);

            String singleItemset;
            int count = 0;

            while ((singleItemset = bufferedreader.readLine())!=null){
                String[] ItemArray = singleItemset.split(" ");
                String last= ItemArray[ItemArray.length-1];
                String[] ItemSet = singleItemset.split(last);
                itemsMapping.put(ItemSet[0], count);

            }
            bufferedreader.close();
        }

        @Override
        public void map(Object key, Text value, Context context

        ) throws IOException, InterruptedException {

            String input = value.toString();

            String [] data_in = input.split("\n");
            String line;

            for (Map.Entry<String, Integer> entry : itemsMapping.entrySet()) {
                Set<String> itemSet = new HashSet<>(Arrays.asList(entry.getKey().split(" ")));
                int count;
                count = 0;
                itemsMapping.put(entry.getKey(), count);
                for (String s : data_in) {
                    line = s.trim();
                    Set<String> transSet = new HashSet<>(Arrays.asList(line.split(" ")));

                    if (transSet.containsAll(itemSet)) {
                        count = itemsMapping.get(entry.getKey());
                        itemsMapping.put(entry.getKey(), count + 1);
                    }
                }
            }

            for (Map.Entry<String, Integer> entry : itemsMapping.entrySet()) {
                context.write(new Text( entry.getKey()), new IntWritable(entry.getValue()));
            }
        }
    }

    public static class ReducerSumTwo
            extends Reducer<Text,IntWritable,Text,IntWritable> {

        double supportThreshold;
        private final IntWritable result = new IntWritable();

        @Override
        public void setup(Context context) throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();
            supportThreshold = conf.getFloat("support", 0.01f);
            long totalNumOfBasket = conf.getLong("TOTALNUM", 10000);
            if(totalNumOfBasket == 0)
                throw new InterruptedException("Total number of baskets can not be zero!");

            supportThreshold = (int) (supportThreshold * totalNumOfBasket);
            System.out.println("supportThreshold is " + supportThreshold);
        }

        @Override
        public void reduce(Text key, Iterable<IntWritable> values,
                           Context context
        ) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }

            if (sum > supportThreshold){
                result.set(sum);
                context.write(key, result);
            }
        }
    }
}
