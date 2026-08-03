package org.odata2ts.library;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.olingo.odata2.annotation.processor.core.ListsProcessor;
import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource;
import org.apache.olingo.odata2.annotation.processor.core.datasource.ValueAccess;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.EdmFunctionImport;
import org.apache.olingo.odata2.api.edm.EdmLiteral;
import org.apache.olingo.odata2.api.edm.EdmLiteralKind;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.info.GetFunctionImportUriInfo;

/**
 * {@code ListsProcessor} with one gap closed: <strong>operations that return nothing</strong>.
 *
 * <p>V2 allows a service operation to return no value at all - [MS-ODATA] grades return values as "may
 * return nothing" - and the reference model uses it twice, for {@code ClosureDay} and {@code CheckOut}.
 * Olingo's processor cannot serve those: {@code executeFunctionImport} dereferences
 * {@code functionImport.getReturnType().getType()} before anything else, so a function import declared
 * without a return type fails with a {@code NullPointerException}, and even if it survived that, the
 * method answers {@code 404} whenever the data source returns {@code null}.
 *
 * <p>So a void operation is handled here and everything else is delegated. This is the only place in
 * this server where Olingo's processor had to be extended rather than used.
 */
public class LibraryProcessor extends ListsProcessor {

  private final DataSource dataSource;

  public LibraryProcessor(final DataSource dataSource, final ValueAccess valueAccess) {
    super(dataSource, valueAccess);
    this.dataSource = dataSource;
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
