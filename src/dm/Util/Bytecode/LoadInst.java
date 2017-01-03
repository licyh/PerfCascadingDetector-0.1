package dm.Util.Bytecode;

import javassist.*;
import javassist.bytecode.*;

/* All load instructions:
 * 1. aload
 * 2. aaload
 * 3. aload_<n>
 * 4. baload: load byte or boolean from array
 * 5. caload: load char from array
 * 6. daload: load double from array
 * 7. dload: load double from local var
 * 8. dload_<n>: load double from local var
 * 9. faload: load float from array
 * 10. fload: load float from local var
 * 11. fload_<n>: load float from local var
 * 12. iaload: load int from array
 * 13. iload: load int from local var
 * 14. iload_<n>: load int from local var
 * 15. laload: load long from array
 * 16. lload: load long from local var
 * 17. lload_<n>: load long from local var
 * 18. saload: load short from array
 */

public class LoadInst extends Instruction {

  public LoadInst(Instruction i) {
    this.setMethod(i.getMethod());
    this.setPos(i.getPos());
  }

  public int getIndex() {
    if (opcode().matches("[a-zA-Z]+load_[\\d+]")) { //aload_n
      return Integer.parseInt(opcode().substring(opcode().indexOf('_')+1));
    }
    else { //aload
      return (int) codeIter.u16bitAt(pos + 1);
    }
  }

  public String toString() {
    String op = opcode();
    String str = op.substring(op.length()-1);
    int index = Integer.parseInt(str);
    System.out.println("debug: " + index);

    if (opcode().endsWith("load")) {
      //Modified by JX
      //return opcode() + " " + codeIter.byteAt(pos + 1);
      return opcode() + " " + constPool.getNameAndTypeName( codeIter.byteAt(pos + 1) );
      //end-Modified
    }
    else {
      //Modified by JX
      //return opcode() + " " + constPool.getNameAndTypeName(index);
      LocalVariableAttribute table = (LocalVariableAttribute) codeAttr.getAttribute(LocalVariableAttribute.tag); 
      String variableName = table.variableName( index );  //??
      return opcode() + " " + variableName;
      //end-Modified
    }
  }


} 
