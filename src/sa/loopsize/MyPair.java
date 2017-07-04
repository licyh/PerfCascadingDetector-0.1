package sa.loopsize;

import com.ibm.wala.ssa.SSAInstruction;

public class MyPair {
	private SSAInstruction l;
	private int r;
	public MyPair(SSAInstruction l, int r){
		this.l = l;
		this.r = r;
	}
	public SSAInstruction getL(){
		return this.l;
	}
	public void setL(SSAInstruction l){
		this.l = l;
	}
	public int getR(){
		return this.r;
	}
	public void setR(int r){
		this.r = r;
	}
}
