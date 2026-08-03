package org.odata2ts.library.edm;

import static java.util.Arrays.asList;
import static org.odata2ts.library.edm.LibraryEdmProvider.ES_BOOKS;
import static org.odata2ts.library.edm.LibraryEdmProvider.ES_COPIES;
import static org.odata2ts.library.edm.LibraryEdmProvider.ES_LOANS;
import static org.odata2ts.library.edm.LibraryEdmProvider.NS_CATALOG;
import static org.odata2ts.library.edm.LibraryEdmProvider.NS_CIRCULATION;
import static org.odata2ts.library.edm.LibraryEdmProvider.action;
import static org.odata2ts.library.edm.LibraryEdmProvider.function;
import static org.odata2ts.library.edm.LibraryEdmProvider.many;
import static org.odata2ts.library.edm.LibraryEdmProvider.param;
import static org.odata2ts.library.edm.LibraryEdmProvider.single;

import java.util.List;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.provider.FunctionImport;

/**
 * The 26 service operations of the V2 model, declared as function imports.
 *
 * <p>V2 knows exactly one kind of server-defined operation. There is no binding, no composability and no
 * overloading, and parameters are primitive-only - so what the V4 model declares as a bound operation
 * appears here with the key of its receiver as an ordinary parameter, and {@code GET} versus {@code POST}
 * is the only thing left that separates a function from an action.
 *
 * <p>Two consequences of the table-per-leaf-class layout (see {@link LibraryEdmProvider}) reach this
 * file: the operations that return media in the reference model - {@code MostReadMedium},
 * {@code NewReleases}, {@code Search}, {@code RunStockCheck} - are typed on {@code Book} and bound to the
 * {@code Books} set here, because an entity-returning operation is serialized through its entity set and
 * would truncate a mixed result exactly as a {@code Media} set would.
 */
final class LibraryOperations {

  private LibraryOperations() {}

  static List<FunctionImport> functionImports() {
    return asList(
        // ---- functions (GET) ----------------------------------------------------------------------
        function("TotalMediaCount", single(EdmSimpleTypeKind.Int64), null),
        function("AllLanguages", many(EdmSimpleTypeKind.String), null),
        // the V4 model passes a complex `DateRange`; V2 parameters are primitive-only
        function("LoanStatistics", single(NS_CIRCULATION, "LoanStats"), null,
            param("From", EdmSimpleTypeKind.DateTime, false),
            param("To", EdmSimpleTypeKind.DateTime, false)),
        function("StatsPerBranch", many(NS_CIRCULATION, "BranchStats"), null),
        function("MostReadMedium", single(NS_CATALOG, "Book"), ES_BOOKS),
        function("NewReleases", many(NS_CATALOG, "Book"), ES_BOOKS),
        function("Search", many(NS_CATALOG, "Book"), ES_BOOKS,
            param("Term", EdmSimpleTypeKind.String, true),
            param("MaxResults", EdmSimpleTypeKind.Int32, false)),

        // what the V4 model declares bound: the receiver arrives as an ordinary key parameter
        function("OutstandingBalance", single(EdmSimpleTypeKind.Decimal), null,
            param("MemberId", EdmSimpleTypeKind.Int32, true)),
        function("NoticeHistory", many(NS_CIRCULATION, "OverdueNotice"), null,
            param("MemberId", EdmSimpleTypeKind.Int32, true)),
        function("LoanMetrics", single(NS_CATALOG, "MediumStats"), null,
            param("MediumId", EdmSimpleTypeKind.Guid, true)),
        function("AvailableLanguages", many(EdmSimpleTypeKind.String), null),
        function("AvailableCopy", single(NS_CIRCULATION, "Copy"), ES_COPIES,
            param("MediumId", EdmSimpleTypeKind.Guid, true)),
        function("AvailableCopies", many(NS_CIRCULATION, "Copy"), ES_COPIES,
            param("MediumId", EdmSimpleTypeKind.Guid, true)),

        // ---- actions (POST) -----------------------------------------------------------------------
        action("ClosureDay", null, null,
            param("Day", EdmSimpleTypeKind.DateTime, true)),
        action("NextInventoryNumber", single(EdmSimpleTypeKind.Int32), null),
        // the V4 model passes a collection here; collection parameters are 3.0
        action("CleanUpKeywords", many(EdmSimpleTypeKind.String), null),
        action("YearEndClosing", single(NS_CIRCULATION, "AnnualReport"), null,
            param("Year", EdmSimpleTypeKind.Int32, true)),
        action("RunOverdueNotices", many(NS_CIRCULATION, "OverdueNotice"), null),
        action("RunStockCheck", many(NS_CATALOG, "Book"), ES_BOOKS),
        action("CheckOut", null, null,
            param("MediumId", EdmSimpleTypeKind.Guid, true),
            param("InventoryNumber", EdmSimpleTypeKind.Int32, true),
            param("MemberId", EdmSimpleTypeKind.Int32, true)),
        action("AssessCondition", single(NS_CATALOG, "ConditionReport"), null,
            param("MediumId", EdmSimpleTypeKind.Guid, true),
            param("InventoryNumber", EdmSimpleTypeKind.Int32, true),
            param("NewCondition", EdmSimpleTypeKind.Byte, true),
            param("Remark", EdmSimpleTypeKind.String, false)),
        action("Reserve", single(EdmSimpleTypeKind.Int32), null,
            param("MediumId", EdmSimpleTypeKind.Guid, true),
            param("MemberId", EdmSimpleTypeKind.Int32, true)),
        action("RunReminders", many(NS_CIRCULATION, "OverdueNotice"), null,
            param("MemberId", EdmSimpleTypeKind.Int32, true)),
        action("Renew", single(NS_CIRCULATION, "Loan"), ES_LOANS,
            param("LoanId", EdmSimpleTypeKind.Guid, true)),
        action("RenewAll", many(NS_CIRCULATION, "Loan"), ES_LOANS,
            param("MemberId", EdmSimpleTypeKind.Int32, true)),
        action("BulkRenew", many(EdmSimpleTypeKind.String), null));
  }
}
