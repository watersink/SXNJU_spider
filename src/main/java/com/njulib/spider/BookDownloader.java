package com.njulib.spider;

import com.njulib.fix.MissingPageCompletion;
import com.njulib.object.Book;
import com.njulib.object.InfoReader;
import com.njulib.object.exception.BookDLException;
import com.njulib.object.exception.BookPagesDLException;
import com.njulib.object.exception.PageDLException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import utils.network.MyHttpRequest;
import utils.network.ReturnData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 书本的下载器，分离了下载相关的函数及变量。
 *
 * @author padeoe
 *         Date: 2016/12/09
 */
public class BookDownloader {
    private String errorLogPath = ERROR_LOG_NAME;
    private int threadNumber = 5;

    /**
     * 获取下载器对应的{@code Book}
     *
     * @return 下载器对应的{@code Book}
     */
    public Book getBook() {
        return book;
    }

    private Book book;
    private Map<PageType, Integer> pageNumberMap;
    private String savePath = System.getProperty("user.dir");
    private Path directory;
    private String urlPrefix;
    private PageType[] pageTypes = {PageType.COVER, PageType.BOOKNAME, PageType.LEGALINFO, PageType.INTRODUCTION,
            PageType.DIRECTORY, PageType.CONTENT, PageType.APPENDIX, PageType.BACKCOVER};
    private AtomicInteger needDownload = new AtomicInteger(1);

    /**
     * 获取{@code Book}的页组成结构。
     *
     * @return 记录了每种{@link PageType}的数量。
     * @throws BookDLException 页组成获取失败，书本下载放弃
     */
    public Map<PageType, Integer> getPageNumberMap() throws BookDLException {
        if (pageNumberMap == null) {
            initialBookPara();
            return pageNumberMap;
        }
        return pageNumberMap;
    }

    /**
     * 获取{@code Book}图片的URL前缀
     *
     * @return {@code Book}图片的URL前缀
     * @throws BookDLException 前缀获取失败，书本下载被放弃。
     */
    public String getUrlPrefix() throws BookDLException {
        if (urlPrefix == null) {
            initialBookPara();
            return urlPrefix;
        }
        return urlPrefix;
    }

    /**
     * 错误日志的默认文件名
     */
    public static final String ERROR_LOG_NAME = "error.log";
    /**
     * 书本信息记录的默认文件名
     */
    public static final String INFO_FILE_NAME = "info.txt";

    /**
     * 创建指定{@code book}的下载器
     *
     * @param book 指定的书本
     */
    public BookDownloader(Book book) {
        this.book = book;
    }

    /**
     * 创建指定{@code Book}的下载器，将根据{@code bookid}创建{@link Book}对象
     *
     * @param bookid 书本id
     */
    BookDownloader(String bookid) {
        this.book = new Book(bookid);
    }

    /**
     * 查看下载线程数
     *
     * @return 当前指定的下载线程数。默认为5
     */
    public int getThreadNumber() {
        return threadNumber;
    }

    /**
     * 设置下载线程数。书本与书本之间将会依次单线程下载。书本的所有页将会采用多线程下载。
     *
     * @param threadNumber 线程数
     */
    public void setThreadNumber(int threadNumber) {
        this.threadNumber = threadNumber;
    }

    /**
     * 设置文件夹名
     *
     * @param directoryString 文件夹名
     */
    public void setDirectory(String directoryString) {
        String directoryName = directoryString.replaceAll("[/\\\\:\"*?<>|]", " ");
        directory = Paths.get(savePath, directoryName);
    }

    /**
     * 设置保存路径
     *
     * @param savePath 下载保存路径
     */
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    /**
     * 下载图片
     *
     * @param url      图片的url
     * @param pathname 保存的路径，包括文件名(不含图片后缀),例如"C:/Users/username/a"，函数执行后会保存为"C:/Users/username/a.png"
     * @throws IOException 下载出错
     */
    public static void downloadImage(String url, String pathname) throws IOException {
        ReturnData returnData = MyHttpRequest.action_returnbyte("GET", null, url, null, null, null, 2000);
        byte[] a = returnData.getData();
        List<String> types = returnData.getHeaders().get("Content-Type");
        String suffix = ".png";
        if (types != null && types.get(0) != null) {
            suffix = types.get(0).substring(types.get(0).indexOf('/') + 1, types.get(0).length()).toLowerCase();
            suffix = suffix.equals("jpeg") ? ".jpg" : (suffix.equals("png") ? ".png" : suffix);
        }
        File file = new File(pathname + suffix);
        BufferedOutputStream bf = new BufferedOutputStream(new FileOutputStream(file));
        bf.write(a, 0, a.length);
        bf.close();
    }


    /**
     * 初始化下载参数，从服务器查询书本下载所需的参数,包括书页url，书本页数，页类型
     * 执行后
     *
     * @throws BookDLException 查询参数出错，书本下载被终止
     */
    private void initialBookPara() throws BookDLException {
        //获取页面地址
        String url;
        try {
            url = book.getbookread();
            getBookPara(url);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BookDLException(book);
        }
    }

    public String getBookViewPageHtml(String url) throws BookDLException {
        if (url == null || url.length() == 0) {
            throw new BookDLException(book);
        }
        //获取书本参数，包括下载地址前缀，页数
        String html;
        try {
            html = MyHttpRequest.get(url, null, "UTF-8", 2000);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BookDLException(book);
        }
        return html;
    }

    private void getBookPara(String url) throws BookDLException {
        String html = getBookViewPageHtml(url);
        Document doc = Jsoup.parse(html);
        Element infoNode = doc.getElementsByTag("script").last();
        pageNumberMap = new HashMap<>();
        int epage = 0;
        if (infoNode.dataNodes().size() > 0) {
            String paraJs = infoNode.dataNodes().get(0).getWholeData();
            Pattern pattern = Pattern.compile("var str='(.*)';.*epage = (\\d+);.*pages :\\[\\[1,(\\d+)\\],\\[1,(\\d+)\\],\\[1,(\\d+)\\]," +
                    "\\[1,(\\d+)\\], \\[1,(\\d+)\\], \\[spage, epage\\], \\[1,(\\d+)\\], \\[1,(\\d+)\\]\\],.*", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(paraJs);
            if (matcher.find()) {
                urlPrefix = matcher.group(1);
                pageNumberMap.put(pageTypes[5], Integer.parseInt(matcher.group(2)));
                pageNumberMap.put(pageTypes[0], Integer.parseInt(matcher.group(3)));
                pageNumberMap.put(pageTypes[1], Integer.parseInt(matcher.group(4)));
                pageNumberMap.put(pageTypes[2], Integer.parseInt(matcher.group(5)));
                pageNumberMap.put(pageTypes[3], Integer.parseInt(matcher.group(6)));
                pageNumberMap.put(pageTypes[4], Integer.parseInt(matcher.group(7)));
                pageNumberMap.put(pageTypes[6], Integer.parseInt(matcher.group(8)));
                pageNumberMap.put(pageTypes[7], Integer.parseInt(matcher.group(9)));
            } else {
                throw new BookDLException(book);
            }
        } else {
            System.out.println(book.getId() + " 参数获取失败");
            throw new BookDLException(book);
        }
    }

    /**
     * 通过书页页数判断是否是同一本书，如果是则补全info文件，如果不是则不执行操作
     *
     * @throws BookDLException 从服务器查询书本参数时出错
     */
    private void checkOldDirByPageSize() throws BookDLException {
        File[] oldfiles = directory.toFile().listFiles();
        int oldBookSize = oldfiles == null ? 0 : oldfiles.length;
        //查询当前书本的页数
        initialBookPara();
        int newBookSize = pageNumberMap.values().stream().mapToInt(number -> number).sum();
        //若书页数相同，假定为同一本书，帮他补全info文件
        if (oldBookSize == newBookSize) {
            logBookInfo();
            System.out.println("已存在，跳过并补全了info文件" + book.toString());
        }
    }

    /**
     * 开始创建文件夹并下载，该函数调用前保存路径以及文件夹名必须已经设置完毕。该环节有多个出口：
     * 如果文件夹存在，将会调用{@link #handleOldDir()}进行下一步处理
     * 如果文件夹不存在，将会初始化参数并调用{@link #downloadFromParaSetDone()} 进行下一步处理
     *
     * @throws BookPagesDLException 书本下载过程中发生了缺页
     * @throws BookDLException      书本下载未开始
     */
    private void downloadFromMkdir() throws BookPagesDLException, BookDLException {
        File path = directory.toFile();
        //若目录存在，进入目录存在的处理例程
        if (path.exists()) {
            handleOldDir();
            return;
        }
        //目录不存，准备下载。首先获取下载参数
        System.out.println("开始下载 " + book);
        //获取书本参数
        initialBookPara();
        if (!path.mkdirs()) {
            System.out.println("文件夹创建失败");
            throw new BookDLException(book);
        }
        downloadFromParaSetDone();
    }

    /**
     * 书本参数已经从服务器获取完毕，直接进行下载并保存。
     *
     * @throws BookPagesDLException 书本的某些页下载失败
     */
    private void downloadFromParaSetDone() throws BookPagesDLException {
        Vector<PageDLException> pageDLExceptions = new Vector<>();
        //首先顺序下载非正文内容
        for (int i = 0; i < pageTypes.length; i++) {
            if (i != 5) {
                try {
                    download(pageTypes[i]);
                } catch (BookPagesDLException e) {
                    pageDLExceptions.addAll(e.getPageDLExceptions());
                }
            }
        }
        try {
            downloadContent();
            //日志记录书本信息
            logBookInfo();
        } catch (BookPagesDLException e) {
            pageDLExceptions.addAll(e.getPageDLExceptions());
        }
        if (!pageDLExceptions.isEmpty()) {
            throw new BookPagesDLException(pageDLExceptions);
        }
    }

    /**
     * 下载文件夹已存在的处理函数。该函数会读取旧的文件夹下的info文件来判断待下载是不是同一本书。
     * 该步骤有多个出口：
     * 如果info文件不存在或效，将调用{@link #checkOldDirByPageSize()}做进一步判断
     * 如果info文件存在且有效，读取info中书本id比对是否是同一本书：
     * 如果是同一本书，将跳过；如果不是同一本书，将重新设置保存路径和文件夹名，并调用{@link #downloadFromMkdir()}进行下一步处理
     *
     * @throws BookPagesDLException 书本下载过程中发生了缺页
     * @throws BookDLException      书本下载未开始
     */
    private void handleOldDir() throws BookPagesDLException, BookDLException {
        //开始检查是否真的是重复还是同名而已，根据书的id判断
        //读取info文件
        Path infoFilePath = directory.resolve(INFO_FILE_NAME);
        File infoFile = infoFilePath.toFile();
        if (infoFile.exists()) {
            //info文件存在，读取info文件记录的书本id
            Book oldbook;
            oldbook = new InfoReader(infoFilePath.toString()).read();
            //读出了旧的书本信息
            if (oldbook != null) {
                String oldBookId = oldbook.getId();
                //两本书是同一本书
                if (oldBookId.equals(book.getId())) {
                    System.out.println("已存在，跳过" + book.toString());
                    return;
                }
                //两本书是不同的书
                else {
                    //如果两本书作者不同，文件夹添加作者名进行命名,并开始下载
                    if (!book.getAuthor().equals(oldbook.getAuthor())) {
                        setDirectory(book.getName() + "-" + book.getAuthor());
                        downloadFromMkdir();
                    }
                    //如果两本书作者相同，用作者名加id命名
                    else {
                        setDirectory(book.getName() + "-" + book.getAuthor() + "-" + book.getId());
                        downloadFromMkdir();
                    }
                }
            }
            //info文件格式不正确，没有读出信息
            //假定就文件夹是一本旧的书目,文件夹添加作者名进行命名,并开始下载
            else {
                setDirectory(book.getName() + "-" + book.getAuthor());
                downloadFromMkdir();
            }
        } else {
            //info文件不存在，比对书本页数数量是否是同一本书决定下一步操作
         //   checkOldDirByPageSize();
            System.out.println("将删除没有info文件的目录"+directory.getFileName());
            if(deleteDir(directory.toFile())){
                downloadFromMkdir();
            }
            else{
                throw new BookDLException(this.book);
            }

        }
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     * @param dir 将要删除的文件目录
     * @return boolean Returns "true" if all deletions were successful.
     *                 If a deletion fails, the method stops attempting to
     *                 delete and returns "false".
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }


    /**
     * 将书本下载保存为图片格式，书的每一页将会保存为一张图片
     */
    public void downloadAllImages() {
        setDirectory(book.getName() != null ? book.getName() : book.getId());
        try {
            downloadFromMkdir();
        } catch (BookDLException e) {
            logBookFail(errorLogPath);//错误日志，记录未下载书籍
        } catch (BookPagesDLException e) {
            logPageFail(e, errorLogPath);//错误日志，记录单页下载失败
        }
    }


    /**
     * 在同文件夹下创建记录{@code Book}信息的文件，
     * 文件名是""info.txt""
     */
    private void logBookInfo() {
        try {
            FileWriter fileWriter = new FileWriter(new File(directory.resolve("info.txt").toString()));
            fileWriter.write(book.toString());
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载{@code PageType.CONTENT}部分所有的页，即正文部分。
     * 不直接调用{@link #download(PageType)}下载正文部分是因为其采用了单线程下载。正文部分书页较多，因此本方法会使用多线程下载。
     *
     * @throws BookPagesDLException 书本的某些页下载失败
     */
    private void downloadContent() throws BookPagesDLException {
        int firstPage = getFirstPage(PageType.CONTENT);//第一页的序号
        final int pageSize = pageNumberMap.get(PageType.CONTENT);//正文总页数
        //System.out.println("正文页码" + firstPage + "~" + lastPage);
        needDownload.set(1);
        Vector<PageDLException> pageDLExceptions = new Vector<>();
        ArrayList<Thread> threadArrayList = new ArrayList<>();
        for (int i = 0; i < threadNumber; i++) {
            threadArrayList.add(new Thread() {
                @Override
                public void run() {
                    super.run();
                    while (true) {
                        int downloading = needDownload.getAndIncrement();
                        if (downloading <= pageSize) {
                            //System.out.println("假装在下载 "+downloading);
                            try {
                                download(PageType.CONTENT, downloading, String.format("%04d", firstPage + downloading - 1));
                            } catch (PageDLException e) {
                                pageDLExceptions.add(e);
                            }
                        } else {
                            break;
                        }
                    }
                }
            });
        }
        for (Thread thread : threadArrayList) {
            thread.start();
        }
        for (Thread thread : threadArrayList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!pageDLExceptions.isEmpty()) {
            throw new BookPagesDLException(pageDLExceptions);
        }
    }

    /**
     * 下载某一种页类型的所有页。
     *
     * @param pageType 页类型
     * @throws BookPagesDLException 某些页下载失败
     */
    private void download(PageType pageType) throws BookPagesDLException {
        Vector<PageDLException> pageDLExceptions = new Vector<>();
        int base = getFirstPage(pageType);
        for (int i = 0; i < pageNumberMap.get(pageType); i++) {
            try {
                download(pageType, i + 1, String.format("%04d", base + i));
            } catch (PageDLException e) {
                pageDLExceptions.add(e);
            }
        }
        if (!pageDLExceptions.isEmpty()) {
            throw new BookPagesDLException(pageDLExceptions);
        }
    }

    /**
     * 获取某一种类型页的第一页页码
     *
     * @param pageType 书页类型
     * @return 相对于整本书的页码
     */
    private int getFirstPage(PageType pageType) {
        int base = 1;//该种类型页的第一页的页码
        for (PageType pageType1 : pageTypes) {
            if (pageType1.equals(pageType)) {
                break;
            } else {
                base += pageNumberMap.get(pageType1);
            }
        }
        return base;
    }

    /**
     * 下载某一种页类型的特定页
     *
     * @param pageType 页类型
     * @param page     图书的页码
     * @throws PageDLException 某些页下载失败
     */
    private void download(PageType pageType, int page, String filename) throws PageDLException {
        int pageNumberLength = 6 - pageType.name.length();
        StringBuilder url = new StringBuilder();
        url.append(urlPrefix).append(pageType.name);
        for (int i = 0; i < pageNumberLength - String.valueOf(page).length(); i++) {
            url.append('0');
        }
        url.append(page);
        url.append(".jpg");
        String finalurl = url.toString();
        String pathname = directory.resolve(filename).toString();
        try {
            downloadImage(finalurl, pathname);
        } catch (IOException e) {
            try {
                downloadImage(finalurl, pathname);
            } catch (IOException e1) {
                throw new PageDLException(finalurl, pathname);
            }

        }
    }

    /**
     * 输出单页下载失败的日志，可以使用{@link MissingPageCompletion}来读取错误日志并恢复
     *
     * @param bookPagesDLException 单页失败异常
     * @param pageFailLogPath      日志路径
     */
    private static void logPageFail(BookPagesDLException bookPagesDLException, String pageFailLogPath) {
        Vector<PageDLException> pageDLExceptions = bookPagesDLException.getPageDLExceptions();
        for (PageDLException pageDLException : pageDLExceptions) {
            writeFile(pageFailLogPath, pageDLException.toString());
        }

    }

    /**
     * 输出整本书下载失败的日志，用于后期恢复(暂未完成)
     *
     * @param bookFailLogPath 日志路径
     */
    private void logBookFail(String bookFailLogPath) {
        writeFile(bookFailLogPath, book.toString());
    }

    public static void writeFile(String filepath, String content) {
        Writer fstream = null;
        try {
            fstream = new OutputStreamWriter(new FileOutputStream(filepath, true), StandardCharsets.UTF_8);
            fstream.write(content);
            fstream.write(System.getProperty("line.separator"));
            fstream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setErrorLogPath(String errorLogPath) {
        this.errorLogPath = errorLogPath;
    }

    /**
     * 书页的类型。每本书都由"封面"，"正文"，"目录"等若干种固定的页类型组成。
     */
    public enum PageType {
        COVER("cov", 1), BOOKNAME("bok", 2), LEGALINFO("leg", 3), INTRODUCTION("fow", 4), DIRECTORY("!", 5),
        CONTENT("", 6), APPENDIX("att", 7), BACKCOVER("cov", 8);
        private String name;
        private int index;

        PageType(String name, int index) {
            this.name = name;
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }
    }
}
