package com.mauri.juryroster;
import java.util.*;
public final class CoreTests {
 static int passed=0;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);passed++;}
 static RosterCore.Person person(String first,String last,String email){RosterCore.Person p=new RosterCore.Person();p.court.put("firstName",first);p.court.put("lastName",last);p.court.put("countyEmail",email);return p;}
 static RosterCore.SourceRow row(String first,String last,String email){RosterCore.SourceRow r=new RosterCore.SourceRow();r.fields.put("firstName",first);r.fields.put("lastName",last);r.fields.put("countyEmail",email);r.fields.put("role","Juror");r.fields.put("status","Active");r.source="Synthetic court file";return r;}
 static void fails(Runnable action,String name){try{action.run();throw new AssertionError(name);}catch(IllegalArgumentException expected){passed++;}}
 public static void main(String[] args)throws Exception{
  RosterCore r=new RosterCore();RosterCore.Person p=person("Test","Juror","old@example.org");r.savePerson(p,null,"");p.note="Operational note must survive";p.onboarding.add("Oath completed");p.emailTest="Confirmed";p.emailTestAt="2026-10-27";p.lists.add("All jurors");
  r.stage(Arrays.asList(row("Test","Juror","new@example.org")),"Test.xlsx",false,false);
  check(p.get("countyEmail").equals("old@example.org"),"Staging does not change roster");check(r.pending()==1,"One field review");RosterCore.Proposal q=r.review.get(0);r.apply(q,new HashSet<>(q.incoming.keySet()),"");
  check(p.note.equals("Operational note must survive")&&p.onboarding.contains("Oath completed"),"Notes and onboarding survive court changes");check(p.emailTest.equals("Not tested")&&p.emailTestAt.isEmpty(),"County change resets verification");check(p.previousEmails.contains("old@example.org"),"Previous county address retained");check(r.tasks.size()==1&&r.tasks.get(0).action.equals("Update"),"County change queues list update");
  r.finishTask(r.tasks.get(0),true);check(p.lists.contains("All jurors"),"Completing update retains membership");
  Map<String,String> before=new LinkedHashMap<>(p.court);p.court.put("status","Resigned");r.savePerson(p,before,p.emailTest);check(r.count("Juror")==0&&r.resigned()==1,"Resignation opens vacancy and retains person");check(r.tasks.get(r.tasks.size()-1).action.equals("Remove"),"Departure queues list removal");r.finishTask(r.tasks.get(r.tasks.size()-1),true);check(p.lists.isEmpty(),"Removal completes membership change");
  RosterCore blank=new RosterCore();RosterCore.Person b=person("Blank","Test","b@example.org");blank.savePerson(b,null,"");blank.stage(Arrays.asList(row("Blank","Test","")),"Blank.csv",false,false);check(blank.pending()==0&&b.get("countyEmail").equals("b@example.org"),"Blank imported email does not erase local value");
  blank.stage(new ArrayList<>(),"Missing.csv",true,false);check(blank.pending()==1&&blank.review.get(0).kind.equals("Missing")&&b.active(),"Missing source row never auto-resigns");blank.skip(blank.review.get(0));check(b.active(),"Retain missing member leaves active status");
  RosterCore dupe=new RosterCore();dupe.stage(Arrays.asList(row("Same","Name","a@example.org"),row("Same","Name","b@example.org")),"Duplicate.xlsx",false,false);check(dupe.review.stream().allMatch(x->x.kind.equals("Conflict")),"Duplicate names become conflicts");fails(()->dupe.apply(dupe.review.get(0),new HashSet<>(dupe.review.get(0).incoming.keySet()),""),"Ambiguous identity cannot silently create member");
  RosterCore stale=new RosterCore();RosterCore.Person s=person("Stale","Test","a@example.org");stale.savePerson(s,null,"");stale.stage(Arrays.asList(row("Stale","Test","b@example.org")),"Stale.csv",false,false);s.court.put("countyEmail","manual@example.org");fails(()->stale.apply(stale.review.get(0),Collections.singleton("countyEmail"),""),"Stale imported change rejected");
  List<List<String>> parsed=CourtFile.csv("First Name,Last Name,Email Address\r\n\"Test, First\",\"Last\"\"Name\",a@example.org\r\n");check(parsed.size()==2&&parsed.get(1).get(0).equals("Test, First")&&parsed.get(1).get(1).equals("Last\"Name"),"CSV quotes commas and escapes");check(CourtFile.csvCell("=CMD()").startsWith("\"'="),"CSV prevents formula injection");
  char[] pass="synthetic test password".toCharArray();byte[] plain="Synthetic private roster".getBytes("UTF-8"),packed=HandoffCrypto.encrypt(plain,pass);check(Arrays.equals(plain,HandoffCrypto.decrypt(packed,pass)),"Encrypted handoff roundtrip");try{HandoffCrypto.decrypt(packed,"wrong password".toCharArray());throw new AssertionError("Wrong password accepted");}catch(javax.crypto.AEADBadTagException expected){passed++;}packed[packed.length-1]^=1;try{HandoffCrypto.decrypt(packed,pass);throw new AssertionError("Tampering accepted");}catch(javax.crypto.AEADBadTagException expected){passed++;}

  System.out.println("PASS: "+passed+" regression checks");
 }
}
