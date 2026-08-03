package org.odata2ts.library.model;

import java.math.BigDecimal;

public class AnnualReport {
  private Integer Year;
  private Long TotalLoans;
  private BigDecimal TotalLateFees;

  public AnnualReport() {}

  public AnnualReport(Integer year, Long totalLoans, BigDecimal totalLateFees) {
    Year = year;
    TotalLoans = totalLoans;
    TotalLateFees = totalLateFees;
  }

  public Integer getYear() { return Year; }
  public void setYear(Integer v) { Year = v; }
  public Long getTotalLoans() { return TotalLoans; }
  public void setTotalLoans(Long v) { TotalLoans = v; }
  public BigDecimal getTotalLateFees() { return TotalLateFees; }
  public void setTotalLateFees(BigDecimal v) { TotalLateFees = v; }
}
