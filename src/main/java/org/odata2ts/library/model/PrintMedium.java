package org.odata2ts.library.model;

/** Abstract intermediate level: carries the ISBN shared by Book, Magazine and TradeJournal. */
public abstract class PrintMedium extends Medium {
  private String ISBN;

  public String getISBN() { return ISBN; }
  public void setISBN(String isbn) { ISBN = isbn; }
}
