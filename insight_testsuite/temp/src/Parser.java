import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ryan on 4/4/17.
 */
class Info{
    String address;
    String time;
    long sec;
    String request;
    String content;
    String state;
    long size;
}
class Parser {
    Parser(String str){
        this.pattern = Pattern.compile(str);
    }
    Info parse(String input){
        Matcher matcher = pattern.matcher(input);
        if(matcher.matches()){
            Info res = new Info();
            //get host
            res.address = matcher.group(1);
            //get timestamp and change it to seconds
            res.time = matcher.group(2);
            try{
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyy:HH:mm:ss Z");
                Date parsedDate = dateFormat.parse(res.time);
                Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
                res.sec = timestamp.getTime() / 1000;

            }catch(Exception e) {//this generic but you can control another types of exception
                e.printStackTrace();
            }
            //get request
            String temp = matcher.group(3);
            if(!temp.isEmpty()){
                int div = temp.indexOf(" ");
                if(div != -1) {
                    res.request = temp.substring(0, div);
                    res.content = temp.substring(div + 1);
                    if(res.content.endsWith("HTTP/1.0")){
                        res.content = res.content.substring(0, res.content.length() - 8);
                    }
                }
                else{
                    res.request = res.content = "";
                }
            }
            //get state
            res.state = matcher.group(4);
            temp = matcher.group(5);
            //get transmit bytes
            if(temp.equals("-")){
                res.size = 0;
            }
            else {
                res.size = Long.parseLong(temp);
            }
            return res;
        }
        else{
            return null;
        }
    }
    private Pattern pattern;
}
