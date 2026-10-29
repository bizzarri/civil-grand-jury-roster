package com.mauri.juryroster;
import java.util.*;
import org.json.*;
public final class PersistenceTests {
 public static void main(String[] args)throws Exception{
  int checks=0;RosterCore r=new RosterCore();r.foreperson="Synthetic Test Owner";r.handoffNotes="Keep operational notes across a handoff — including Unicode.";
  RosterCore.Person p=new RosterCore.Person();p.court.put("firstName","Synthetic");p.court.put("lastName","Member");p.court.put("countyEmail","synthetic@example.org");r.savePerson(p,null,"");p.note="Local note\nSecond line";p.emailTest="Confirmed";p.emailTestAt=RosterCore.now();p.previousEmails.add("former@example.org");p.onboarding.add("Oath completed");p.lists.add("Synthetic list");r.queue(p,"Update","Synthetic list","former@example.org","synthetic@example.org","Synthetic pending task");
  RosterCore.SourceRow row=new RosterCore.SourceRow();row.fields.put("firstName","Synthetic");row.fields.put("lastName","Member");row.fields.put("countyEmail","changed@example.org");row.source="Synthetic.csv";r.stage(Arrays.asList(row),"Synthetic.csv",false,false);
  byte[] serialized=Store.encode(r);RosterCore copy=Store.decode(serialized);CoreTests.check(copy.people.size()==1&&copy.tasks.size()==1&&copy.pending()==1,"JSON includes members, pending list work and import review");checks++;
  RosterCore.Person c=copy.people.get(0);CoreTests.check(c.note.equals(p.note)&&c.emailTest.equals(p.emailTest)&&c.previousEmails.equals(p.previousEmails)&&c.lists.equals(p.lists)&&c.onboarding.equals(p.onboarding),"All operational fields survive handoff serialization");checks++;
  CoreTests.check(copy.handoffNotes.equals(r.handoffNotes)&&copy.foreperson.equals(r.foreperson)&&copy.events.size()==r.events.size(),"Term notes and history roundtrip");checks++;
  char[] password="test handoff password".toCharArray();RosterCore decrypted=Store.decode(HandoffCrypto.decrypt(HandoffCrypto.encrypt(serialized,password),password));CoreTests.check(decrypted.pending()==1&&decrypted.people.get(0).note.equals(p.note),"Encrypted full handoff roundtrip");checks++;
  JSONObject bad=new JSONObject(new String(serialized,"UTF-8"));bad.getJSONArray("people").put(bad.getJSONArray("people").getJSONObject(0));try{Store.decode(bad.toString().getBytes("UTF-8"));throw new AssertionError("Duplicate member IDs accepted");}catch(java.io.IOException expected){checks++;}

  System.out.println("PASS: "+checks+" persistence and full-handoff checks");
 }
}
