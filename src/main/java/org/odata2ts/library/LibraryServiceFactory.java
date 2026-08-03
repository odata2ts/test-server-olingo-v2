package org.odata2ts.library;


import org.apache.olingo.odata2.annotation.processor.core.datasource.BeanPropertyAccess;
import org.apache.olingo.odata2.annotation.processor.core.datasource.DataSource;
import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.odata2ts.library.data.LibraryDataSource;
import org.odata2ts.library.edm.LibraryEdmProvider;

/**
 * Assembles the service from the two halves this server is built on: a hand-written {@code EdmProvider}
 * for the model, and Olingo's {@code ListsProcessor} for everything the protocol does with it.
 *
 * <p>The data source is a singleton: it holds the state, so every request has to see the same one. The
 * EDM provider is stateless but built once for the same reason - there is no point re-deriving it.
 */
public class LibraryServiceFactory extends ODataServiceFactory {

  private static final LibraryEdmProvider EDM_PROVIDER = new LibraryEdmProvider();
  private static final DataSource DATA_SOURCE = new LibraryDataSource();

  @Override
  public ODataService createService(final ODataContext context) {
    ODataSingleProcessor processor = new LibraryProcessor(DATA_SOURCE, new BeanPropertyAccess());
    return createODataSingleProcessorService(EDM_PROVIDER, processor);
  }
}
