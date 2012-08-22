/**
 *  search
 *  Copyright 2012 by Michael Peter Christen, mc@yacy.net, Frankfurt am Main, Germany
 *  First released 14.08.2012 at http://yacy.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import net.yacy.cora.document.UTF8;
import net.yacy.cora.protocol.HeaderFramework;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.services.federated.solr.GSAResponseWriter;
import net.yacy.kelondro.logging.Log;
import net.yacy.search.Switchboard;
import net.yacy.search.query.SnippetProcess;
import net.yacy.search.solr.EmbeddedSolrConnector;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.FastWriter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;

// try
// http://localhost:8090/gsa/searchresult?q=chicken+teriyaki&output=xml&client=test&site=test&sort=date:D:S:d1

/**
 * This is a gsa result formatter for solr search results.
 * The result format is implemented according to
 * https://developers.google.com/search-appliance/documentation/68/xml_reference#results_xml
 */
public class searchresult {

    private final static GSAResponseWriter responseWriter = new GSAResponseWriter();

    /**
     * get the right mime type for this streamed result page
     * @param header
     * @param post
     * @param env
     * @return
     */
    public static String mime(final RequestHeader header, final serverObjects post, final serverSwitch env) {
        return "text/xml";
    }


    /**
     * @param header
     * @param post
     * @param env
     * @param out
     * @return
     */
    public static serverObjects respond(final RequestHeader header, final serverObjects post, final serverSwitch env, final OutputStream out) {

        // this uses the methods in the jetty servlet environment and can be removed if jetty in implemented
        Switchboard sb = (Switchboard) env;

        // remember the peer contact for peer statistics
        final String clientip = header.get(HeaderFramework.CONNECTION_PROP_CLIENTIP, "<unknown>"); // read an artificial header addendum
        final String userAgent = header.get(HeaderFramework.USER_AGENT, "<unknown>");
        sb.peers.peerActions.setUserAgent(clientip, userAgent);

        // check if user is allowed to search (can be switched in /ConfigPortal.html)
        boolean authenticated = sb.adminAuthenticated(header) >= 2;
        final boolean searchAllowed = authenticated || sb.getConfigBool("publicSearchpage", true);
        if (!searchAllowed) return null;

        // check post
        if (post == null) return null;
        sb.intermissionAllThreads(3000); // tell all threads to do nothing for a specific time

        // rename post fields according to result style
        //post.put(CommonParams.Q, post.remove("q")); // same as solr
        //post.put(CommonParams.START, post.remove("start")); // same as solr
        //post.put(, post.remove("site"));//required, example: col1|col2
        //post.put(, post.remove("client"));//required, example: myfrontend
        //post.put(, post.remove("output"));//required, example: xml,xml_no_dtd
        post.put(CommonParams.ROWS, post.remove("num"));
        post.put(CommonParams.ROWS, Math.min(post.getInt("num", 10), (authenticated) ? 5000 : 100));
        post.remove("num");
        post.put("hl", "true");
        post.put("hl.fl", "text_t,h1,h2");
        post.put("hl.simple.pre", "<b>");
        post.put("hl.simple.post", "</b>");
        post.put("hl.fragsize", Integer.toString(SnippetProcess.SNIPPET_MAX_LENGTH));
        GSAResponseWriter.Sort sort = new GSAResponseWriter.Sort(post.get(CommonParams.SORT, ""));
        String sorts = sort.toSolr();
        if (sorts == null) {
            post.remove(CommonParams.SORT);
        } else {
            post.put(CommonParams.SORT, sorts);
        }
        String site = post.remove("site");
        String access = post.remove("access");
        String entqr = post.remove("entqr");

        // get the embedded connector
        EmbeddedSolrConnector connector = (EmbeddedSolrConnector) sb.index.fulltext().getLocalSolr();
        if (connector == null) return null;

        // do the solr request
        SolrQueryRequest req = connector.request(post.toSolrParams());
        SolrQueryResponse response = null;
        Exception e = null;
        try {response = connector.query(req);} catch (SolrException ee) {e = ee;}
        if (response != null) e = response.getException();
        if (e != null) {
            Log.logException(e);
            return null;
        }

        // set some context for the writer
        Map<Object,Object> context = req.getContext();
        context.put("ip", header.get("CLIENTIP", ""));
        context.put("client", header.get("User-Agent", ""));
        context.put("sort", sort.sort);
        context.put("site", site == null ? "" : site);
        context.put("access", access == null ? "p" : access);
        context.put("entqr", entqr == null ? "3" : entqr);

        // write the result directly to the output stream
        Writer ow = new FastWriter(new OutputStreamWriter(out, UTF8.charset));
        try {
            responseWriter.write(ow, req, response);
            ow.flush();
        } catch (IOException e1) {
        } finally {
            req.close();
            try {ow.close();} catch (IOException e1) {}
        }

        return null;
    }
}