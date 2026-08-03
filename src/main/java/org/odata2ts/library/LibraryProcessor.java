package org.odata2ts.library;

import java.io.InputStream;
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
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataPreconditionFailedException;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo;
import org.apache.olingo.odata2.api.uri.info.GetFunctionImportUriInfo;
import org.apache.olingo.odata2.api.uri.info.PutMergePatchUriInfo;

/**
 * {@code ListsProcessor} with two gaps closed: <strong>operations that return nothing</strong>, and
 * <strong>optimistic concurrency that is actually enforced</strong>.
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

  @Override
  public ODataResponse updateEntity(final PutMergePatchUriInfo uriInfo, final InputStream content,
      final String requestContentType, final boolean merge, final String contentType) throws ODataException {
    checkConcurrencyToken(uriInfo.getTargetEntitySet(), uriInfo.getKeyPredicates());
    return super.updateEntity(uriInfo, content, requestContentType, merge, contentType);
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
