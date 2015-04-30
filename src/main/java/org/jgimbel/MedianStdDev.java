package org.jgimbel;
import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;

import java.util.*;
import java.lang.Long;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
public class MedianStdDev {

    public static class MedianStdDevMapper extends
            Mapper <Object, Text, Text, Message > {

        private Text person = new Text();
        private Message outmsg = new Message();

        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {

            Map < String, String > parsed = MedianStdDev.transformXmlToMap(value.toString());

            String text = parsed.get("address");
            String strDate = parsed.get("date");
            String type = parsed.get("type");

            if("".equals(text) || "".equals(strDate) || "".equals(type)){
                System.out.println("address was " + text + ". Date was " + strDate);
                return;
            }
            if(text == null){
                return;
            }

            person.set(text);
            outmsg.setType(Integer.parseInt(type));
            outmsg.setDate(Long.parseLong(strDate));

            context.write(person, outmsg);
        }
    }

    public static class MedianStdDevReducer extends
            Reducer < Text, Message, Text, LongWritable > {
            //Reducer < Text, Message, Text, Text > {

        public void reduce(Text key, Iterable < Message > values,Context context)
                throws IOException, InterruptedException {

            String s = "";
            int c = 0;
            long sum = 0L;
            Message lastTime = new Message();
            try {
                lastTime = (Message) values.iterator().next().clone();
            }catch(Exception e){
                System.out.println(e.getStackTrace());
            }
            Message me;
            Iterator<Message> i = values.iterator();
            while(i.hasNext()){
                me = i.next();
                s += "\n" + lastTime.getType() + "\t" + me.getType();
                try {
                    lastTime = (Message) values.iterator().next().clone();
                }catch(Exception e){
                    System.out.println(e.getStackTrace());
                }
                if(lastTime.getType() != me.getType()){
                    sum += me.getDate() - lastTime.getDate();
                }
                c++;
            }
            s += "\n" + c;


            LongWritable average = new LongWritable();
            if(c == 0){
                return;
            }
            average.set(sum / c);
            context.write(key, average);
//            context.write(key, new Text(s));
        }
    }

    public static class Message implements WritableComparable<Message>, Comparator<Message>, Cloneable {
        int type = 0;
        long date = 0L;

        public Message() { }
        public Message(int t){ setType(t);}
        public Message(long d) { setDate(d);}
        public Message(int t, long d){setType(t); setDate(d);}
        public void setType(int t){ type = t; }
        public void setDate(long d){ date = d; }

        public int getType(){ return this.type; }
        public long getDate(){ return this.date; }

        public void readFields(DataInput in) throws IOException {
            type = in.readInt();
            date = in.readLong();
        }

        public void write(DataOutput out) throws IOException{
            out.writeInt(type);
            out.writeLong(date);
        }

        public int hashCode() {
            return (int)(this.date+this.type);
        }

        public String toString() {
            return Integer.toString(type) + Long.toString(date);
        }


        protected Object clone() throws CloneNotSupportedException {

            Message clone=(Message)super.clone();
            return clone;

        }

        public int compareTo(Message e2) {
            int c = Long.compare(this.getDate(), e2.getDate());
            return c == 0 ? Integer.compare(this.getType(), e2.getType()) : c;
        }

        public int compare(Message e1, Message e2) {
            int c = Long.compare(e1.getDate(), e2.getDate());
            return c == 0 ? Integer.compare(e1.getType(), e2.getType()) : c;
        }

    }

    public static Map < String, String > transformXmlToMap(String xml) {
        Map < String, String > map = new HashMap < String, String > ();
        try {
            String[] tokens = xml.trim().substring(5, xml.trim().length() - 3).split("\"");
            for (int i = 0; i < tokens.length - 1; i += 2) {
                String key = tokens[i].trim();
                String val = tokens[i + 1];
                map.put(key.substring(0, key.length() - 1), val);
            }
        }
        catch (StringIndexOutOfBoundsException e) {
            System.err.println(xml);
        }

        return map;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs =
                new GenericOptionsParser(conf, args).getRemainingArgs();

        if (otherArgs.length != 2) {
            System.err.println("Usage: MedianStdDev <in> <out>");
            System.exit(2);
        }

        Job job = Job.getInstance(conf, "Getting Average Response time of text messages");
        job.setJarByClass(MedianStdDev.class);
        job.setMapperClass(MedianStdDevMapper.class);
        job.setReducerClass(MedianStdDevReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Message.class);
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

