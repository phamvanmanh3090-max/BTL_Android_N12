from pathlib import Path
import textwrap, hashlib, json, zipfile
from PIL import Image, ImageDraw, ImageFont
from pygments import lex
from pygments.lexers import JavaLexer
from pygments.styles import get_style_by_name

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'app/src/main/java/com/example/medicationreminderapp'
OUT = ROOT / 'docs/code-history'
OUT.mkdir(parents=True, exist_ok=True)
mono = ImageFont.truetype('C:/Windows/Fonts/consola.ttf', 27)
titlefont = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 33)
smallfont = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 22)
style = get_style_by_name('friendly')

def select(file, marker=None, until_end=False):
    raw = (BASE/file).read_text(encoding='utf-8')
    lines = raw.splitlines()
    if marker is None: return lines, 1, raw
    start = next(i for i,line in enumerate(lines) if marker in line)
    if until_end: return lines[start:], start+1, raw
    depth=0
    begun=False
    for end in range(start,len(lines)):
        line=lines[end]
        if '{' in line: begun=True
        depth += line.count('{')-line.count('}')
        if begun and depth==0: return lines[start:end+1],start+1,raw
    raise ValueError(marker)

snips = [
 ('01-model-dose-record','Mô hình lưu lịch sử uống thuốc','model/DoseRecord.java',None,False),
 ('02-dao-dose-record','Lưu và truy vấn lịch sử theo ngày','database/DoseRecordDao.java',None,False),
 ('03-room-database','Khai báo bảng lịch sử trong Room','database/AppDatabase.java','@Database(',True),
 ('04-chon-ngay','Chọn ngày để xem các liều đã ghi nhận','activity/HomeActivity.java','private void initRecyclerView()',False),
 ('05-luu-xac-nhan-uong','Xác nhận uống thuốc và hoàn tác','activity/HomeActivity.java','private void handleUongClick(',False),
 ('06-doc-lich-su-theo-ngay','Đọc lịch sử và ghép trạng thái từng liều','activity/HomeActivity.java','private void filterMedicinesForDate(',False),
 ('07-hien-thi-trang-thai','Hiển thị trạng thái và giờ đã uống','adapter/MedicineAdapter.java','public void onBindViewHolder(',False),
 ('08-tong-hop-va-trang-thai-rong','Tổng số liều đã uống và trạng thái rỗng','activity/HomeActivity.java','private void updateDashboard()',False),
 ('09-luu-tu-chuong-bao','Ghi nhận lần uống từ luồng chuông báo','service/AlarmService.java','private void saveDoseAndNotify(',False),
]
manifest=[]
for slug,title,file,marker,tail in snips:
    lines,start,raw=select(file,marker,tail)
    code='\n'.join(lines)
    # Retain every original character and line number; wrap only for image layout.
    richlines=[[]]
    for token,value in lex(code,JavaLexer()):
        color=style.style_for_token(token)['color'] or '20252B'
        for char in value:
            if char=='\n': richlines.append([])
            else: richlines[-1].append((char,'#'+color))
    richlines=richlines[:len(lines)]
    while len(richlines)<len(lines):richlines.append([])
    charw=mono.getlength('M')
    maxchars=125
    rendered=[]
    for n,chars in enumerate(richlines,start):
        chunks=[]
        remaining=chars
        while len(remaining)>maxchars:
            cuts=[i+1 for i,(char,_) in enumerate(remaining[:maxchars]) if char.isspace() and i>maxchars//2]
            cut=cuts[-1] if cuts else maxchars
            chunks.append(remaining[:cut])
            remaining=remaining[cut:]
        chunks.append(remaining)
        for j,chunk in enumerate(chunks):rendered.append((str(n) if j==0 else '>',chunk))
    columns=max(82,max((len(row) for _,row in rendered),default=0))
    width=int(145+columns*charw+42)
    top=157
    lineheight=38
    height=top+len(rendered)*lineheight+55
    im=Image.new('RGB',(width,height),'white')
    d=ImageDraw.Draw(im)
    d.rectangle((0,0,width,129),fill='#F2F5F7')
    d.rectangle((0,0,9,129),fill='#087958')
    d.text((30,18),title,font=titlefont,fill='#142B22')
    d.text((32,68),'app/src/main/java/com/example/medicationreminderapp/'+file,font=smallfont,fill='#35424C')
    end=start+len(lines)-1
    d.text((32,99),f'Java  |  Dòng {start}–{end}  |  Trích nguyên văn mã nguồn dự án',font=smallfont,fill='#53616B')
    d.rectangle((0,145,114,height-32),fill='#F8FAFB')
    d.line((114,145,114,height-32),fill='#DDE4E8',width=1)
    for row,(num,chars) in enumerate(rendered):
        y=top+row*lineheight
        d.text((96-mono.getlength(num),y),num,font=mono,fill='#89939B')
        for col,(char,color) in enumerate(chars):d.text((139+col*charw,y),char,font=mono,fill=color)
    dest=OUT/(slug+'.png')
    im.save(dest)
    manifest.append(dict(image=dest.name,title=title,source='app/src/main/java/com/example/medicationreminderapp/'+file,startLine=start,endLine=end,sha256=hashlib.sha256(raw.encode('utf-8')).hexdigest(),size=list(im.size)))

readme=['ẢNH CODE — USE CASE THEO DÕI LỊCH SỬ UỐNG THUỐC','',
 'Các ảnh được dựng từ code gốc trong dự án, không dùng AI sinh nội dung code.',
 'Mỗi ảnh có tên file và số dòng nguồn. Dòng dài được xuống hàng để đọc; ký hiệu > là phần tiếp của cùng dòng.',
 'Logic xem theo ngày hiện nằm trong HomeActivity. Các Activity lịch sử/chi tiết ở biểu đồ thiết kế trước chưa được triển khai thành lớp riêng.',
 'isTaken=false biểu thị chưa được đánh dấu đã uống; không phải trường dữ liệu riêng xác nhận bỏ lỡ.', '']
for item in manifest:readme.append(f"{item['image']}\n  {item['title']}\n  {item['source']} — dòng {item['startLine']}–{item['endLine']}\n")
(OUT/'README.txt').write_text('\n'.join(readme),encoding='utf-8')
(OUT/'sources.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
with zipfile.ZipFile(OUT/'anh-code-lich-su-uong-thuoc.zip','w',zipfile.ZIP_DEFLATED) as z:
    for item in manifest:z.write(OUT/item['image'],item['image'])
    for f in ['README.txt','sources.json']:z.write(OUT/f,f)
for item in manifest:print(item['image'],item['startLine'],item['endLine'],item['size'])
