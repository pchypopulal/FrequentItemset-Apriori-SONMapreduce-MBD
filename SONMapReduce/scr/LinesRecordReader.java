import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.util.LineReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class LinesRecordReader extends RecordReader<LongWritable, Text>{
    private int NUMLINESTOPROCESS;
    private LineReader inputLineReader;
    private LongWritable key;
    private Text value = new Text();
    private long start =0;
    private long end =0;
    private long pos =0;
    private int maximumLineLength;

    public float getProgress() {
        if (start == end) {
            return 0.0f;
        }
        else {
            return Math.min(1.0f, (pos - start) / (float)(end - start));
        }
    }

    public LongWritable getCurrentKey() {
        return key;
    }

    public Text getCurrentValue() {
        return value;
    }

    public void close() throws IOException {
        if (inputLineReader != null) {
            inputLineReader.close();
        }
    }

    public void initialize(InputSplit genericSplit, TaskAttemptContext context)throws IOException, InterruptedException {
        FileSplit split = (FileSplit) genericSplit;
        final Path file = split.getPath();
        Configuration conf = context.getConfiguration();
        this.NUMLINESTOPROCESS = conf.getInt("NumOfLines", 100);
        
        this.maximumLineLength = conf.getInt("mapred.linerecordreader.maxlength",Integer.MAX_VALUE);
        FileSystem fs = file.getFileSystem(conf);
        start = split.getStart();
        end= start + split.getLength();
        boolean skipfl = false;//skip the first line
        FSDataInputStream fileInput = fs.open(split.getPath());
 
        if (start != 0){
            skipfl = true;
            --start;
            fileInput.seek(start);
        }
        inputLineReader = new LineReader(fileInput,conf);
        if(skipfl){
            start += inputLineReader.readLine(new Text(),0,(int)Math.min((long)Integer.MAX_VALUE, end - start));
        }
        this.pos = start;
    }

    public boolean nextKeyValue() throws IOException, InterruptedException {
        int setNewSize = 0;
        final Text endline = new Text("\n");

        if (key == null) {
            key = new LongWritable();
        }
        key.set(pos);
        if (value == null) {
            value = new Text();
        }
        value.clear();

        for(int i=0;i<NUMLINESTOPROCESS;i++){
            Text vtemp = new Text();
            while (pos < end) {
                setNewSize = inputLineReader.readLine(vtemp, maximumLineLength,Math.max((int)Math.min(Integer.MAX_VALUE, end-pos),maximumLineLength));
                value.append(vtemp.getBytes(),0, vtemp.getLength());
                value.append(endline.getBytes(),0, endline.getLength());
                if (setNewSize == 0) {
                    break;
                }
                pos += setNewSize;
                if (setNewSize < maximumLineLength) {
                    break;
                }
            }
        }
        if (setNewSize == 0) {
            key = null;
            value = null;
            return false;
        } else {
            return true;
        }
    }
}