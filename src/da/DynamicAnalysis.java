/**
 * Created by guangpu on 3/21/16.
 */
//import cmd.da.GraphBuilder;
package da;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.system.Timer;

import da.cascading.CascadingAnalyzer;
import da.graph.AccidentalHBGraph;
import da.graph.HappensBeforeGraph;

public class DynamicAnalysis {
	
    public static void main (String [] argv) {
    	System.out.println("JX - INFO - DynamicAnalysis: main... (da begin)");
    	// start Timer
    	Timer timer = new Timer( Paths.get(argv[0], "src/da", "output/timer.txt") );
    	timer.tic("da begin");
    			
    	//Added by JX
		if (argv.length != 2){
            System.out.println( "Please specify a correct project dir or a xml file dir!! (argv.lenth=" + argv.length + ")" );
            return;
        }
		
		HappensBeforeGraph g = new HappensBeforeGraph( argv[1] );
		g.buildsyncgraph();
    	
    	//jx: Add Edges manually for DEBUGGING
        //this is for mr-4576
    	//graphBuilder.addEdgesManually();
    		
		// build ReachSet
		g.buildReachSet();
		
		// for DEBUGGING
		if (Files.exists(Paths.get("/tmp/relations.txt"))) {
			System.out.println("JX - DEBUG - happens-before graph test");
			g.queryHappensBeforeRelations("/tmp/relations.txt");
		}
		
				
		timer.toc("end Happens before analysis");
		
		AccidentalHBGraph ag = new AccidentalHBGraph( g );
    	ag.buildLockmemref(); 
		
    	timer.toc("end Accidental Happens before analysis");
		
		// Cascading Analysis
		CascadingAnalyzer cascadingAnalyzer = new CascadingAnalyzer( argv[0], g, ag );
		cascadingAnalyzer.doWork();
		
		timer.toc("end cacading analysis");
		timer.close();
		
		// find out 1.flipped order 2.lock relationship graph by the same locks
		g.findflippedorder();  
		// build lock relationship between different locks
		// TODO
    	//end-Added
		
		/*
		if (argv.length == 0){
            System.out.println("Please specify the xml file, dynamic analysis fails");
        }
        HappensBeforeGraph graphBuilder = new HappensBeforeGraph(argv[0]);
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
