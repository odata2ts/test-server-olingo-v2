package org.odata2ts.library.edm;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.olingo.odata2.api.edm.EdmConcurrencyMode;
import org.apache.olingo.odata2.api.edm.EdmMultiplicity;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeKind;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.edm.provider.AnnotationAttribute;
import org.apache.olingo.odata2.api.edm.provider.Association;
import org.apache.olingo.odata2.api.edm.provider.AssociationEnd;
import org.apache.olingo.odata2.api.edm.provider.AssociationSet;
import org.apache.olingo.odata2.api.edm.provider.AssociationSetEnd;
import org.apache.olingo.odata2.api.edm.provider.ComplexProperty;
import org.apache.olingo.odata2.api.edm.provider.ComplexType;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.edm.provider.EntityContainer;
import org.apache.olingo.odata2.api.edm.provider.EntityContainerInfo;
import org.apache.olingo.odata2.api.edm.provider.EntitySet;
import org.apache.olingo.odata2.api.edm.provider.EntityType;
import org.apache.olingo.odata2.api.edm.provider.Facets;
import org.apache.olingo.odata2.api.edm.provider.FunctionImport;
import org.apache.olingo.odata2.api.edm.provider.FunctionImportParameter;
import org.apache.olingo.odata2.api.edm.provider.Key;
import org.apache.olingo.odata2.api.edm.provider.Mapping;
import org.apache.olingo.odata2.api.edm.provider.NavigationProperty;
import org.apache.olingo.odata2.api.edm.provider.OnDelete;
import org.apache.olingo.odata2.api.edm.provider.Property;
import org.apache.olingo.odata2.api.edm.provider.PropertyRef;
import org.apache.olingo.odata2.api.edm.provider.ReferentialConstraint;
import org.apache.olingo.odata2.api.edm.provider.ReferentialConstraintRole;
import org.apache.olingo.odata2.api.edm.provider.ReturnType;
import org.apache.olingo.odata2.api.edm.provider.Schema;
import org.apache.olingo.odata2.api.edm.provider.SimpleProperty;
import org.apache.olingo.odata2.api.exception.ODataException;

/**
 * The EDM of the "Library" V2 test model, written by hand.
 *
 * <p>Hand-written rather than derived, because the two processors Olingo ships that <em>would</em> derive
 * it cannot express what this model is mostly about. The annotation processor's {@code @EdmEntityType}
 * carries only a name and a namespace, so it has no way to say {@code BaseType} or {@code Abstract}; the
 * JPA processor derives the EDM from a JPA metamodel, which shapes the names and facets after the
 * persistence layer. Only {@code EdmProvider} gives full control - and it is the one place in Olingo 2
 * where inheritance is understood at all.
 *
 * <p>The model this mirrors is
 * <a href="https://github.com/odata2ts/test-reference-model/blob/main/model/library-v2.xml">library-v2.xml</a>.
 * It is reproduced faithfully, with exactly one deliberate deviation, described next.
 *
 * <h2>Why there is no {@code Media} entity set</h2>
 *
 * The reference model exposes the whole media hierarchy through a single entity set {@code Media}, typed
 * on the abstract {@code Medium}. Olingo 2 renders such a model into {@code $metadata} correctly -
 * {@code BaseType} and {@code Abstract} come out exactly as declared - but it <strong>cannot serialize
 * it</strong>: {@code EntityInfoAggregator} takes the type from the entity set, so every entry of a
 * {@code Media} set would be written as a bare {@code Library.Catalog.Medium}, with the derived
 * properties silently dropped and {@code __metadata.type} naming the wrong type. Nothing in the
 * {@code ep/producer} package ever consults {@code getBaseType()}.
 *
 * <p>Serving a truncated entity is worse than not serving it, so this service uses
 * <strong>table-per-leaf-class</strong>: one entity set per concrete type ({@code Books},
 * {@code Magazines}, {@code TradeJournals}, {@code Audiobooks}, {@code DVDs}, {@code EBooks}). Each of
 * those is typed on a concrete type, and Olingo then serializes the inherited properties correctly -
 * verified, not assumed. The type hierarchy itself is fully declared and does reach the client through
 * {@code $metadata}; only the addressing differs.
 *
 * <p>See FEATURE-COVERAGE.md for what that costs.
 */
public class LibraryEdmProvider extends EdmProvider {

  public static final String NS_CATALOG = "Library.Catalog";
  public static final String NS_CIRCULATION = "Library.Circulation";
  public static final String NS_REGISTRY = "PublisherRegistry";
  public static final String NS_SERVICE = "Library.Service";
  public static final String CONTAINER = "LibraryService";

  /** Microsoft's annotation namespace, the home of {@code StoreGeneratedPattern}. */
  public static final String NS_MS_ANNOTATION = "http://schemas.microsoft.com/ado/2009/02/edm/annotation";
  /** SAP Gateway's annotation namespace, the home of {@code creatable} / {@code updatable}. */
  public static final String NS_SAP = "http://www.sap.com/Protocols/SAPData";

  // entity sets
  public static final String ES_BOOKS = "Books";
  public static final String ES_MAGAZINES = "Magazines";
  public static final String ES_TRADE_JOURNALS = "TradeJournals";
  public static final String ES_AUDIOBOOKS = "Audiobooks";
  public static final String ES_AUDIOBOOK_CHAPTERS = "AudiobookChapters";
  public static final String ES_DVDS = "DVDs";
  public static final String ES_EBOOKS = "EBooks";
  public static final String ES_COPIES = "Copies";
  public static final String ES_MEMBERS = "Members";
  public static final String ES_LOANS = "Loans";
  public static final String ES_RESERVATIONS = "Reservations";
  public static final String ES_ID_DOCUMENTS = "IdDocuments";
  public static final String ES_BRANCHES = "Branches";
  public static final String ES_PUBLISHERS = "Publishers";
  public static final String ES_PUBLISHER_BRANCHES = "PublisherBranches";

  /** The concrete media sets, in declaration order - the hierarchy flattened for addressing. */
  public static final List<String> MEDIA_SETS =
      asList(ES_BOOKS, ES_MAGAZINES, ES_TRADE_JOURNALS, ES_AUDIOBOOKS, ES_DVDS, ES_EBOOKS);

  private final List<Schema> schemas;
  private final Map<String, EntityType> entityTypes = new LinkedHashMap<String, EntityType>();
  private final Map<String, ComplexType> complexTypes = new LinkedHashMap<String, ComplexType>();
  private final Map<String, Association> associations = new LinkedHashMap<String, Association>();
  private final Map<String, EntitySet> entitySets = new LinkedHashMap<String, EntitySet>();
  private final Map<String, AssociationSet> associationSets = new LinkedHashMap<String, AssociationSet>();
  private final Map<String, FunctionImport> functionImports = new LinkedHashMap<String, FunctionImport>();

  public LibraryEdmProvider() {
    schemas = asList(catalogSchema(), circulationSchema(), registrySchema(), serviceSchema());
    index();
  }

  // ------------------------------------------------------------------------------------------------
  // Library.Catalog - the media hierarchy and the shared complex types
  // ------------------------------------------------------------------------------------------------

  private Schema catalogSchema() {
    List<ComplexType> complex = asList(
        // abstract complex type plus a derived one: legal in CSDL 2.0, unlike in some later readings
        new ComplexType().setName("Address").setAbstract(true).setProperties(asList(
            simple("Street", EdmSimpleTypeKind.String, facets().setMaxLength(120)),
            simple("City", EdmSimpleTypeKind.String, facets().setMaxLength(80)))),
        new ComplexType().setName("PostalAddress").setBaseType(fqn(NS_CATALOG, "Address")).setProperties(asList(
            simple("PostalCode", EdmSimpleTypeKind.String, facets().setMaxLength(10)),
            simple("Country", EdmSimpleTypeKind.String, facets().setMaxLength(60)))),
        new ComplexType().setName("ConditionReport").setProperties(asList(
            simple("ConditionBefore", EdmSimpleTypeKind.Byte, null),
            simple("ConditionAfter", EdmSimpleTypeKind.Byte, null),
            simple("Remark", EdmSimpleTypeKind.String, null))),
        new ComplexType().setName("MediumStats").setProperties(asList(
            simple("TotalLoanCount", EdmSimpleTypeKind.Int64, null),
            // Edm.Duration is V4; V2's Edm.Time carries the same value
            simple("AverageLoanDuration", EdmSimpleTypeKind.Time, null))));

    List<EntityType> types = new ArrayList<EntityType>();

    types.add(new EntityType().setName("Medium").setAbstract(true)
        .setKey(key("Id"))
        .setProperties(asList(
            generatedKey(simple("Id", EdmSimpleTypeKind.Guid, facets().setNullable(false))),
            simple("Title", EdmSimpleTypeKind.String, facets().setNullable(false).setMaxLength(200)),
            simple("Language", EdmSimpleTypeKind.String, facets().setMaxLength(40)),
            simple("PublicationDate", EdmSimpleTypeKind.DateTime, null),
            computed(simple("PopularityScore", EdmSimpleTypeKind.Double, null))))
        .setNavigationProperties(singletonList(
            nav("Copies", fqn(NS_CIRCULATION, "Medium_Copies"), "Medium", "Copy"))));

    types.add(new EntityType().setName("PrintMedium").setAbstract(true)
        .setBaseType(fqn(NS_CATALOG, "Medium"))
        .setProperties(singletonList(
            simple("ISBN", EdmSimpleTypeKind.String, facets().setMaxLength(13)))));

    types.add(new EntityType().setName("Book").setBaseType(fqn(NS_CATALOG, "PrintMedium"))
        .setProperties(asList(
            simple("PageCount", EdmSimpleTypeKind.Int16, null),
            simple("AgeRating", EdmSimpleTypeKind.Byte, null)))
        .setNavigationProperties(singletonList(
            nav("Publisher", fqn(NS_CATALOG, "Book_Publisher"), "Book", "Publisher"))));

    types.add(new EntityType().setName("Magazine").setBaseType(fqn(NS_CATALOG, "PrintMedium"))
        .setProperties(singletonList(simple("IssueNumber", EdmSimpleTypeKind.Int32, null))));

    // the fourth level of the hierarchy
    types.add(new EntityType().setName("TradeJournal").setBaseType(fqn(NS_CATALOG, "Magazine"))
        .setProperties(singletonList(simple("Field", EdmSimpleTypeKind.String, null))));

    types.add(new EntityType().setName("AudioMedium").setAbstract(true)
        .setBaseType(fqn(NS_CATALOG, "Medium"))
        .setProperties(singletonList(simple("Duration", EdmSimpleTypeKind.Time, null))));

    types.add(new EntityType().setName("Audiobook").setBaseType(fqn(NS_CATALOG, "AudioMedium"))
        .setProperties(singletonList(simple("Narrator", EdmSimpleTypeKind.String, null)))
        .setNavigationProperties(singletonList(
            nav("Chapters", fqn(NS_CATALOG, "Audiobook_Chapters"), "Audiobook", "Chapter"))));

    // media link entry: the content is served from .../$value, its MIME type read off the instance
    types.add(new EntityType().setName("AudiobookChapter").setHasStream(true)
        .setKey(key("Id"))
        .setMapping(new Mapping().setMediaResourceMimeTypeKey("getContentType"))
        .setProperties(asList(
            generatedKey(simple("Id", EdmSimpleTypeKind.Int32, facets().setNullable(false))),
            simple("Title", EdmSimpleTypeKind.String, null)))
        .setNavigationProperties(singletonList(
            nav("Audiobook", fqn(NS_CATALOG, "Audiobook_Chapters"), "Chapter", "Audiobook"))));

    types.add(new EntityType().setName("DVD").setBaseType(fqn(NS_CATALOG, "AudioMedium"))
        .setProperties(singletonList(simple("RegionCode", EdmSimpleTypeKind.Byte, null))));

    // a media link entry *inside* the inheritance hierarchy - the combination is the point
    types.add(new EntityType().setName("EBook").setBaseType(fqn(NS_CATALOG, "Medium")).setHasStream(true)
        .setMapping(new Mapping().setMediaResourceMimeTypeKey("getContentType"))
        .setProperties(singletonList(
            simple("FileFormat", EdmSimpleTypeKind.String, facets().setMaxLength(20)))));

    List<Association> assocs = asList(
        // cross-namespace: Book is here, Publisher in PublisherRegistry
        new Association().setName("Book_Publisher")
            .setEnd1(end(NS_CATALOG, "Book", "Book", EdmMultiplicity.MANY))
            .setEnd2(end(NS_REGISTRY, "Publisher", "Publisher", EdmMultiplicity.ZERO_TO_ONE)),
        new Association().setName("Audiobook_Chapters")
            .setEnd1(end(NS_CATALOG, "Audiobook", "Audiobook", EdmMultiplicity.ONE))
            .setEnd2(end(NS_CATALOG, "AudiobookChapter", "Chapter", EdmMultiplicity.MANY)));

    return new Schema().setNamespace(NS_CATALOG)
        .setComplexTypes(complex).setEntityTypes(types).setAssociations(assocs);
  }

  // ------------------------------------------------------------------------------------------------
  // Library.Circulation
  // ------------------------------------------------------------------------------------------------

  private Schema circulationSchema() {
    List<ComplexType> complex = asList(
        new ComplexType().setName("OverdueNotice").setProperties(asList(
            simple("Reason", EdmSimpleTypeKind.String, null),
            simple("Amount", EdmSimpleTypeKind.Decimal, facets().setPrecision(5).setScale(2)),
            simple("CreatedAt", EdmSimpleTypeKind.DateTimeOffset, facets().setPrecision(7)))),
        new ComplexType().setName("LoanStats").setProperties(asList(
            simple("TotalLoans", EdmSimpleTypeKind.Int64, null),
            simple("AverageLoanDuration", EdmSimpleTypeKind.Time, null))),
        new ComplexType().setName("BranchStats").setProperties(asList(
            simple("BranchId", EdmSimpleTypeKind.Int32, null),
            simple("LoanCount", EdmSimpleTypeKind.Int64, null))),
        new ComplexType().setName("AnnualReport").setProperties(asList(
            simple("Year", EdmSimpleTypeKind.Int32, null),
            simple("TotalLoans", EdmSimpleTypeKind.Int64, null),
            simple("TotalLateFees", EdmSimpleTypeKind.Decimal, facets().setPrecision(12).setScale(2)))));

    List<EntityType> types = new ArrayList<EntityType>();

    types.add(new EntityType().setName("Member")
        .setKey(key("Id"))
        .setProperties(asList(
            generatedKey(simple("Id", EdmSimpleTypeKind.Int32, facets().setNullable(false))),
            simple("Name", EdmSimpleTypeKind.String, facets().setNullable(false).setMaxLength(100)),
            simple("DateOfBirth", EdmSimpleTypeKind.DateTime, null),
            complexProp("Address", fqn(NS_CATALOG, "PostalAddress")),
            simple("ActiveSince", EdmSimpleTypeKind.DateTimeOffset, facets().setPrecision(7)),
            simple("Balance", EdmSimpleTypeKind.Decimal, facets().setPrecision(9).setScale(2))))
        .setNavigationProperties(asList(
            nav("Loans", fqn(NS_CIRCULATION, "Member_Loans"), "Member", "Loan"),
            nav("Reservations", fqn(NS_CIRCULATION, "Member_Reservations"), "Member", "Reservation"),
            nav("IdDocument", fqn(NS_CIRCULATION, "Member_IdDocument"), "Member", "IdDocument"))));

    types.add(new EntityType().setName("Copy")
        .setKey(key("MediumId", "InventoryNumber"))
        .setProperties(asList(
            simple("MediumId", EdmSimpleTypeKind.Guid, facets().setNullable(false)),
            simple("InventoryNumber", EdmSimpleTypeKind.Int32, facets().setNullable(false)),
            // the concurrency token - a facet in V2, a vocabulary annotation in V4
            simple("Condition", EdmSimpleTypeKind.Byte,
                facets().setConcurrencyMode(EdmConcurrencyMode.Fixed)),
            simple("IsLoanable", EdmSimpleTypeKind.Boolean,
                facets().setNullable(false).setDefaultValue("true"))
                .setMapping(new Mapping().setInternalName("getIsLoanable")),
            simple("Status", EdmSimpleTypeKind.Byte, null),
            simple("AcquisitionDate", EdmSimpleTypeKind.DateTime, null),
            simple("WeightKg", EdmSimpleTypeKind.Single, null),
            // trailing underscore on purpose: it collides with the navigation property `Location`
            // under a client renaming strategy (odata2ts#142)
            simple("Location_", EdmSimpleTypeKind.String, facets().setMaxLength(10).setUnicode(false))
                .setMapping(new Mapping().setInternalName("getShelfCode"))))
        .setNavigationProperties(asList(
            nav("Medium", fqn(NS_CIRCULATION, "Medium_Copies"), "Copy", "Medium"),
            nav("Location", fqn(NS_CIRCULATION, "Copy_Location"), "Copy", "Branch"))));

    types.add(new EntityType().setName("Loan")
        .setKey(key("Id"))
        .setProperties(asList(
            generatedKey(simple("Id", EdmSimpleTypeKind.Guid, facets().setNullable(false))),
            immutable(simple("LoanedAt", EdmSimpleTypeKind.DateTimeOffset,
                facets().setNullable(false).setPrecision(7))),
            simple("DueDate", EdmSimpleTypeKind.DateTime, facets().setNullable(false)),
            // nullable on purpose: the explicit-null-vs-absent case
            simple("ReturnedAt", EdmSimpleTypeKind.DateTimeOffset, facets().setPrecision(7)),
            simple("LateFee", EdmSimpleTypeKind.Decimal, facets().setPrecision(5).setScale(2))))
        .setNavigationProperties(asList(
            nav("Member", fqn(NS_CIRCULATION, "Member_Loans"), "Loan", "Member"),
            nav("Copy", fqn(NS_CIRCULATION, "Loan_Copy"), "Loan", "Copy"))));

    types.add(new EntityType().setName("Reservation")
        .setKey(key("Id"))
        .setProperties(asList(
            generatedKey(simple("Id", EdmSimpleTypeKind.Guid, facets().setNullable(false))),
            simple("ReservedAt", EdmSimpleTypeKind.DateTimeOffset, facets().setPrecision(7)))));

    types.add(new EntityType().setName("IdDocument")
        .setKey(key("Id"))
        .setProperties(asList(
            generatedKey(simple("Id", EdmSimpleTypeKind.Guid, facets().setNullable(false))),
            simple("Scan", EdmSimpleTypeKind.Binary, null),
            simple("UploadedAt", EdmSimpleTypeKind.DateTimeOffset, facets().setPrecision(7)))));

    types.add(new EntityType().setName("Branch")
        .setKey(key("Id"))
        .setProperties(asList(
            simple("Id", EdmSimpleTypeKind.Int32, facets().setNullable(false)),
            simple("Name", EdmSimpleTypeKind.String, facets().setNullable(false).setMaxLength(100)),
            complexProp("Address", fqn(NS_CATALOG, "PostalAddress")),
            simple("LowestFloor", EdmSimpleTypeKind.SByte, null),
            simple("OpensAt", EdmSimpleTypeKind.Time, null),
            simple("ClosesAt", EdmSimpleTypeKind.Time, null),
            // a flags enum in V4; enumeration types are 3.0, so a plain bitmask here
            simple("Amenities", EdmSimpleTypeKind.Int32, null),
            simple("Population", EdmSimpleTypeKind.Int64, null))));

    List<Association> assocs = asList(
        new Association().setName("Medium_Copies")
            .setEnd1(end(NS_CATALOG, "Medium", "Medium", EdmMultiplicity.ONE))
            .setEnd2(end(NS_CIRCULATION, "Copy", "Copy", EdmMultiplicity.MANY))
            .setReferentialConstraint(new ReferentialConstraint()
                .setPrincipal(new ReferentialConstraintRole().setRole("Medium")
                    .setPropertyRefs(singletonList(new PropertyRef().setName("Id"))))
                .setDependent(new ReferentialConstraintRole().setRole("Copy")
                    .setPropertyRefs(singletonList(new PropertyRef().setName("MediumId"))))),
        new Association().setName("Member_Loans")
            .setEnd1(end(NS_CIRCULATION, "Member", "Member", EdmMultiplicity.ONE)
                .setOnDelete(new OnDelete().setAction(org.apache.olingo.odata2.api.edm.EdmAction.Cascade)))
            .setEnd2(end(NS_CIRCULATION, "Loan", "Loan", EdmMultiplicity.MANY)),
        new Association().setName("Member_Reservations")
            .setEnd1(end(NS_CIRCULATION, "Member", "Member", EdmMultiplicity.ZERO_TO_ONE))
            .setEnd2(end(NS_CIRCULATION, "Reservation", "Reservation", EdmMultiplicity.MANY)),
        new Association().setName("Member_IdDocument")
            .setEnd1(end(NS_CIRCULATION, "Member", "Member", EdmMultiplicity.ZERO_TO_ONE))
            .setEnd2(end(NS_CIRCULATION, "IdDocument", "IdDocument", EdmMultiplicity.ZERO_TO_ONE)),
        new Association().setName("Copy_Location")
            .setEnd1(end(NS_CIRCULATION, "Copy", "Copy", EdmMultiplicity.MANY))
            .setEnd2(end(NS_CIRCULATION, "Branch", "Branch", EdmMultiplicity.ZERO_TO_ONE)),
        new Association().setName("Loan_Copy")
            .setEnd1(end(NS_CIRCULATION, "Loan", "Loan", EdmMultiplicity.MANY))
            .setEnd2(end(NS_CIRCULATION, "Copy", "Copy", EdmMultiplicity.ONE)));

    return new Schema().setNamespace(NS_CIRCULATION)
        .setComplexTypes(complex).setEntityTypes(types).setAssociations(assocs);
  }

  // ------------------------------------------------------------------------------------------------
  // PublisherRegistry - a standalone namespace, deliberately reusing the type name `Branch`
  // ------------------------------------------------------------------------------------------------

  private Schema registrySchema() {
    List<EntityType> types = asList(
        new EntityType().setName("Publisher")
            .setKey(key("Id"))
            .setProperties(asList(
                generatedKey(simple("Id", EdmSimpleTypeKind.Int32, facets().setNullable(false))),
                simple("Name", EdmSimpleTypeKind.String, facets().setNullable(false).setMaxLength(100)),
                simple("Country", EdmSimpleTypeKind.String, facets().setMaxLength(60)),
                simple("Founded", EdmSimpleTypeKind.DateTime, null)))
            .setNavigationProperties(singletonList(
                nav("Books", fqn(NS_CATALOG, "Book_Publisher"), "Publisher", "Book"))),
        new EntityType().setName("Branch")
            .setKey(key("Id"))
            .setProperties(asList(
                generatedKey(simple("Id", EdmSimpleTypeKind.Int32, facets().setNullable(false))),
                simple("City", EdmSimpleTypeKind.String, facets().setMaxLength(80)),
                simple("Country", EdmSimpleTypeKind.String, facets().setMaxLength(60)))));

    return new Schema().setNamespace(NS_REGISTRY).setEntityTypes(types);
  }

  // ------------------------------------------------------------------------------------------------
  // Library.Service - the container
  // ------------------------------------------------------------------------------------------------

  private Schema serviceSchema() {
    List<EntitySet> sets = asList(
        // one set per concrete media type; see the class comment for why there is no `Media`
        set(ES_BOOKS, NS_CATALOG, "Book"),
        set(ES_MAGAZINES, NS_CATALOG, "Magazine"),
        set(ES_TRADE_JOURNALS, NS_CATALOG, "TradeJournal"),
        set(ES_AUDIOBOOKS, NS_CATALOG, "Audiobook"),
        set(ES_AUDIOBOOK_CHAPTERS, NS_CATALOG, "AudiobookChapter"),
        set(ES_DVDS, NS_CATALOG, "DVD"),
        set(ES_EBOOKS, NS_CATALOG, "EBook"),
        set(ES_COPIES, NS_CIRCULATION, "Copy"),
        set(ES_MEMBERS, NS_CIRCULATION, "Member"),
        set(ES_LOANS, NS_CIRCULATION, "Loan"),
        set(ES_RESERVATIONS, NS_CIRCULATION, "Reservation"),
        set(ES_ID_DOCUMENTS, NS_CIRCULATION, "IdDocument"),
        set(ES_BRANCHES, NS_CIRCULATION, "Branch"),
        set(ES_PUBLISHERS, NS_REGISTRY, "Publisher"),
        set(ES_PUBLISHER_BRANCHES, NS_REGISTRY, "Branch"));

    List<AssociationSet> assocSets = new ArrayList<AssociationSet>();
    // Medium_Copies and Book_Publisher are bound per concrete media set - the price of
    // table-per-leaf-class is that one association needs one association set per leaf
    for (String mediaSet : MEDIA_SETS) {
      assocSets.add(assocSet("Medium_Copies_" + mediaSet, fqn(NS_CIRCULATION, "Medium_Copies"),
          "Medium", mediaSet, "Copy", ES_COPIES));
    }
    assocSets.add(assocSet("Book_Publisher", fqn(NS_CATALOG, "Book_Publisher"),
        "Book", ES_BOOKS, "Publisher", ES_PUBLISHERS));
    assocSets.add(assocSet("Audiobook_Chapters", fqn(NS_CATALOG, "Audiobook_Chapters"),
        "Audiobook", ES_AUDIOBOOKS, "Chapter", ES_AUDIOBOOK_CHAPTERS));
    assocSets.add(assocSet("Member_Loans", fqn(NS_CIRCULATION, "Member_Loans"),
        "Member", ES_MEMBERS, "Loan", ES_LOANS));
    assocSets.add(assocSet("Member_Reservations", fqn(NS_CIRCULATION, "Member_Reservations"),
        "Member", ES_MEMBERS, "Reservation", ES_RESERVATIONS));
    assocSets.add(assocSet("Member_IdDocument", fqn(NS_CIRCULATION, "Member_IdDocument"),
        "Member", ES_MEMBERS, "IdDocument", ES_ID_DOCUMENTS));
    assocSets.add(assocSet("Copy_Location", fqn(NS_CIRCULATION, "Copy_Location"),
        "Copy", ES_COPIES, "Branch", ES_BRANCHES));
    assocSets.add(assocSet("Loan_Copy", fqn(NS_CIRCULATION, "Loan_Copy"),
        "Loan", ES_LOANS, "Copy", ES_COPIES));

    EntityContainer container = new EntityContainer().setName(CONTAINER).setDefaultEntityContainer(true)
        .setEntitySets(sets).setAssociationSets(assocSets)
        .setFunctionImports(LibraryOperations.functionImports());

    return new Schema().setNamespace(NS_SERVICE).setEntityContainers(singletonList(container));
  }

  // ------------------------------------------------------------------------------------------------
  // EdmProvider contract - everything is built eagerly and answered from the index
  // ------------------------------------------------------------------------------------------------

  private void index() {
    for (Schema schema : schemas) {
      if (schema.getEntityTypes() != null) {
        for (EntityType t : schema.getEntityTypes()) {
          entityTypes.put(schema.getNamespace() + "." + t.getName(), t);
        }
      }
      if (schema.getComplexTypes() != null) {
        for (ComplexType t : schema.getComplexTypes()) {
          complexTypes.put(schema.getNamespace() + "." + t.getName(), t);
        }
      }
      if (schema.getAssociations() != null) {
        for (Association a : schema.getAssociations()) {
          associations.put(schema.getNamespace() + "." + a.getName(), a);
        }
      }
      if (schema.getEntityContainers() != null) {
        for (EntityContainer c : schema.getEntityContainers()) {
          for (EntitySet s : c.getEntitySets()) {
            entitySets.put(s.getName(), s);
          }
          for (AssociationSet s : c.getAssociationSets()) {
            associationSets.put(s.getName(), s);
          }
          for (FunctionImport f : c.getFunctionImports()) {
            functionImports.put(f.getName(), f);
          }
        }
      }
    }
  }

  @Override
  public List<Schema> getSchemas() {
    return schemas;
  }

  @Override
  public EntityType getEntityType(final FullQualifiedName name) {
    return entityTypes.get(name.toString());
  }

  @Override
  public ComplexType getComplexType(final FullQualifiedName name) {
    return complexTypes.get(name.toString());
  }

  @Override
  public Association getAssociation(final FullQualifiedName name) {
    return associations.get(name.toString());
  }

  @Override
  public EntityContainerInfo getEntityContainerInfo(final String name) {
    return name == null || CONTAINER.equals(name)
        ? new EntityContainerInfo().setName(CONTAINER).setDefaultEntityContainer(true)
        : null;
  }

  @Override
  public EntitySet getEntitySet(final String container, final String name) {
    return CONTAINER.equals(container) ? entitySets.get(name) : null;
  }

  @Override
  public FunctionImport getFunctionImport(final String container, final String name) {
    return CONTAINER.equals(container) ? functionImports.get(name) : null;
  }

  @Override
  public AssociationSet getAssociationSet(final String container, final FullQualifiedName association,
      final String sourceEntitySetName, final String sourceEntitySetRole) throws ODataException {
    if (!CONTAINER.equals(container)) {
      return null;
    }
    for (AssociationSet candidate : associationSets.values()) {
      if (!candidate.getAssociation().equals(association)) {
        continue;
      }
      if (matches(candidate.getEnd1(), sourceEntitySetName, sourceEntitySetRole)
          || matches(candidate.getEnd2(), sourceEntitySetName, sourceEntitySetRole)) {
        return candidate;
      }
    }
    return null;
  }

  private boolean matches(final AssociationSetEnd end, final String entitySet, final String role) {
    return end.getEntitySet().equals(entitySet) && end.getRole().equals(role);
  }

  // ------------------------------------------------------------------------------------------------
  // builders
  // ------------------------------------------------------------------------------------------------

  static FullQualifiedName fqn(final String namespace, final String name) {
    return new FullQualifiedName(namespace, name);
  }

  private static Facets facets() {
    return new Facets();
  }

  private static Property simple(final String name, final EdmSimpleTypeKind type, final Facets facets) {
    SimpleProperty property = new SimpleProperty().setName(name).setType(type);
    return facets == null ? property : property.setFacets(facets);
  }

  /**
   * The V2 spelling of {@code Core.Computed}: the store owns the value and a client never supplies one.
   *
   * <p>Both era-typical dialects are stated, because they are what a V2 client actually meets in the
   * wild and they normalize onto the same V4 term - Microsoft's {@code StoreGeneratedPattern} from WCF
   * Data Services, and SAP Gateway's {@code creatable}/{@code updatable} pair, which only means anything
   * as a pair. They agree here; see library-v2.xml for the full mapping.
   */
  private static Property computed(final Property property) {
    return property.setAnnotationAttributes(asList(
        new AnnotationAttribute().setNamespace(NS_MS_ANNOTATION).setPrefix("annotation")
            .setName("StoreGeneratedPattern").setText("Computed"),
        new AnnotationAttribute().setNamespace(NS_SAP).setPrefix("sap")
            .setName("creatable").setText("false"),
        new AnnotationAttribute().setNamespace(NS_SAP).setPrefix("sap")
            .setName("updatable").setText("false")));
  }

  /**
   * The V2 spelling for a key the store hands out: generated on insert, and never a client's to supply.
   *
   * <p>{@code Identity} rather than {@code Computed}, which is the whole of the difference between the
   * two: {@code Identity} is generated when the row appears and stands still afterwards, {@code Computed}
   * is recomputed on every update. A key is the former. Both normalize onto {@code Core.Computed} for a
   * client, so nothing downstream can tell them apart - but a reference server should still say the true
   * one, and this is the only place either value is exercised.
   *
   * <p>The SAP pair says the same thing the only way it can, and says it for the clients that read that
   * dialect instead. Note what V2 cannot say at all: "the client may supply one, otherwise the server
   * does" - {@code Core.ComputedDefaultValue} has no V2 form, so a key which behaves that way must either
   * be overstated as generated or left silent. See library-v2-v3.md in the reference model.
   */
  private static Property generatedKey(final Property property) {
    return property.setAnnotationAttributes(asList(
        new AnnotationAttribute().setNamespace(NS_MS_ANNOTATION).setPrefix("annotation")
            .setName("StoreGeneratedPattern").setText("Identity"),
        new AnnotationAttribute().setNamespace(NS_SAP).setPrefix("sap")
            .setName("creatable").setText("false"),
        new AnnotationAttribute().setNamespace(NS_SAP).setPrefix("sap")
            .setName("updatable").setText("false")));
  }

  /**
   * The V2 spelling of {@code Core.Immutable}: settable on insert, fixed from then on.
   *
   * <p>Only SAP can say this. {@code StoreGeneratedPattern} has no value for it, because it describes
   * who <em>generates</em> a value rather than when a client may <em>send</em> one. {@code creatable} is
   * left at its default {@code true}, which is the whole of the distinction against {@link #computed}.
   */
  private static Property immutable(final Property property) {
    return property.setAnnotationAttributes(singletonList(
        new AnnotationAttribute().setNamespace(NS_SAP).setPrefix("sap")
            .setName("updatable").setText("false")));
  }

  private static Property complexProp(final String name, final FullQualifiedName type) {
    return new ComplexProperty().setName(name).setType(type);
  }

  private static Key key(final String... names) {
    List<PropertyRef> refs = new ArrayList<PropertyRef>();
    for (String name : names) {
      refs.add(new PropertyRef().setName(name));
    }
    return new Key().setKeys(refs);
  }

  private static NavigationProperty nav(final String name, final FullQualifiedName relationship,
      final String fromRole, final String toRole) {
    return new NavigationProperty().setName(name).setRelationship(relationship)
        .setFromRole(fromRole).setToRole(toRole);
  }

  private static AssociationEnd end(final String namespace, final String type, final String role,
      final EdmMultiplicity multiplicity) {
    return new AssociationEnd().setType(fqn(namespace, type)).setRole(role).setMultiplicity(multiplicity);
  }

  private static EntitySet set(final String name, final String namespace, final String type) {
    return new EntitySet().setName(name).setEntityType(fqn(namespace, type));
  }

  private static AssociationSet assocSet(final String name, final FullQualifiedName association,
      final String role1, final String set1, final String role2, final String set2) {
    return new AssociationSet().setName(name).setAssociation(association)
        .setEnd1(new AssociationSetEnd().setRole(role1).setEntitySet(set1))
        .setEnd2(new AssociationSetEnd().setRole(role2).setEntitySet(set2));
  }

  static FunctionImport function(final String name, final ReturnType returnType, final String entitySet,
      final FunctionImportParameter... parameters) {
    FunctionImport fi = new FunctionImport().setName(name).setHttpMethod("GET").setReturnType(returnType);
    if (entitySet != null) {
      fi.setEntitySet(entitySet);
    }
    return fi.setParameters(asList(parameters));
  }

  static FunctionImport action(final String name, final ReturnType returnType, final String entitySet,
      final FunctionImportParameter... parameters) {
    FunctionImport fi = new FunctionImport().setName(name).setHttpMethod("POST");
    if (returnType != null) {
      fi.setReturnType(returnType);
    }
    if (entitySet != null) {
      fi.setEntitySet(entitySet);
    }
    return fi.setParameters(asList(parameters));
  }

  static ReturnType single(final EdmSimpleTypeKind type) {
    return new ReturnType().setTypeName(type.getFullQualifiedName()).setMultiplicity(EdmMultiplicity.ONE);
  }

  static ReturnType many(final EdmSimpleTypeKind type) {
    return new ReturnType().setTypeName(type.getFullQualifiedName()).setMultiplicity(EdmMultiplicity.MANY);
  }

  static ReturnType single(final String namespace, final String name) {
    return new ReturnType().setTypeName(fqn(namespace, name)).setMultiplicity(EdmMultiplicity.ONE);
  }

  static ReturnType many(final String namespace, final String name) {
    return new ReturnType().setTypeName(fqn(namespace, name)).setMultiplicity(EdmMultiplicity.MANY);
  }

  static FunctionImportParameter param(final String name, final EdmSimpleTypeKind type,
      final boolean required) {
    return new FunctionImportParameter().setName(name).setType(type).setMode("In")
        .setFacets(new Facets().setNullable(!required));
  }
}
