

import os


base_dir="./tmp/"
book_names=os.listdir(base_dir)

for book_name in book_names:
    pdf_names=os.listdir(base_dir+book_name)
    for pdf_name in pdf_names:
        try:
            os.remove(base_dir+book_name+"/"+pdf_name)
            print("processed",book_name,pdf_name)
        except Exception as e:
            print(e)