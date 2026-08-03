package org.odata2ts.library.model;

public class ConditionReport {
  private Short ConditionBefore;
  private Short ConditionAfter;
  private String Remark;

  public ConditionReport() {}

  public ConditionReport(Short before, Short after, String remark) {
    ConditionBefore = before;
    ConditionAfter = after;
    Remark = remark;
  }

  public Short getConditionBefore() { return ConditionBefore; }
  public void setConditionBefore(Short v) { ConditionBefore = v; }
  public Short getConditionAfter() { return ConditionAfter; }
  public void setConditionAfter(Short v) { ConditionAfter = v; }
  public String getRemark() { return Remark; }
  public void setRemark(String v) { Remark = v; }
}
