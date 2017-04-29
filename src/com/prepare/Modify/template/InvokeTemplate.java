package com.prepare.Modify.template;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtInvocation;
import java.util.*;

public interface InvokeTemplate {
  enum MODE { INSERT_BEFORE, REPLACE };

  public MODE getMode();
  public CtStatement getInvokeToInsert(CtExpression argu);
  public CtExpression getArguToInsert();
}
