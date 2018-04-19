# SXNJU SPIDER
download all the books in pdf format from [SXNJU](http://sxnju.chineseall.cn/home/index)


## environment
* linux
* java
* python3

## denpendencies
* Java 8 +
#### add ppa
    sudo add-apt-repository ppa:webupd8team/java
    sudo apt-get update
#### install oracle-java-installer
    sudo apt-get install oracle-java8-installer
#### set default jdk
    sudo update-java-alternatives -s java-8-oracle
#### test
    java -version
    javac -version
* GhostScript
    apt-get install ghostscript
* PDFTK
    apt-get install pdftk
* QPDF
    apt-get install qpdf



## run

#### options:
    -t thread num,default=8,
    
    mvn package
    java -jar libpdf.jar [options] <url>
#### example: 
    java -jar libpdf.jar http://sxnju.chineseall.cn/v3/book/detail/VPeZj

## my changes
- [x] crawl all the book url,one book one url,about 133996 books
    pthon3 crawl_book_urls.py
- [x] run this .py,will download all the books without interacte 
    python3 run_all.py
- [x] delete the pdf in tmp folder
    python3 delete_tmp_pdf.py

## result
<div>
<img width="350" height="250" src="https://github.com/watersink/SXNJU_spider/raw/master/result/result1.jpg"/>
<img width="350" height="250" src="https://github.com/watersink/SXNJU_spider/raw/master/result/result2.jpg"/>
</div>

## others
    only used for study and communication
## reference:
[NJU-lib-Downloader](https://github.com/padeoe/nju-lib-downloader)