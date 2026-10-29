package com.mauri.juryroster;
import java.util.*;
import java.time.*;

/** Domain logic shared by Android and the JVM regression tests. */
public final class RosterCore {
 public static final String[] FIELDS={"courtId","firstName","lastName","role","status","personalEmail","countyEmail","phone","street","unit","city","state","zip","position","badge","roomKey","departureDate","departureReason","startDate"};
 public static final String[] LABELS={"Court ID","First name","Last name","Roster role","Service status","Previous / personal email","Current county email","Phone","Street","Unit","City","State","ZIP","Court position / alternate number","Badge number","Room key","Departure date","Departure reason","Start date"};
 public static final String[] CHECKS={"Oath completed","Orientation completed","Contact details checked","County account accessible"};
 public static final String[] ROSTER_GROUPS={"Active jurors","Alternates","Resigned","Other departures"};
 public final Set<String> rosterGroups=new LinkedHashSet<>(Arrays.asList(ROSTER_GROUPS));
 public boolean visibleInRoster(Person p){String group=p.active()?(p.get("role").equals("Alternate")?"Alternates":"Active jurors"):(p.get("status").equals("Resigned")?"Resigned":"Other departures");return rosterGroups.contains(group);}
 public void setRosterGroups(Set<String> groups){for(String group:groups)if(!Arrays.asList(ROSTER_GROUPS).contains(group))throw new IllegalArgumentException("Unknown roster filter.");rosterGroups.clear();rosterGroups.addAll(groups);}
 public String appName="Civil Grand Jury Roster";
 public String[] titles={"Foreperson","Foreperson pro tem","Recording Secretary","Corresponding Secretary","Assistant Secretary","","",""};
 public String term="2026-2027",foreperson="",handoffNotes="";
 public int seats=19,alternateSlots=20;
 public String countyDomain="amadorcounty.gov",countyPattern="{first_initial}{last}";
 public final List<Person> people=new ArrayList<>();
 public final List<Task> tasks=new ArrayList<>();
 public final List<Event> events=new ArrayList<>();
 public final List<Proposal> review=new ArrayList<>();
 public String importSource="",importedAt="";
 public static String emailPart(String name){
  return java.text.Normalizer.normalize(clean(name),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
 }
 public static String generatedEmail(String first,String last,String domain,String pattern){
  String f=emailPart(first),l=emailPart(last),d=clean(domain).toLowerCase(Locale.ROOT),t=clean(pattern);
  if(f.isEmpty()||l.isEmpty())throw new IllegalArgumentException("First and last names must contain letters or numbers to generate an address. Enter unusual names manually.");
  if(!d.matches("(?=.{1,253}$)[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+"))throw new IllegalArgumentException("Enter a county domain such as amadorcounty.gov, without @.");
  if(!t.contains("{last}")||!(t.contains("{first}")||t.contains("{first_initial}")))throw new IllegalArgumentException("The pattern must contain {last} and either {first} or {first_initial}.");
  String local=t.replace("{first_initial}",f.substring(0,1)).replace("{first}",f).replace("{last}",l).toLowerCase(Locale.ROOT);
  if(local.length()>64||!local.matches("[a-z0-9]+(?:[._-][a-z0-9]+)*"))throw new IllegalArgumentException("Use only the name tokens and single dots, hyphens or underscores between them.");
  return local+"@"+d;
 }
 public String suggestCountyEmail(Person p){
  String address=generatedEmail(p.get("firstName"),p.get("lastName"),countyDomain,countyPattern);
  for(Person other:people)if(!other.id.equals(p.id)&&key(address).equals(key(other.get("countyEmail"))))throw new IllegalArgumentException("Generated address is already assigned to another member. Confirm the correct address with the county; no numeric suffix has been invented.");
  return address;
 }
 public String titleText(Person p){List<String> labels=new ArrayList<>();for(String id:p.titleIds){int slot=Integer.parseInt(id);if(slot>=0&&slot<titles.length&&!titles[slot].isEmpty())labels.add(titles[slot]);}return String.join(" · ",labels);}
 public void configure(String name,String years,int regular,int alternate,String[] names){
  name=clean(name);years=clean(years);if(name.isEmpty()||name.length()>60)throw new IllegalArgumentException("Enter an app name of 1–60 characters.");
  if(!years.matches("[0-9]{4}[-–][0-9]{4}"))throw new IllegalArgumentException("Enter term years as YYYY-YYYY, such as 2026-2027.");
  int start=Integer.parseInt(years.substring(0,4)),end=Integer.parseInt(years.substring(5));if(end!=start+1)throw new IllegalArgumentException("The end year must follow the start year for a year-long term.");
  if(regular<1||regular>100||alternate<0||alternate>100)throw new IllegalArgumentException("Enter 1–100 regular seats and 0–100 alternate slots.");
  if(names.length!=8)throw new IllegalArgumentException("Eight title slots are required.");Set<String> unique=new HashSet<>();String[] normalized=new String[8];
  for(int i=0;i<8;i++){String title=clean(names[i]);if(title.length()>60)throw new IllegalArgumentException("Titles must be 60 characters or fewer.");if(!title.isEmpty()&&!unique.add(key(title)))throw new IllegalArgumentException("Title names must be unique.");if(title.isEmpty())for(Person p:people)if(p.titleIds.contains(String.valueOf(i)))throw new IllegalArgumentException("Title slot "+(i+1)+" is still assigned. Unassign it from members before clearing it.");normalized[i]=title;}
  appName=name;term=years.replace('–','-');seats=regular;alternateSlots=alternate;titles=normalized;log("","Configuration updated",name+" · "+term+" · title names updated");
 }
 public static String now(){return Instant.now().toString();}
 public static String clean(String s){return s==null?"":s.trim();}
 public static String key(String s){return clean(s).toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
 public static boolean email(String s){return s.isEmpty()||s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");}
 public static String label(String field){for(int i=0;i<FIELDS.length;i++)if(FIELDS[i].equals(field))return LABELS[i];return field;}
 public static Map<String,String> defaults(){Map<String,String> c=new LinkedHashMap<>();for(String f:FIELDS)c.put(f,"");c.put("role","Juror");c.put("status","Active");return c;}
 public static final class Person {
  public String id=UUID.randomUUID().toString(),note="",emailTest="Not tested",emailTestAt="",createdAt=now(),updatedAt=now();
  public Map<String,String> court=defaults();
  public final Set<String> titleIds=new LinkedHashSet<>();
  public final Set<String> onboarding=new LinkedHashSet<>(),lists=new LinkedHashSet<>(),previousEmails=new LinkedHashSet<>();
  public String get(String f){return court.getOrDefault(f,"");}
  public String name(){return clean(get("firstName")+" "+get("lastName"));}
  public boolean active(){return get("status").equals("Active");}
 }
 public static final class Task {
  public String id=UUID.randomUUID().toString(),personId="",list="",action="Add",status="Pending",oldEmail="",newEmail="",note="",createdAt=now(),completedAt="";
 }
 public static final class Event {
  public String at=now(),personId="",action="",detail="";
  public Event(){} public Event(String p,String a,String d){personId=p;action=a;detail=d;}
 }
 public static final class SourceRow {
  public Map<String,String> fields=new LinkedHashMap<>();
  public String source="",warning="";
 }
 public static final class Proposal {
  public String id=UUID.randomUUID().toString(),personId="",kind="New",source="",warning="",decision="Pending";
  public Map<String,String> incoming=new LinkedHashMap<>(),before=new LinkedHashMap<>();
  public String name(){return clean(incoming.getOrDefault("firstName","")+" "+incoming.getOrDefault("lastName",""));}
 }
 public Person find(String id){for(Person p:people)if(p.id.equals(id))return p;return null;}
 public int count(String role){int n=0;for(Person p:people)if(p.active()&&p.get("role").equals(role))n++;return n;}
 public int pending(){int n=0;for(Proposal p:review)if(p.decision.equals("Pending"))n++;return n;}
 public int resigned(){int n=0;for(Person p:people)if(p.get("status").equals("Resigned"))n++;return n;}
 public void log(String p,String a,String d){events.add(new Event(p,a,d));}
 public void validate(Person p){
  for(String id:p.titleIds){int slot;try{slot=Integer.parseInt(id);}catch(Exception e){throw new IllegalArgumentException("Invalid title slot.");}if(slot<0||slot>=titles.length||titles[slot].isEmpty())throw new IllegalArgumentException("Assigned title slot is not configured.");}
  if(p.name().isEmpty())throw new IllegalArgumentException("Enter at least a first or last name.");
  if(!Arrays.asList("Juror","Alternate").contains(p.get("role")))throw new IllegalArgumentException("Choose Juror or Alternate.");
  if(!Arrays.asList("Active","Resigned","Excused","Completed").contains(p.get("status")))throw new IllegalArgumentException("Choose a valid service status.");
  for(String f:new String[]{"personalEmail","countyEmail"})if(!email(p.get(f)))throw new IllegalArgumentException(label(f)+" is not a valid email address.");
  for(String f:new String[]{"departureDate","startDate"})if(!p.get(f).isEmpty())try{LocalDate.parse(p.get(f));}catch(Exception e){throw new IllegalArgumentException(label(f)+" must use YYYY-MM-DD.");}
  if(p.emailTest.equals("Confirmed")&&p.get("countyEmail").isEmpty())throw new IllegalArgumentException("Enter a county email before confirming it.");
  String county=key(p.get("countyEmail"));if(!county.isEmpty())for(Person other:people)if(!other.id.equals(p.id)&&county.equals(key(other.get("countyEmail"))))throw new IllegalArgumentException("County email is already assigned to another member.");
  String cid=key(p.get("courtId"));if(!cid.isEmpty())for(Person other:people)if(!other.id.equals(p.id)&&cid.equals(key(other.get("courtId"))))throw new IllegalArgumentException("Court ID is already assigned to another member.");
 }
 public void savePerson(Person p,Map<String,String> old,String oldTest){
  if(old!=null&&!old.getOrDefault("countyEmail","").equals(p.get("countyEmail"))){p.emailTest="Not tested";p.emailTestAt="";}
  validate(p);
  if(old!=null){
   for(String f:FIELDS)if(!Objects.equals(old.get(f),p.get(f)))log(p.id,"Field changed",label(f)+": "+old.getOrDefault(f,"")+" → "+p.get(f));
   String previousPersonal=old.getOrDefault("personalEmail","");if(!previousPersonal.isEmpty()&&!previousPersonal.equals(p.get("personalEmail")))p.previousEmails.add(previousPersonal);
   String before=old.getOrDefault("countyEmail","");
   if(!before.equals(p.get("countyEmail"))){
    if(!before.isEmpty())p.previousEmails.add(before);
    p.emailTest="Not tested";p.emailTestAt="";
    for(String list:p.lists)queue(p,"Update",list,before,p.get("countyEmail"),"County email changed; confirmation reset.");
   }
   if(old.getOrDefault("status","Active").equals("Active")&&!p.active()){
    for(Task t:tasks)if(t.personId.equals(p.id)&&t.status.equals("Pending")&&!t.action.equals("Remove"))t.status="Cancelled";
    for(String list:p.lists)queue(p,"Remove",list,before,"","Member departed. Complete after the actual list is changed.");
   }
  }else {people.add(p);log(p.id,"Member added",p.name()+" · "+p.get("role")+" · "+p.get("status"));}
  if(!p.emailTest.equals(oldTest))log(p.id,"County email test",p.emailTest);
  p.updatedAt=now();
 }
 public void queue(Person p,String action,String list,String oldEmail,String newEmail,String note){
  list=clean(list);if(list.isEmpty())throw new IllegalArgumentException("Enter a distribution-list name.");
  for(Task t:tasks)if(t.personId.equals(p.id)&&t.list.equals(list)&&t.action.equals(action)&&t.status.equals("Pending")&&t.newEmail.equals(newEmail))return;
  Task t=new Task();t.personId=p.id;t.action=action;t.list=list;t.oldEmail=oldEmail;t.newEmail=newEmail;t.note=note;tasks.add(t);log(p.id,"List task queued",action+" · "+list);
 }
 public void finishTask(Task t,boolean done){
  for(Task stored:tasks)if(stored.id.equals(t.id)){t=stored;break;}
  if(!t.status.equals("Pending"))return;
  Person p=find(t.personId);if(done&&p!=null){if(t.action.equals("Add")&&!p.active())throw new IllegalArgumentException("A departed member cannot be added to a list.");if(t.action.equals("Remove"))p.lists.remove(t.list);else p.lists.add(t.list);}
  t.status=done?"Done":"Cancelled";t.completedAt=now();log(t.personId,"List task "+t.status,t.action+" · "+t.list+" · "+t.oldEmail+" → "+t.newEmail);
 }
 private List<Person> matches(SourceRow r){
  Set<Person> hits=new LinkedHashSet<>();String id=key(r.fields.get("courtId"));
  if(!id.isEmpty()){for(Person p:people)if(id.equals(key(p.get("courtId"))))hits.add(p);if(!hits.isEmpty())return new ArrayList<>(hits);}
  for(Person p:people){for(String f:new String[]{"personalEmail","countyEmail"}){String mail=key(r.fields.get(f));if(!mail.isEmpty()&&(mail.equals(key(p.get("personalEmail")))||mail.equals(key(p.get("countyEmail")))||p.previousEmails.stream().anyMatch(x->key(x).equals(mail))))hits.add(p);}}
  if(!hits.isEmpty())return new ArrayList<>(hits);
  String name=key(r.fields.get("firstName")+" "+r.fields.get("lastName"));for(Person p:people)if(name.equals(key(p.name())))hits.add(p);
  return new ArrayList<>(hits);
 }
 public void stage(List<SourceRow> rows,String source,boolean missing,boolean clearBlanks){
  if(pending()>0)throw new IllegalArgumentException("Resolve the current import review before importing another file.");
  review.clear();importSource=source;importedAt=now();Set<String> seen=new HashSet<>();Map<String,Integer> names=new HashMap<>(),ids=new HashMap<>();
  for(SourceRow r:rows){String name=key(r.fields.get("firstName")+" "+r.fields.get("lastName"));names.put(name,names.getOrDefault(name,0)+1);String cid=key(r.fields.get("courtId"));if(!cid.isEmpty())ids.put(cid,ids.getOrDefault(cid,0)+1);}
  for(SourceRow r:rows){
   Proposal q=new Proposal();q.source=r.source;q.warning=r.warning;q.incoming.putAll(r.fields);List<Person> hits=matches(r);
   String name=key(r.fields.get("firstName")+" "+r.fields.get("lastName")),cid=key(r.fields.get("courtId"));
   if(names.get(name)>1||(!cid.isEmpty()&&ids.get(cid)>1)){q.kind="Conflict";q.warning+=" Duplicate name or court ID in this import; resolve identity before applying.";}
   else if(hits.size()>1){q.kind="Conflict";q.warning+=" More than one existing member matches.";for(Person p:hits)seen.add(p.id);}
   else if(hits.size()==1){
    Person p=hits.get(0);q.personId=p.id;seen.add(p.id);q.before.putAll(p.court);q.kind="Change";
    if(!cid.isEmpty()&&!p.get("courtId").isEmpty()&&!cid.equals(key(p.get("courtId")))){q.kind="Conflict";q.warning+=" Court IDs disagree.";}
    if(!clearBlanks){Iterator<Map.Entry<String,String>> it=q.incoming.entrySet().iterator();while(it.hasNext())if(it.next().getValue().isEmpty())it.remove();}
    boolean diff=false;for(String f:q.incoming.keySet())if(!q.incoming.get(f).equals(p.get(f)))diff=true;
    if(!diff&&q.warning.isEmpty())continue;
   }
   review.add(q);
  }
  if(missing)for(Person p:people)if(p.active()&&!seen.contains(p.id)){Proposal q=new Proposal();q.kind="Missing";q.personId=p.id;q.before.putAll(p.court);q.incoming.putAll(p.court);q.source=source;q.warning="Not found in this import. This does not establish resignation or departure. Keep the member, or edit their service status after confirming with the court.";review.add(q);}
  log("","Import staged",source+" · "+rows.size()+" source rows · "+review.size()+" review items");
 }
 public void apply(Proposal q,Set<String> selected,String matchId){
  for(Proposal stored:review)if(stored.id.equals(q.id)){q=stored;break;}
  if(!q.decision.equals("Pending"))throw new IllegalArgumentException("This item was already reviewed.");
  if(q.kind.equals("Missing"))throw new IllegalArgumentException("Missing members must be retained or edited explicitly.");
  Person p=find(matchId.isEmpty()?q.personId:matchId);
  if(q.kind.equals("Conflict")&&p==null)throw new IllegalArgumentException("Match this conflict to an existing member or skip it and add/edit the member manually.");
  Map<String,String> old=p==null?null:new LinkedHashMap<>(p.court);Person candidate=new Person();
  if(p!=null){candidate.id=p.id;candidate.court.putAll(p.court);candidate.emailTest=p.emailTest;}
  for(String f:selected){if(!q.incoming.containsKey(f))continue;if(p!=null&&q.personId.equals(p.id)&&!Objects.equals(q.before.get(f),p.get(f)))throw new IllegalArgumentException("This member changed after import. Skip this item and re-import to compare current values.");candidate.court.put(f,q.incoming.get(f));}
  if(p!=null&&!p.get("countyEmail").equals(candidate.get("countyEmail"))){candidate.emailTest="Not tested";candidate.emailTestAt="";}
  validate(candidate);
  if(p==null){p=candidate;savePerson(p,null,"");}
  else {p.court.putAll(candidate.court);savePerson(p,old,p.emailTest);}
  q.decision="Accepted";log(p.id,"Court import accepted",q.source+" · fields: "+String.join(", ",selected));
 }
 public void skip(Proposal q){for(Proposal stored:review)if(stored.id.equals(q.id)){q=stored;break;}q.decision="Skipped";log(q.personId,"Court import skipped",q.source+" · "+q.name());}
}


