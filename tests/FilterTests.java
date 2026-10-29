package com.mauri.juryroster;
import java.util.*;
import org.json.*;
public final class FilterTests {
 static int n=0;static void check(boolean b,String s){if(!b)throw new AssertionError(s);n++;}
 static RosterCore.Person p(String role,String status){RosterCore.Person p=new RosterCore.Person();p.court.put("firstName","Synthetic");p.court.put("lastName",role+status);p.court.put("role",role);p.court.put("status",status);return p;}
 public static void main(String[] args)throws Exception{
  RosterCore r=new RosterCore();RosterCore.Person regular=p("Juror","Active"),alternate=p("Alternate","Active"),resigned=p("Juror","Resigned"),resignedAlternate=p("Alternate","Resigned"),excused=p("Juror","Excused"),completed=p("Alternate","Completed");
  Collections.addAll(r.people,regular,alternate,resigned,resignedAlternate,excused,completed);check(r.people.stream().allMatch(r::visibleInRoster),"Old All behavior preserved by default");
  r.setRosterGroups(new LinkedHashSet<>(Arrays.asList("Active jurors")));check(r.visibleInRoster(regular)&&!r.visibleInRoster(alternate)&&!r.visibleInRoster(resigned),"Active regular jurors only");
  r.setRosterGroups(new LinkedHashSet<>(Arrays.asList("Alternates")));check(r.visibleInRoster(alternate)&&!r.visibleInRoster(regular)&&!r.visibleInRoster(resignedAlternate),"Active alternates only");
  r.setRosterGroups(new LinkedHashSet<>(Arrays.asList("Resigned")));check(r.visibleInRoster(resigned)&&r.visibleInRoster(resignedAlternate)&&!r.visibleInRoster(alternate),"Resigned independent of original role");
  r.setRosterGroups(new LinkedHashSet<>(Arrays.asList("Active jurors","Alternates")));check(r.people.stream().filter(r::visibleInRoster).count()==2,"Multiple checkboxes combine as union");check(r.count("Juror")==1&&r.count("Alternate")==1&&r.resigned()==2,"Seat and resignation totals ignore display filters");
  r.setRosterGroups(new LinkedHashSet<>(Arrays.asList("Other departures")));check(r.visibleInRoster(excused)&&r.visibleInRoster(completed)&&!r.visibleInRoster(resigned),"Excused and completed remain accessible");
  r.setRosterGroups(new LinkedHashSet<>());check(r.people.stream().noneMatch(r::visibleInRoster),"No selection shows no records without changing data");RosterCore reopened=Store.decode(Store.encode(r));check(reopened.rosterGroups.isEmpty()&&reopened.people.size()==6,"Empty selection survives save/reopen");
  r.setRosterGroups(new LinkedHashSet<>(Arrays.asList("Active jurors","Resigned")));reopened=Store.decode(Store.encode(r));check(reopened.rosterGroups.equals(r.rosterGroups),"Selections survive handoff");String csv=new String(Store.csv(r),"UTF-8");check(csv.contains("AlternateActive")&&csv.contains("JurorResigned"),"CSV exports complete roster regardless of filters");
  JSONObject old=new JSONObject(new String(Store.encode(r),"UTF-8"));old.remove("rosterGroups");RosterCore migrated=Store.decode(old.toString().getBytes("UTF-8"));check(migrated.rosterGroups.size()==4&&migrated.people.size()==6,"Old roster migration retains all data and defaults");
  try{r.setRosterGroups(Collections.singleton("Invalid"));throw new AssertionError("Invalid filter accepted");}catch(IllegalArgumentException expected){n++;}check(r.rosterGroups.contains("Resigned"),"Invalid update leaves preferences unchanged");
  System.out.println("PASS: "+n+" roster-filter checks");
 }
}
