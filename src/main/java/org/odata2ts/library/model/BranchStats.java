package org.odata2ts.library.model;

public class BranchStats {
  private Integer BranchId;
  private Long LoanCount;

  public BranchStats() {}

  public BranchStats(Integer branchId, Long loanCount) {
    BranchId = branchId;
    LoanCount = loanCount;
  }

  public Integer getBranchId() { return BranchId; }
  public void setBranchId(Integer v) { BranchId = v; }
  public Long getLoanCount() { return LoanCount; }
  public void setLoanCount(Long v) { LoanCount = v; }
}
