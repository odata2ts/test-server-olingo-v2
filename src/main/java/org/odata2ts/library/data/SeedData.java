package org.odata2ts.library.data;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.odata2ts.library.model.Audiobook;
import org.odata2ts.library.model.AudiobookChapter;
import org.odata2ts.library.model.Book;
import org.odata2ts.library.model.Branch;
import org.odata2ts.library.model.Copy;
import org.odata2ts.library.model.DVD;
import org.odata2ts.library.model.EBook;
import org.odata2ts.library.model.IdDocument;
import org.odata2ts.library.model.Loan;
import org.odata2ts.library.model.Magazine;
import org.odata2ts.library.model.Member;
import org.odata2ts.library.model.PostalAddress;
import org.odata2ts.library.model.Publisher;
import org.odata2ts.library.model.PublisherBranch;
import org.odata2ts.library.model.Reservation;
import org.odata2ts.library.model.TradeJournal;

/**
 * The fixed, well-known starting state of the service.
 *
 * <p>Held in memory and rebuilt per process, so every container starts from an identical state and there
 * is nothing to migrate or deploy. The keys deliberately match those of
 * <a href="https://github.com/odata2ts/test-server-cap">test-server-cap</a> where the entity exists in
 * both, so assertions can be compared across the two servers rather than re-derived.
 */
public final class SeedData {

  public static final UUID BOOK_DER_PROZESS = UUID.fromString("11111111-1111-1111-1111-111111111111");
  public static final UUID BOOK_DIE_VERWANDLUNG = UUID.fromString("11111111-1111-1111-1111-111111111112");
  public static final UUID BOOK_MOMO = UUID.fromString("11111111-1111-1111-1111-111111111113");
  public static final UUID BOOK_HOBBIT = UUID.fromString("11111111-1111-1111-1111-111111111114");
  public static final UUID MAGAZINE_SPEKTRUM = UUID.fromString("22222222-2222-2222-2222-222222222221");
  public static final UUID TRADE_JOURNAL_NATURE = UUID.fromString("33333333-3333-3333-3333-333333333331");
  public static final UUID AUDIOBOOK_ODYSSEE = UUID.fromString("44444444-4444-4444-4444-444444444441");
  public static final UUID DVD_METROPOLIS = UUID.fromString("55555555-5555-5555-5555-555555555551");
  public static final UUID EBOOK_CLEAN_CODE = UUID.fromString("66666666-6666-6666-6666-666666666661");

  public static final UUID LOAN_OPEN = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
  public static final UUID LOAN_OVERDUE = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
  public static final UUID LOAN_RETURNED = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000003");
  public static final UUID ID_DOCUMENT_ANNA = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  public static final UUID RESERVATION_ANNA = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

  private static final Charset UTF8 = Charset.forName("UTF-8");

  private SeedData() {}

  public static List<Book> books() {
    List<Book> books = new ArrayList<Book>();
    books.add(book(BOOK_DER_PROZESS, "Der Prozess", "de", date(1925, 4, 26), 87.5,
        "9783150094440", (short) 224, (short) 16, 1));
    books.add(book(BOOK_DIE_VERWANDLUNG, "Die Verwandlung", "de", date(1915, 11, 1), 92.25,
        "9783150097007", (short) 66, (short) 14, 1));
    books.add(book(BOOK_MOMO, "Momo", "de", date(1973, 1, 1), 95.0,
        "9783522177504", (short) 304, (short) 8, 2));
    books.add(book(BOOK_HOBBIT, "The Hobbit", "en", date(1937, 9, 21), 99.75,
        "9780261102217", (short) 310, (short) 10, 3));
    return books;
  }

  public static List<Magazine> magazines() {
    Magazine magazine = new Magazine();
    magazine.setId(MAGAZINE_SPEKTRUM);
    magazine.setTitle("Spektrum der Wissenschaft");
    magazine.setLanguage("de");
    magazine.setPublicationDate(date(2024, 3, 1));
    magazine.setPopularityScore(61.0);
    magazine.setISBN("9770170294003");
    magazine.setIssueNumber(3);
    return new ArrayList<Magazine>(java.util.Collections.singletonList(magazine));
  }

  public static List<TradeJournal> tradeJournals() {
    TradeJournal journal = new TradeJournal();
    journal.setId(TRADE_JOURNAL_NATURE);
    journal.setTitle("Nature");
    journal.setLanguage("en");
    journal.setPublicationDate(date(2024, 6, 13));
    journal.setPopularityScore(88.0);
    journal.setISBN("9770028083610");
    journal.setIssueNumber(7965);
    journal.setField("Multidisciplinary");
    return new ArrayList<TradeJournal>(java.util.Collections.singletonList(journal));
  }

  public static List<Audiobook> audiobooks() {
    Audiobook audiobook = new Audiobook();
    audiobook.setId(AUDIOBOOK_ODYSSEE);
    audiobook.setTitle("Die Odyssee");
    audiobook.setLanguage("de");
    audiobook.setPublicationDate(date(2019, 10, 4));
    audiobook.setPopularityScore(73.5);
    audiobook.setDuration(duration(11, 20, 0));
    audiobook.setNarrator("Hans Meiser");
    return new ArrayList<Audiobook>(java.util.Collections.singletonList(audiobook));
  }

  public static List<AudiobookChapter> audiobookChapters() {
    List<AudiobookChapter> chapters = new ArrayList<AudiobookChapter>();
    chapters.add(chapter(1, "Erster Gesang"));
    chapters.add(chapter(2, "Zweiter Gesang"));
    return chapters;
  }

  public static List<DVD> dvds() {
    DVD dvd = new DVD();
    dvd.setId(DVD_METROPOLIS);
    dvd.setTitle("Metropolis");
    dvd.setLanguage("de");
    dvd.setPublicationDate(date(1927, 1, 10));
    dvd.setPopularityScore(80.0);
    dvd.setDuration(duration(2, 33, 0));
    dvd.setRegionCode((short) 2);
    return new ArrayList<DVD>(java.util.Collections.singletonList(dvd));
  }

  public static List<EBook> ebooks() {
    EBook ebook = new EBook();
    ebook.setId(EBOOK_CLEAN_CODE);
    ebook.setTitle("Clean Code");
    ebook.setLanguage("en");
    ebook.setPublicationDate(date(2008, 8, 1));
    ebook.setPopularityScore(88.5);
    ebook.setFileFormat("EPUB");
    ebook.setContent("PK seed-epub-content".getBytes(UTF8));
    ebook.setContentType("application/epub+zip");
    return new ArrayList<EBook>(java.util.Collections.singletonList(ebook));
  }

  public static List<Copy> copies() {
    List<Copy> copies = new ArrayList<Copy>();
    copies.add(copy(BOOK_DER_PROZESS, 1001, (short) 1, true, (short) 0, date(2018, 2, 1), 0.31f, "A-01-04", 1));
    copies.add(copy(BOOK_DER_PROZESS, 1002, (short) 3, false, (short) 1, date(2020, 8, 15), 0.31f, "A-01-05", 1));
    copies.add(copy(BOOK_DIE_VERWANDLUNG, 1003, (short) 1, true, (short) 0, date(2019, 5, 9), 0.12f, "A-02-01", 1));
    copies.add(copy(BOOK_MOMO, 1004, (short) 2, true, (short) 0, date(2021, 3, 3), 0.42f, "B-01-01", 2));
    copies.add(copy(BOOK_HOBBIT, 1005, (short) 1, true, (short) 0, date(2017, 11, 20), 0.38f, "B-02-07", 2));
    copies.add(copy(AUDIOBOOK_ODYSSEE, 1006, (short) 1, true, (short) 0, date(2020, 1, 7), 0.09f, "C-01-01", 3));
    copies.add(copy(DVD_METROPOLIS, 1007, (short) 2, false, (short) 2, date(2016, 6, 6), 0.08f, "C-02-02", 3));
    return copies;
  }

  public static List<Member> members() {
    List<Member> members = new ArrayList<Member>();
    members.add(member(1, "Anna Berger", date(1988, 3, 14),
        new PostalAddress("Lindenweg 4", "Hamburg", "22765", "DE"),
        timestamp(2019, 4, 1, 10, 35), new BigDecimal("0.00"), ID_DOCUMENT_ANNA));
    members.add(member(2, "Bruno Faltz", date(1975, 7, 2),
        new PostalAddress("Marktplatz 12", "Bremen", "28195", "DE"),
        timestamp(2021, 9, 17, 8, 5), new BigDecimal("4.50"), null));
    members.add(member(3, "Clara Dinh", date(2001, 12, 30),
        new PostalAddress("Ringstrasse 88", "Hamburg", "20095", "DE"),
        timestamp(2023, 1, 4, 16, 20), new BigDecimal("12.50"), null));
    return members;
  }

  public static List<Loan> loans() {
    List<Loan> loans = new ArrayList<Loan>();
    loans.add(loan(LOAN_OPEN, timestamp(2026, 7, 1, 9, 0), date(2026, 8, 15), null,
        null, 1, BOOK_DER_PROZESS, 1001));
    loans.add(loan(LOAN_OVERDUE, timestamp(2026, 4, 30, 14, 30), date(2026, 5, 30), null,
        new BigDecimal("4.50"), 2, BOOK_MOMO, 1004));
    // returned: the explicit-null-vs-absent case has its counterpart here
    loans.add(loan(LOAN_RETURNED, timestamp(2026, 6, 1, 11, 15), date(2026, 7, 29),
        timestamp(2026, 7, 20, 15, 45), new BigDecimal("0.00"), 1, BOOK_HOBBIT, 1005));
    return loans;
  }

  public static List<Reservation> reservations() {
    Reservation reservation = new Reservation();
    reservation.setId(RESERVATION_ANNA);
    reservation.setReservedAt(timestamp(2026, 7, 25, 12, 0));
    reservation.setMemberId(1);
    return new ArrayList<Reservation>(java.util.Collections.singletonList(reservation));
  }

  public static List<IdDocument> idDocuments() {
    IdDocument document = new IdDocument();
    document.setId(ID_DOCUMENT_ANNA);
    document.setScan("seed-scan-bytes".getBytes(UTF8));
    document.setUploadedAt(timestamp(2019, 4, 1, 10, 30));
    return new ArrayList<IdDocument>(java.util.Collections.singletonList(document));
  }

  public static List<Branch> branches() {
    List<Branch> branches = new ArrayList<Branch>();
    branches.add(branch(1, "Zentralbibliothek",
        new PostalAddress("Hauptstrasse 1", "Hamburg", "20095", "DE"),
        (byte) -2, time(9, 0), time(20, 0), 31, 1841000L));
    branches.add(branch(2, "Stadtteilbibliothek Altona",
        new PostalAddress("Ottenser Hauptstrasse 10", "Hamburg", "22765", "DE"),
        (byte) 0, time(10, 0), time(18, 0), 11, 275000L));
    branches.add(branch(3, "Bibliothek Bremen Mitte",
        new PostalAddress("Am Wall 201", "Bremen", "28195", "DE"),
        (byte) -1, time(8, 30), time(19, 0), 7, 569000L));
    return branches;
  }

  public static List<Publisher> publishers() {
    List<Publisher> publishers = new ArrayList<Publisher>();
    publishers.add(publisher(1, "Reclam", "DE", date(1828, 10, 1)));
    publishers.add(publisher(2, "Thienemann", "DE", date(1849, 1, 1)));
    publishers.add(publisher(3, "George Allen & Unwin", "GB", date(1914, 1, 1)));
    return publishers;
  }

  public static List<PublisherBranch> publisherBranches() {
    List<PublisherBranch> branches = new ArrayList<PublisherBranch>();
    branches.add(publisherBranch(1, "Stuttgart", "DE"));
    branches.add(publisherBranch(2, "London", "GB"));
    return branches;
  }

  // ------------------------------------------------------------------------------------------------
  // builders
  // ------------------------------------------------------------------------------------------------

  private static Book book(UUID id, String title, String language, Calendar published, double score,
      String isbn, Short pageCount, Short ageRating, Integer publisherId) {
    Book book = new Book();
    book.setId(id);
    book.setTitle(title);
    book.setLanguage(language);
    book.setPublicationDate(published);
    book.setPopularityScore(score);
    book.setISBN(isbn);
    book.setPageCount(pageCount);
    book.setAgeRating(ageRating);
    book.setPublisherId(publisherId);
    return book;
  }

  private static AudiobookChapter chapter(int id, String title) {
    AudiobookChapter chapter = new AudiobookChapter();
    chapter.setId(id);
    chapter.setTitle(title);
    chapter.setAudiobookId(AUDIOBOOK_ODYSSEE);
    chapter.setContent(("seed-audio-chapter-" + id).getBytes(UTF8));
    chapter.setContentType("audio/mpeg");
    return chapter;
  }

  private static Copy copy(UUID mediumId, int inventoryNumber, Short condition, boolean loanable,
      Short status, Calendar acquired, float weight, String shelfCode, Integer locationId) {
    Copy copy = new Copy();
    copy.setMediumId(mediumId);
    copy.setInventoryNumber(inventoryNumber);
    copy.setCondition(condition);
    copy.setIsLoanable(loanable);
    copy.setStatus(status);
    copy.setAcquisitionDate(acquired);
    copy.setWeightKg(weight);
    copy.setShelfCode(shelfCode);
    copy.setLocationId(locationId);
    return copy;
  }

  private static Member member(int id, String name, Calendar dateOfBirth, PostalAddress address,
      Calendar activeSince, BigDecimal balance, UUID idDocumentId) {
    Member member = new Member();
    member.setId(id);
    member.setName(name);
    member.setDateOfBirth(dateOfBirth);
    member.setAddress(address);
    member.setActiveSince(activeSince);
    member.setBalance(balance);
    member.setIdDocumentId(idDocumentId);
    return member;
  }

  private static Loan loan(UUID id, Calendar loanedAt, Calendar dueDate, Calendar returnedAt,
      BigDecimal lateFee, Integer memberId, UUID copyMediumId, Integer copyInventoryNumber) {
    Loan loan = new Loan();
    loan.setId(id);
    loan.setLoanedAt(loanedAt);
    loan.setDueDate(dueDate);
    loan.setReturnedAt(returnedAt);
    loan.setLateFee(lateFee);
    loan.setMemberId(memberId);
    loan.setCopyMediumId(copyMediumId);
    loan.setCopyInventoryNumber(copyInventoryNumber);
    return loan;
  }

  private static Branch branch(int id, String name, PostalAddress address, byte lowestFloor,
      Calendar opensAt, Calendar closesAt, Integer amenities, Long population) {
    Branch branch = new Branch();
    branch.setId(id);
    branch.setName(name);
    branch.setAddress(address);
    branch.setLowestFloor(lowestFloor);
    branch.setOpensAt(opensAt);
    branch.setClosesAt(closesAt);
    branch.setAmenities(amenities);
    branch.setPopulation(population);
    return branch;
  }

  private static Publisher publisher(int id, String name, String country, Calendar founded) {
    Publisher publisher = new Publisher();
    publisher.setId(id);
    publisher.setName(name);
    publisher.setCountry(country);
    publisher.setFounded(founded);
    return publisher;
  }

  private static PublisherBranch publisherBranch(int id, String city, String country) {
    PublisherBranch branch = new PublisherBranch();
    branch.setId(id);
    branch.setCity(city);
    branch.setCountry(country);
    return branch;
  }

  /** All Calendars are UTC, so the serialized ticks are stable regardless of the container's timezone. */
  private static Calendar utc() {
    Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    calendar.clear();
    return calendar;
  }

  static Calendar date(int year, int month, int day) {
    Calendar calendar = utc();
    calendar.set(year, month - 1, day);
    return calendar;
  }

  static Calendar timestamp(int year, int month, int day, int hour, int minute) {
    Calendar calendar = utc();
    calendar.set(year, month - 1, day, hour, minute, 0);
    return calendar;
  }

  static Calendar time(int hour, int minute) {
    Calendar calendar = utc();
    calendar.set(Calendar.HOUR_OF_DAY, hour);
    calendar.set(Calendar.MINUTE, minute);
    return calendar;
  }

  static Calendar duration(int hours, int minutes, int seconds) {
    Calendar calendar = utc();
    calendar.set(Calendar.HOUR_OF_DAY, hours);
    calendar.set(Calendar.MINUTE, minutes);
    calendar.set(Calendar.SECOND, seconds);
    return calendar;
  }
}
