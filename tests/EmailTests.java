package com.mauri.juryroster;
import java.util.*;
import org.json.*;
public final class EmailTests {
 static int n=0;static void check(boolean b,String s){if(!b)throw new AssertionError(s);n++;}
 static void fail(Runnable r){try{r.run();throw new AssertionError("Expected invalid rule/collision");}catch(IllegalArgumentException expected){n++;}}
 public static void main(String[] args)throws Exception{
  check(RosterCore.generatedEmail("Alex","Sample","amadorcounty.gov","{first_initial}{last}").equals("asample@amadorcounty.gov"),"Amador convention");
  check(RosterCore.generatedEmail(" Marie "," O’Neil-Smith ","AmadorCounty.GOV","{first_initial}{last}").equals("moneilsmith@amadorcounty.gov"),"Whitespace punctuation and case");
  check(RosterCore.generatedEmail("José","García","example.gov","{first}.{last}").equals("jose.garcia@example.gov"),"Accents and alternate county template");
  check(RosterCore.generatedEmail("Anne","San Nicholas","example.gov","{first}_{last}").equals("anne_sannicholas@example.gov"),"Multiword names");
  fail(()->RosterCore.generatedEmail("","Sample","example.gov","{first_initial}{last}"));fail(()->RosterCore.generatedEmail("Alex","Sample","@example.gov","{first_initial}{last}"));fail(()->RosterCore.generatedEmail("Alex","Sample","example.gov","{nickname}{last}"));fail(()->RosterCore.generatedEmail("Alex","Sample","example.gov","{first}..{last}"));fail(()->RosterCore.generatedEmail("李","王","example.gov","{first}{last}"));
  RosterCore r=new RosterCore();RosterCore.Person p=new RosterCore.Person();p.court.put("firstName","Test");p.court.put("lastName","Person");p.court.put("countyEmail","tperson@amadorcounty.gov");r.savePerson(p,null,"");RosterCore.Person other=new RosterCore.Person();other.court.put("firstName","Tim");other.court.put("lastName","Person");fail(()->r.suggestCountyEmail(other));check(r.suggestCountyEmail(p).equals(p.get("countyEmail")),"Member may retain own address");
  p.emailTest="Confirmed";p.emailTestAt="Synthetic time";p.note="Retain this operational note";p.onboarding.add("Oath completed");p.lists.add("All jurors");r.countyDomain="other.example.gov";String generated=r.suggestCountyEmail(p);Map<String,String> before=new LinkedHashMap<>(p.court);p.court.put("countyEmail",generated);r.savePerson(p,before,"Confirmed");check(p.emailTest.equals("Not tested")&&p.emailTestAt.isEmpty(),"Regeneration resets verification");check(p.previousEmails.contains("tperson@amadorcounty.gov")&&p.note.equals("Retain this operational note")&&p.onboarding.size()==1,"Regeneration retains history and operations");check(r.tasks.size()==1&&r.tasks.get(0).newEmail.equals(generated),"Regeneration queues membership update");
  r.countyPattern="{first}.{last}";RosterCore reopened=Store.decode(Store.encode(r));check(reopened.countyDomain.equals(r.countyDomain)&&reopened.countyPattern.equals(r.countyPattern),"Rule persists through handoff");
  JSONObject legacy=new JSONObject(new String(Store.encode(r),"UTF-8"));legacy.remove("countyDomain");legacy.remove("countyPattern");RosterCore migrated=Store.decode(legacy.toString().getBytes("UTF-8"));check(migrated.countyDomain.equals("amadorcounty.gov")&&migrated.countyPattern.equals("{first_initial}{last}")&&migrated.people.size()==1,"Old data and handoffs gain defaults without loss");
  System.out.println("PASS: "+n+" email generation checks");
 }
}

