/**
 * Created by guangpu on 3/21/16.
 */
//import cmd.da.GraphBuilder;
package da;

public class DynamicAnalysis {
    public static void main (String [] argv){
        if (argv.length == 0){
            System.out.println("Please specify the xml file, dynamic analysis fails");
        }
        GraphBuilder graphBuilder = new GraphBuilder(argv[0]);
        graphBuilder.buildsyncgraph();    // build relation across threads based on xml file information
        //graphBuilder.buildtreepic(); // build a graphic view of the xml tree
        graphBuilder.buildmemref();  // build a map of memory address and the access to this location
	if (argv.length > 1 ){
            graphBuilder.addspecialedges(argv[1]);
	    System.out.println("---------S S S S S S S S S S S---------------");
	}
        graphBuilder.buildvectorclock(); // build the vecterclock of each node
        //graphBuilder.buildreachset(); // build a to reachable set of each node in order to identify the happen before relation
        //graphBuilder.findconcurrent();
        graphBuilder.findflippedorder();
	graphBuilder.queryhbrelation("/home/cstjygpl/Workspace/DC-Detector/build/classes/qhbr");
    }
}
