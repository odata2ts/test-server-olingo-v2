package org.odata2ts.library;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Corrects the one thing Olingo gets wrong about this service's own version.
 *
 * <p>The runtime is OData 2.0 throughout: every response carries {@code DataServiceVersion: 2.0}, every
 * collection is wrapped in {@code results}, {@code $inlinecount} and {@code $select} work. But the
 * {@code $metadata} document declares <strong>{@code m:DataServiceVersion="1.0"}</strong>, and a
 * hand-written {@code EdmProvider} has no way to say otherwise: Olingo derives the value in
 * {@code EdmServiceMetadataImplProv.getDataServiceVersion()}, which returns 1.0 unless it happens to find
 * a property carrying {@code CustomizableFeedMappings} with {@code FcKeepInContent=false} - an Atom
 * feed-customisation flag with nothing to do with the protocol version. There is no setter, and the
 * {@code DataServices} object that would carry it is built internally.
 *
 * <p>Leaving it would make the service lie about itself: a client that reads the declaration and believes
 * it would stop expecting the {@code results} wrapper, {@code __count}, {@code $select} and
 * {@code $skiptoken}, all of which this service actually supports. So the attribute is corrected on the
 * way out, on the metadata document only, and nothing else is touched.
 *
 * <p>See FEATURE-COVERAGE.md - this is recorded there as an Olingo limitation, not hidden by the fix.
 */
public class DataServiceVersionFilter implements Filter {

  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final String DECLARED = "m:DataServiceVersion=\"1.0\"";
  private static final String CORRECTED = "m:DataServiceVersion=\"2.0\"";

  @Override
  public void init(final FilterConfig config) {
    // nothing to configure
  }

  @Override
  public void destroy() {
    // nothing to release
  }

  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, ServletException {
    if (!isMetadataRequest(request)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletResponse httpResponse = (HttpServletResponse) response;
    CapturingResponse captured = new CapturingResponse(httpResponse);
    chain.doFilter(request, captured);

    String body = new String(captured.body(), UTF8);
    int at = body.indexOf(DECLARED);
    byte[] corrected =
        (at < 0 ? body : body.substring(0, at) + CORRECTED + body.substring(at + DECLARED.length()))
            .getBytes(UTF8);

    httpResponse.setContentLength(corrected.length);
    httpResponse.getOutputStream().write(corrected);
  }

  private boolean isMetadataRequest(final ServletRequest request) {
    if (!(request instanceof HttpServletRequest)) {
      return false;
    }
    String path = ((HttpServletRequest) request).getRequestURI();
    return path != null && path.endsWith("/$metadata");
  }

  /** Buffers the response so the body can be rewritten before anything reaches the wire. */
  private static final class CapturingResponse extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private PrintWriter writer;

    CapturingResponse(final HttpServletResponse response) {
      super(response);
    }

    byte[] body() throws IOException {
      if (writer != null) {
        writer.flush();
      }
      return buffer.toByteArray();
    }

    @Override
    public ServletOutputStream getOutputStream() {
      return new ServletOutputStream() {
        @Override
        public void write(final int b) {
          buffer.write(b);
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setWriteListener(final WriteListener listener) {
          // synchronous only
        }
      };
    }

    @Override
    public PrintWriter getWriter() {
      if (writer == null) {
        writer = new PrintWriter(new java.io.OutputStreamWriter(buffer, UTF8));
      }
      return writer;
    }

    @Override
    public void setContentLength(final int length) {
      // the corrected body has its own length
    }
  }
}
