import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Ryan on 4/4/17.
 */
public class Solution {
    public static void main(String[] args) {
        if(args.length < 5){
            System.out.println("Not enough parameters.");
            return;
        }
        Parser par = new Parser("([^ ]+) - - \\[(.+)\\] \"(.+)\" (\\d+) ([\\d-]+)");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyy:HH:mm:ss Z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT-04"));

        minHeapMap topVisitors = new minHeapMap(10), topRes = new minHeapMap(10);
        Map<String, Long> visitorHash = new HashMap<String, Long>(), resHash = new HashMap<String, Long>();
        PriorityQueue<Wrapper> topWindows = new PriorityQueue<Wrapper>(new Comparator<Wrapper>() {
            @Override
            public int compare(Wrapper o1, Wrapper o2) {
                if(!o1.count.equals(o2.count)) {
                    return o1.count < o2.count ? -1 : 1;
                }
                else{
                    return o2.name.compareTo(o1.name);
                }
            }
        });

        int[] hourWindow= new int[3600];
        long hourStart = -1;

        ArrayList<String>[] blockWindow = new ArrayList[20];
        Map<String, Integer> candidateHash = new HashMap<String, Integer>();
        Map<String, Integer> blockList = new HashMap<String, Integer>();

        long pre = -1;
        long curNum = 0;
        int totalT = 0;
        try {
            File file = new File(args[0]);
            FileInputStream fis = new FileInputStream(file);
            File blockfile = new File(args[4]);
            FileWriter writer = new FileWriter(blockfile);
            //Construct BufferedReader from InputStreamReader
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line = null;
            //int i = 0;
            while ((line = br.readLine()) != null) {
                //generate feature1 & feature2
                Info res = par.parse(line);
                Long times = visitorHash.get(res.address);
                if(times == null){
                    times = 1L;
                }
                else{
                    times++;
                }
                visitorHash.put(res.address, times);
                if(topVisitors.contains(res.address)){
                    topVisitors.change(res.address, times);
                }
                else {
                    if (topVisitors.size() < 10) {
                        topVisitors.offer(new Wrapper(res.address, times));
                    } else if (topVisitors.peek().count < times) {
                        topVisitors.poll();
                        topVisitors.offer(new Wrapper(res.address, times));
                    }
                }

                if(res.request != null) {
                    Long size = resHash.get(res.content);
                    if (size == null) {
                        size = res.size;
                    } else {
                        size += res.size;
                    }
                    resHash.put(res.content, size);
                    if(topRes.contains(res.content)){
                        topRes.change(res.content, size);
                    }
                    else {
                        if (topRes.size() < 10) {
                            topRes.offer(new Wrapper(res.content, size));
                        } else if (topRes.peek().count < size) {
                            topRes.poll();
                            topRes.offer(new Wrapper(res.content, size));
                        }
                    }
                }
                //generate feature 3
                int index = (int)(res.sec % 3600);
                if(hourStart == -1){
                    hourStart = res.sec;
                }
                if(res.sec == pre){
                    hourWindow[index]++;
                    curNum++;
                }
                else{
                    if(pre != -1) {
                        for (long k = pre; k <= res.sec; k++) {
                            if (k - hourStart >= 3600) {
                                String window = dateFormat.format(new Date(hourStart * 1000L));
                                if (topWindows.size() < 10) {
                                    topWindows.offer(new Wrapper(window, curNum));
                                } else if (topWindows.peek().count < curNum) {
                                    topWindows.poll();
                                    topWindows.offer(new Wrapper(window, curNum));
                                }
                                hourStart++;
                                int idx = (int) (k % 3600);
                                curNum -= hourWindow[idx];
                                hourWindow[idx] = 0;
                            }
                        }
                    }
                    hourWindow[index] = 1;
                    curNum++;
                }

                //generate feature 4
                index = (int)(res.sec % 20);
                if(res.sec != pre && blockWindow[index] != null && blockWindow[index].size() > 0) {
                    for (String str : blockWindow[index]) {
                        Integer val = candidateHash.get(str);
                        if(val != null) {
                            if (val == 1) {
                                candidateHash.remove(str);
                            } else {
                                candidateHash.put(str, val - 1);
                            }
                        }
                    }
                    blockWindow[index].clear();
                }
                if(!blockList.isEmpty() && res.sec != pre){
                    Set<String> toRemove = new HashSet<String>();
                    for(String str : blockList.keySet()){
                        long timeElapsed = res.sec - pre;
                        int remain = blockList.get(str);
                        if(timeElapsed > remain){
                            toRemove.add(str);
                        }
                        blockList.put(str, remain - (int) timeElapsed);
                    }
                    if(!toRemove.isEmpty()) {
                        blockList.keySet().removeAll(toRemove);
                    }
                }

                if (blockList.containsKey(res.address)) {
                    //add into block file
                    writer.write(line + "\n");
                }//if didn't block
                else if(!blockList.containsKey(res.address) && res.request.equals("POST") && res.content.startsWith("/login")){
                    if(res.state.equals("401")) {
                        if (blockWindow[index] == null) {
                            blockWindow[index] = new ArrayList<String>();
                        }
                        if (res.sec == pre) {
                            blockWindow[index].add(res.address);
                        } else {
                            blockWindow[index].clear();
                            blockWindow[index].add(res.address);
                        }
                        int haveTried = candidateHash.getOrDefault(res.address, 0) + 1;
                        if (haveTried > 2) {
                            candidateHash.remove(res.address);
                            blockList.put(res.address, 300);
                        }
                        else {
                            candidateHash.put(res.address, haveTried);
                        }
                    }
                    else if(res.state.equals("200")){
                        if(candidateHash.containsKey(res.address)){
                            candidateHash.remove(res.address);
                        }
                    }
                }
                pre = res.sec;
                //System.out.println(i);
                //i++;
            }
            //corner case for 60min window
            while(hourStart <= pre) {
                String window = dateFormat.format(new Date(hourStart * 1000L));
                if (topWindows.size() < 10) {
                    topWindows.offer(new Wrapper(window, curNum));
                } else if (topWindows.peek().count < curNum) {
                    topWindows.poll();
                    topWindows.offer(new Wrapper(window, curNum));
                }
                int idx = (int)(hourStart % 3600);
                curNum -= hourWindow[idx];
                hourWindow[idx] = 0;
                hourStart++;
            }
            br.close();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Wrapper> topV = new ArrayList<Wrapper>(), topW = new ArrayList<Wrapper>();
        List<String> topR = new ArrayList<String>();
        try {
            //write feature1 to hosts.txt
            File file = new File(args[1]);
            FileWriter writer = new FileWriter(file);

            while(topVisitors.size() > 0){
                topV.add(topVisitors.poll());
            }
            for(int i = topV.size() - 1;i >= 0;i--){
                Wrapper cur = topV.get(i);
                writer.write(cur.name + "," + cur.count + "\n");
            }
            writer.flush();
            writer.close();

            //write feature2 to resources.txt
            file = new File(args[3]);
            writer = new FileWriter(file);

            while(topRes.size() > 0){
                topR.add(topRes.poll().name);
            }
            for(int i = topR.size() - 1;i >= 0;i--){
                writer.write(topR.get(i) + "\n");
            }
            writer.flush();
            writer.close();

            //write feature3 to hours.txt
            file = new File(args[2]);
            writer = new FileWriter(file);

            while(topWindows.size() > 0){
                topW.add(topWindows.poll());
            }
            for(int i = topW.size() - 1;i >= 0;i--){
                Wrapper cur = topW.get(i);
                writer.write(cur.name + "," + cur.count + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
