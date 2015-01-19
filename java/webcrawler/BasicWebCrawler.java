package iweb2.ch2.webcrawler;

import iweb2.ch2.webcrawler.db.FetchedDocsDB;
import iweb2.ch2.webcrawler.db.KnownUrlDB;
import iweb2.ch2.webcrawler.db.ProcessedDocsDB;
import iweb2.ch2.webcrawler.model.FetchedDocument;
import iweb2.ch2.webcrawler.model.KnownUrlEntry;
import iweb2.ch2.webcrawler.model.Outlink;
import iweb2.ch2.webcrawler.model.ProcessedDocument;
import iweb2.ch2.webcrawler.parser.common.DocumentParser;
import iweb2.ch2.webcrawler.parser.common.DocumentParserFactory;
import iweb2.ch2.webcrawler.transport.common.Transport;
import iweb2.ch2.webcrawler.transport.file.FileTransport;
import iweb2.ch2.webcrawler.transport.http.HTTPTransport;
import iweb2.ch2.webcrawler.utils.DocumentIdUtils;
import iweb2.ch2.webcrawler.utils.UrlGroup;
import iweb2.ch2.webcrawler.utils.UrlUtils;

import java.util.List;




/**
 * 爬虫的核心类
 *
 * - 链接处理的状态
 *       未处理,已处理
 * - 页面处理的状态
 *     fetched(拉取过), processed(处理过保存的文档, 包括解析文档成 ProcessedDocument),
 *     processLinks(处理链接,这部分是将 ProcessedDocument 中的外链添加到处理队列中, 之后保存链接关系信息)
 *
 * - 网页爬虫需要解决的问题
 *     首先是广度优先的遍历
 *     处理链接
 *         记录访问过的链接(包括已经爬过和将来要爬的链接)
 *         是否跟踪302重定向的页面
 *             HttpClient会有对应的方法
 *         跨域的链接的处理
 *         处理带 base 标签页面的链接
 *         处理 Robots 信息
 *             robots.txt
 *             <meta name="robots" content="nofollow"/>
 *             <a rel="nofollow" />
 *     设置程序访问链接的超时
 *         如果用HttpClient会有对应的方法
 *     过滤url
 *     处理页面
 *         判断页面编码
 *         判断页面的类型
 *         是否用了compress
 *         解析页面
 *             处理页面外链
 *         持久化信息
 *             持久化页面的最大内容长度需不需要设置
 *
 */


public class BasicWebCrawler {

    private CrawlData crawlData;

    private URLFilter urlFilter;

    private static final int DEFAULT_MAX_BATCH_SIZE = 50;

    private long DEFAULT_PAUSE_IN_MILLIS = 500;
    private long pauseBetweenFetchesInMillis = DEFAULT_PAUSE_IN_MILLIS;

    /*
     * Number of URLs to fetch and parse at a time.
     */
    private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;

    /*
     * Number of fetched and parsed URLs so far.
     */
    private int processedUrlCount = 0;


    public BasicWebCrawler(CrawlData crawlData) {
        this.crawlData = crawlData;
    }

    public void addSeedUrls(List<String> seedUrls) {
        int seedUrlDepth = 0;
        KnownUrlDB knownUrlsDB = crawlData.getKnownUrlsDB();
        for(String url : seedUrls) {
            knownUrlsDB.addNewUrl(url, seedUrlDepth);
        }
    }

    public void fetchAndProcess(int maxDepth, int maxDocs) {

        boolean maxUrlsLimitReached = false;
        int documentGroup = 1;

        crawlData.init();

        if( maxBatchSize <= 0) {
            throw new RuntimeException("Invalid value for maxBatchSize = " + maxBatchSize);
        }

        for(int depth = 0; depth < maxDepth; depth++) {

            int urlsProcessedAtThisDepth = 0;

            boolean noMoreUrlsAtThisDepth = false;

            //分批次处理链接
            while( maxUrlsLimitReached == false && noMoreUrlsAtThisDepth == false) {

                System.out.println("Starting url group: " + documentGroup +
                        ", current depth: " + depth +
                        ", total known urls: " +
                            crawlData.getKnownUrlsDB().getTotalUrlCount() +
                        ", maxDepth: " + maxDepth +
                        ", maxDocs: " + maxDocs +
                        ", maxDocs per group: " + maxBatchSize +
                        ", pause between docs: " + pauseBetweenFetchesInMillis + "(ms)");

                List<String> urlsToProcess = selectNextBatchOfUrlsToCrawl(maxBatchSize, depth);//在某个深度下每次最多处理 maxBatchSize 条URL


                /* for batch of urls create a separate document group */
                String currentGroupId = String.valueOf(documentGroup);
                fetchPages(urlsToProcess, crawlData.getFetchedDocsDB(), currentGroupId);

                // process downloaded data
                processPages(currentGroupId, crawlData.getProcessedDocsDB(), crawlData.getFetchedDocsDB());

                // get processed doc, get links, add links to all-known-urls.dat
                processLinks(currentGroupId, depth + 1, crawlData.getProcessedDocsDB());

                int lastProcessedBatchSize = urlsToProcess.size();
                processedUrlCount += lastProcessedBatchSize;
                urlsProcessedAtThisDepth += lastProcessedBatchSize;

                System.out.println("Finished url group: " + documentGroup +
                        ", urls processed in this group: " + lastProcessedBatchSize +
                        ", current depth: " + depth +
                        ", total urls processed: " + processedUrlCount);

                documentGroup += 1;

                if( processedUrlCount >= maxDocs ) {//处理的文档数
                    maxUrlsLimitReached = true;
                }

                if( lastProcessedBatchSize == 0 ) {//要处理的链接数
                    noMoreUrlsAtThisDepth = true;
                }
            }

            if( urlsProcessedAtThisDepth == 0) {//这层深度没有任何url
                break;
            }

            if( maxUrlsLimitReached ) {//达到处理的文档上限
                break;
            }

        }
     }

    //根据Http协议返回对应的处理方式
    private Transport getTransport(String protocol) {
        if( "http".equalsIgnoreCase(protocol) ) {
            return new HTTPTransport();
        }
        else if ( "file".equalsIgnoreCase(protocol) ) {
            return new FileTransport();
        }
        else {
            throw new RuntimeException("Unsupported protocol: '" + protocol + "'.");
        }
    }

    /**
     * 根据url列表获取页面信息, 保存获取的文档信息, 并设置url的状态
     * @param urls          [description]
     * @param fetchedDocsDB [description]
     * @param groupId       [description]
     */
    private void fetchPages(List<String> urls, FetchedDocsDB fetchedDocsDB, String groupId) {
        DocumentIdUtils docIdUtils = new DocumentIdUtils();//Doc id 的规则生成工具类
        int docSequenceInGroup = 1;
        List<UrlGroup> urlGroups = UrlUtils.groupByProtocolAndHost(urls);//按 protocol+host 分组 url
        for( UrlGroup urlGroup : urlGroups ) {
            Transport t = getTransport(urlGroup.getProtocol());
            try {
                t.init();
                for(String url : urlGroup.getUrls() ) {
                    try {
                        FetchedDocument doc = t.fetch(url);
                        String documentId = docIdUtils.getDocumentId(groupId, docSequenceInGroup);
                        doc.setDocumentId(documentId);
                        fetchedDocsDB.saveDocument(doc);
                        if( t.pauseRequired() ) {//todo: 为什么需要pause
                            pause();
                        }
                    }
                    catch(Exception e) {
                        System.out.println("Failed to fetch document from url: '" + url + "'.\n"+
                                e.getMessage());
                        crawlData.getKnownUrlsDB().updateUrlStatus(url, KnownUrlEntry.STATUS_PROCESSED_ERROR); //todo: 这步是怎么关联的,比如错误的url是怎么处理的，会不会在下次碰到会重试
                    }
                    docSequenceInGroup++;
                }
            }
            finally {
                t.clear();
            }
        }
    }

    public long getPauseBetweenFetchesInMillis() {
        return pauseBetweenFetchesInMillis;
    }

    public void setPauseBetweenFetchesInMillis(long pauseBetweenFetchesInMillis) {
        this.pauseBetweenFetchesInMillis = pauseBetweenFetchesInMillis;
    }

    public void pause() {
        try {
            Thread.sleep(pauseBetweenFetchesInMillis);
        }
        catch(InterruptedException e) {
            // do nothing
        }
    }

    /**
     * 处理下载过的文档, 将处理过的文档保存, 并改变连接的状态 STATUS_PROCESSED_SUCCESS
     *
     * @param groupId           文档的组ID
     * @param parsedDocsService 用于保存处理过的文档的DB接口
     * @param fetchedDocsDB     已经保存过的文档DB
     */
    private void processPages(String groupId, ProcessedDocsDB parsedDocsService, FetchedDocsDB fetchedDocsDB) {

        List<String> docIds = fetchedDocsDB.getDocumentIds(groupId);

        for(String id : docIds) {
            FetchedDocument doc = null;
            try {
                doc = fetchedDocsDB.getDocument(id);
                String url = doc.getDocumentURL();
                // process saved document
                DocumentParser docParser = DocumentParserFactory.getInstance().getDocumentParser(doc.getContentType());
                ProcessedDocument parsedDoc = docParser.parse(doc);
                // persist parsed document
                parsedDocsService.saveDocument(parsedDoc);
                //update link status
                crawlData.getKnownUrlsDB().updateUrlStatus(url, KnownUrlEntry.STATUS_PROCESSED_SUCCESS);
            }
            catch(Exception e) {

                if( doc != null  ) {

                    System.out.println("ERROR:\n");
                    System.out.println("Unexpected exception while processing: '" + id + "', ");
                    System.out.println("   URL='" + doc.getDocumentURL() + "'\n");
                    System.out.println("Exception message: "+e.getMessage());

                } else {
                    System.out.println("ERROR:\n");
                    System.out.println("Unexpected exception while processing: '" + id + "', ");
                    System.out.println("Exception message: "+e.getMessage());
                }
            }
        }
    }

    // process links, save to db
    // todo: 页面的层级 currentDepth 是如何确定的
    private void processLinks(String groupId, int currentDepth, ProcessedDocsDB parsedDocs) {
        URLNormalizer urlNormalizer = new URLNormalizer();//格式化url, 当前没做任何有意义的操作
        if( urlFilter == null ) {//默认接受 `file://` File的文档
            urlFilter = new URLFilter();
            urlFilter.setAllowFileUrls(true);
            urlFilter.setAllowHttpUrls(false);
            System.out.println("Using default URLFilter configuration that only accepts 'file://' urls");
        }

        List<String> docIds = parsedDocs.getDocumentIds(groupId);
        for(String documentId : docIds) {
            ProcessedDocument doc = parsedDocs.loadDocument(documentId);
            // register url without any outlinks first
            crawlData.getPageLinkDB().addLink(doc.getDocumentURL());//todo: diff between pageLinkDB and knowUrlDB ?
            List<Outlink> outlinks = doc.getOutlinks();//获取这个页面对应的外链
            //Outlink = {url: , name: }
            for(Outlink outlink : outlinks) {
                String url = outlink.getLinkUrl();
                String normalizedUrl = urlNormalizer.normalizeUrl(url);
                if( urlFilter.accept(normalizedUrl) ) {//todo: what these two lines do ?
                    crawlData.getKnownUrlsDB().addNewUrl(url, currentDepth);//添加新的未处理连接
                    crawlData.getPageLinkDB().addLink(doc.getDocumentURL(), url);//保存页面链接关系 todo: pageLinkDB保存的关系是做什么用的
                }
            }
        }
        crawlData.getKnownUrlsDB().save();
        crawlData.getPageLinkDB().save();
    }

    /**
     * @deprecated use method that uses depth
     *
     * @param maxDocs
     * @return
     */
    @Deprecated
    public List<String> selectURLsForNextCrawl(int maxDocs) {
        return crawlData.getKnownUrlsDB().findUnprocessedUrls(maxDocs);
    }

    private List<String> selectNextBatchOfUrlsToCrawl(int maxBatchSize, int depth) {
        return crawlData.getKnownUrlsDB().findUnprocessedUrls(maxBatchSize, depth);
    }


    public URLFilter getURLFilter() {
        return urlFilter;
    }

    public void setURLFilter(URLFilter urlFilter) {
        this.urlFilter = urlFilter;
    }

}
