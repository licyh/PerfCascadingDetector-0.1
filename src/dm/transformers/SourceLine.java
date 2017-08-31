package dm.transformers;

public class SourceLine {

	String className;
	String methodName;
	int lineNumber;
	
	//more for other purposes
	boolean flag = false;            
	String type;
	String ID;
	
	
	public SourceLine(String className, String methodName, int lineNumber) {
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = lineNumber;
	}
	
	public SourceLine(String className, String methodName, String lineNumber) {
		this(className, methodName, Integer.parseInt(lineNumber));
	}
	
	public String toString() {
		return this.className + "-" + this.methodName + "-" + this.lineNumber + "-" + this.type + "-" + this.ID;
	}
	
	
	public void setFlag(boolean flag) {
		this.flag = flag;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setID(String ID) {
		this.ID = ID;
	}
	
	
	/**
	 * Get APIs
	 */
	public String getClassName() {
		return this.className;
	}
	
	public String getMethodName() {
		return this.methodName;
	}
	
	public int getLineNumber() {
		return this.lineNumber;
	}
	
	public boolean getFlag() {
		return this.flag;
	}
	
	public String getType() {
		return this.type;
	}
	
	public String getID() {
		return this.ID;
	}
	
	
}
