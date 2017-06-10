/**
 * Created by guangpu on 3/21/16.
 */
package da.graph;

public class Pair {
    public int destination;
    public int otype;
    //1 = thread executing order
    //2 = thread creating order
    public Pair (int x , int y){
        this.destination = x;
        this.otype = y;
    }

}
