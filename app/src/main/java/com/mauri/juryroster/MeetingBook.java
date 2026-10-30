package com.mauri.juryroster;
import java.util.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.json.*;

/** Local meeting dates, persistent schedule exceptions, and historical attendance snapshots. */
public final class MeetingBook {
 public static final String[] ATTENDANCE={"Not recorded","In person","Online","Present","Absent","Excused"};
 public int weekday=3; // ISO Monday=1 ... Sunday=7
 public Set<Integer> weeks=new LinkedHashSet<>(Arrays.asList(1,3));
 public String from=LocalDate.now().toString(),through=LocalDate.now().plusMonths(6).toString();
 public final Set<String> excludedDates=new LinkedHashSet<>();
 public final List<Meeting> meetings=new ArrayList<>();
 public static final class Member {
  public String id="",name="",role="Juror",status="Not recorded",updatedAt="";
 }
 public static final class Meeting {
  public String id=UUID.randomUUID().toString(),date="",title="Grand Jury meeting",state="Scheduled",reason="",notes="",createdAt=RosterCore.now();
  public boolean started=false;
  public final Map<String,Member> members=new LinkedHashMap<>();
  public final List<String> history=new ArrayList<>();
 }
 public static LocalDate date(String s){try{return LocalDate.parse(s);}catch(Exception e){throw new IllegalArgumentException("Use a valid date in YYYY-MM-DD format.");}}
 public Meeting get(String id){for(Meeting m:meetings)if(m.id.equals(id))return m;throw new IllegalArgumentException("Meeting no longer exists.");}
 private void freeDate(String d,String except){date(d);for(Meeting m:meetings)if(!m.id.equals(except)&&!m.state.equals("Deleted")&&m.date.equals(d))throw new IllegalArgumentException("A meeting already exists on that date. Open or restore it instead.");}
 private void record(Meeting m,String message){m.history.add(RosterCore.now()+" · "+message);}
 public Meeting add(String d,String title){freeDate(d,"");if(title.trim().isEmpty())throw new IllegalArgumentException("Enter a meeting name.");Meeting m=new Meeting();m.date=d;m.title=title.trim();meetings.add(m);excludedDates.remove(d);record(m,"Meeting added");return m;}
 public List<String> patternDates(int day,Set<Integer> occurrences,String start,String end){
  LocalDate a=date(start),b=date(end);if(b.isBefore(a)||ChronoUnit.DAYS.between(a,b)>400)throw new IllegalArgumentException("Choose an end date after the start, up to 400 days apart.");
  if(day<1||day>7||occurrences.isEmpty())throw new IllegalArgumentException("Choose a weekday and at least one week of the month.");for(int n:occurrences)if(n<1||n>5)throw new IllegalArgumentException("Choose the first through fifth week.");
  List<String> dates=new ArrayList<>();for(LocalDate d=a;!d.isAfter(b);d=d.plusDays(1))if(d.getDayOfWeek().getValue()==day&&occurrences.contains((d.getDayOfMonth()-1)/7+1))dates.add(d.toString());return dates;
 }
 public List<String> additions(int day,Set<Integer> occurrences,String start,String end){List<String> result=patternDates(day,occurrences,start,end);result.removeIf(d->excludedDates.contains(d)||meetings.stream().anyMatch(m->m.date.equals(d)&&!m.state.equals("Deleted")));return result;}
 public int generate(int day,Set<Integer> occurrences,String start,String end){List<String> dates=additions(day,occurrences,start,end);for(String d:dates)add(d,"Grand Jury meeting");weekday=day;weeks=new LinkedHashSet<>(occurrences);from=start;through=end;return dates.size();}
 public void edit(String id,String d,String title,String notes){Meeting m=get(id);if(m.state.equals("Deleted"))throw new IllegalArgumentException("Restore this removed meeting before editing it.");freeDate(d,id);if(title.trim().isEmpty())throw new IllegalArgumentException("Enter a meeting name.");String old=m.date;if(!old.equals(d)){excludedDates.add(old);excludedDates.remove(d);}m.date=d;m.title=title.trim();m.notes=notes;record(m,"Meeting edited"+(!old.equals(d)?"; date "+old+" → "+d:""));}
 public void cancel(String id,String reason){Meeting m=get(id);if(!m.state.equals("Scheduled"))throw new IllegalArgumentException("Only a scheduled meeting can be cancelled.");m.state="Cancelled";m.reason=reason.trim();record(m,"Cancelled: "+m.reason);}
 public void remove(String id){Meeting m=get(id);if(m.state.equals("Deleted"))return;m.state="Deleted";excludedDates.add(m.date);record(m,"Removed from schedule; attendance retained");}
 public void restore(String id){Meeting m=get(id);if(m.state.equals("Scheduled"))return;freeDate(m.date,id);m.state="Scheduled";m.reason="";excludedDates.remove(m.date);record(m,"Meeting restored");}
 private void scheduled(Meeting m){if(!m.state.equals("Scheduled"))throw new IllegalArgumentException("Restore the meeting before recording attendance.");}
 public void start(String id,List<RosterCore.Person> roster){Meeting m=get(id);scheduled(m);if(m.started)return;for(RosterCore.Person p:roster)if(p.active())include(m,p);m.started=true;record(m,"Attendance roster captured from current active members");}
 private void include(Meeting m,RosterCore.Person p){Member member=new Member();member.id=p.id;member.name=p.name();member.role=p.get("role");m.members.put(member.id,member);}
 public void addMember(String id,RosterCore.Person p){Meeting m=get(id);scheduled(m);if(m.members.containsKey(p.id))return;include(m,p);m.started=true;record(m,"Attendance member added: "+p.name());}
 public void removeMember(String id,String person){Meeting m=get(id);scheduled(m);Member member=m.members.get(person);if(member==null)return;if(!member.status.equals("Not recorded"))throw new IllegalArgumentException("Set this member to Not recorded before removing them from this meeting.");m.members.remove(person);record(m,"Attendance member removed: "+member.name);}
 public void mark(String id,String person,String status){Meeting m=get(id);scheduled(m);if(!Arrays.asList(ATTENDANCE).contains(status))throw new IllegalArgumentException("Unknown attendance status.");Member p=m.members.get(person);if(p==null)throw new IllegalArgumentException("This person is not in this meeting's attendance roster.");if(p.status.equals(status))return;String before=p.status;p.status=status;p.updatedAt=RosterCore.now();record(m,p.name+": "+before+" → "+status);}
 public Meeting scheduledOn(String day){date(day);for(Meeting m:meetings)if(m.date.equals(day)&&m.state.equals("Scheduled"))return m;return null;}
 public void markFromRoster(String meetingId,RosterCore.Person person,String status,String today,List<RosterCore.Person> roster){
  Meeting m=get(meetingId);scheduled(m);if(!m.date.equals(today))throw new IllegalArgumentException("This is no longer today's meeting. Open the meeting to edit its attendance.");if(!person.active())throw new IllegalArgumentException("Only active members can use today's roster attendance.");
  if(!Arrays.asList("In person","Online","Not recorded").contains(status))throw new IllegalArgumentException("Choose in person or online.");start(meetingId,roster);if(!m.members.containsKey(person.id))addMember(meetingId,person);mark(meetingId,person.id,status);
 }
 public String summary(Meeting m){if(m.state.equals("Deleted"))return "Removed · attendance retained";if(m.state.equals("Cancelled"))return "Cancelled"+(m.reason.isEmpty()?"":" · "+m.reason);if(!m.started)return "Attendance not started";int present=0,missing=0;for(Member p:m.members.values()){if(p.status.equals("Present")||p.status.equals("In person")||p.status.equals("Online"))present++;if(p.status.equals("Not recorded"))missing++;}return present+" present · "+missing+" unrecorded";}
 public JSONObject toJson()throws Exception{
  JSONObject o=new JSONObject();o.put("version",1);o.put("weekday",weekday);o.put("weeks",new JSONArray(weeks));o.put("from",from);o.put("through",through);o.put("excludedDates",new JSONArray(excludedDates));JSONArray list=new JSONArray();
  for(Meeting m:meetings){JSONObject j=new JSONObject();j.put("id",m.id);j.put("date",m.date);j.put("title",m.title);j.put("state",m.state);j.put("reason",m.reason);j.put("notes",m.notes);j.put("createdAt",m.createdAt);j.put("started",m.started);j.put("history",new JSONArray(m.history));JSONArray members=new JSONArray();for(Member p:m.members.values()){JSONObject x=new JSONObject();x.put("id",p.id);x.put("name",p.name);x.put("role",p.role);x.put("status",p.status);x.put("updatedAt",p.updatedAt);members.put(x);}j.put("members",members);list.put(j);}o.put("meetings",list);return o;
 }
 public static MeetingBook fromJson(JSONObject o)throws Exception{
  if(o.getInt("version")!=1)throw new IllegalArgumentException("Unsupported meeting data version.");MeetingBook b=new MeetingBook();b.weekday=o.getInt("weekday");b.weeks.clear();JSONArray w=o.getJSONArray("weeks");for(int i=0;i<w.length();i++)b.weeks.add(w.getInt(i));b.from=o.getString("from");b.through=o.getString("through");b.patternDates(b.weekday,b.weeks,b.from,b.through);JSONArray exclusions=o.getJSONArray("excludedDates");for(int i=0;i<exclusions.length();i++){String d=exclusions.getString(i);date(d);b.excludedDates.add(d);}JSONArray list=o.getJSONArray("meetings");if(list.length()>3000)throw new IllegalArgumentException("Too many meetings.");Set<String> ids=new HashSet<>(),dates=new HashSet<>();
  for(int i=0;i<list.length();i++){JSONObject j=list.getJSONObject(i);Meeting m=new Meeting();m.id=j.getString("id");m.date=j.getString("date");date(m.date);m.title=j.getString("title");m.state=j.getString("state");if(m.id.isEmpty()||!ids.add(m.id)||!Arrays.asList("Scheduled","Cancelled","Deleted").contains(m.state))throw new IllegalArgumentException("Invalid meeting identity or state.");if(!m.state.equals("Deleted")&&!dates.add(m.date))throw new IllegalArgumentException("Duplicate meeting dates.");m.reason=j.getString("reason");m.notes=j.getString("notes");m.createdAt=j.getString("createdAt");m.started=j.getBoolean("started");JSONArray history=j.getJSONArray("history");if(history.length()>10000)throw new IllegalArgumentException("Meeting history too large.");for(int h=0;h<history.length();h++)m.history.add(history.getString(h));JSONArray members=j.getJSONArray("members");if(members.length()>1000)throw new IllegalArgumentException("Too many attendance members.");for(int n=0;n<members.length();n++){JSONObject x=members.getJSONObject(n);Member p=new Member();p.id=x.getString("id");p.name=x.getString("name");p.role=x.getString("role");p.status=x.getString("status");p.updatedAt=x.getString("updatedAt");if(p.id.isEmpty()||m.members.containsKey(p.id)||!Arrays.asList(ATTENDANCE).contains(p.status)||!Arrays.asList("Juror","Alternate").contains(p.role))throw new IllegalArgumentException("Invalid attendance record.");m.members.put(p.id,p);}if(!m.started&&!m.members.isEmpty())throw new IllegalArgumentException("Unstarted meeting has attendance members.");b.meetings.add(m);}
  for(Meeting removed:b.meetings)if(removed.state.equals("Deleted")&&b.meetings.stream().noneMatch(live->!live.state.equals("Deleted")&&live.date.equals(removed.date)))b.excludedDates.add(removed.date);
  return b;
 }
 public byte[] csv(){StringBuilder out=new StringBuilder("\ufeffMeeting ID,Date,Meeting,Meeting status,Member ID,Member name,Role,Attendance,Updated at\r\n");for(Meeting m:meetings){if(m.members.isEmpty())csvRow(out,Arrays.asList(m.id,m.date,m.title,m.state,"","","",m.started?"No participants":"Not started",""));else for(Member p:m.members.values())csvRow(out,Arrays.asList(m.id,m.date,m.title,m.state,p.id,p.name,p.role,p.status,p.updatedAt));}return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);}
 private void csvRow(StringBuilder out,List<String> fields){for(int i=0;i<fields.size();i++){if(i>0)out.append(',');out.append(CourtFile.csvCell(fields.get(i)));}out.append("\r\n");}
}


