import re
import urllib
import urllib.request
import requests
from bs4 import BeautifulSoup

import time


class SXZG():
    def __init__(self):
        self.headers = {
            "X-Requested-With": 'XMLHttpRequest',
            "User-Agent": 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.89 Safari/537.36',
            "Origin": 'http://book.chaoxing.com',
            "Cookie": 'user_trace_token=20170211115515-2db01e4efbb24178989f2b6139d3698e; LGUID=20170211115515-e593a6c4-f00d-11e6-8f71-5254005c3644; showExpriedIndex=1; showExpriedCompanyHome=1; showExpriedMyPublish=1; hasDeliver=0; index_location_city=%E5%85%A8%E5%9B%BD; login=false; unick=""; _putrc=""; Hm_lvt_4233e74dff0ae5bd0a3d81c6ccf756e6=1486785316; Hm_lpvt_4233e74dff0ae5bd0a3d81c6ccf756e6=1486789519; _ga=GA1.2.1374329991.1486785316; LGRID=20170211130519-af0ec03c-f017-11e6-a32c-525400f775ce; TG-TRACK-CODE=search_code; JSESSIONID=A5AC6E7C54130E13C1519ABA7F70BC3C; SEARCH_ID=053c985ab53e463eb5f747658872ef29',
            "Connection": 'keep-alive'
        }
        self.base_url="http://sxnju.chineseall.cn/org/show/2322/search/all/"
        self.book_header="http://sxnju.chineseall.cn"

    def parse_url(self, url):
        page = urllib.request.Request(url, headers=self.headers)
        #print(page)
        page_info = urllib.request.urlopen(page).read().decode('utf-8')
        #print(page_info)

        soup = BeautifulSoup(page_info, 'html.parser')
        time.sleep(1)
        # 以格式化的形式打印html
        # print(soup.prettify())
        return soup

    def processing(self):
        out_file=open("book_url.txt","w",encoding="utf-8")
        for i in range(4467):
            book_url = self.base_url + str(i)

            soup = self.parse_url(book_url)
            titles= soup.find_all("a",target="_blank",href=re.compile(r'/v3/book/detail'))  # ,,href=re.compile(r'.html'),title=re.compile(r'*')

            for j in range(len(titles)):
                out_file.write(self.book_header + str(titles[j].get("href"))+"\n")
            print("processed %d:",i)
            

        out_file.close()




if __name__=="__main__":
    sxzg=SXZG()
    sxzg.processing()

