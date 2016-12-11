package dm.Util.Bytecode;

import javassist.*;
import javassist.bytecode.*;

public class FieldInst extends Instruction {

  /* Field instructions:
   * - getstatic
   * - getfield
   * - putstatic
   * - putfield
   */

  public FieldInst(Instruction i) {
    this.setMethod(i.getMethod());
    this.setPos(i.getPos());
  }

  public int fieldIndex() {
    if (opcode().matches("(get|put)(static|field)_[\\d+]")) {
      return Integer.parseInt(opcode().substring(opcode().indexOf('_')+1));
    }
    else {
      return (int) codeIter.u16bitAt(pos + 1);
    }
  }

  public String fieldRefClass() {
    int index = fieldIndex();
    return constPool.getFieldrefClassName(index);
  }

  public String fieldRefName() {
    int index = fieldIndex();
    return constPool.getFieldrefName(index);
  }

  public String fieldRefType() {
    int index = fieldIndex();
    return constPool.getFieldrefType(index);
  }

  /* get != read */
  public boolean isGet() {
    return opcode().matches("get(static|field)($|_[\\d+])");
  }

  /* put != write */
  public boolean isPut() {
    return opcode().matches("put(static|field)($|_[\\d+])");
  }

  public boolean isStatic() {
    return opcode().matches("(get|put)static($|_[\\d+])");
  }

}
