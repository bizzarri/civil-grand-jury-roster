package com.mauri.juryroster;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.charset.*;
import java.time.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public final class CourtFile {
 public static final int MAX_FILE=8*1024*1024;
 public static final class Sheet {public String name;public List<List<String>> rows=new ArrayList<>();public int header=-1;public String role="Juror",status="Active";}
 public static List<Sheet> read(byte[] bytes) throws Exception {
  if(bytes.length>MAX_FILE)throw new IOException("File exceeds 8 MB.");
  if(bytes.length>2&&bytes[0]=='P'&&bytes[1]=='K')return xlsx(bytes);
  String text=new String(bytes,StandardCharsets.UTF_8);if(text.indexOf('\0')>=0)throw new IOException("Use an XLSX or UTF-8 CSV file; legacy XLS is not supported.");
  Sheet s=new Sheet();s.name="CSV roster";s.rows=csv(text);s.header=header(s.rows);if(s.header<0)throw new IOException("No First Name and Last Name headings found. Use separate first/last name columns.");return Arrays.asList(s);
 }
 private static Document xml(byte[] data) throws Exception {
  String text;
  if(data.length>1&&((data[0]&255)==255||(data[0]&255)==254))text=new String(data,StandardCharsets.UTF_16);
  else text=new String(data,StandardCharsets.UTF_8);
  if(text.toUpperCase(Locale.ROOT).contains("<!DOCTYPE")||text.toUpperCase(Locale.ROOT).contains("<!ENTITY"))throw new IOException("XML entities are not supported.");
  DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setExpandEntityReferences(false);
  DocumentBuilder b=f.newDocumentBuilder();b.setEntityResolver((publicId,systemId)->{throw new org.xml.sax.SAXException("External XML resources are not allowed.");});
  return b.parse(new InputSource(new StringReader(text)));
 }
 private static List<Sheet> xlsx(byte[] file) throws Exception {
  Map<String,byte[]> parts=new HashMap<>();int total=0;
  try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(file))){ZipEntry e;int entries=0;while((e=z.getNextEntry())!=null){if(++entries>2000)throw new IOException("Too many workbook parts.");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=z.read(buf))!=-1){total+=n;if(total>32*1024*1024)throw new IOException("Expanded workbook exceeds 32 MB.");out.write(buf,0,n);}parts.put(e.getName(),out.toByteArray());}}
  if(!parts.containsKey("xl/workbook.xml")||!parts.containsKey("xl/_rels/workbook.xml.rels"))throw new IOException("Not a supported XLSX workbook.");
  List<String> strings=new ArrayList<>();if(parts.containsKey("xl/sharedStrings.xml")){NodeList si=xml(parts.get("xl/sharedStrings.xml")).getElementsByTagNameNS("*","si");for(int i=0;i<si.getLength();i++)strings.add(text((Element)si.item(i),"t"));}
  Map<String,String> rel=new HashMap<>();NodeList rs=xml(parts.get("xl/_rels/workbook.xml.rels")).getElementsByTagNameNS("*","Relationship");
  for(int i=0;i<rs.getLength();i++){Element e=(Element)rs.item(i);if(!e.getAttribute("TargetMode").equals("External")){String target=e.getAttribute("Target");String path=target.startsWith("/")?target.substring(1):"xl/"+target;rel.put(e.getAttribute("Id"),path.replace("xl/./","xl/"));}}
  Document wb=xml(parts.get("xl/workbook.xml"));boolean date1904=false;NodeList props=wb.getElementsByTagNameNS("*","workbookPr");if(props.getLength()>0){String d=((Element)props.item(0)).getAttribute("date1904");date1904=d.equals("1")||d.equals("true");}
  List<Sheet> sheets=new ArrayList<>();NodeList names=wb.getElementsByTagNameNS("*","sheet");
  for(int i=0;i<names.getLength();i++){
   Element meta=(Element)names.item(i);String id=meta.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id"),path=rel.get(id);if(path==null||!parts.containsKey(path))throw new IOException("Worksheet relationship not found.");
   Sheet s=new Sheet();s.name=meta.getAttribute("name");String title=s.name.toLowerCase(Locale.ROOT);if(title.contains("alternate"))s.role="Alternate";if(title.contains("resign"))s.status="Resigned";
   NodeList rows=xml(parts.get(path)).getElementsByTagNameNS("*","row");if(rows.getLength()>10000)throw new IOException("Worksheet exceeds 10,000 rows.");
   for(int r=0;r<rows.getLength();r++){Element row=(Element)rows.item(r);List<String> cells=new ArrayList<>();NodeList cs=row.getElementsByTagNameNS("*","c");for(int c=0;c<cs.getLength();c++){Element cell=(Element)cs.item(c);int col=column(cell.getAttribute("r"));if(col>=128)continue;while(cells.size()<=col)cells.add("");String type=cell.getAttribute("t"),value=text(cell,"v");if(type.equals("s")){int index=Integer.parseInt(value);if(index<0||index>=strings.size())throw new IOException("Invalid shared string index.");value=strings.get(index);}else if(type.equals("inlineStr"))value=text(cell,"t");cells.set(col,value);}
    s.rows.add(cells);
   }
   s.header=header(s.rows);if(s.header>=0){
    List<String> h=s.rows.get(s.header);int dateCol=-1;for(int c=0;c<h.size();c++)if(norm(h.get(c)).equals("resigneddate")||norm(h.get(c)).equals("departuredate"))dateCol=c;
    if(dateCol>=0)for(int r=s.header+1;r<s.rows.size();r++){List<String> row=s.rows.get(r);if(row.size()>dateCol&&row.get(dateCol).matches("[0-9]{4,6}(\\.[0-9]+)?")){long days=(long)Double.parseDouble(row.get(dateCol));row.set(dateCol,(date1904?LocalDate.of(1904,1,1):LocalDate.of(1899,12,30)).plusDays(days).toString());}}
    sheets.add(s);
   }
  }
  if(sheets.isEmpty())throw new IOException("No roster sheet with First Name and Last Name headings was found.");return sheets;
 }
 private static int column(String ref){int n=0;for(char c:ref.toCharArray()){if(c>='A'&&c<='Z')n=n*26+c-'A'+1;else break;}return Math.max(0,n-1);}
 private static String text(Element e,String tag){NodeList nodes=e.getElementsByTagNameNS("*",tag);StringBuilder b=new StringBuilder();for(int i=0;i<nodes.getLength();i++)b.append(nodes.item(i).getTextContent());return b.toString();}
 public static String norm(String s){return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
 public static int header(List<List<String>> rows){for(int r=0;r<Math.min(rows.size(),40);r++){Set<String> values=new HashSet<>();for(String v:rows.get(r))values.add(norm(v));if(values.contains("firstname")&&values.contains("lastname"))return r;}return -1;}
 public static String field(String header){
  String h=norm(header);String[] aliases={"courtid","firstname","lastname","role","status","emailaddress","newcurrentemailaddress","phone","street","spc","city","state","zip","position","badgenumber","gjroomkey","resigneddate","departurereason","startdate"};
  for(int i=0;i<aliases.length;i++)if(h.equals(aliases[i])||h.equals(norm(RosterCore.LABELS[i]))||h.equals(norm(RosterCore.FIELDS[i])))return RosterCore.FIELDS[i];
  return "";
 }
 public static List<RosterCore.SourceRow> extract(Sheet s,Map<Integer,String> mapping){
  List<RosterCore.SourceRow> out=new ArrayList<>();
  for(int r=s.header+1;r<s.rows.size();r++){
   List<String> cells=s.rows.get(r);RosterCore.SourceRow row=new RosterCore.SourceRow();row.source=s.name+" · data row "+(r-s.header);
   row.fields.put("role",s.role);row.fields.put("status",s.status);
   for(Map.Entry<Integer,String> m:mapping.entrySet()){if(m.getValue().isEmpty())continue;String v=m.getKey()<cells.size()?cells.get(m.getKey()).trim():"";row.fields.put(m.getValue(),v);}
   String first=row.fields.getOrDefault("firstName",""),last=row.fields.getOrDefault("lastName","");if(first.isEmpty()&&last.isEmpty())continue;
   for(String f:new String[]{"personalEmail","countyEmail"})if(!RosterCore.email(row.fields.getOrDefault(f,""))){row.warning+=RosterCore.label(f)+" has non-email source text (“"+row.fields.get(f)+"”); not proposed. ";row.fields.remove(f);}
   for(String f:new String[]{"departureDate","startDate"}){String value=row.fields.getOrDefault(f,"");if(!value.isEmpty())try{LocalDate.parse(value);}catch(Exception invalid){row.warning+=RosterCore.label(f)+" contains non-date source text (“"+value+"”); not proposed. ";row.fields.remove(f);}}
   out.add(row);
  }return out;
 }
 public static List<List<String>> csv(String input) throws IOException {
  String text=input.startsWith("\ufeff")?input.substring(1):input;char delimiter=text.indexOf('\t')>=0&&text.indexOf('\t')<Math.max(text.indexOf('\n'),0)?'\t':',';
  List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder cell=new StringBuilder();boolean quoted=false;
  for(int i=0;i<text.length();i++){char c=text.charAt(i);if(c=='"'){if(quoted&&i+1<text.length()&&text.charAt(i+1)=='"'){cell.append('"');i++;}else if(quoted||cell.length()==0)quoted=!quoted;else cell.append(c);}
   else if(c==delimiter&&!quoted){row.add(cell.toString());cell.setLength(0);}else if((c=='\n'||c=='\r')&&!quoted){if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;row.add(cell.toString());rows.add(row);row=new ArrayList<>();cell.setLength(0);}else cell.append(c);
   if(rows.size()>10000||row.size()>128||cell.length()>100000)throw new IOException("Spreadsheet is too large.");
  }
  if(quoted)throw new IOException("CSV contains an unclosed quoted field.");if(cell.length()>0||!row.isEmpty()){row.add(cell.toString());rows.add(row);}return rows;
 }
 public static String csvCell(String s){String value=s==null?"":s;if(value.matches("(?s)^[\\s]*[=+@-].*"))value="'"+value;return "\""+value.replace("\"","\"\"")+"\"";}
}

