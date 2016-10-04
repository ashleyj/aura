/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.4
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package aura.llvm.binding;

public enum AtomicRMWBinOp {
  AtomicRMWBinOpXchg,
  AtomicRMWBinOpAdd,
  AtomicRMWBinOpSub,
  AtomicRMWBinOpAnd,
  AtomicRMWBinOpNand,
  AtomicRMWBinOpOr,
  AtomicRMWBinOpXor,
  AtomicRMWBinOpMax,
  AtomicRMWBinOpMin,
  AtomicRMWBinOpUMax,
  AtomicRMWBinOpUMin;

  public final int swigValue() {
    return swigValue;
  }

  public static AtomicRMWBinOp swigToEnum(int swigValue) {
    AtomicRMWBinOp[] swigValues = AtomicRMWBinOp.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (AtomicRMWBinOp swigEnum : swigValues)
      if (swigEnum.swigValue == swigValue)
        return swigEnum;
    throw new IllegalArgumentException("No enum " + AtomicRMWBinOp.class + " with value " + swigValue);
  }

  @SuppressWarnings("unused")
  private AtomicRMWBinOp() {
    this.swigValue = SwigNext.next++;
  }

  @SuppressWarnings("unused")
  private AtomicRMWBinOp(int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue+1;
  }

  @SuppressWarnings("unused")
  private AtomicRMWBinOp(AtomicRMWBinOp swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue+1;
  }

  private final int swigValue;

  private static class SwigNext {
    private static int next = 0;
  }
}
