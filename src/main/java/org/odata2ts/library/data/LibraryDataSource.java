package org.odata2ts.library.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataNotFoundException;
import org.apache.olingo.odata2.api.exception.ODataNotImplementedException;
import org.odata2ts.library.edm.LibraryEdmProvider;
import org.odata2ts.library.model.Audiobook;
import org.odata2ts.library.model.AudiobookChapter;
import org.odata2ts.library.model.AnnualReport;
import org.odata2ts.library.model.Book;
import org.odata2ts.library.model.Branch;
import org.odata2ts.library.model.BranchStats;
import org.odata2ts.library.model.ConditionReport;
import org.odata2ts.library.model.Copy;
import org.odata2ts.library.model.DVD;
import org.odata2ts.library.model.EBook;
import org.odata2ts.library.model.IdDocument;
import org.odata2ts.library.model.Loan;
import org.odata2ts.library.model.LoanStats;
import org.odata2ts.library.model.Magazine;
import org.odata2ts.library.model.Medium;
import org.odata2ts.library.model.MediumStats;
import org.odata2ts.library.model.Member;
import org.odata2ts.library.model.OverdueNotice;
import org.odata2ts.library.model.Publisher;
import org.odata2ts.library.model.PublisherBranch;
import org.odata2ts.library.model.Reservation;
import org.odata2ts.library.model.TradeJournal;

/**
 * The one interface {@code ListsProcessor} drives, over in-memory lists.
 *
 * <p>Everything above this class - {@code $filter}, {@code $orderby}, {@code $top}, {@code $skip},
 * {@code $inlinecount}, {@code $expand}, {@code $select}, {@code $count}, {@code $links} and the
 * serialization of every payload shape - is Olingo's. This file only answers "give me the rows", "give
 * me the related rows" and "run this operation".
 */
public class LibraryDataSource implements DataSource {

  private final Map<String, List<Object>> stores = new LinkedHashMap<String, List<Object>>();
  private int nextInventoryNumber = 6001;

  public LibraryDataSource() {
    store(LibraryEdmProvider.ES_BOOKS, SeedData.books());
    store(LibraryEdmProvider.ES_MAGAZINES, SeedData.magazines());
    store(LibraryEdmProvider.ES_TRADE_JOURNALS, SeedData.tradeJournals());
    store(LibraryEdmProvider.ES_AUDIOBOOKS, SeedData.audiobooks());
    store(LibraryEdmProvider.ES_AUDIOBOOK_CHAPTERS, SeedData.audiobookChapters());
    store(LibraryEdmProvider.ES_DVDS, SeedData.dvds());
    store(LibraryEdmProvider.ES_EBOOKS, SeedData.ebooks());
    store(LibraryEdmProvider.ES_COPIES, SeedData.copies());
    store(LibraryEdmProvider.ES_MEMBERS, SeedData.members());
    store(LibraryEdmProvider.ES_LOANS, SeedData.loans());
    store(LibraryEdmProvider.ES_RESERVATIONS, SeedData.reservations());
    store(LibraryEdmProvider.ES_ID_DOCUMENTS, SeedData.idDocuments());
    store(LibraryEdmProvider.ES_BRANCHES, SeedData.branches());
    store(LibraryEdmProvider.ES_PUBLISHERS, SeedData.publishers());
    store(LibraryEdmProvider.ES_PUBLISHER_BRANCHES, SeedData.publisherBranches());
  }

  private void store(final String entitySet, final List<?> data) {
    stores.put(entitySet, new ArrayList<Object>(data));
  }

  // ------------------------------------------------------------------------------------------------
  // reading
  // ------------------------------------------------------------------------------------------------

  @Override
  public List<?> readData(final EdmEntitySet entitySet) throws ODataNotFoundException, EdmException {
    List<Object> data = stores.get(entitySet.getName());
    if (data == null) {
      throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
    }
    return data;
  }

  @Override
  public Object readData(final EdmEntitySet entitySet, final Map<String, Object> keys)
      throws ODataNotFoundException, EdmException {
    for (Object row : readData(entitySet)) {
      if (matchesKeys(entitySet.getName(), row, keys)) {
        return row;
      }
    }
    throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
  }

  @Override
  public Object readRelatedData(final EdmEntitySet sourceEntitySet, final Object sourceData,
      final EdmEntitySet targetEntitySet, final Map<String, Object> targetKeys)
      throws ODataNotFoundException, EdmException {
    List<Object> related = related(sourceEntitySet.getName(), sourceData, targetEntitySet.getName());

    if (targetKeys.isEmpty()) {
      // a to-one navigation still arrives here with no keys; the processor decides by multiplicity
      return isToOne(sourceEntitySet.getName(), targetEntitySet.getName())
          ? (related.isEmpty() ? null : related.get(0))
          : related;
    }
    for (Object row : related) {
      if (matchesKeys(targetEntitySet.getName(), row, targetKeys)) {
        return row;
      }
    }
    throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
  }

  private boolean isToOne(final String source, final String target) {
    return (LibraryEdmProvider.ES_COPIES.equals(source) && LibraryEdmProvider.ES_BOOKS.equals(target))
        || (LibraryEdmProvider.ES_COPIES.equals(source) && LibraryEdmProvider.ES_BRANCHES.equals(target))
        || (LibraryEdmProvider.ES_BOOKS.equals(source) && LibraryEdmProvider.ES_PUBLISHERS.equals(target))
        || (LibraryEdmProvider.ES_MEMBERS.equals(source) && LibraryEdmProvider.ES_ID_DOCUMENTS.equals(target))
        || (LibraryEdmProvider.ES_LOANS.equals(source) && LibraryEdmProvider.ES_MEMBERS.equals(target))
        || (LibraryEdmProvider.ES_LOANS.equals(source) && LibraryEdmProvider.ES_COPIES.equals(target))
        || (LibraryEdmProvider.ES_AUDIOBOOK_CHAPTERS.equals(source)
            && LibraryEdmProvider.ES_AUDIOBOOKS.equals(target));
  }

  private List<Object> related(final String source, final Object sourceData, final String target) {
    List<Object> result = new ArrayList<Object>();

    if (LibraryEdmProvider.ES_COPIES.equals(target) && sourceData instanceof Medium) {
      UUID mediumId = ((Medium) sourceData).getId();
      for (Object row : stores.get(LibraryEdmProvider.ES_COPIES)) {
        if (((Copy) row).getMediumId().equals(mediumId)) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_BOOKS.equals(target) && sourceData instanceof Copy) {
      // the reverse of Medium_Copies. With one entity set per concrete type there is no single set
      // this can point at, and the association set binds it to Books - so a copy of a DVD or an
      // audiobook has no reachable `Medium` here. See FEATURE-COVERAGE.md.
      UUID mediumId = ((Copy) sourceData).getMediumId();
      for (Object row : stores.get(LibraryEdmProvider.ES_BOOKS)) {
        if (((Book) row).getId().equals(mediumId)) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_PUBLISHERS.equals(target) && sourceData instanceof Book) {
      Integer publisherId = ((Book) sourceData).getPublisherId();
      for (Object row : stores.get(LibraryEdmProvider.ES_PUBLISHERS)) {
        if (((Publisher) row).getId().equals(publisherId)) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_BOOKS.equals(target) && sourceData instanceof Publisher) {
      Integer publisherId = ((Publisher) sourceData).getId();
      for (Object row : stores.get(LibraryEdmProvider.ES_BOOKS)) {
        if (publisherId.equals(((Book) row).getPublisherId())) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_AUDIOBOOK_CHAPTERS.equals(target) && sourceData instanceof Audiobook) {
      UUID audiobookId = ((Audiobook) sourceData).getId();
      for (Object row : stores.get(LibraryEdmProvider.ES_AUDIOBOOK_CHAPTERS)) {
        if (audiobookId.equals(((AudiobookChapter) row).getAudiobookId())) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_AUDIOBOOKS.equals(target) && sourceData instanceof AudiobookChapter) {
      UUID audiobookId = ((AudiobookChapter) sourceData).getAudiobookId();
      for (Object row : stores.get(LibraryEdmProvider.ES_AUDIOBOOKS)) {
        if (((Audiobook) row).getId().equals(audiobookId)) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_LOANS.equals(target) && sourceData instanceof Member) {
      Integer memberId = ((Member) sourceData).getId();
      for (Object row : stores.get(LibraryEdmProvider.ES_LOANS)) {
        if (memberId.equals(((Loan) row).getMemberId())) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_MEMBERS.equals(target) && sourceData instanceof Loan) {
      Integer memberId = ((Loan) sourceData).getMemberId();
      for (Object row : stores.get(LibraryEdmProvider.ES_MEMBERS)) {
        if (((Member) row).getId().equals(memberId)) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_RESERVATIONS.equals(target) && sourceData instanceof Member) {
      Integer memberId = ((Member) sourceData).getId();
      for (Object row : stores.get(LibraryEdmProvider.ES_RESERVATIONS)) {
        if (memberId.equals(((Reservation) row).getMemberId())) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_ID_DOCUMENTS.equals(target) && sourceData instanceof Member) {
      UUID documentId = ((Member) sourceData).getIdDocumentId();
      for (Object row : stores.get(LibraryEdmProvider.ES_ID_DOCUMENTS)) {
        if (((IdDocument) row).getId().equals(documentId)) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_COPIES.equals(target) && sourceData instanceof Loan) {
      Loan loan = (Loan) sourceData;
      for (Object row : stores.get(LibraryEdmProvider.ES_COPIES)) {
        Copy copy = (Copy) row;
        if (copy.getMediumId().equals(loan.getCopyMediumId())
            && copy.getInventoryNumber().equals(loan.getCopyInventoryNumber())) {
          result.add(row);
        }
      }
    } else if (LibraryEdmProvider.ES_BRANCHES.equals(target) && sourceData instanceof Copy) {
      Integer locationId = ((Copy) sourceData).getLocationId();
      for (Object row : stores.get(LibraryEdmProvider.ES_BRANCHES)) {
        if (((Branch) row).getId().equals(locationId)) {
          result.add(row);
        }
      }
    }
    return result;
  }

  // ------------------------------------------------------------------------------------------------
  // operations
  // ------------------------------------------------------------------------------------------------

  @Override
  public Object readData(final EdmFunctionImport function, final Map<String, Object> parameters,
      final Map<String, Object> keys) throws ODataNotFoundException, EdmException,
      ODataApplicationException {
    String name = function.getName();

    if ("TotalMediaCount".equals(name)) {
      long total = 0;
      for (String set : LibraryEdmProvider.MEDIA_SETS) {
        total += stores.get(set).size();
      }
      return total;
    }
    if ("AllLanguages".equals(name) || "AvailableLanguages".equals(name)) {
      List<String> languages = new ArrayList<String>();
      for (String set : LibraryEdmProvider.MEDIA_SETS) {
        for (Object row : stores.get(set)) {
          String language = ((Medium) row).getLanguage();
          if (language != null && !languages.contains(language)) {
            languages.add(language);
          }
        }
      }
      Collections.sort(languages);
      return languages;
    }
    if ("LoanStatistics".equals(name)) {
      return new LoanStats((long) stores.get(LibraryEdmProvider.ES_LOANS).size(), SeedData.duration(0, 0, 0));
    }
    if ("StatsPerBranch".equals(name)) {
      List<BranchStats> stats = new ArrayList<BranchStats>();
      for (Object row : stores.get(LibraryEdmProvider.ES_BRANCHES)) {
        Branch branch = (Branch) row;
        long count = 0;
        for (Object copyRow : stores.get(LibraryEdmProvider.ES_COPIES)) {
          if (branch.getId().equals(((Copy) copyRow).getLocationId())) {
            count++;
          }
        }
        stats.add(new BranchStats(branch.getId(), count));
      }
      return stats;
    }
    if ("MostReadMedium".equals(name)) {
      Book best = null;
      for (Object row : stores.get(LibraryEdmProvider.ES_BOOKS)) {
        Book book = (Book) row;
        // PopularityScore is nullable and a created entity need not carry it, so this must not unbox
        if (best == null || score(book) > score(best)) {
          best = book;
        }
      }
      return best;
    }
    if ("NewReleases".equals(name) || "RunStockCheck".equals(name)) {
      return new ArrayList<Object>(stores.get(LibraryEdmProvider.ES_BOOKS));
    }
    if ("Search".equals(name)) {
      String term = (String) parameters.get("Term");
      Integer max = asInt(parameters.get("MaxResults"));
      List<Object> hits = new ArrayList<Object>();
      for (Object row : stores.get(LibraryEdmProvider.ES_BOOKS)) {
        Book book = (Book) row;
        if (term == null || book.getTitle().toLowerCase().contains(term.toLowerCase())) {
          hits.add(book);
        }
      }
      return max != null && max < hits.size() ? new ArrayList<Object>(hits.subList(0, max)) : hits;
    }
    if ("OutstandingBalance".equals(name)) {
      Member member = memberById(asInt(parameters.get("MemberId")));
      return member.getBalance() == null ? BigDecimal.ZERO : member.getBalance();
    }
    if ("NoticeHistory".equals(name) || "RunReminders".equals(name)) {
      Integer memberId = asInt(parameters.get("MemberId"));
      List<OverdueNotice> notices = new ArrayList<OverdueNotice>();
      for (Object row : stores.get(LibraryEdmProvider.ES_LOANS)) {
        Loan loan = (Loan) row;
        if (memberId.equals(loan.getMemberId()) && loan.getReturnedAt() == null) {
          notices.add(new OverdueNotice("Loan " + loan.getId() + " is outstanding",
              loan.getLateFee() == null ? BigDecimal.ZERO : loan.getLateFee(), loan.getDueDate()));
        }
      }
      return notices;
    }
    if ("RunOverdueNotices".equals(name)) {
      List<OverdueNotice> notices = new ArrayList<OverdueNotice>();
      for (Object row : stores.get(LibraryEdmProvider.ES_LOANS)) {
        Loan loan = (Loan) row;
        if (loan.getReturnedAt() == null) {
          notices.add(new OverdueNotice("Loan " + loan.getId() + " overdue",
              loan.getLateFee() == null ? BigDecimal.ZERO : loan.getLateFee(), loan.getDueDate()));
        }
      }
      return notices;
    }
    if ("LoanMetrics".equals(name)) {
      UUID mediumId = (UUID) parameters.get("MediumId");
      long count = 0;
      for (Object row : stores.get(LibraryEdmProvider.ES_LOANS)) {
        if (mediumId.equals(((Loan) row).getCopyMediumId())) {
          count++;
        }
      }
      return new MediumStats(count, SeedData.duration(0, 0, 0));
    }
    if ("AvailableCopy".equals(name) || "AvailableCopies".equals(name)) {
      UUID mediumId = (UUID) parameters.get("MediumId");
      List<Object> available = new ArrayList<Object>();
      for (Object row : stores.get(LibraryEdmProvider.ES_COPIES)) {
        Copy copy = (Copy) row;
        if (copy.getMediumId().equals(mediumId) && Boolean.TRUE.equals(copy.getIsLoanable())) {
          available.add(copy);
        }
      }
      if ("AvailableCopies".equals(name)) {
        return available;
      }
      return available.isEmpty() ? null : available.get(0);
    }
    if ("ClosureDay".equals(name)) {
      return null;
    }
    if ("NextInventoryNumber".equals(name)) {
      return nextInventoryNumber++;
    }
    if ("CleanUpKeywords".equals(name) || "BulkRenew".equals(name)) {
      return new ArrayList<String>();
    }
    if ("YearEndClosing".equals(name)) {
      BigDecimal fees = BigDecimal.ZERO;
      for (Object row : stores.get(LibraryEdmProvider.ES_LOANS)) {
        BigDecimal fee = ((Loan) row).getLateFee();
        if (fee != null) {
          fees = fees.add(fee);
        }
      }
      return new AnnualReport(asInt(parameters.get("Year")),
          (long) stores.get(LibraryEdmProvider.ES_LOANS).size(), fees);
    }
    if ("CheckOut".equals(name)) {
      copyByKey((UUID) parameters.get("MediumId"), asInt(parameters.get("InventoryNumber")));
      return null;
    }
    if ("AssessCondition".equals(name)) {
      Copy copy = copyByKey((UUID) parameters.get("MediumId"), asInt(parameters.get("InventoryNumber")));
      Short before = copy.getCondition();
      copy.setCondition(asShort(parameters.get("NewCondition")));
      return new ConditionReport(before, copy.getCondition(), (String) parameters.get("Remark"));
    }
    if ("Reserve".equals(name)) {
      return stores.get(LibraryEdmProvider.ES_RESERVATIONS).size() + 1;
    }
    if ("Renew".equals(name)) {
      UUID loanId = (UUID) parameters.get("LoanId");
      for (Object row : stores.get(LibraryEdmProvider.ES_LOANS)) {
        Loan loan = (Loan) row;
        if (loan.getId().equals(loanId)) {
          Calendar due = (Calendar) loan.getDueDate().clone();
          due.add(Calendar.DAY_OF_MONTH, 28);
          loan.setDueDate(due);
          return loan;
        }
      }
      throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
    }
    if ("RenewAll".equals(name)) {
      Integer memberId = asInt(parameters.get("MemberId"));
      List<Object> renewed = new ArrayList<Object>();
      for (Object row : stores.get(LibraryEdmProvider.ES_LOANS)) {
        Loan loan = (Loan) row;
        if (memberId.equals(loan.getMemberId())) {
          Calendar due = (Calendar) loan.getDueDate().clone();
          due.add(Calendar.DAY_OF_MONTH, 28);
          loan.setDueDate(due);
          renewed.add(loan);
        }
      }
      return renewed;
    }
    throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
  }

  /**
   * Olingo types a function-import parameter from the literal it received, not from the declaration:
   * {@code MemberId=2} arrives as a {@code Byte}, {@code Year=2024} as a {@code Short}, and the same
   * parameter with a larger value arrives as an {@code Integer}. Every numeric parameter is therefore
   * read through these two, never cast.
   */
  private static Integer asInt(final Object value) {
    return value == null ? null : ((Number) value).intValue();
  }

  private static Short asShort(final Object value) {
    return value == null ? null : ((Number) value).shortValue();
  }

  /** Nullable in the model, so never unboxed: a medium without a score sorts last. */
  private static double score(final Book book) {
    return book.getPopularityScore() == null ? Double.NEGATIVE_INFINITY : book.getPopularityScore();
  }

  private Member memberById(final Integer id) throws ODataNotFoundException {
    for (Object row : stores.get(LibraryEdmProvider.ES_MEMBERS)) {
      if (((Member) row).getId().equals(id)) {
        return (Member) row;
      }
    }
    throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
  }

  private Copy copyByKey(final UUID mediumId, final Integer inventoryNumber)
      throws ODataNotFoundException {
    for (Object row : stores.get(LibraryEdmProvider.ES_COPIES)) {
      Copy copy = (Copy) row;
      if (copy.getMediumId().equals(mediumId) && copy.getInventoryNumber().equals(inventoryNumber)) {
        return copy;
      }
    }
    throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
  }

  // ------------------------------------------------------------------------------------------------
  // media resources
  // ------------------------------------------------------------------------------------------------

  @Override
  public BinaryData readBinaryData(final EdmEntitySet entitySet, final Object mediaLinkEntry)
      throws ODataNotFoundException, EdmException {
    if (mediaLinkEntry instanceof EBook) {
      EBook ebook = (EBook) mediaLinkEntry;
      return new BinaryData(ebook.getContent(), ebook.getContentType());
    }
    if (mediaLinkEntry instanceof AudiobookChapter) {
      AudiobookChapter chapter = (AudiobookChapter) mediaLinkEntry;
      return new BinaryData(chapter.getContent(), chapter.getContentType());
    }
    throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
  }

  @Override
  public void writeBinaryData(final EdmEntitySet entitySet, final Object mediaLinkEntry,
      final BinaryData binaryData) throws ODataNotFoundException, EdmException {
    if (mediaLinkEntry instanceof EBook) {
      ((EBook) mediaLinkEntry).setContent(binaryData.getData());
      ((EBook) mediaLinkEntry).setContentType(binaryData.getMimeType());
      return;
    }
    if (mediaLinkEntry instanceof AudiobookChapter) {
      ((AudiobookChapter) mediaLinkEntry).setContent(binaryData.getData());
      ((AudiobookChapter) mediaLinkEntry).setContentType(binaryData.getMimeType());
      return;
    }
    throw new ODataNotFoundException(ODataNotFoundException.ENTITY);
  }

  // ------------------------------------------------------------------------------------------------
  // writing
  // ------------------------------------------------------------------------------------------------

  @Override
  public Object newDataObject(final EdmEntitySet entitySet) throws EdmException,
      ODataApplicationException {
    String name = entitySet.getName();
    try {
      if (LibraryEdmProvider.ES_BOOKS.equals(name)) {
        return new Book();
      } else if (LibraryEdmProvider.ES_MAGAZINES.equals(name)) {
        return new Magazine();
      } else if (LibraryEdmProvider.ES_TRADE_JOURNALS.equals(name)) {
        return new TradeJournal();
      } else if (LibraryEdmProvider.ES_AUDIOBOOKS.equals(name)) {
        return new Audiobook();
      } else if (LibraryEdmProvider.ES_AUDIOBOOK_CHAPTERS.equals(name)) {
        return new AudiobookChapter();
      } else if (LibraryEdmProvider.ES_DVDS.equals(name)) {
        return new DVD();
      } else if (LibraryEdmProvider.ES_EBOOKS.equals(name)) {
        return new EBook();
      } else if (LibraryEdmProvider.ES_COPIES.equals(name)) {
        return new Copy();
      } else if (LibraryEdmProvider.ES_MEMBERS.equals(name)) {
        return new Member();
      } else if (LibraryEdmProvider.ES_LOANS.equals(name)) {
        return new Loan();
      } else if (LibraryEdmProvider.ES_RESERVATIONS.equals(name)) {
        return new Reservation();
      } else if (LibraryEdmProvider.ES_ID_DOCUMENTS.equals(name)) {
        return new IdDocument();
      } else if (LibraryEdmProvider.ES_BRANCHES.equals(name)) {
        return new Branch();
      } else if (LibraryEdmProvider.ES_PUBLISHERS.equals(name)) {
        return new Publisher();
      } else if (LibraryEdmProvider.ES_PUBLISHER_BRANCHES.equals(name)) {
        return new PublisherBranch();
      }
    } catch (RuntimeException e) {
      throw new ODataApplicationException("Cannot instantiate " + name, java.util.Locale.ENGLISH, e);
    }
    throw new ODataApplicationException("Unknown entity set " + name, java.util.Locale.ENGLISH);
  }

  @Override
  public void createData(final EdmEntitySet entitySet, final Object data) throws EdmException {
    // server-generated keys, so a client can create without inventing one
    if (data instanceof Medium && ((Medium) data).getId() == null) {
      ((Medium) data).setId(UUID.randomUUID());
    } else if (data instanceof Loan && ((Loan) data).getId() == null) {
      ((Loan) data).setId(UUID.randomUUID());
    } else if (data instanceof Reservation && ((Reservation) data).getId() == null) {
      ((Reservation) data).setId(UUID.randomUUID());
    } else if (data instanceof IdDocument && ((IdDocument) data).getId() == null) {
      ((IdDocument) data).setId(UUID.randomUUID());
    } else if (data instanceof Member && ((Member) data).getId() == null) {
      ((Member) data).setId(nextIntegerKey(LibraryEdmProvider.ES_MEMBERS));
    } else if (data instanceof Branch && ((Branch) data).getId() == null) {
      ((Branch) data).setId(nextIntegerKey(LibraryEdmProvider.ES_BRANCHES));
    }
    stores.get(entitySet.getName()).add(data);
  }

  private Integer nextIntegerKey(final String entitySet) {
    int max = 0;
    for (Object row : stores.get(entitySet)) {
      Integer id = row instanceof Member ? ((Member) row).getId() : ((Branch) row).getId();
      if (id != null && id > max) {
        max = id;
      }
    }
    return max + 1;
  }

  @Override
  public void deleteData(final EdmEntitySet entitySet, final Map<String, Object> keys)
      throws ODataNotFoundException, EdmException {
    Object row = readData(entitySet, keys);
    stores.get(entitySet.getName()).remove(row);
  }

  @Override
  public void writeRelation(final EdmEntitySet sourceEntitySet, final Object sourceData,
      final EdmEntitySet targetEntitySet, final Map<String, Object> targetKeys)
      throws ODataNotImplementedException {
    // $links write is a MAY in V1-V3 and is not implemented here: the relationships of this model are
    // all carried by foreign keys on the dependent entity, so a link write would have to reach into
    // the other side. Reading links works.
    throw new ODataNotImplementedException();
  }

  @Override
  public void deleteRelation(final EdmEntitySet sourceEntitySet, final Object sourceData,
      final EdmEntitySet targetEntitySet, final Map<String, Object> targetKeys)
      throws ODataNotImplementedException {
    throw new ODataNotImplementedException();
  }

  // ------------------------------------------------------------------------------------------------
  // key matching
  // ------------------------------------------------------------------------------------------------

  private boolean matchesKeys(final String entitySet, final Object row, final Map<String, Object> keys) {
    if (LibraryEdmProvider.ES_COPIES.equals(entitySet)) {
      Copy copy = (Copy) row;
      return copy.getMediumId().equals(keys.get("MediumId"))
          && copy.getInventoryNumber().equals(keys.get("InventoryNumber"));
    }
    Object key = keys.get("Id");
    if (row instanceof Medium) {
      return ((Medium) row).getId().equals(key);
    } else if (row instanceof AudiobookChapter) {
      return ((AudiobookChapter) row).getId().equals(key);
    } else if (row instanceof Member) {
      return ((Member) row).getId().equals(key);
    } else if (row instanceof Loan) {
      return ((Loan) row).getId().equals(key);
    } else if (row instanceof Reservation) {
      return ((Reservation) row).getId().equals(key);
    } else if (row instanceof IdDocument) {
      return ((IdDocument) row).getId().equals(key);
    } else if (row instanceof Branch) {
      return ((Branch) row).getId().equals(key);
    } else if (row instanceof Publisher) {
      return ((Publisher) row).getId().equals(key);
    } else if (row instanceof PublisherBranch) {
      return ((PublisherBranch) row).getId().equals(key);
    }
    return false;
  }
}
