package org.odata2ts.library;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.olingo.odata2.annotation.processor.core.ListsProcessor;
import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource;
import org.apache.olingo.odata2.annotation.processor.core.datasource.ValueAccess;
import org.apache.olingo.odata2.api.commons.HttpHeaders;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.EdmConcurrencyMode;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmFacets;
import org.apache.olingo.odata2.api.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.edm.EdmLiteral;
import org.apache.olingo.odata2.api.edm.EdmLiteralKind;
import org.apache.olingo.odata2.api.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeException;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataBadRequestException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataPreconditionFailedException;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetFunctionImportUriInfo;
import org.apache.olingo.odata2.api.uri.info.PutMergePatchUriInfo;

/**
 * {@code ListsProcessor} with three gaps closed: <strong>operations that return nothing</strong>,
 * <strong>optimistic concurrency that is actually enforced</strong>, and <strong>links stated in the
 * payload of an update</strong>.
 *
 * <p>V2 allows a service operation to return no value at all - [MS-ODATA] grades return values as "may
 * return nothing" - and the reference model uses it twice, for {@code ClosureDay} and {@code CheckOut}.
 * Olingo's processor cannot serve those: {@code executeFunctionImport} dereferences
 * {@code functionImport.getReturnType().getType()} before anything else, so a function import declared
 * without a return type fails with a {@code NullPointerException}, and even if it survived that, the
 * method answers {@code 404} whenever the data source returns {@code null}.
 *
 * <p>So a void operation is handled here and everything else is delegated.
 *
 * <h2>Optimistic concurrency</h2>
 *
 * Olingo checks a concurrency token for <em>presence</em> and never for <em>value</em>:
 * {@code ODataRequestHandler.checkConditions} refuses a modifying request that carries none of the
 * conditional headers with 428, and that is the whole implementation. Any {@code If-Match} at all is
 * then accepted, so a stale token succeeds, two clients overwrite each other while both believe they are
 * protected, and 412 is unreachable.
 *
 * <p>That is worse than not implementing it: a client that probes for 428 concludes the service supports
 * optimistic concurrency. So the token is compared here, using the same rule Olingo itself uses to
 * produce it - {@code AtomEntryEntityProducer.createETag}: every property whose facets say
 * {@code ConcurrencyMode="Fixed"}, rendered with {@code valueToString} and joined, wrapped in
 * {@code W/"..."}.
 *
 * <h2>Links in an update payload</h2>
 *
 * Olingo honours a reference sent along with a create and drops the same one sent along with an update,
 * without a word - see {@link #updateEntity}.
 */
public class LibraryProcessor extends ListsProcessor {

  private final DataSource dataSource;
  private final ValueAccess valueAccess;

  public LibraryProcessor(final DataSource dataSource, final ValueAccess valueAccess) {
    super(dataSource, valueAccess);
    this.dataSource = dataSource;
    this.valueAccess = valueAccess;
  }

  @Override
  public ODataResponse executeFunctionImport(final GetFunctionImportUriInfo uriInfo,
      final String contentType) throws ODataException {
    EdmFunctionImport functionImport = uriInfo.getFunctionImport();
    if (functionImport.getReturnType() != null) {
      return super.executeFunctionImport(uriInfo, contentType);
    }

    dataSource.readData(functionImport, parameters(uriInfo.getFunctionImportParameters()), null);
    return ODataResponse.status(HttpStatusCodes.NO_CONTENT).build();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Extended by the navigation properties of the payload, which Olingo drops on an update:
   * {@code updateEntity} parses the entry and then only calls {@code setStructuralTypeValuesFromMap},
   * so a reference sent along - {@code "Publisher": {"__metadata": {"uri": "Publishers(1)"}}} - is
   * silently ignored. On a create the very same payload works, because {@code createEntity} runs it
   * through {@code createInlinedEntities}. Re-pointing a link would therefore answer 204 and change
   * nothing, which is the worst of the three possible outcomes.
   *
   * <p>The body is read into memory so that it can be parsed a second time here, after the structural
   * update has gone through: an entity that fails to update must not have its links rewritten.
   */
  @Override
  public ODataResponse updateEntity(final PutMergePatchUriInfo uriInfo, final InputStream content,
      final String requestContentType, final boolean merge, final String contentType) throws ODataException {
    checkConcurrencyToken(uriInfo.getTargetEntitySet(), uriInfo.getKeyPredicates());

    byte[] body = readFully(content);
    ODataResponse response =
        super.updateEntity(uriInfo, new ByteArrayInputStream(body), requestContentType, merge, contentType);
    writeRelations(uriInfo, body, requestContentType, merge);
    return response;
  }

  /**
   * Links whatever the payload of an update references, by the same data source call Olingo makes for a
   * create or a {@code $links} write.
   *
   * <p>Only references are honoured, not a nested entity carrying data: creating or changing one along
   * the way is a deep insert or deep update, and this is neither.
   */
  private void writeRelations(final PutMergePatchUriInfo uriInfo, final byte[] body,
      final String requestContentType, final boolean merge) throws ODataException {
    EdmEntitySet entitySet = uriInfo.getTargetEntitySet();
    EdmEntityType entityType = entitySet.getEntityType();
    if (entityType.getNavigationPropertyNames().isEmpty()) {
      return;
    }

    ODataEntry entry = EntityProvider.readEntry(requestContentType, entitySet, new ByteArrayInputStream(body),
        EntityProviderReadProperties.init().mergeSemantic(merge).build());
    URI serviceRoot = getContext().getPathInfo().getServiceRoot();
    Object data = null;

    for (String navigationPropertyName : entityType.getNavigationPropertyNames()) {
      List<String> links = referencedUris(entry, navigationPropertyName);
      if (links.isEmpty()) {
        continue;
      }
      EdmNavigationProperty navigationProperty =
          (EdmNavigationProperty) entityType.getProperty(navigationPropertyName);
      EdmEntitySet targetEntitySet = entitySet.getRelatedEntitySet(navigationProperty);
      if (data == null) {
        // read once, and only when there is something to link
        data = dataSource.readData(entitySet, keyMap(uriInfo.getKeyPredicates()));
      }
      for (String link : links) {
        Map<String, Object> targetKeys =
            keyMap(UriParser.getKeyPredicatesFromEntityLink(targetEntitySet, link, serviceRoot));
        dataSource.writeRelation(entitySet, data, targetEntitySet, targetKeys);
      }
    }
  }

  /**
   * The entities a navigation property of the payload points at, in the two shapes a reference reaches
   * the parser in - the same two {@code createInlinedEntities} distinguishes.
   *
   * <p>A reference either sits in the parent's own metadata, which is where a deferred Atom link ends up,
   * or it arrives as a nested entry that carries nothing but a URI - which is what V2's JSON
   * {@code {"__metadata": {"uri": "Publishers(1)"}}} parses into. A nested entry with properties of its
   * own is left alone: it means to create or change an entity, not to point at one.
   */
  private static List<String> referencedUris(final ODataEntry entry, final String navigationPropertyName) {
    List<String> uris = new ArrayList<String>();

    List<String> associationUris = entry.getMetadata().getAssociationUris(navigationPropertyName);
    if (associationUris != null) {
      uris.addAll(associationUris);
    }

    Object inline = entry.getProperties().get(navigationPropertyName);
    if (inline instanceof ODataEntry) {
      addIfReference((ODataEntry) inline, uris);
    } else if (inline instanceof ODataFeed) {
      for (ODataEntry inlineEntry : ((ODataFeed) inline).getEntries()) {
        addIfReference(inlineEntry, uris);
      }
    }
    return uris;
  }

  private static void addIfReference(final ODataEntry entry, final List<String> uris) {
    if (entry.getProperties().isEmpty() && entry.getMetadata().getUri() != null) {
      uris.add(entry.getMetadata().getUri());
    }
  }

  private static byte[] readFully(final InputStream content) throws ODataException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    try {
      int read;
      while ((read = content.read(chunk)) != -1) {
        buffer.write(chunk, 0, read);
      }
    } catch (IOException e) {
      throw new ODataBadRequestException(ODataBadRequestException.COMMON, e);
    }
    return buffer.toByteArray();
  }

  @Override
  public ODataResponse deleteEntity(final DeleteUriInfo uriInfo, final String contentType)
      throws ODataException {
    checkConcurrencyToken(uriInfo.getTargetEntitySet(), uriInfo.getKeyPredicates());
    return super.deleteEntity(uriInfo, contentType);
  }

  /**
   * Answers 412 when the client's {@code If-Match} does not match the entity's current token.
   *
   * <p>Only reached once Olingo's own 428 check has passed, so the header is known to be present for an
   * entity type that has a token. {@code If-Match: *} matches anything, by definition.
   */
  private void checkConcurrencyToken(final EdmEntitySet entitySet, final List<KeyPredicate> keys)
      throws ODataException {
    String ifMatch = getContext().getRequestHeader(HttpHeaders.IF_MATCH);
    if (ifMatch == null || "*".equals(ifMatch.trim()) || entitySet == null) {
      return;
    }
    String current = currentETag(entitySet, keys);
    if (current == null) {
      return;
    }
    // a client may send several tokens; the request succeeds if any of them is current
    for (String candidate : ifMatch.split(",")) {
      if (current.equals(candidate.trim())) {
        return;
      }
    }
    throw new ODataPreconditionFailedException(ODataPreconditionFailedException.COMMON);
  }

  /**
   * Builds the entity's token exactly as Olingo builds the one it hands out, so the two can be compared:
   * every {@code ConcurrencyMode="Fixed"} property rendered with {@code valueToString}, joined by the EDM
   * delimiter, wrapped in {@code W/"..."}. Returns {@code null} when the type carries no token.
   */
  private String currentETag(final EdmEntitySet entitySet, final List<KeyPredicate> keys)
      throws ODataException {
    EdmEntityType entityType = entitySet.getEntityType();
    StringBuilder token = null;

    for (String propertyName : entityType.getPropertyNames()) {
      EdmProperty property = (EdmProperty) entityType.getProperty(propertyName);
      EdmFacets facets = property.getFacets();
      if (facets == null || facets.getConcurrencyMode() != EdmConcurrencyMode.Fixed) {
        continue;
      }
      if (token == null) {
        // the entity is only read once a token property has been found
        Object data = dataSource.readData(entitySet, keyMap(keys));
        token = new StringBuilder(renderToken(data, property, facets));
      } else {
        Object data = dataSource.readData(entitySet, keyMap(keys));
        token.append(".").append(renderToken(data, property, facets));
      }
    }
    return token == null ? null : "W/\"" + token + "\"";
  }

  private String renderToken(final Object data, final EdmProperty property, final EdmFacets facets)
      throws ODataException {
    Object value = valueAccess.getPropertyValue(data, property);
    return ((EdmSimpleType) property.getType()).valueToString(value, EdmLiteralKind.DEFAULT, facets);
  }

  private static Map<String, Object> keyMap(final List<KeyPredicate> keys) throws ODataException {
    Map<String, Object> asMap = new HashMap<String, Object>();
    for (KeyPredicate key : keys) {
      EdmProperty property = key.getProperty();
      EdmSimpleType type = (EdmSimpleType) property.getType();
      asMap.put(property.getName(),
          type.valueOfString(key.getLiteral(), EdmLiteralKind.DEFAULT, property.getFacets(),
              type.getDefaultType()));
    }
    return asMap;
  }

  /**
   * The same conversion {@code ListsProcessor} does privately.
   *
   * <p>Worth knowing what it does, because it is not what the declaration says: the value is converted
   * with the type Olingo <em>infers from the literal text</em>, not with the parameter's declared type.
   * {@code MemberId=2} therefore arrives as a {@code Byte} even though the parameter is declared
   * {@code Edm.Int32}, and the same call with a larger number arrives as an {@code Integer}. Handlers
   * have to normalise; see {@code LibraryDataSource.asInt}.
   */
  private static Map<String, Object> parameters(final Map<String, EdmLiteral> functionImportParameters)
      throws EdmSimpleTypeException {
    if (functionImportParameters == null) {
      return Collections.emptyMap();
    }
    Map<String, Object> converted = new HashMap<String, Object>();
    for (Map.Entry<String, EdmLiteral> parameter : functionImportParameters.entrySet()) {
      EdmLiteral literal = parameter.getValue();
      EdmSimpleType type = literal.getType();
      converted.put(parameter.getKey(),
          type.valueOfString(literal.getLiteral(), EdmLiteralKind.DEFAULT, null, type.getDefaultType()));
    }
    return converted;
  }
}
