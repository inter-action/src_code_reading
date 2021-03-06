package iweb2.ch2.webcrawler.parser.html;

import iweb2.ch2.webcrawler.model.FetchedDocument;
import iweb2.ch2.webcrawler.model.Outlink;
import iweb2.ch2.webcrawler.model.ProcessedDocument;
import iweb2.ch2.webcrawler.parser.common.DocumentParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.cyberneko.html.filters.ElementRemover;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @reference
 *     http://nekohtml.sourceforge.net/
 *     sitemap: http://zh.wikipedia.org/zh/Robots.txt
 */


/**
 * Parser for HTML documents.
 */
public class HTMLDocumentParser implements DocumentParser {

    public ProcessedDocument parse(Reader reader)
        throws HTMLDocumentParserException {

        ProcessedDocument htmlDoc = new ProcessedDocument();
        htmlDoc.setDocumentType(ProcessedDocument.DOCUMENT_TYPE_HTML);
        htmlDoc.setDocumentId(null);
        htmlDoc.setDocumentURL(null);
        InputSource inputSource = new InputSource();
        inputSource.setCharacterStream(reader);
        parseHTML(htmlDoc, inputSource);
        return htmlDoc;
    }

    // 入口方法
    public ProcessedDocument parse(FetchedDocument doc) throws HTMLDocumentParserException {

        ProcessedDocument htmlDoc = new ProcessedDocument();
        htmlDoc.setDocumentType(ProcessedDocument.DOCUMENT_TYPE_HTML);
        htmlDoc.setDocumentId(doc.getDocumentId());
        htmlDoc.setDocumentURL(doc.getDocumentURL());
        String documentCharset = doc.getContentCharset();
        InputStream contentBytes = new ByteArrayInputStream(doc.getDocumentContent());
        try {
            /*
             * Up to this point document content was treated as byte array.
             * Here we convert byte array into character based stream.
             * Processed document will be stored using UTF-8 encoding.
             */
            InputStreamReader characterStream = new InputStreamReader(contentBytes, documentCharset);
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(characterStream);
            parseHTML(htmlDoc, inputSource);

        } catch(UnsupportedEncodingException e) {
            throw new HTMLDocumentParserException("Document parsing error: ", e);
        }
        return htmlDoc;
    }

    private void parseHTML(ProcessedDocument htmlDoc, InputSource inputSource) throws HTMLDocumentParserException {
        // [NekoHTML parser](http://nekohtml.sourceforge.net/)
        DOMParser parser = new DOMParser();

        // Create filter to remove elements that we don't care about.
        ElementRemover remover = new ElementRemover();
        // keep only a subset of elements (text and links)
        remover.acceptElement("html", null);
        remover.acceptElement("meta", new String[] { "name", "content" });
        remover.acceptElement("title", null);
        remover.acceptElement("body", null);
        remover.acceptElement("base", new String[] { "href" });
        remover.acceptElement("b", null);
        remover.acceptElement("i", null);
        remover.acceptElement("u", null);
        remover.acceptElement("p", null);
        remover.acceptElement("br", null);
        remover.acceptElement("a", new String[] { "href", "rel" });
        // completely remove these elements
        remover.removeElement("script");
        remover.removeElement("style");

        StringWriter sw = new StringWriter();
        XMLDocumentFilter writer = new HTMLWriter(sw, "UTF-8");

        XMLDocumentFilter[] filters = { remover, writer };
        try {
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        } catch (SAXException e) {
            throw new HTMLDocumentParserException("Property is not supported", e);
        }

        try {
            parser.parse(inputSource);
        } catch (SAXException e) {
            throw new HTMLDocumentParserException("Parsing error: ", e);
        } catch (IOException e) {
            throw new HTMLDocumentParserException("Parsing error: ", e);
        }

        // cleaned up html.
        String cleanHTML = cleanText(sw.toString());
        htmlDoc.setContent(cleanHTML);

        // just the text
        Node node = parser.getDocument();
        String text = cleanText(getText(node));
        htmlDoc.setText(text);

        // content of <title/>
        String title = getTitle(node);
        htmlDoc.setDocumentTitle(title);

        if( htmlDoc.getDocumentURL() != null ) {
            String baseUrl = getBaseUrl(node);

            // links to other pages
            List<Outlink> outlinks = extractLinks(node, htmlDoc.getDocumentURL(), baseUrl);
            htmlDoc.setOutlinks(outlinks);
        }
    }

    //移除多余的换行和空格
    private String cleanText(String text) {
        if( text == null ) {
            return null;
        }
        String t = text.replaceAll("[ \t]+", " ");
        t = t.replaceAll("[ \t][\r\n]", "\n");
        t = t.replaceAll("[\r\n]+", "\n");
        return t;
    }

    //处理带 base 标签的页面
    private String getBaseUrl(Node node) {
        if( node == null ) {
            return null;
        }
        org.w3c.dom.Document doc = getDocumentNode(node);
        NodeList nodeList = doc.getElementsByTagName("base");
        Node baseNode = nodeList.item(0);
        if( baseNode != null ) {
            NamedNodeMap attrs = baseNode.getAttributes();
            if( attrs != null ) {
                Node href = attrs.getNamedItem("href");
                if( href != null ) {
                    return href.getNodeValue();
                }
            }
        }
        return null;
    }


    //获取标题
    private String getTitle(Node node) {
        if( node == null ) {
            return "";
        }

        String cleanTitle = null;
        org.w3c.dom.Document doc = getDocumentNode(node);
        NodeList nodeList = doc.getElementsByTagName("title");
        Node matchedNode = nodeList.item(0);
        if( matchedNode != null ) {
            String title = matchedNode.getTextContent();
            if( title != null ) {
                cleanTitle = title.replaceAll("[\r\n\t]", " ").trim();
            }
        }

        return cleanTitle;
    }


    //返回robots信息
    private String getRobotsMeta(Node node) {
        if( node == null ) {
            return null;
        }
        org.w3c.dom.Document doc = getDocumentNode(node);
        NodeList nodeList = doc.getElementsByTagName("meta");
        if( nodeList != null ) {
            for(int i = 0, n = nodeList.getLength(); i < n; i++) {
                Node currentNode = nodeList.item(i);
                NamedNodeMap attrs = currentNode.getAttributes();
                if( attrs != null ) {
                    Node contentNode = attrs.getNamedItem("content");
                    Node nameNode = attrs.getNamedItem("name");
                    if(  nameNode != null && contentNode != null) {
                        if("ROBOTS".equalsIgnoreCase(nameNode.getNodeValue())) {
                            if( contentNode != null ) {
                                return contentNode.getNodeValue();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    // 返回某个节点的 text 信息
    private String getText(Node node) {
        if( node == null ) {
            return "";
        }

        org.w3c.dom.Document doc = getDocumentNode(node);
        org.w3c.dom.traversal.DocumentTraversal traversable = (DocumentTraversal) doc;
        int whatToShow = NodeFilter.SHOW_TEXT;
        NodeIterator nodeIterator = traversable.createNodeIterator(node, whatToShow, null, true);

        StringBuffer text = new StringBuffer();
        Node currentNode = null;
        while( (currentNode = nodeIterator.nextNode()) != null ) {
            text.append(currentNode.getNodeValue());
        }
        return text.toString();
    }

    //返回 Document 节点
    private org.w3c.dom.Document getDocumentNode(Node node) {
        if( node == null ) {
            return null;
        }

        if( Node.DOCUMENT_NODE == node.getNodeType() ) {
            return (org.w3c.dom.Document) node;
        }
        else {
            return node.getOwnerDocument();
        }
    }

    //根据 Robots 协议判断是否页面可爬
    private boolean isNoFollowForDocument(Node node) {
        boolean noFollow = false;

        // Check <META name="robots" content="..."/>
        String robotsMeta = getRobotsMeta(node);
        if( robotsMeta != null && robotsMeta.toLowerCase().indexOf("nofollow") > -1 ) {
            noFollow = true;
        }

        return noFollow;
    }

    // extract all the links under certain node.
    private List<Outlink> extractLinks(Node node, String docUrl, String baseUrl) {
        if( isNoFollowForDocument(node) ) {
            return new ArrayList<Outlink>();
        }

        org.w3c.dom.Document doc = getDocumentNode(node);
        DocumentTraversal traversableDoc = (DocumentTraversal) doc;
        NodeFilter linkFilter = getLinkNodeFilter();
        NodeIterator iterator = traversableDoc.createNodeIterator(node,
                NodeFilter.SHOW_ELEMENT,
                linkFilter,
                true);
        Node currentNode = null;

        List<Outlink> outlinks = new ArrayList<Outlink>();

        while( (currentNode = iterator.nextNode()) != null ) {//遍历可见的带href属性的a标签
            String href = currentNode.getAttributes().getNamedItem("href").getNodeValue();
            boolean nofollow = isNoFollowPresent(currentNode);
            if( nofollow == false ) {
                //todo: base should not appear in here?
                if( "BASE".equalsIgnoreCase(node.getNodeName()) ) {
                    // ignore this link
                }
                else {
                    String url = buildUrl(href, baseUrl, docUrl);
                    if( url != null ) {
                        String anchorText = getAnchorText(currentNode);
                        Outlink link = new Outlink(url, anchorText);
                        outlinks.add(link);
                    }
                }
            }
        }

        return outlinks;
    }

    /*
     * Extracts url protocol if present. Handles two cases:
     *
     * 1. "<protocol>://<host>"
     * 2. "mailto:<email address>"
     */
    private String extractProtocol(String url) {
        String protocol = null;
        if( url.startsWith("mailto:") ) {
            protocol = "mailto";
        }
        else {
            int i = url.indexOf("://");
            if( i > -1) {
                protocol = url.substring(0, i);
            }
        }
        return protocol;
    }

    //获取相对路径url的文档的父节点路径
    private String extractParent(String url) {
        String parent = url;
        int i = url.lastIndexOf("/");
        if( i > -1) {
            parent = url.substring(0, i + "/".length() );
        }
        return parent;
    }


    /*
     * @params
     *     href : attribute value in `a` link
     *     baseUrl: attribute value specified in `<base>` tag
     *     documentUrl: <todo>
     * Builds absolute URL. For relative URLs will use source document URL and
     * base URL.
     */
    private String buildUrl(String href, String baseUrl, String documentUrl) {

        String url = null;

        String protocol = extractProtocol(href);

        if( protocol != null ) {//有protocol
            url = href;

        } else if( baseUrl != null ) {//拼接baseUrl
            url = baseUrl + href;

        } else if( href.startsWith("/") ) {//绝对路径
            try {
                URL docUrl = new URL(documentUrl);
                if( docUrl.getPort() == -1 ) {//无端口
                    url = docUrl.getProtocol() + "://" + docUrl.getHost() + href;

                } else {//有端口
                    url = docUrl.getProtocol() + "://" + docUrl.getHost() + ":" + docUrl.getPort() + href;
                }

            } catch(MalformedURLException e) {
                url = null;
            }
        } else {//相对路径
            url = extractParent(documentUrl) + href;
        }

        return url;
    }

    //返回a标签内的文本内容
    private String getAnchorText(Node currentNode) {
        String text = getText(currentNode);
        String cleanText = null;
        if( text != null ) {
            cleanText = text.replaceAll("[\r\n\t]", " ").trim();
        }
        return cleanText;
    }

    /*
     * Checks for presense of rel="nofollow" attribute.
     */
    private boolean isNoFollowPresent(Node currentNode) {
        Node relAttr = currentNode.getAttributes().getNamedItem("rel");
        boolean nofollow = false;
        if( relAttr != null ) {
            String relAttrValue = relAttr.getNodeValue();
            if( "nofollow".equalsIgnoreCase(relAttrValue)) {
                nofollow = true;
            }
        }
        return nofollow;
    }

    //返回接受带 href属性的a标签 的Filter
    private NodeFilter getLinkNodeFilter() {
        CompositeFilter linkFilter = new CompositeFilter();
        // For now doing the simplest thing possible - only consider <A> elements
        linkFilter.addAcceptFilter(new ElementNodeFilter("a", "href"));
        /*
           Other elements to consider:

           linkFilter.addAcceptFilter(new LinkNodeFilter("frame", "src"));
           linkFilter.addAcceptFilter(new LinkNodeFilter("link", "href"));
         */
        return linkFilter;
    }
}
