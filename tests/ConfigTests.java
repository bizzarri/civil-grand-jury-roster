package com.mauri.juryroster;
import java.util.*;
import org.json.*;
public final class ConfigTests {
 static int n=0;static void check(boolean b,String s){if(!b)throw new AssertionError(s);n++;}
 static void fails(Runnable r){try{r.run();throw new AssertionError("Expected invalid configuration");}catch(IllegalArgumentException expected){n++;}}
 public static void main(String[] args)throws Exception{
  RosterCore r=new RosterCore();check(r.appName.equals("Civil Grand Jury Roster")&&r.term.equals("2026-2027"),"Default app identity and years");check(r.titles.length==8&&r.titles[4].equals("Assistant Secretary")&&r.titles[5].isEmpty(),"Five initial and three extra titles");
  RosterCore.Person p=new RosterCore.Person();p.court.put("firstName","Synthetic");p.court.put("lastName","Member");p.titleIds.add("0");p.titleIds.add("2");r.savePerson(p,null,"");check(r.titleText(p).equals("Foreperson · Recording Secretary"),"Multiple juror titles");
  String[] names=r.titles.clone();names[0]="Presiding Foreperson";names[5]="Technology Coordinator";r.configure("County Civil Jury","2027-2028",19,20,names);check(r.titleText(p).startsWith("Presiding Foreperson")&&p.titleIds.contains("0"),"Renaming preserves stable assignments");p.titleIds.add("5");check(r.titleText(p).contains("Technology Coordinator"),"Extra title assignable");
  String[] clear=names.clone();clear[0]="";fails(()->r.configure("Valid","2027-2028",19,20,clear));check(r.titles[0].equals("Presiding Foreperson"),"Failed clearing does not mutate configuration");
  String[] duplicate=names.clone();duplicate[7]="recording secretary";fails(()->r.configure("Valid","2027-2028",19,20,duplicate));fails(()->r.configure("","2027-2028",19,20,names));fails(()->r.configure("Valid","2027-2029",19,20,names));fails(()->r.configure("Valid","2027",19,20,names));
  RosterCore copy=Store.decode(Store.encode(r));check(copy.appName.equals(r.appName)&&copy.term.equals(r.term)&&copy.people.get(0).titleIds.equals(p.titleIds)&&copy.titles[5].equals(names[5]),"Configuration and assignments survive save/handoff");
  String csv=new String(Store.csv(r),"UTF-8");check(csv.contains("Juror titles")&&csv.contains("Technology Coordinator"),"Titles included in CSV");
  RosterCore.SourceRow row=new RosterCore.SourceRow();row.fields.put("firstName","Synthetic");row.fields.put("lastName","Member");row.fields.put("phone","555-0100");r.stage(Arrays.asList(row),"Synthetic.csv",false,false);r.apply(r.review.get(0),Collections.singleton("phone"),"");check(p.titleIds.size()==3&&r.titleText(p).contains("Technology Coordinator"),"Court import preserves operational titles");
  JSONObject legacy=new JSONObject(new String(Store.encode(r),"UTF-8"));legacy.remove("appName");legacy.remove("titles");for(int i=0;i<legacy.getJSONArray("people").length();i++)legacy.getJSONArray("people").getJSONObject(i).remove("titleIds");RosterCore migrated=Store.decode(legacy.toString().getBytes("UTF-8"));check(migrated.appName.equals("Civil Grand Jury Roster")&&migrated.titles.length==8&&migrated.people.size()==1&&migrated.people.get(0).titleIds.isEmpty(),"Older roster gains configuration defaults without invented assignments");
  System.out.println("PASS: "+n+" configuration/title checks");
 }
}
