package LogClass;

import java.util.HashMap;
import java.util.Map;

public enum LogType {
	
	//Process
	ProcessCreate, 	//process create
	//Thread
	ThdCreate,  	//create thread
    ThdEnter,   	//enter thread
    ThdExit,    	//exit thread
    ThdJoin,    	//join thread
    //model RPC
    MsgProcEnter, 	//msg handler enter
    MsgProcExit, 	//msg handler exit
    MsgSending, 	//msg sending (rpc or sendSocket)
    //model EventHandler
    EventCreate, 	//event create
    EventProcEnter, //event handler enter
    EventProcExit, 	//event handler exit
    
    // heap R/W - no use in my project
    HeapRead,   	//read a heap var
    HeapWrite,  	//write a heap var
    
    //Added by JX
    
    //log Locks
    LockRequire, 	//require lock
    LockRelease, 	//release lock
    RWLockCreate,   	//jx - for creating ReentrantReadWriteLock
    //tmp event - no use now
    EventHandlerCreate,   //no use now
    EventHandlerBegin,
    EventHandlerEnd,
    //Sink
    TargetCodeBegin,
    TargetCodeEnd,
    //useless now
    LargeLoopBegin,
    //LargeLoopCenter,         //tmp
    //loop
    LoopBegin,               //also for spoon
    
    //only for dt
    LoopCenter,
    LoopPrint,     //only for javassist
    LoopEnd,       //only for spoon
    IO,
    RPC,
   
    //for dynamic slicing
    DynamicPoint,
	
    //for trigger
	//source - similar to loops but a little different
    SourceTimingBegin,
	SourceTimingEnd,
	SinkTimingBegin,
	SinkTimingEnd;
	
	

    
	static Map<LogType,LogType> logTypeMapping = new HashMap<LogType,LogType>() {{
		put(EventHandlerBegin, EventHandlerCreate);
		put(EventProcEnter, EventCreate);
		put(MsgProcEnter, MsgSending);
		put(ThdEnter, ThdCreate);  // or ProcessCreate
	}};
	
	
	
	static LogType getEndByBegin(LogType type) {
		if ( type.equals(SourceTimingBegin) ) {
			return SourceTimingEnd;
		}
		else if ( type.equals(SinkTimingBegin) ) {
			return SinkTimingEnd;
		}
		else if ( type.equals(TargetCodeBegin) ) {
			return TargetCodeEnd;
		}
		else {
		}
		return null;
	}
	
	
	
}
