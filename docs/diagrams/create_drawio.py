from pathlib import Path
import xml.etree.ElementTree as E

OUT = Path(__file__).parent
doc = E.Element('mxfile', host='app.diagrams.net', version='24.7.17', type='device')
FONT = 'fontFamily=Arial;fontSize=16;fontColor=#111111;'

def page(name, ident):
    d = E.SubElement(doc, 'diagram', id=ident, name=name)
    model = E.SubElement(d, 'mxGraphModel', dx='1600', dy='1050', grid='1', gridSize='10', guides='1', tooltips='1', connect='1', arrows='1', fold='1', page='1', pageScale='1', pageWidth='1600', pageHeight='1050', math='0', shadow='0')
    root = E.SubElement(model, 'root')
    E.SubElement(root, 'mxCell', id='0')
    E.SubElement(root, 'mxCell', id='1', parent='0')
    return root

def box(root, ident, value, x,y,w,h, style='', parent='1'):
    c=E.SubElement(root,'mxCell',id=ident,value=value,style=FONT+style,vertex='1',parent=parent)
    E.SubElement(c,'mxGeometry',x=str(x),y=str(y),width=str(w),height=str(h),**{'as':'geometry'})
    return c

def text(root, ident, value, x,y,w,h, size=16, bold=False, align='center', parent='1'):
    return box(root,ident,value,x,y,w,h,f'text;html=0;whiteSpace=wrap;align={align};verticalAlign=middle;strokeColor=none;fillColor=none;fontSize={size};fontStyle={1 if bold else 0};spacing=5;',parent)

def edge(root,ident,source,target,label='',style='',points=(),label_y=0):
    c=E.SubElement(root,'mxCell',id=ident,value=label,style=FONT+'edgeStyle=orthogonalEdgeStyle;rounded=0;html=0;strokeColor=#333333;strokeWidth=1.5;endArrow=open;endFill=0;labelBackgroundColor=#FFFFFF;whiteSpace=wrap;'+style,edge='1',parent='1',source=source,target=target)
    g=E.SubElement(c,'mxGeometry',relative='1',y=str(label_y),**{'as':'geometry'})
    if points:
        a=E.SubElement(g,'Array',{'as':'points'})
        for x,y in points:E.SubElement(a,'mxPoint',x=str(x),y=str(y))
    return c

def klass(root,ident,name,attrs,methods,x,y,w,h):
    box(root,ident,'',x,y,w,h,'rounded=0;fillColor=#78CDE8;strokeColor=#222222;strokeWidth=1.5;container=1;collapsible=0;')
    box(root,ident+'head','«boundary»\n'+name,0,0,w,65,'whiteSpace=wrap;align=center;verticalAlign=middle;fillColor=#78CDE8;strokeColor=#222222;fontStyle=1;',ident)
    ah=70 if len(attrs)<=2 else 100
    box(root,ident+'attrs','\n'.join(attrs),0,65,w,ah,'whiteSpace=wrap;align=left;verticalAlign=middle;spacingLeft=12;fillColor=none;strokeColor=#222222;fontSize=15;',ident)
    text(root,ident+'ops','\n'.join(methods),8,65+ah,w-16,h-65-ah,15,False,'left',ident)

r=page('1. Biểu đồ lớp màn hình','screen-classes')
text(r,'title','BIỂU ĐỒ LỚP MÀN HÌNH',50,25,1500,42,27,True)
text(r,'subtitle','Use case: Theo dõi lịch sử uống thuốc • Thiết kế đề xuất tầng giao diện',80,75,1440,35,18)
klass(r,'home','HomeActivity',['− selectedDate: String'],['+ openHistory(): void'],65,210,320,205)
klass(r,'history','MedicationHistoryActivity',['− selectedDate: String','− records: List<DoseRecord>','− selectedRecord: DoseRecord'],['+ showHistory(date: String): void','+ openDatePicker(): void','+ onDateSelected(date: String): void','+ openDoseDetail(record): void','+ showEmptyState(): void'],565,180,440,350)
klass(r,'picker','HistoryDatePickerDialog',['− selectedDate: String'],['+ selectDate(date: String): void','+ confirmDate(): void','+ cancel(): void'],1185,195,350,250)
klass(r,'detail','DoseRecordDetailActivity',['− record: DoseRecord','− medicine: Medicine'],['+ showRecord(record, medicine): void','+ close(): void'],565,700,440,230)
klass(r,'empty','EmptyHistoryView',['− displayedDate: String'],['+ show(date: String): void','+ onChooseAnotherDate(): void'],1185,700,350,230)
edge(r,'home-history','home','history','mở màn hình lịch sử','exitX=1;exitY=0.5;entryX=0;entryY=0.38;')
edge(r,'history-picker','history','picker','mở / nhận ngày chọn','dashed=1;exitX=1;exitY=0.25;entryX=0;entryY=0.29;')
edge(r,'history-detail','history','detail','mở chi tiết lần uống','exitX=0.45;exitY=1;entryX=0.45;entryY=0;')
edge(r,'history-empty','history','empty','1                 trạng thái rỗng                 0..1','startArrow=diamond;startFill=1;endArrow=none;exitX=1;exitY=0.8;entryX=0;entryY=0.4;',[(1090,460),(1090,792)])
text(r,'note','Ghi chú thiết kế\n• HomeActivity đã có trong dự án; thao tác mở lịch sử là phần bổ sung đề xuất.\n• Các lớp còn lại là giao diện đề xuất theo bản hình dung màn hình.\n• EmptyHistoryView là trạng thái con của màn hình lịch sử, không phải một Activity riêng.\n• DoseRecord và Medicine là kiểu dữ liệu hiện có; DAO và cơ sở dữ liệu không thuộc phạm vi biểu đồ màn hình.',55,690,455,255,15,False,'left')
text(r,'legend','«boundary»: lớp giao diện  •  Mũi tên nét liền: điều hướng  •  Nét đứt: sử dụng hộp thoại  •  Hình thoi đặc: thành phần được sở hữu',90,970,1420,40,14)

r=page('2. Biểu đồ cộng tác màn hình','screen-collaboration')
text(r,'title','BIỂU ĐỒ CỘNG TÁC MÀN HÌNH',50,25,1500,42,27,True)
text(r,'subtitle','Use case: Theo dõi lịch sử uống thuốc • Các thông điệp được đánh số theo luồng thao tác',80,75,1440,35,18)
box(r,'actor','Người dùng',70,430,95,150,'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;fillColor=#FFFFFF;strokeColor=#222222;')
def obj(ident,value,x,y,w=340,h=120):
    box(r,ident,'«boundary»\n'+value,x,y,w,h,'rounded=0;whiteSpace=wrap;align=center;verticalAlign=middle;fillColor=#F6F6F6;strokeColor=#222222;fontStyle=4;')
obj('home',':HomeActivity',230,185,330)
obj('history',':MedicationHistoryActivity',660,445,380,135)
obj('picker',':HistoryDatePickerDialog',1180,185,355)
obj('detail',':DoseRecordDetailActivity',230,740,350)
obj('empty',':EmptyHistoryView',1180,740,355)
base='edgeStyle=none;endArrow=none;startArrow=none;'
edge(r,'u-home','actor','home','1 → Chọn “Lịch sử uống thuốc”',base+'exitX=0.5;exitY=0;entryX=0;entryY=0.5;',[(117,245)],-25)
edge(r,'home-history','home','history','1.1 → Mở lịch sử theo ngày mặc định',base+'exitX=1;exitY=0.75;entryX=0.2;entryY=0;',[], -30)
edge(r,'u-history','actor','history','2 → Chọn ngày xem lịch sử\n3 → Chọn một lần uống',base+'exitX=1;exitY=0.5;entryX=0;entryY=0.5;',[],-28)
edge(r,'history-picker','history','picker','2.1 → Mở bộ chọn ngày\n2.2 ← Áp dụng ngày đã chọn',base+'exitX=0.8;exitY=0;entryX=0;entryY=0.75;',[], -32)
edge(r,'history-detail','history','detail','3.1 → Hiển thị chi tiết lần uống\n3.2 ← Quay lại danh sách',base+'exitX=0.2;exitY=1;entryX=1;entryY=0.25;',[], -35)
edge(r,'history-empty','history','empty','2.3 → [Không có dữ liệu] Hiển thị rỗng\n2.4 ← Yêu cầu chọn ngày khác',base+'exitX=0.8;exitY=1;entryX=0;entryY=0.25;',[], 35)
text(r,'refresh','Sau 2.2:\n[Có dữ liệu] Cập nhật danh sách và tổng số liều.\n[Không có dữ liệu] Thực hiện 2.3.\nSau 2.4: lặp lại 2.1 để chọn ngày khác.',665,790,400,110,15,False,'left')
text(r,'legend','Quy ước: trên mỗi liên kết, → gửi đến đối tượng đích; ← trả về đối tượng nguồn.\nSố 1, 2, 3 là thao tác của người dùng; các số thập phân là thông điệp xử lý giao diện tương ứng.',140,935,1320,65,16)
text(r,'scope','Phạm vi: tương tác giữa các màn hình; không mô tả truy vấn DAO hoặc luồng lưu dữ liệu.',140,1000,1320,30,14)

E.indent(doc,space='  ')
out=OUT/'theo-doi-lich-su-uong-thuoc.drawio'
E.ElementTree(doc).write(out,encoding='utf-8',xml_declaration=True)
parsed=E.parse(out)
for diagram in parsed.getroot().findall('diagram'):
    cells=diagram.findall('./mxGraphModel/root/mxCell')
    ids=[c.get('id') for c in cells]
    assert len(ids)==len(set(ids))
    for c in cells:
        for attr in ('source','target','parent'):
            if c.get(attr): assert c.get(attr) in ids
    print(diagram.get('name'),':',len(cells),'thành phần XML hợp lệ')
print(out)
