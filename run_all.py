

import os
import subprocess


with open("book_url.txt","r",encoding="utf-8")as bu:
    book_urls=bu.readlines()

for i in range(len(book_urls)):
    book_url=book_urls[i].rstrip("\n")

    if os.path.exists("./tmp/"+book_url.split("/")[-1]):
        print("existing :",book_url)
        continue
    else:
        print("proccessing :",book_url)
        status, output = subprocess.getstatusoutput('java -jar libpdf.jar %s'%(book_urls[i]))
        print(status, output)


