from pathlib import Path
import re, sqlite3, xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont
from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

ROOT=Path('E:/MedicationReminderApp')
OUT=ROOT/'output/database-design'
OUT.mkdir(parents=True,exist_ok=True)
schema_source=ROOT/'app/build/generated/ap_generated_sources/debug/out/com/example/medicationreminderapp/database/AppDatabase_Impl.java'
sqls=re.findall(r'db.execSQL\("(CREATE (?:TABLE|INDEX).*?)"\);',schema_source.read_text(encoding='utf-8'))
sqls=[s for s in sqls if 'room_master_table' not in s]
con=sqlite3.connect(':memory:')
for sql in sqls:con.execute(sql)
assert len(con.execute('PRAGMA foreign_key_list(alarms)').fetchall())==1
assert not con.execute('PRAGMA foreign_key_list(dose_records)').fetchall()
assert len([r for r in con.execute('PRAGMA table_info(dose_records)') if r[5]])==3
(OUT/'schema-hien-tai.sql').write_text('-- Cấu trúc ba bảng nghiệp vụ do Room sinh từ dự án phiên bản 7.\n\n'+';\n\n'.join(sqls)+';\n',encoding='utf-8')

tables={
'medicines':[
('medicineId','INTEGER','PK','Không','Mã thuốc tự tăng, định danh duy nhất mỗi bản ghi.'),
('name','TEXT','-','Có','Tên thuốc do người dùng nhập.'),
('frequency','TEXT','-','Có','Tần suất sử dụng, ví dụ Hàng ngày hoặc Hàng tuần.'),
('startDate','INTEGER','-','Không','Ngày bắt đầu sử dụng, lưu bằng mili giây từ Unix epoch.'),
('endDate','INTEGER','-','Không','Ngày kết thúc sử dụng, lưu bằng mili giây từ Unix epoch.'),
('notes','TEXT','-','Có','Ghi chú hoặc hướng dẫn sử dụng thuốc.'),
('times','TEXT','-','Có','Danh sách giờ uống được ghép thành chuỗi, phân cách bằng dấu phẩy.')],
'alarms':[
('id','INTEGER','PK','Không','Mã báo thức tự tăng, định danh mỗi chuông báo.'),
('medicine_id','INTEGER','FK','Không','Mã thuốc tham chiếu medicines.medicineId.'),
('time','INTEGER','-','Không','Thời điểm báo thức, lưu bằng mili giây từ Unix epoch.'),
('enabled','INTEGER','-','Không','Trạng thái bật hoặc tắt, tương ứng giá trị 1 hoặc 0.'),
('scheduled_time','TEXT','-','Có','Giờ uống dự kiến tương ứng với chuông báo.')],
'dose_records':[
('medicineId','INTEGER','PK1','Không','Mã thuốc của lần uống; thành phần thứ nhất của khóa chính.'),
('date','TEXT','PK2','Không','Ngày ghi nhận theo định dạng yyyy-MM-dd.'),
('scheduledTime','TEXT','PK3','Không','Giờ uống dự kiến; thành phần thứ ba của khóa chính.'),
('isTaken','INTEGER','-','Không','Đã xác nhận uống: 1; chưa xác nhận hoặc đã hoàn tác: 0.'),
('timeTaken','TEXT','-','Có','Giờ thực tế đã xác nhận uống, định dạng HH:mm; có thể để NULL.')]
}
for table,fields in tables.items():
 actual=con.execute(f'PRAGMA table_info({table})').fetchall()
 assert [x[1] for x in actual]==[x[0] for x in fields]
 for a,f in zip(actual,fields):assert a[2]==f[1] and bool(a[3])==(f[3]=='Không')

# Physical model: solid link is an enforced foreign key, dashed link is logical only.
DW,DH=1400,1160
locations={'medicines':(390,40,620,422),'alarms':(25,695,620,330),'dose_records':(755,695,620,330)}
im=Image.new('RGB',(DW*2,DH*2),'white');d=ImageDraw.Draw(im)
def f(size,bold=False):return ImageFont.truetype('C:/Windows/Fonts/'+('arialbd.ttf' if bold else 'arial.ttf'),size*2)
def line(points,fill='#3F4448',width=2):d.line([(x*2,y*2) for x,y in points],fill=fill,width=width*2)
def label(s,x,y,size=29,bold=False,fill='#181818'):d.text((x*2,y*2),s,font=f(size,bold),fill=fill)
for table,(x,y,w,h) in locations.items():
 d.rectangle((x*2,y*2,(x+w)*2,(y+h)*2),fill='#F8D5AA',outline='#654A30',width=3)
 d.rectangle((x*2,y*2,(x+w)*2,(y+58)*2),fill='#F0B877',outline='#654A30',width=3)
 label(table,x+18,y+10,33,True)
 for i,(name,typ,key,nullable,meaning) in enumerate(tables[table]):
  yy=y+70+i*46
  label(key if key!='-' else '',x+12,yy,26,True)
  label(name,x+112,yy,30)
  label(typ,x+w-150,yy,28)
  if i<len(tables[table])-1:line([(x,yy+39),(x+w,yy+39)],'#CCB18F',1)
line([(530,462),(530,585),(335,585),(335,695)])
label('1',495,475,29,True);label('0..*',265,650,29,True)
label('ON DELETE CASCADE',40,540,28)
points=[(870,462),(870,585),(1065,585),(1065,695)]
for (x1,y1),(x2,y2) in zip(points,points[1:]):
 length=abs(x2-x1)+abs(y2-y1)
 for a in range(0,int(length),18):
  b=min(a+10,length)
  line([(x1+(x2-x1)*a/length,y1+(y2-y1)*a/length),(x1+(x2-x1)*b/length,y1+(y2-y1)*b/length)],'#825A34')
label('1',890,475,29,True);label('0..*',1090,650,29,True)
label('Liên hệ logic',1000,505,28);label('chưa khai báo FK',1000,545,28)
label('PK: khóa chính    FK: khóa ngoại',25,1070,28)
label('PK1 + PK2 + PK3: khóa chính ghép của dose_records',25,1113,28)
png=OUT/'mo-hinh-thiet-ke-vat-ly.png';im.save(png)

# Native editable diagrams.net document.
mx=ET.Element('mxfile',host='app.diagrams.net',type='device')
dia=ET.SubElement(mx,'diagram',id='physical-db',name='Mô hình thiết kế vật lý')
model=ET.SubElement(dia,'mxGraphModel',page='1',pageScale='1',pageWidth=str(DW),pageHeight=str(DH),grid='1',gridSize='10')
root=ET.SubElement(model,'root');ET.SubElement(root,'mxCell',id='0');ET.SubElement(root,'mxCell',id='1',parent='0')
def box(ident,value,x,y,w,h,style,parent='1'):
 c=ET.SubElement(root,'mxCell',id=ident,value=value,vertex='1',parent=parent,style='html=0;whiteSpace=wrap;fontFamily=Arial;fontSize=28;'+style)
 ET.SubElement(c,'mxGeometry',x=str(x),y=str(y),width=str(w),height=str(h),**{'as':'geometry'})
for table,(x,y,w,h) in locations.items():
 box(table,'',x,y,w,h,'fillColor=#F8D5AA;strokeColor=#654A30;container=1;')
 box(table+'head',table,0,0,w,58,'fillColor=#F0B877;strokeColor=#654A30;fontSize=33;fontStyle=1;align=left;spacingLeft=18;',table)
 for i,(name,typ,key,nullable,meaning) in enumerate(tables[table]):
  box(table+str(i),(key if key!='-' else '')+'    '+name+'    '+typ,8,65+i*46,w-16,46,'fillColor=none;strokeColor=none;align=left;spacingLeft=4;',table)
for ident,table,ax,labelstr,dashed in [('fk-alarm','alarms',.225,'1 : 0..*   ON DELETE CASCADE',False),('logic-dose','dose_records',.775,'1 : 0..*   Liên hệ logic, chưa có FK',True)]:
 c=ET.SubElement(root,'mxCell',id=ident,value=labelstr,edge='1',parent='1',source='medicines',target=table,style=f'edgeStyle=orthogonalEdgeStyle;exitX={ax};exitY=1;entryX=0.5;entryY=0;endArrow=none;strokeColor=#654A30;strokeWidth=2;dashed={int(dashed)};fontFamily=Arial;fontSize=25;labelBackgroundColor=#FFFFFF;')
 ET.SubElement(c,'mxGeometry',relative='1',**{'as':'geometry'})
box('legend','PK: khóa chính | FK: khóa ngoại | PK1 + PK2 + PK3: khóa chính ghép',25,1070,1350,70,'fillColor=none;strokeColor=none;align=left;')
ET.indent(mx,space='  ');ET.ElementTree(mx).write(OUT/'mo-hinh-thiet-ke-vat-ly.drawio',encoding='utf-8',xml_declaration=True)

doc=Document();sec=doc.sections[0]
sec.page_width=Cm(21);sec.page_height=Cm(29.7)
sec.top_margin=Cm(2);sec.bottom_margin=Cm(2);sec.left_margin=Cm(2.5);sec.right_margin=Cm(2)
for name in ['Normal','Title','Heading 1','Heading 2','Heading 3','Caption']:
 s=doc.styles[name];s.font.name='Times New Roman';s.font.color.rgb=RGBColor(0,0,0)
 rf=s._element.get_or_add_rPr().rFonts
 for attr in list(rf.attrib):
  if attr.endswith('Theme'):del rf.attrib[attr]
 for key in ['ascii','hAnsi','eastAsia','cs']:rf.set(qn('w:'+key),'Times New Roman')
 s.font.size=Pt(13)
 s.paragraph_format.space_after=Pt(6)
 s.paragraph_format.line_spacing=1.15
doc.styles['Title'].font.size=Pt(15);doc.styles['Title'].font.bold=True
for n in ['Heading 1','Heading 2','Heading 3']:
 doc.styles[n].font.size=Pt(14);doc.styles[n].font.bold=True
 doc.styles[n].paragraph_format.space_before=Pt(10)
 doc.styles[n].paragraph_format.keep_with_next=True
doc.styles['Caption'].font.italic=True;doc.styles['Caption'].font.bold=False;doc.styles['Caption'].font.size=Pt(12)
for s in doc.styles:
 for border in list(s._element.iter(qn('w:pBdr'))):border.getparent().remove(border)
def para(s='',bold=False):
 p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.JUSTIFY
 p.paragraph_format.first_line_indent=Cm(.7)
 p.add_run(s).bold=bold
 return p
def caption(s):
 p=doc.add_paragraph(s,'Caption');p.alignment=WD_ALIGN_PARAGRAPH.CENTER
 return p
def table(name,number):
 p=doc.add_paragraph('Bảng '+name);p.runs[0].bold=True;p.paragraph_format.keep_with_next=True
 t=doc.add_table(rows=1,cols=5);t.style='Table Grid';t.alignment=WD_TABLE_ALIGNMENT.CENTER;t.autofit=False
 widths=[3.2,2.1,1.4,1.7,8.1]
 for c,w in zip(t.columns,widths):c.width=Cm(w)
 for c,txt,w in zip(t.rows[0].cells,['Tên cột','Kiểu','Khóa','NULL','Ý nghĩa'],widths):c.text=txt;c.width=Cm(w)
 hdr=OxmlElement('w:tblHeader');t.rows[0]._tr.get_or_add_trPr().append(hdr)
 for row in tables[name]:
  cells=t.add_row().cells
  for c,txt,w in zip(cells,row,widths):c.text=txt;c.width=Cm(w)
 for idx,row in enumerate(t.rows):
  pr=row._tr.get_or_add_trPr();pr.append(OxmlElement('w:cantSplit'))
  for j,c in enumerate(row.cells):
   c.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
   mar=OxmlElement('w:tcMar')
   for side in ['left','right']:
    elem=OxmlElement('w:'+side);elem.set(qn('w:w'),'70');elem.set(qn('w:type'),'dxa');mar.append(elem)
   c._tc.get_or_add_tcPr().append(mar)
   for p in c.paragraphs:
    p.paragraph_format.space_after=Pt(3);p.paragraph_format.space_before=Pt(3);p.paragraph_format.line_spacing=1.05
    if j in [1,2,3]:p.alignment=WD_ALIGN_PARAGRAPH.CENTER
    for run in p.runs:run.font.size=Pt(11);run.bold=idx==0
   if idx==0:
    sh=OxmlElement('w:shd');sh.set(qn('w:fill'),'EAEAEA');c._tc.get_or_add_tcPr().append(sh)
 caption(f'Bảng 3.{number}. Cấu trúc bảng {name}')
 return t

doc.add_paragraph('3.5. Thiết kế cơ sở dữ liệu','Title')
para('Ứng dụng nhắc nhở uống thuốc sử dụng cơ sở dữ liệu SQLite cục bộ thông qua thư viện Room. Cơ sở dữ liệu có tên medication_database, phiên bản 7, lưu trữ thông tin thuốc, chuông báo và các lần xác nhận uống thuốc. Ba bảng nghiệp vụ được khai báo trong AppDatabase là medicines, alarms và dose_records.')
doc.add_paragraph('3.5.1. Từ điển dữ liệu','Heading 1')
para('Từ điển dữ liệu mô tả các đối tượng được lưu trữ, thông tin chính của từng đối tượng và quan hệ giữa các đối tượng trong hệ thống.')
para('Thuốc (Medicine, bảng medicines): Gồm mã thuốc, tên thuốc, tần suất sử dụng, ngày bắt đầu, ngày kết thúc, ghi chú và danh sách giờ uống. Đây là đối tượng trung tâm của cơ sở dữ liệu. Một thuốc có thể có nhiều chuông báo và nhiều bản ghi xác nhận uống theo ngày, giờ dự kiến. Danh sách giờ uống được chuyển đổi giữa List<String> trong Java và chuỗi TEXT trong SQLite bằng lớp Converters.')
para('Chuông báo (Alarm, bảng alarms): Gồm mã chuông báo, mã thuốc, thời điểm báo thức, trạng thái bật hoặc tắt và giờ uống dự kiến. Mỗi chuông báo gắn với một thuốc. Quan hệ này được ràng buộc bằng khóa ngoại medicine_id tham chiếu medicineId của bảng medicines. Khi xóa thuốc, các chuông báo liên quan được xóa theo cơ chế ON DELETE CASCADE.')
para('Lịch sử uống thuốc (DoseRecord, bảng dose_records): Gồm mã thuốc, ngày ghi nhận, giờ uống dự kiến, trạng thái đã uống và giờ thực tế xác nhận uống. Bộ ba medicineId, date và scheduledTime tạo thành khóa chính ghép để phân biệt các liều của cùng một thuốc theo ngày và giờ. Việc xác nhận hoặc hoàn tác cập nhật bản ghi ứng với cùng bộ khóa này.')
para('Trong luồng theo dõi lịch sử, HomeActivity đọc các bản ghi theo ngày và ghép chúng với thuốc, giờ uống tương ứng để hiển thị trạng thái. Quan hệ giữa dose_records và medicines hiện được xử lý bằng medicineId trong chương trình; bảng dose_records chưa khai báo khóa ngoại tại tầng SQLite.')

doc.add_page_break()
doc.add_paragraph('3.5.2. Các bảng thiết kế','Heading 1')
para('Các bảng dưới đây sử dụng tên cột và kiểu dữ liệu vật lý do Room sinh. Cột NULL cho biết cơ sở dữ liệu có cho phép giá trị rỗng hay không. PK là khóa chính, FK là khóa ngoại; dấu gạch ngang biểu thị cột không thuộc khóa được khai báo.')
table('medicines',1)
para('medicineId là khóa chính tự tăng. Các trường name, frequency và times được kiểm tra khi lưu thuốc trên giao diện, nhưng vẫn được phép NULL theo lược đồ SQLite. times lưu các giờ theo chuỗi phân cách bằng dấu phẩy, ví dụ 08:00 AM,08:00 PM; đây không phải dữ liệu JSON.')
table('alarms',2)
para('medicine_id có chỉ mục index_alarms_medicine_id. Khóa ngoại tham chiếu medicines(medicineId), có ON DELETE CASCADE và ON UPDATE NO ACTION. scheduled_time giữ chuỗi giờ tương ứng với một phần tử trong medicines.times.')

doc.add_page_break()
table('dose_records',3)
para('Khóa chính của bảng là (medicineId, date, scheduledTime). Ba thành phần đều bắt buộc có giá trị. Một thuốc có thể có nhiều bản ghi cùng ngày nếu scheduledTime khác nhau. Bảng không có cột mã lịch sử tự tăng và không khai báo khóa ngoại cho medicineId.')
doc.add_paragraph('Quy tắc lưu trữ và cập nhật dữ liệu','Heading 2')
para('Ngày ghi nhận được lưu bằng chuỗi yyyy-MM-dd. Giờ uống dự kiến được tạo theo định dạng hh:mm a, chẳng hạn 08:00 AM; giờ thực tế xác nhận uống được lưu theo định dạng HH:mm, chẳng hạn 08:04. Các trường startDate, endDate và time sử dụng số nguyên biểu diễn thời gian tính bằng mili giây.')
para('SQLite lưu các giá trị boolean isTaken và enabled dưới dạng INTEGER. Trong chương trình, true tương ứng với 1 và false tương ứng với 0. isTaken = 0 biểu thị chưa được xác nhận uống hoặc đã hoàn tác; dữ liệu này không phải một trạng thái riêng xác nhận người dùng đã bỏ lỡ liều.')
para('DoseRecordDao sử dụng INSERT với chiến lược OnConflictStrategy.REPLACE. Khi bản ghi có cùng mã thuốc, ngày và giờ dự kiến đã tồn tại, dữ liệu mới thay thế bản ghi đó. Cơ chế này lưu trạng thái hiện tại của mỗi liều, không tạo một chuỗi sự kiện riêng cho từng lần xác nhận và hoàn tác.')
para('Việc xem lịch sử theo ngày sử dụng điều kiện WHERE date = :date. Việc tra cứu một liều cụ thể sử dụng đồng thời medicineId, date và scheduledTime. Ngoài khóa chính ghép, bảng dose_records chưa khai báo thêm chỉ mục riêng cho cột date.')
para('Các cột TEXT không được khai báo giới hạn độ dài cố định trong lược đồ hiện tại. Vì vậy, thiết kế không gán các độ dài VARCHAR tùy ý cho tên thuốc, ghi chú hoặc giờ uống. Các bảng nghiệp vụ cũng chưa khai báo ràng buộc CHECK cho giá trị boolean hoặc định dạng ngày, giờ.')

doc.add_page_break()
doc.add_paragraph('3.5.3. Mô hình thiết kế vật lý','Heading 1')
para('Mô hình dưới đây thể hiện ba bảng nghiệp vụ, các khóa chính và quan hệ dữ liệu đang được sử dụng. Nét liền biểu thị khóa ngoại đã được khai báo trong SQLite; nét đứt biểu thị liên hệ logic do chương trình xử lý.')
p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.add_run().add_picture(str(png),width=Cm(16))
caption('Hình 3.45. Mô hình thiết kế vật lý cơ sở dữ liệu ứng dụng nhắc nhở uống thuốc')
para('Quan hệ medicines - alarms là một - nhiều: một thuốc có thể có từ không đến nhiều chuông báo; mỗi chuông báo tham chiếu một thuốc. Chỉ mục trên medicine_id hỗ trợ truy vấn chuông báo theo thuốc. Khóa ngoại bảo đảm không có chuông báo tham chiếu một mã thuốc không tồn tại.')
para('Quan hệ medicines - dose_records là liên hệ một - nhiều ở mức nghiệp vụ. SQLite chưa áp dụng khóa ngoại hoặc xóa dây chuyền cho liên hệ này. Do đó, bản ghi lịch sử không tự bị xóa theo ràng buộc cơ sở dữ liệu khi xóa thuốc; việc ghép và hiển thị dữ liệu được thực hiện tại tầng ứng dụng.')
para('Room còn tạo bảng nội bộ room_master_table để quản lý thông tin lược đồ. Bảng này không phải bảng nghiệp vụ nên không được đưa vào mô hình trên.')

# Standalone section pagination.
foot=sec.footer.paragraphs[0];foot.alignment=WD_ALIGN_PARAGRAPH.CENTER
field=OxmlElement('w:fldSimple');field.set(qn('w:instr'),'PAGE');foot._p.append(field)
doc.core_properties.title='Thiết kế cơ sở dữ liệu ứng dụng nhắc nhở uống thuốc'
doc.core_properties.subject='Mục 3.5 gồm từ điển dữ liệu, các bảng thiết kế và mô hình vật lý'
doc.core_properties.author=''
dest=OUT/'3-5-thiet-ke-co-so-du-lieu.docx';doc.save(dest)
print(dest)
print('Verified physical field types, nullability, composite primary key and foreign keys against generated Room DDL.')
