/**
 * Created by guangpu on 3/21/16.
 */
//import cmd.da.GraphBuilder;
package da;

public class DynamicAnalysis {
	
    public static void main (String [] argv) {
        
    	//Added by JX
		if (argv.length != 1){
            System.out.println( "Please specify a correct xml file dir!! (argv.lenth=" + argv.length + ")" );
            return;
        }
		
		System.out.println("JX - da begin ...");
		long start_time = System.currentTimeMillis();         
		
		//init and get 'base' file
		GraphBuilder graphBuilder = new GraphBuilder( argv[0] );
    	//GraphBuilder graphBuilder = new GraphBuilder("input/MR-4813-xml"); //"input/JX-MR-xml" //Test-HB-4729-v6-3-xml");   "Test-HB-4729-v6-3-xml"
    	
		System.out.println("Completion Time: " + (System.currentTimeMillis()-start_time)/1000 + "s"); 
		
    	graphBuilder.buildsyncgraph();
    	//graphBuilder.buildmemref();
    	
    	System.out.println("Completion Time: " + (System.currentTimeMillis()-start_time)/1000 + "s"); 
    	
    	graphBuilder.buildlockmemref(); 
		if (argv.length > 1 ){                         //what's this for??
            graphBuilder.addspecialedges(argv[1]);
            System.out.println("---------S S S S S S S S S S S---------------");
		} 
		
		System.out.println("Completion Time: " + (System.currentTimeMillis()-start_time)/1000 + "s"); 
		
		// build ReachSet
		graphBuilder.buildReachSet();
		
		// 
		graphBuilder.traverseTargetCodes();
		
		// find out 1.flipped order 2.lock relationship graph by the same locks
		graphBuilder.findflippedorder();  
		// build lock relationship between different locks
		// TODO
    	//end-Added
		
		/*
		if (argv.length == 0){
            System.out.println("Please specify the xml file, dynamic analysis fails");
        }
        GraphBuilder graphBuilder = new GraphBuilder(argv[0]);
        graphBuilder.buildsyncgraph();    // build relation across threads based on xml file information
        
        //graphBuilder.buildtreepic();    // build a graphic view of the xml tree
        graphBuilder.buildmemref();       // build a map of memory address and the access to this location
		if (argv.length > 1 ){
	            graphBuilder.addspecialedges(argv[1]);
		    System.out.println("---------S S S S S S S S S S S---------------");
		}
        graphBuilder.buildvectorclock();  //JX: haopeng said this is the same as reachbitset used in 'findflippedorder'        // build the vecterclock of each node
        //graphBuilder.buildreachset();   // build a to reachable set of each node in order to identify the happen before relation
        //graphBuilder.findconcurrent();
        graphBuilder.findflippedorder();
        graphBuilder.queryhbrelation("/home/cstjygpl/Workspace/DC-Detector/build/classes/qhbr");
        */
    }
}
