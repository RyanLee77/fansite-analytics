import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ryan on 4/4/17.
 */
class Wrapper{
        Wrapper(String str, long n){
            this.name = str;
            this.count = n;
        }
        String name;
        Long count;
}
class minHeapMap {
    private Wrapper[] heap;
    private int size;
    private int maxSize;
    private Map<String, Integer> pos;

    minHeapMap(int size){
        this.size = 0;
        this.pos = new HashMap<String, Integer>();
        this.maxSize = (Integer.highestOneBit(size) << 1) - 1;
        this.heap = new Wrapper[this.maxSize + 1];
        this.heap[0] = new Wrapper(null, Long.MIN_VALUE);
    }

    int offer(Wrapper item){
        if(this.size == this.maxSize) {
            return -1;
        }
        this.heap[++this.size] = item;
        int cur = this.size;
        this.pos.put(item.name, cur);
        while(compare(this.heap[cur], this.heap[this.parent(cur)])) {
            swap(cur, this.parent(cur));
            cur = this.parent(cur);
        }
        return 0;
    }
    Wrapper poll(){
        if(this.size == 0){
            return null;
        }
        Wrapper toRemove = this.heap[1];
        this.pos.remove(toRemove.name);
        this.heap[1] = this.heap[this.size--];
        this.minHeapify(1);
        return toRemove;
    }
    Wrapper peek(){
        if(this.size > 0) {
            return this.heap[1];
        }
        else{
            return null;
        }
    }
    boolean contains(String str){
        return this.pos.containsKey(str);
    }
    int change(String str, long val){
        if(this.contains(str)){
            int p = this.pos.get(str);
            this.heap[p].count = val;
            minHeapify(p);
            return 0;
        }
        else{
            return -1;
        }
    }
    int size(){
        return this.size;
    }
    private void constructHeap(){
        for(int i=(this.size/2);i>=1;i--){
            minHeapify(i);
        }
    }
    private void minHeapify(int i){
        if(!this.isLeaf(i)){
            int lc=this.leftChild(i);
            int rc=this.rightChild(i);
            if(this.heap[rc] == null){
                if(!compare(this.heap[i], this.heap[lc])) {
                    this.swap(i, lc);
                }
            }
            else if(!compare(this.heap[i], this.heap[lc]) || !compare(this.heap[i], this.heap[rc])){
                if(compare(this.heap[lc], this.heap[rc])){
                    this.swap(i, lc);
                    minHeapify(lc);
                }
                else{
                    this.swap(i, rc);
                    minHeapify(rc);
                }
            }
        }
    }
    private boolean compare(Wrapper a, Wrapper b){
        if(!a.count.equals(b.count)){
            return a.count < b.count;
        }
        else{
            return a.name.compareTo(b.name) > 0;
        }
    }
    private int parent(int i) {
        return i / 2;
    }
    private int leftChild(int i){
        return i << 1;
    }
    private int rightChild(int i){
        return (i << 1) + 1;
    }
    private boolean isLeaf(int i){
        return i > (this.size / 2);
    }
    private void swap(int i, int j){
        Wrapper temp;
        temp = this.heap[i];
        this.pos.put(temp.name, j);
        this.heap[i] = this.heap[j];
        this.pos.put(this.heap[j].name, i);
        this.heap[j] = temp;
    }
}