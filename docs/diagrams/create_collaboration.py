from pathlib import Path
import math, copy
import xml.etree.ElementTree as E
from PIL import Image, ImageDraw, ImageFont

OUT=Path(__file__).parent
W,H=2000,1300
BLUE='#78CDE8'
nodes={
 'home':(350,560,320,130,'Màn hình trang chủ','trangChu : HomeActivity'),
 'history':(900,560,410,150,'Màn hình lịch sử uống thuốc','lichSu : MedicationHistoryActivity'),
 'picker':(1550,560,380,130,'Hộp thoại chọn ngày','chonNgay : HistoryDatePickerDialog'),
 'empty':(900,160,410,130,'Trạng thái chưa có lịch sử','rong : EmptyHistoryView'),
 'detail':(900,1040,410,130,'Màn hình chi tiết lần uống','chiTiet : DoseRecordDetailActivity'),
}
# Message labels have explicit rectangles; arrow paths use the same coordinates in both formats.
edges=[
 ('e1','user','home',[(165,615),(350,615)],'1: Chọn “Lịch sử”',(130,485,210,60)),
 ('e11','home','history',[(670,600),(900,600)],'1.1: Mở lịch sử',(670,535,230,55)),
 ('euser','user','history',[(85,615),(25,615),(25,880),(1040,880),(1040,710)],'2: Chọn ngày xem lịch sử\n3: Chọn một lần uống',(370,800,430,65)),
 ('e21','history','picker',[(1310,600),(1550,600)],'2.1: Mở bộ chọn ngày',(1312,530,237,65)),
 ('e22','picker','history',[(1550,665),(1310,665)],'2.2: Áp dụng ngày',(1312,680,237,60)),
 ('e23','history','empty',[(1030,560),(1030,290)],'[Không có dữ liệu]\n2.3: Hiển thị trạng thái rỗng',(680,365,330,80)),
 ('e24','empty','history',[(1200,290),(1200,560)],'2.4: Chọn ngày khác\n(lặp lại 2.1)',(1225,365,325,80)),
 ('e31','history','detail',[(1200,710),(1200,1040)],'3.1: Mở chi tiết lần uống',(1230,900,340,65)),
 ('e32','detail','history',[(1120,1040),(1120,710)],'3.2: Quay lại lịch sử',(785,955,315,60)),
]
doc=E.Element('mxfile',host='app.diagrams.net',type='device',version='24.7.17')
page=E.SubElement(doc,'diagram',id='screen-collaboration',name='Biểu đồ cộng tác màn hình')
model=E.SubElement(page,'mxGraphModel',grid='1',gridSize='10',guides='1',page='1',pageScale='1',pageWidth=str(W),pageHeight=str(H),dx=str(W),dy=str(H))
root=E.SubElement(model,'root')
E.SubElement(root,'mxCell',id='0')
E.SubElement(root,'mxCell',id='1',parent='0')
def cell(ident,value,rect,style):
 c=E.SubElement(root,'mxCell',id=ident,value=value,parent='1',vertex='1',style='fontFamily=Arial;fontColor=#111111;html=0;whiteSpace=wrap;'+style)
 x,y,w,h=rect
 E.SubElement(c,'mxGeometry',x=str(x),y=str(y),width=str(w),height=str(h),**{'as':'geometry'})
 return c
def label(ident,value,rect,size=22,bold=False):
 return cell(ident,value,rect,f'text;strokeColor=none;fillColor=#FFFFFF;align=center;verticalAlign=middle;fontSize={size};fontStyle={1 if bold else 0};spacing=3;')
label('title','BIỂU ĐỒ CỘNG TÁC MÀN HÌNH',(100,25,1800,55),34,True)
label('subtitle','Use case: Theo dõi lịch sử uống thuốc',(100,85,1800,40),25)
cell('user','Người dùng',(85,550,80,170),'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;fontSize=22;strokeColor=#222222;fillColor=#FFFFFF;')
for ident,(x,y,w,h,title,name) in nodes.items():
 cell(ident,'',(x,y,w,h),f'rounded=0;fillColor={BLUE};strokeColor=#222222;strokeWidth=1.5;')
 label(ident+'stereo','«boundary»',(x+5,y+10,w-10,27),19).set('style','text;html=0;fillColor=none;strokeColor=none;align=center;fontFamily=Arial;fontSize=19;')
 label(ident+'title',title,(x+5,y+40,w-10,35),21,True).set('style','text;html=0;fillColor=none;strokeColor=none;align=center;fontFamily=Arial;fontSize=21;fontStyle=1;')
 label(ident+'name',name,(x+3,y+83,w-6,35),18).set('style','text;html=0;fillColor=none;strokeColor=none;align=center;fontFamily=Arial;fontSize=18;fontStyle=4;')
for ident,source,target,points,value,rect in edges:
 # Explicit terminal points make the paths deterministic and keep labels clear of objects.
 source_rect=nodes[source][:4] if source!='user' else (85,550,80,170)
 target_rect=nodes[target][:4]
 sx,sy,sw,sh=source_rect; tx,ty,tw,th=target_rect
 anchors=f'exitX={(points[0][0]-sx)/sw};exitY={(points[0][1]-sy)/sh};entryX={(points[-1][0]-tx)/tw};entryY={(points[-1][1]-ty)/th};exitPerimeter=0;entryPerimeter=0;'
 c=E.SubElement(root,'mxCell',id=ident,parent='1',edge='1',source=source,target=target,style='edgeStyle=none;rounded=0;strokeColor=#222222;strokeWidth=1.6;endArrow=open;endSize=12;endFill=0;'+anchors)
 g=E.SubElement(c,'mxGeometry',relative='1',**{'as':'geometry'})
 for name,point in [('sourcePoint',points[0]),('targetPoint',points[-1])]:E.SubElement(g,'mxPoint',x=str(point[0]),y=str(point[1]),**{'as':name})
 if len(points)>2:
  a=E.SubElement(g,'Array',**{'as':'points'})
  for x,y in points[1:-1]:E.SubElement(a,'mxPoint',x=str(x),y=str(y))
 label(ident+'label',value,rect,21)
note='Sau 2.2: có dữ liệu → cập nhật danh sách; không có dữ liệu → thực hiện 2.3.\nCác lớp giao diện lịch sử/chi tiết là thiết kế đề xuất; HomeActivity đã có trong dự án.'
label('note',note,(160,1200,1680,70),20)
E.indent(doc,space='  ')
dest=OUT/'bieu-do-cong-tac-man-hinh.drawio'
E.ElementTree(doc).write(dest,encoding='utf-8',xml_declaration=True)
# Keep the two-page document consistent with the standalone collaboration diagram.
combined=OUT/'theo-doi-lich-su-uong-thuoc.drawio'
if combined.exists():
 tree=E.parse(combined)
 for old in list(tree.getroot().findall('diagram')):
  if old.get('id')=='screen-collaboration':tree.getroot().remove(old)
 tree.getroot().append(copy.deepcopy(page))
 E.indent(tree,space='  ')
 tree.write(combined,encoding='utf-8',xml_declaration=True)

scale=1.5
im=Image.new('RGB',(int(W*scale),int(H*scale)),'white')
d=ImageDraw.Draw(im)
def font(size,bold=False):return ImageFont.truetype('C:/Windows/Fonts/'+('arialbd.ttf' if bold else 'arial.ttf'),round(size*scale))
def text(value,rect,size=22,bold=False):
 x,y,w,h=rect; f=font(size,bold)
 bounds=d.multiline_textbbox((0,0),value,font=f,spacing=7*scale,align='center')
 tw,th=bounds[2]-bounds[0],bounds[3]-bounds[1]
 d.multiline_text(((x+w/2)*scale-tw/2,(y+h/2)*scale-th/2-bounds[1]),value,font=f,fill='#111111',spacing=7*scale,align='center')
text('BIỂU ĐỒ CỘNG TÁC MÀN HÌNH',(100,25,1800,55),34,True)
text('Use case: Theo dõi lịch sử uống thuốc',(100,85,1800,40),25)
for ident,(x,y,w,h,title,name) in nodes.items():
 d.rectangle((x*scale,y*scale,(x+w)*scale,(y+h)*scale),fill=BLUE,outline='#222222',width=2)
 text('«boundary»',(x+5,y+10,w-10,27),19)
 text(title,(x+5,y+40,w-10,35),21,True)
 text(name,(x+3,y+83,w-6,35),18)
 length=d.textlength(name,font=font(18))
 d.line(((x+w/2)*scale-length/2,(y+113)*scale,(x+w/2)*scale+length/2,(y+113)*scale),fill='#222222',width=1)
# UML actor.
d.ellipse((105*scale,551*scale,145*scale,591*scale),outline='#222222',width=3)
for a,b in [((125,591),(125,659)),((85,615),(165,615)),((125,659),(85,715)),((125,659),(165,715))]:d.line(tuple(v*scale for p in [a,b] for v in p),fill='#222222',width=3)
text('Người dùng',(40,725,180,40),22)
for ident,source,target,points,value,rect in edges:
 d.line([(x*scale,y*scale) for x,y in points],fill='#222222',width=2)
 x,y=points[-1]; px,py=points[-2]; angle=math.atan2(y-py,x-px)
 for sign in [-1,1]:
  end=(x-13*math.cos(angle+sign*.45),y-13*math.sin(angle+sign*.45))
  d.line((x*scale,y*scale,end[0]*scale,end[1]*scale),fill='#222222',width=2)
 x,y,w,h=rect
 d.rectangle((x*scale,y*scale,(x+w)*scale,(y+h)*scale),fill='white')
 text(value,rect,21)
text(note,(160,1200,1680,70),20)
png=OUT/'bieu-do-cong-tac-man-hinh.png'
im.save(png)
assert len(E.parse(dest).findall('.//mxCell[@edge="1"]'))==9
print(dest)
print(png)
