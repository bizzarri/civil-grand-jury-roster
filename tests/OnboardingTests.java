package com.mauri.juryroster;
import java.util.*;
public final class OnboardingTests {
 static int n=0;static void check(boolean b,String s){if(!b)throw new AssertionError(s);n++;}
 static Set<String> set(String... x){return new LinkedHashSet<>(Arrays.asList(x));}
 static RosterCore.Person person(RosterCore r,String role,String status){RosterCore.Person p=new RosterCore.Person();p.court.put("firstName","Synthetic");p.court.put("lastName",role+status);p.court.put("role",role);p.court.put("status",status);r.savePerson(p,null,"");return p;}
 static void fails(Runnable r){try{r.run();throw new AssertionError("Expected invalid bulk action");}catch(IllegalArgumentException expected){n++;}}
 public static void main(String[] args)throws Exception{
  RosterCore r=new RosterCore();RosterCore.Person j=person(r,"Juror","Active"),a=person(r,"Alternate","Active"),resigned=person(r,"Juror","Resigned");j.note="Keep operational notes";j.onboarding.add("Contact details checked");j.titleIds.add("0");int events=r.events.size();
  check(r.completeOnboarding(set(j.id,a.id),set("Oath completed"),set("Juror","Alternate"))==2,"Day-one oath for regulars and alternates");check(j.onboarding.contains("Oath completed")&&a.onboarding.contains("Oath completed")&&resigned.onboarding.isEmpty(),"Only selected active people updated");check(r.events.size()==events+2,"One history entry per changed member");check(j.note.equals("Keep operational notes")&&j.onboarding.contains("Contact details checked")&&j.titleIds.contains("0"),"Unrelated data retained");
  events=r.events.size();check(r.completeOnboarding(set(j.id,a.id),set("Oath completed"),set("Juror","Alternate"))==0&&r.events.size()==events,"Repeated completion is idempotent");
  check(r.completeOnboarding(set(j.id),set("Orientation completed"),set("Juror"))==1&&!a.onboarding.contains("Orientation completed"),"Orientation for active regular jurors only");
  r.rosterGroups.clear();check(r.completeOnboarding(set(a.id),set("Orientation completed"),set("Alternate"))==1,"Bulk onboarding independent of display filters");
  fails(()->r.completeOnboarding(set(j.id,resigned.id),set("Oath completed"),set("Juror")));fails(()->r.completeOnboarding(set(a.id),set("Oath completed"),set("Juror")));fails(()->r.completeOnboarding(set("missing"),set("Oath completed"),set("Juror")));fails(()->r.completeOnboarding(set(j.id),set("County account accessible"),set("Juror")));fails(()->r.completeOnboarding(set(),set("Oath completed"),set("Juror")));fails(()->r.completeOnboarding(set(j.id),set(),set("Juror")));
  RosterCore atomic=new RosterCore();RosterCore.Person valid=person(atomic,"Juror","Active"),gone=person(atomic,"Alternate","Resigned");fails(()->atomic.completeOnboarding(set(valid.id,gone.id),set("Oath completed","Orientation completed"),set("Juror","Alternate")));check(valid.onboarding.isEmpty(),"Invalid batch validates everyone before changing anyone");
  check(atomic.completeOnboarding(set(valid.id),set("Oath completed","Orientation completed"),set("Juror"))==1&&valid.onboarding.size()==2,"Both activities can be marked together");
  RosterCore restored=Store.decode(Store.encode(r));check(restored.find(j.id).onboarding.containsAll(set("Oath completed","Orientation completed"))&&restored.events.size()==r.events.size(),"Bulk checks and history survive save/handoff");
  System.out.println("PASS: "+n+" bulk onboarding checks");
 }
}
