package com.mauri.juryroster;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
 private static final int BG=0xfff4f6f7,INK=0xff142f42,MUTED=0xff536576,TEAL=0xff176c67,WHITE=Color.WHITE,AMBER=0xff94510d;
 private RosterCore data;private Store store;private LinearLayout root,page,nav;private ScrollView scroll;
 private String screen="Roster",query="",editingId="";private LinearLayout rosterList;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());
 private List<CourtFile.Sheet> sheets;private String sourceName="";private byte[] pendingExport;private boolean busy=false;
 private final Map<String,EditText> fields=new LinkedHashMap<>();private final Map<String,CheckBox> titleChecks=new LinkedHashMap<>();private final Map<String,CheckBox> checks=new LinkedHashMap<>();private Spinner role,status,test;private EditText note;private RosterCore.Person draft;
 private final List<CheckBox> sheetInclude=new ArrayList<>();private final List<Spinner> sheetRole=new ArrayList<>(),sheetStatus=new ArrayList<>();private final List<Map<Integer,Spinner>> sheetMaps=new ArrayList<>();
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);store=new Store(this);
  try{data=store.read();}catch(Exception e){errorScreen("The saved roster could not be read. It has not been replaced. Keep the app installed and recover from a handoff file or seek support.\n\n"+e.getMessage());return;}
  if(saved!=null){screen=saved.getString("screen","Roster");query=saved.getString("query","");if(screen.equals("Edit")){try{RosterCore tmp=Store.decode(saved.getByteArray("draft"));draft=tmp.people.get(0);editingId=saved.getString("editingId","");}catch(Exception e){screen="Roster";}}}
  shell();if(screen.equals("Edit")&&draft!=null)editor(draft);else navigate(screen);
 }
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("screen",screen);out.putString("query",query);if(screen.equals("Edit")&&draft!=null){try{RosterCore tmp=new RosterCore();tmp.titles=data.titles.clone();tmp.people.add(readDraft());out.putByteArray("draft",Store.encode(tmp));out.putString("editingId",editingId);}catch(Exception ignored){}}}
 @Override protected void onDestroy(){worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
 private void shell(){
  root=column();root.setBackgroundColor(BG);root.setOnApplyWindowInsetsListener((v,i)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);}else v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
  LinearLayout heading=column();heading.setPadding(dp(20),dp(15),dp(20),dp(12));heading.setBackgroundColor(INK);TextView title=text(data.appName,25,WHITE);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setMaxLines(1);title.setAutoSizeTextTypeUniformWithConfiguration(14,25,1,android.util.TypedValue.COMPLEX_UNIT_SP);heading.addView(title);TextView subtitle=text(data.term.replace('–','-'),16,0xffbde4dc);add(heading,subtitle,3);root.addView(heading);
  scroll=new ScrollView(this);scroll.setFillViewport(true);page=column();page.setPadding(dp(20),dp(18),dp(20),dp(28));scroll.addView(page);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  nav=new LinearLayout(this);nav.setBackgroundColor(WHITE);for(String tab:new String[]{"Roster","Work","Import","Handoff","Config"}){Button b=new Button(this);b.setText(tab);b.setTextSize(12);b.setTextColor(INK);b.setAllCaps(false);b.setOnClickListener(v->requestNav(tab));nav.addView(b,new LinearLayout.LayoutParams(0,dp(56),1));}root.addView(nav);setContentView(root);root.requestApplyInsets();
 }
 private void errorScreen(String message){LinearLayout box=column();box.setPadding(dp(24),dp(48),dp(24),dp(24));box.addView(text(message,18,INK));setContentView(box);}
 private void requestNav(String tab){if(screen.equals("Edit")){new AlertDialog.Builder(this).setTitle("Leave unsaved changes?").setMessage("Save the member first, or discard these edits.").setPositiveButton("Discard",(d,w)->navigate(tab)).setNegativeButton("Keep editing",null).show();}else navigate(tab);}
 @Override public void onBackPressed(){if(screen.equals("Edit"))requestNav("Roster");else if(!screen.equals("Roster"))navigate("Roster");else super.onBackPressed();}
 private void navigate(String tab){screen=tab;page.removeAllViews();scroll.scrollTo(0,0);if(tab.equals("Work"))work();else if(tab.equals("Import"))imports();else if(tab.equals("Handoff"))handoff();else if(tab.equals("Config"))configuration();else{screen="Roster";roster();}}
 private void heading(String title,String sub){TextView t=text(title,26,INK);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);add(page,t,0);if(!sub.isEmpty())add(page,text(sub,14,MUTED),7);}
 private void roster(){
  heading("The working roster","Keep court information and day-to-day updates together.");
  LinearLayout stats=card(page);int regular=data.count("Juror"),alt=data.count("Alternate");
  add(stats,text(regular+" / "+data.seats+" regular seats",23,INK),0);add(stats,text(Math.max(0,data.seats-regular)+" regular vacancies · "+alt+" / "+data.alternateSlots+" alternates · "+Math.max(0,data.alternateSlots-alt)+" alternate openings",14,TEAL),8);
  add(stats,text(data.resigned()+" resigned · "+data.pending()+" import items awaiting review",14,MUTED),6);if(regular>data.seats||alt>data.alternateSlots)add(stats,text("Roster exceeds configured capacity. Review roles or term settings.",14,AMBER),8);
  button(page,"Add member",v->{editingId="";editor(new RosterCore.Person());},true);
  EditText search=input(page,"Find a member",query,false);search.setSingleLine(true);
  rosterList=column();add(page,rosterList,6);search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();renderRoster();}public void afterTextChanged(Editable e){}});
  renderRoster();
 }
 private void renderRoster(){
  rosterList.removeAllViews();List<RosterCore.Person> list=new ArrayList<>(data.people);Collections.sort(list,(a,b)->{int c=Boolean.compare(!a.active(),!b.active());return c!=0?c:(a.get("lastName")+a.get("firstName")).compareToIgnoreCase(b.get("lastName")+b.get("firstName"));});int shown=0;
  for(RosterCore.Person p:list){if(!data.visibleInRoster(p))continue;
   String hay=RosterCore.key(p.name()+" "+p.get("personalEmail")+" "+p.get("countyEmail")+" "+p.get("position")+" "+data.titleText(p));if(!hay.contains(RosterCore.key(query)))continue;shown++;
   LinearLayout c=card(rosterList);TextView name=text(p.name(),20,INK);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);add(c,name,0);if(!data.titleText(p).isEmpty())add(c,text(data.titleText(p),14,TEAL),5);add(c,text(p.get("role")+" · "+p.get("status")+(p.get("position").isEmpty()?"":" · "+p.get("position")),13,MUTED),5);
   add(c,text(p.get("countyEmail").isEmpty()?"County email not recorded":p.get("countyEmail"),15,TEAL),10);add(c,text("Email: "+p.emailTest+" · Onboarding "+p.onboarding.size()+" / "+RosterCore.CHECKS.length,13,MUTED),6);button(c,"Open member",v->{editingId=p.id;editor(copy(p));},false);
  }
  if(shown==0)add(rosterList,text(data.people.isEmpty()?"Start by importing the court workbook or adding a member. No sample jurors are included.":"No members match your search and roster filters. Change status groups in Config.",16,MUTED),18);
 }
 private RosterCore.Person copy(RosterCore.Person p){RosterCore.Person c=new RosterCore.Person();c.id=p.id;c.court.putAll(p.court);c.note=p.note;c.emailTest=p.emailTest;c.emailTestAt=p.emailTestAt;c.createdAt=p.createdAt;c.updatedAt=p.updatedAt;c.titleIds.addAll(p.titleIds);c.onboarding.addAll(p.onboarding);c.lists.addAll(p.lists);c.previousEmails.addAll(p.previousEmails);return c;}
 private void editor(RosterCore.Person person){
  screen="Edit";draft=person;page.removeAllViews();scroll.scrollTo(0,0);fields.clear();checks.clear();titleChecks.clear();heading(editingId.isEmpty()?"Add member":person.name(),"Operational checks and notes are preserved during court imports.");
  button(page,"Save member",v->saveEditor(),true);
  LinearLayout basics=card(page);section(basics,"SERVICE & CONTACT");
  for(String f:new String[]{"courtId","firstName","lastName"})fields.put(f,input(basics,RosterCore.label(f),person.get(f),false));
  role=spinner(basics,"Roster role",new String[]{"Juror","Alternate"},person.get("role"));status=spinner(basics,"Service status",new String[]{"Active","Resigned","Excused","Completed"},person.get("status"));
  for(String f:new String[]{"personalEmail","countyEmail","phone","startDate","departureDate","departureReason"})fields.put(f,input(basics,RosterCore.label(f)+(f.endsWith("Date")?" (YYYY-MM-DD)":""),person.get(f),false));
  button(basics,"Generate county email",v->generateForEditor(),false);
  add(basics,text("Rule: "+data.countyPattern+"@"+data.countyDomain+". Generated addresses are suggestions; the county must issue the account and it still needs testing.",13,MUTED),8);
  LinearLayout leadership=card(page);section(leadership,"JUROR TITLES");add(leadership,text("Select any titles held by this member. Title names are edited in Config.",13,MUTED),8);
  for(int i=0;i<data.titles.length;i++)if(!data.titles[i].isEmpty()){String id=String.valueOf(i);CheckBox cb=new CheckBox(this);cb.setText(data.titles[i]);cb.setTextColor(INK);cb.setChecked(person.titleIds.contains(id));leadership.addView(cb);titleChecks.put(id,cb);}
  LinearLayout ops=card(page);section(ops,"COUNTY EMAIL & ONBOARDING");test=spinner(ops,"County email test",new String[]{"Not tested","Test sent","Confirmed","Failed"},person.emailTest);if(!person.emailTestAt.isEmpty())add(ops,text("Last test update: "+person.emailTestAt,12,MUTED),4);
  add(ops,text("Changing the county address resets confirmation. Save the new address before recording its test result.",13,MUTED),8);
  for(String s:RosterCore.CHECKS){CheckBox cb=new CheckBox(this);cb.setText(s);cb.setTextColor(INK);cb.setChecked(person.onboarding.contains(s));ops.addView(cb);checks.put(s,cb);}
  note=input(ops,"Foreperson operational notes",person.note,true);
  if(!person.previousEmails.isEmpty())add(ops,text("Previous email addresses:\n"+String.join("\n",person.previousEmails),13,MUTED),10);
  LinearLayout more=card(page);section(more,"COURT DETAILS");for(String f:new String[]{"position","badge","roomKey","street","unit","city","state","zip"})fields.put(f,input(more,RosterCore.label(f),person.get(f),false));
  LinearLayout lists=card(page);section(lists,"DISTRIBUTION LISTS");add(lists,text(person.lists.isEmpty()?"No confirmed list memberships recorded.":"Recorded memberships:\n"+String.join("\n",person.lists),14,MUTED),8);add(lists,text("Queue a task, make the actual list change separately, then mark it complete in Work. The app never sends emails or changes external lists.",13,MUTED),8);
  if(!editingId.isEmpty())button(lists,"Queue a list change",v->newTask(person),false);else add(lists,text("Save the member before adding list tasks.",13,MUTED),8);
  if(!editingId.isEmpty()){LinearLayout history=card(page);section(history,"RECENT MEMBER HISTORY");int count=0;for(int i=data.events.size()-1;i>=0&&count<12;i--){RosterCore.Event e=data.events.get(i);if(e.personId.equals(person.id)){add(history,text(e.at+"\n"+e.action+" · "+e.detail,12,MUTED),10);count++;}}}
  button(page,"Save member",v->saveEditor(),true);button(page,"Cancel",v->requestNav("Roster"),false);
 }
 private RosterCore.Person readDraft(){RosterCore.Person p=copy(draft);for(Map.Entry<String,EditText> e:fields.entrySet())p.court.put(e.getKey(),e.getValue().getText().toString().trim());p.court.put("role",role.getSelectedItem().toString());p.court.put("status",status.getSelectedItem().toString());p.emailTest=test.getSelectedItem().toString();p.note=note.getText().toString();if(!draft.get("countyEmail").equals(p.get("countyEmail"))&&!editingId.isEmpty()){p.emailTest="Not tested";p.emailTestAt="";}p.titleIds.clear();for(Map.Entry<String,CheckBox> e:titleChecks.entrySet())if(e.getValue().isChecked())p.titleIds.add(e.getKey());p.onboarding.clear();for(Map.Entry<String,CheckBox> e:checks.entrySet())if(e.getValue().isChecked())p.onboarding.add(e.getKey());return p;}
 private void saveEditor(){
  RosterCore.Person updated=readDraft();RosterCore.Person old=data.find(editingId);
  try{data.validate(updated);}catch(Exception e){error(e);return;}
  if(change(()->{
   if(old!=null){Map<String,String> before=new LinkedHashMap<>(old.court);String oldTest=old.emailTest;if(!updated.emailTest.equals(oldTest))updated.emailTestAt=RosterCore.now();if(!updated.titleIds.equals(old.titleIds))data.log(updated.id,"Juror titles changed",data.titleText(updated));if(!updated.note.equals(old.note))data.log(updated.id,"Operational notes changed","Notes updated");if(!updated.onboarding.equals(old.onboarding))data.log(updated.id,"Onboarding changed",String.join(", ",updated.onboarding));data.people.set(data.people.indexOf(old),updated);data.savePerson(updated,before,oldTest);}
   else{if(!updated.emailTest.equals("Not tested"))updated.emailTestAt=RosterCore.now();data.savePerson(updated,null,"");}
  })){navigate("Roster");toast("Member saved");}
 }
 private void generateForEditor(){
  try{RosterCore.Person candidate=readDraft();String generated=data.suggestCountyEmail(candidate),existing=fields.get("countyEmail").getText().toString().trim();
   if(existing.equalsIgnoreCase(generated)){toast("This address already matches the county rule.");return;}
   Runnable fill=()->{fields.get("countyEmail").setText(generated);test.setSelection(0);toast("Suggested address filled in. Save the member to keep it; the account still needs to be issued and tested.");};
   if(existing.isEmpty())fill.run();else new AlertDialog.Builder(this).setTitle("Replace county email?").setMessage(existing+"\n→ "+generated+"\n\nSaving will retain the previous address, reset email confirmation and queue updates for recorded list memberships.").setPositiveButton("Use generated address",(d,w)->fill.run()).setNegativeButton("Keep existing",null).show();
  }catch(Exception e){error(e);}
 }
 private void generateMissingEmails(){
  LinearLayout box=dialogBox();ScrollView sv=new ScrollView(this);sv.addView(box);add(box,text("Only active jurors and alternates with blank county addresses are included. Review each suggestion. This records an untested address; it does not create a county account.",14,MUTED),0);
  Map<String,String> candidates=new LinkedHashMap<>();Map<String,CheckBox> selected=new LinkedHashMap<>();Map<String,Integer> counts=new HashMap<>();Map<String,String> failures=new LinkedHashMap<>();
  for(RosterCore.Person p:data.people)if(p.active()&&p.get("countyEmail").isEmpty())try{String address=data.suggestCountyEmail(p);candidates.put(p.id,address);counts.put(address,counts.getOrDefault(address,0)+1);}catch(Exception e){failures.put(p.id,e.getMessage());}
  for(Map.Entry<String,String> entry:candidates.entrySet()){RosterCore.Person p=data.find(entry.getKey());if(counts.get(entry.getValue())>1){add(box,text(p.name()+": duplicate generated address; confirm a unique address with the county.",14,AMBER),12);continue;}CheckBox cb=new CheckBox(this);cb.setText(p.name()+"\n"+entry.getValue());cb.setTextColor(INK);cb.setChecked(true);box.addView(cb);selected.put(p.id,cb);}
  for(Map.Entry<String,String> entry:failures.entrySet())add(box,text(data.find(entry.getKey()).name()+": "+entry.getValue(),14,AMBER),12);
  if(candidates.isEmpty()&&failures.isEmpty()){toast("No active members have a blank county email.");return;}
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Review generated county emails").setView(sv).setPositiveButton("Save selected",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{List<String> ids=new ArrayList<>();for(Map.Entry<String,CheckBox> e:selected.entrySet())if(e.getValue().isChecked())ids.add(e.getKey());if(ids.isEmpty()){error(new Exception("Select an address to save, or cancel."));return;}
   if(change(()->{for(String id:ids){RosterCore.Person p=data.find(id);if(p==null||!p.active()||!p.get("countyEmail").isEmpty())throw new IllegalArgumentException("A selected member changed. Reopen the generation preview.");Map<String,String> old=new LinkedHashMap<>(p.court);String oldTest=p.emailTest;p.court.put("countyEmail",candidates.get(id));data.savePerson(p,old,oldTest);data.log(p.id,"County email generated",p.get("countyEmail")+" · untested");}})){dialog.dismiss();navigate("Roster");toast(ids.size()+" county email suggestions saved as Not tested.");}
  }));dialog.show();
 }
 private void work(){
  heading("Operational work","Pending list changes, email checks, and onboarding.");int count=0;
  for(RosterCore.Task t:data.tasks)if(t.status.equals("Pending")){count++;RosterCore.Person p=data.find(t.personId);LinearLayout c=card(page);add(c,text(t.action+" · "+t.list,20,INK),0);add(c,text(p==null?"Unknown member":p.name(),16,TEAL),6);add(c,text("Previous: "+t.oldEmail+"\nTarget: "+t.newEmail+"\n"+t.note,13,MUTED),8);button(c,"Mark actual change complete",v->new AlertDialog.Builder(this).setTitle("Confirm list change").setMessage("Has “"+t.list+"” actually been updated outside this app?").setPositiveButton("Yes, completed",(d,w)->{if(change(()->data.finishTask(t,true)))navigate("Work");}).setNegativeButton("Not yet",null).show(),true);button(c,"Cancel task",v->{if(change(()->data.finishTask(t,false)))navigate("Work");},false);}
  if(count==0)add(page,text("No pending distribution-list changes.",16,TEAL),18);
  LinearLayout needs=card(page);section(needs,"MEMBERS NEEDING FOLLOW-UP");int waiting=0;
  for(RosterCore.Person p:data.people)if(p.active()&&(!p.emailTest.equals("Confirmed")||p.onboarding.size()<RosterCore.CHECKS.length)){waiting++;button(needs,p.name()+" · "+p.emailTest+" · onboarding "+p.onboarding.size()+"/4",v->{editingId=p.id;editor(copy(p));},false);}if(waiting==0)add(needs,text("All active members have confirmed county email and completed onboarding.",14,TEAL),10);
  LinearLayout done=card(page);section(done,"RECENT LIST WORK");int shown=0;for(int i=data.tasks.size()-1;i>=0&&shown<15;i--){RosterCore.Task t=data.tasks.get(i);if(!t.status.equals("Pending")){RosterCore.Person p=data.find(t.personId);add(done,text(t.status+" · "+t.action+" · "+t.list+"\n"+(p==null?"":p.name())+" · "+t.completedAt,13,MUTED),12);shown++;}}
 }
 private void newTask(RosterCore.Person person){
  LinearLayout box=dialogBox();EditText list=input(box,"Distribution-list name","",false);Spinner action=spinner(box,"Change",new String[]{"Add","Update","Remove"},"Add");EditText email=input(box,"Target email",person.get("countyEmail"),false);EditText detail=input(box,"Task notes","",true);
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Queue list change").setView(box).setPositiveButton("Queue",null).setNegativeButton("Cancel",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String a=action.getSelectedItem().toString(),mail=email.getText().toString().trim();if(!a.equals("Remove")&&(mail.isEmpty()||!RosterCore.email(mail))){error(new Exception("Enter a valid target email."));return;}RosterCore.Person current=data.find(person.id);if(current==null)return;if(!current.active()&&!a.equals("Remove")){error(new Exception("Departed members can only have removal tasks."));return;}if(change(()->data.queue(current,a,list.getText().toString(),current.get("countyEmail"),mail,detail.getText().toString()))){d.dismiss();toast("Task queued; complete it in Work after updating the actual list.");}}));d.show();
 }
 private void imports(){
  heading("Court import review","Review source changes before they enter the working roster.");
  add(page,text("XLSX or UTF-8 CSV. Previous/personal and county emails remain separate. Source Position values are preserved; each sheet’s selected role sets current membership. Blank source cells leave existing values alone unless you explicitly choose otherwise.",14,MUTED),14);
  button(page,"Choose court spreadsheet",v->{if(data.pending()>0){error(new Exception("Resolve or skip the current review items before starting another import."));return;}pick(101,"*/*",new String[]{"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","text/csv","text/tab-separated-values"});},true);
  if(!data.importSource.isEmpty())add(page,text("Last staged file: "+data.importSource+"\n"+data.importedAt,12,MUTED),12);
  add(page,text(data.pending()+" changes awaiting review",20,INK),20);int shown=0;
  for(RosterCore.Proposal q:data.review)if(q.decision.equals("Pending")){shown++;LinearLayout c=card(page);RosterCore.Person p=data.find(q.personId);add(c,text(q.kind+" · "+(p==null?q.name():p.name()),19,INK),0);add(c,text(q.source,12,MUTED),6);if(!q.warning.isEmpty())add(c,text(q.warning,14,AMBER),8);int n=0;if(!q.kind.equals("Missing"))for(String f:q.incoming.keySet())if(p==null||!p.get(f).equals(q.incoming.get(f))){n++;if(n<=4)add(c,text(RosterCore.label(f)+": "+(p==null?"":p.get(f)+" → ")+q.incoming.get(f),13,MUTED),6);}if(n>4)add(c,text("+ "+(n-4)+" more fields",12,MUTED),6);button(c,"Review this item",v->review(q),false);}
  if(shown==0)add(page,text("No pending changes. Choosing a spreadsheet only stages a review; it never replaces operational notes or onboarding checks.",15,TEAL),14);
 }
 private void configureImport(){
  screen="Import";page.removeAllViews();scroll.scrollTo(0,0);heading("Set up court import",sourceName+" → "+data.term);sheetInclude.clear();sheetRole.clear();sheetStatus.clear();sheetMaps.clear();
  add(page,text("Confirm each sheet’s current role and status. An old alternate number in Position does not override these selections. Check the column mappings before staging.",14,MUTED),14);
  for(CourtFile.Sheet s:sheets){LinearLayout c=card(page);CheckBox use=new CheckBox(this);use.setText(s.name);use.setTextColor(INK);use.setChecked(true);c.addView(use);sheetInclude.add(use);sheetRole.add(spinner(c,"Current role for this sheet",new String[]{"Juror","Alternate"},s.role));sheetStatus.add(spinner(c,"Service status for this sheet",new String[]{"Active","Resigned","Excused","Completed"},s.status));add(c,text((s.rows.size()-s.header-1)+" rows after detected header (blank names are ignored)",12,MUTED),7);
   Map<Integer,Spinner> mapping=new LinkedHashMap<>();sheetMaps.add(mapping);LinearLayout maps=column();maps.setVisibility(View.GONE);button(c,"Show / hide column mappings",v->maps.setVisibility(maps.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE),false);add(c,maps,0);
   List<String> header=s.rows.get(s.header);String[] choices=new String[RosterCore.FIELDS.length+1];choices[0]="Ignore column";System.arraycopy(RosterCore.LABELS,0,choices,1,RosterCore.LABELS.length);
   for(int col=0;col<header.size();col++){String h=header.get(col);if(h.trim().isEmpty())continue;String field=CourtFile.field(h),selected=field.isEmpty()?choices[0]:RosterCore.label(field);mapping.put(col,spinner(maps,h,choices,selected));}
  }
  CheckBox missing=new CheckBox(this);missing.setText("Flag active members missing from the selected sheets");missing.setTextColor(INK);missing.setChecked(false);add(page,missing,16);
  CheckBox blanks=new CheckBox(this);blanks.setText("Propose blank cells as field clears (requires review)");blanks.setTextColor(INK);add(page,blanks,8);
  button(page,"Stage differences for review",v->{
   try{List<RosterCore.SourceRow> rows=new ArrayList<>();for(int i=0;i<sheets.size();i++)if(sheetInclude.get(i).isChecked()){
     CourtFile.Sheet s=sheets.get(i);s.role=sheetRole.get(i).getSelectedItem().toString();s.status=sheetStatus.get(i).getSelectedItem().toString();Map<Integer,String> mapped=new LinkedHashMap<>();Set<String> used=new HashSet<>();for(Map.Entry<Integer,Spinner> m:sheetMaps.get(i).entrySet()){int at=m.getValue().getSelectedItemPosition();if(at>0){String f=RosterCore.FIELDS[at-1];if(!used.add(f))throw new Exception("Two columns map to "+RosterCore.label(f)+" on "+s.name+". Resolve the duplicate mapping.");mapped.put(m.getKey(),f);}}
     if(!used.contains("firstName")||!used.contains("lastName"))throw new Exception("Map First name and Last name on "+s.name+".");rows.addAll(CourtFile.extract(s,mapped));
    }if(rows.isEmpty())throw new Exception("No named member rows found in the selected sheets.");final List<RosterCore.SourceRow> incoming=rows;if(change(()->data.stage(incoming,sourceName,missing.isChecked(),blanks.isChecked()))){sheets=null;navigate("Import");toast("Differences staged. No member records were changed.");}
   }catch(Exception e){error(e);}
  },true);button(page,"Cancel import",v->{sheets=null;navigate("Import");},false);
 }
 private void review(RosterCore.Proposal q){
  if(q.kind.equals("Missing")){new AlertDialog.Builder(this).setTitle("Member not found in source").setMessage(q.name()+"\n\n"+q.warning).setPositiveButton("Retain member",(d,w)->{if(change(()->data.skip(q)))navigate("Import");}).setNeutralButton("Edit member",(d,w)->{RosterCore.Person p=data.find(q.personId);if(p!=null){editingId=p.id;editor(copy(p));}}).setNegativeButton("Later",null).show();return;}
  LinearLayout box=dialogBox();ScrollView sv=new ScrollView(this);sv.addView(box);if(!q.warning.isEmpty())add(box,text(q.warning,14,AMBER),0);
  List<RosterCore.Person> candidates=new ArrayList<>(data.people);Collections.sort(candidates,(a,b)->a.name().compareToIgnoreCase(b.name()));List<String> names=new ArrayList<>();names.add(q.kind.equals("New")?"Create new member":"Choose existing member (required)");int matched=0;for(int i=0;i<candidates.size();i++){RosterCore.Person p=candidates.get(i);names.add(p.name()+" · "+p.get("status"));if(p.id.equals(q.personId))matched=i+1;}
  Spinner match=spinner(box,"Apply to",names.toArray(new String[0]),names.get(matched));
  Map<String,CheckBox> selections=new LinkedHashMap<>();RosterCore.Person current=data.find(q.personId);
  add(box,text("Select court fields to accept. Operational notes, onboarding and recorded list memberships are not imported. A county address change resets its email confirmation and queues list updates.",13,MUTED),12);
  for(String f:RosterCore.FIELDS)if(q.incoming.containsKey(f)){String incoming=q.incoming.get(f);if(current!=null&&incoming.equals(current.get(f)))continue;CheckBox cb=new CheckBox(this);cb.setText(RosterCore.label(f)+"\n"+(current==null?"":(current.get(f).isEmpty()?"(blank)":current.get(f))+" → ")+(incoming.isEmpty()?"(clear field)":incoming));cb.setTextColor(INK);cb.setChecked(true);box.addView(cb);selections.put(f,cb);}
  AlertDialog d=new AlertDialog.Builder(this).setTitle(q.kind+" · "+q.name()).setView(sv).setPositiveButton("Accept selected",null).setNeutralButton("Skip item",null).setNegativeButton("Later",null).create();
  d.setOnShowListener(x->{d.getButton(-1).setOnClickListener(v->{Set<String> selected=new LinkedHashSet<>();for(Map.Entry<String,CheckBox> e:selections.entrySet())if(e.getValue().isChecked())selected.add(e.getKey());if(selected.isEmpty()){error(new Exception("Select at least one field, or skip this item."));return;}int index=match.getSelectedItemPosition();String id=index==0?"":candidates.get(index-1).id;
   if(!q.kind.equals("New")&&index==0){error(new Exception("Match this conflict to an existing member, or skip it and correct the source file."));return;}
   if(change(()->data.apply(q,selected,id))){d.dismiss();navigate("Import");}
  });d.getButton(-3).setOnClickListener(v->{if(change(()->data.skip(q))){d.dismiss();navigate("Import");}});});d.show();
 }
 private void configuration(){
  heading("Configuration","Roster filters, app identity, term years and juror titles.");
  LinearLayout filters=card(page);section(filters,"ROSTER FILTERS");add(filters,text("Choose the groups shown on the roster page. Active jurors means regular seats; active alternates are separate. Other departures includes excused and completed service. Seat and vacancy totals always cover the full roster.",14,MUTED),8);
  Map<String,CheckBox> groups=new LinkedHashMap<>();for(String group:RosterCore.ROSTER_GROUPS){CheckBox cb=new CheckBox(this);cb.setText(group);cb.setTextColor(INK);cb.setChecked(data.rosterGroups.contains(group));filters.addView(cb);groups.put(group,cb);}
  button(filters,"Apply roster filters",v->{Set<String> selected=new LinkedHashSet<>();for(Map.Entry<String,CheckBox> entry:groups.entrySet())if(entry.getValue().isChecked())selected.add(entry.getKey());if(change(()->data.setRosterGroups(selected))){navigate("Roster");toast("Roster filters saved");}},true);
  LinearLayout identity=card(page);section(identity,"APP & TERM");
  EditText name=input(identity,"App display name",data.appName,false),years=input(identity,"Term years (YYYY-YYYY)",data.term,false),seats=input(identity,"Regular seats",String.valueOf(data.seats),false),alternates=input(identity,"Alternate slots",String.valueOf(data.alternateSlots),false);seats.setInputType(2);alternates.setInputType(2);
  add(identity,text("The name appears in the app header, with the years on a separate line. The Android launcher name is Civil Grand Jury Roster.",13,MUTED),10);
  LinearLayout titles=card(page);section(titles,"JUROR TITLES");add(titles,text("Five starting titles plus three extra slots. Rename a slot to update its name for every assigned member. Leave unused slots blank; unassign a title before clearing its slot.",14,MUTED),10);
  List<EditText> editors=new ArrayList<>();for(int i=0;i<8;i++)editors.add(input(titles,"Title "+(i+1)+(i>=5?" (extra)":""),data.titles[i],false));
  button(page,"Save configuration",v->{try{String[] names=new String[8];for(int i=0;i<8;i++)names[i]=editors.get(i).getText().toString();int regular=Integer.parseInt(seats.getText().toString()),alt=Integer.parseInt(alternates.getText().toString());if(change(()->data.configure(name.getText().toString(),years.getText().toString(),regular,alt,names))){shell();navigate("Config");toast("Configuration saved; title assignments preserved.");}}catch(NumberFormatException e){error(new Exception("Seat counts must be whole numbers."));}},true);
 }
 private void handoff(){
  heading("Term & handoff","Prepare the next foreperson without losing operational history.");
  LinearLayout settings=card(page);section(settings,"LEADERSHIP NOTES");add(settings,text("Term: "+data.term+". Edit app name, years, capacity and title names in Config.",14,MUTED),8);EditText foreperson=input(settings,"Foreperson / roster owner",data.foreperson,false),notes=input(settings,"Leadership handoff notes",data.handoffNotes,true);
  button(settings,"Save leadership notes",v->{if(change(()->{data.foreperson=foreperson.getText().toString();data.handoffNotes=notes.getText().toString();data.log("","Leadership notes updated",data.term);})){toast("Leadership notes saved");navigate("Handoff");}},true);
  LinearLayout emailSettings=card(page);section(emailSettings,"COUNTY EMAIL GENERATION");
  EditText domain=input(emailSettings,"County domain",data.countyDomain,false),pattern=input(emailSettings,"Address pattern",data.countyPattern,false);
  add(emailSettings,text("Amador default: {first_initial}{last}@amadorcounty.gov. Other examples: {first}.{last} or {first}_{last}. Spaces, accents, apostrophes and hyphens in names are normalized; review unusual names and duplicate initials manually.",13,MUTED),10);
  button(emailSettings,"Save email rule",v->{try{String d=domain.getText().toString().trim().toLowerCase(java.util.Locale.ROOT),p=pattern.getText().toString().trim();String example=RosterCore.generatedEmail("Alex","Sample",d,p);if(change(()->{data.countyDomain=d;data.countyPattern=p;data.log("","County email rule updated",p+"@"+d);})){toast("Rule saved. Example: "+example);navigate("Handoff");}}catch(Exception e){error(e);}},false);
  button(emailSettings,"Generate missing county emails",v->generateMissingEmails(),true);
  add(emailSettings,text("Generation uses the saved rule above, skips existing county addresses and departed members, and flags collisions. Every generated address starts as Not tested. Existing members can also use Generate county email in their contact form.",13,MUTED),10);
  LinearLayout exports=card(page);section(exports,"EXPORT & RESTORE");add(exports,text("A handoff file includes every member, previous email, operational check, list task, import decision, and history entry. It is encrypted with a passphrase you choose. Keep the passphrase separately; it cannot be recovered.",14,MUTED),10);
  button(exports,"Create encrypted handoff",v->exportHandoff(),true);button(exports,"Restore a handoff file",v->pick(102,"*/*",null),false);
  add(exports,text("CSV is an unencrypted contact roster for spreadsheet use. It includes personal emails and notes, but not the complete work/history. Use the encrypted handoff for leadership transfer.",13,MUTED),14);
  button(exports,"Export roster CSV",v->new AlertDialog.Builder(this).setTitle("Export personal contact data?").setMessage("This unencrypted CSV includes emails, addresses, operational notes and departures. Choose a location appropriate for this information.").setPositiveButton("Choose location",(d,w)->{pendingExport=Store.csv(data);create("text/csv","jury-roster-"+safeTerm()+".csv");}).setNegativeButton("Cancel",null).show(),false);
  File recovery=new File(getFilesDir(),"before-restore.json");if(recovery.exists())button(exports,"Recover roster from before last restore",v->{try{confirmRestore(Store.decode(read(new FileInputStream(recovery))),"previous local copy");}catch(Exception e){error(e);}},false);
  LinearLayout history=card(page);section(history,"RECENT ACTIVITY");if(data.events.isEmpty())add(history,text("No roster changes yet.",14,MUTED),10);for(int i=data.events.size()-1;i>=Math.max(0,data.events.size()-25);i--){RosterCore.Event e=data.events.get(i);RosterCore.Person p=data.find(e.personId);add(history,text(e.at+"\n"+e.action+(p==null?"":" · "+p.name())+"\n"+e.detail,12,MUTED),12);}
  add(page,text("Local app storage only. No accounts, analytics, or network permission. Attendance and quorum are reserved for a later version.",13,MUTED),18);
 }
 private void exportHandoff(){
  LinearLayout box=dialogBox();EditText pass=input(box,"Passphrase (12+ characters)","",false),again=input(box,"Repeat passphrase","",false);pass.setInputType(129);again.setInputType(129);
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Protect the handoff file").setView(box).setPositiveButton("Prepare file",null).setNegativeButton("Cancel",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String p=pass.getText().toString();if(p.length()<12||!p.equals(again.getText().toString())){error(new Exception("Use matching passphrases of at least 12 characters."));return;}try{byte[] json=Store.encode(data);char[] password=p.toCharArray();d.dismiss();background("Encrypting handoff…",()->{try{return HandoffCrypto.encrypt(json,password);}finally{Arrays.fill(password,'\0');}},bytes->{pendingExport=bytes;create("application/octet-stream","jury-roster-"+safeTerm()+".jury");});}catch(Exception e){error(e);}}));d.show();
 }
 private String safeTerm(){return data.term.replaceAll("[^a-zA-Z0-9-]","_");}
 private void restorePassword(byte[] encrypted,String name){
  LinearLayout box=dialogBox();EditText pass=input(box,"Handoff passphrase","",false);pass.setInputType(129);
  AlertDialog d=new AlertDialog.Builder(this).setTitle("Unlock handoff").setView(box).setPositiveButton("Unlock",null).setNegativeButton("Cancel",null).create();d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{char[] password=pass.getText().toString().toCharArray();d.dismiss();background("Unlocking handoff…",()->{try{return HandoffCrypto.decrypt(encrypted,password);}catch(javax.crypto.AEADBadTagException wrong){throw new Exception("Passphrase is incorrect, or this file was altered.");}finally{Arrays.fill(password,'\0');}},bytes->{try{confirmRestore(Store.decode(bytes),name);}catch(Exception e){error(e);}});}));d.show();
 }
 private void confirmRestore(RosterCore incoming,String name){
  new AlertDialog.Builder(this).setTitle("Replace working roster?").setMessage("File: "+name+"\nTerm: "+incoming.term+"\nMembers: "+incoming.people.size()+"\nRegular: "+incoming.count("Juror")+" · Alternates: "+incoming.count("Alternate")+" · Resigned: "+incoming.resigned()+"\nPending review: "+incoming.pending()+"\n\nThis replaces the current roster, including its operational updates. A local copy of the current roster will be kept for recovery.").setPositiveButton("Restore this roster",(d,w)->{try{byte[] previous=Store.encode(data);android.util.AtomicFile before=new android.util.AtomicFile(new File(getFilesDir(),"before-restore.json"));FileOutputStream stream=before.startWrite();try{stream.write(previous);before.finishWrite(stream);}catch(Exception e){before.failWrite(stream);throw e;}incoming.log("","Handoff restored",name);store.save(incoming);data=incoming;shell();navigate("Roster");toast("Handoff restored");}catch(Exception e){error(e);}}).setNegativeButton("Keep current roster",null).show();
 }
 private void pick(int request,String mime,String[] extras){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);if(extras!=null)i.putExtra(Intent.EXTRA_MIME_TYPES,extras);try{startActivityForResult(i,request);}catch(ActivityNotFoundException e){error(new Exception("No document picker is available on this device."));}}
 private void create(String mime,String name){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);i.putExtra(Intent.EXTRA_TITLE,name);try{startActivityForResult(i,103);}catch(ActivityNotFoundException e){pendingExport=null;error(e);}}
 @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);if(result!=RESULT_OK||intent==null||intent.getData()==null){if(request==103)pendingExport=null;return;}Uri uri=intent.getData();
  if(request==103){if(pendingExport==null){error(new Exception("Export was interrupted. Choose Export again."));return;}final byte[] bytes=pendingExport;pendingExport=null;background("Saving export…",()->{try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Cannot open export destination.");out.write(bytes);out.flush();}return bytes;},ignored->toast("Export saved"));return;}
  String filename="Selected file";try(android.database.Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())filename=c.getString(0);}catch(Exception ignored){}final String name=filename;
  if(request==101){background("Reading court spreadsheet…",()->read(getContentResolver().openInputStream(uri)),bytes->{try{sheets=CourtFile.read(bytes);sourceName=name;configureImport();}catch(Exception e){error(e);}});}
  else if(request==102)background("Reading handoff file…",()->read(getContentResolver().openInputStream(uri)),bytes->restorePassword(bytes,name));
 }
 private byte[] read(InputStream input)throws Exception{if(input==null)throw new IOException("Cannot open the selected file.");try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>CourtFile.MAX_FILE)throw new IOException("File exceeds 8 MB.");out.write(buf,0,n);}return out.toByteArray();}}
 private interface Job{byte[] run()throws Exception;}private interface Result{void done(byte[] bytes);}
 private void background(String message,Job job,Result result){if(busy){error(new Exception("A file operation is already running."));return;}busy=true;ProgressDialog progress=new ProgressDialog(this);progress.setMessage(message);progress.setCancelable(false);progress.show();worker.submit(()->{try{byte[] bytes=job.run();main.post(()->{busy=false;if(isDestroyed()||isFinishing())return;progress.dismiss();result.done(bytes);});}catch(Exception e){main.post(()->{busy=false;if(isDestroyed()||isFinishing())return;progress.dismiss();error(e);});}});}
 private boolean change(Runnable operation){byte[] before=null;try{before=Store.encode(data);operation.run();store.save(data);return true;}catch(Exception e){if(before!=null)try{data=Store.decode(before);}catch(Exception fatal){errorScreen("Roster operation failed. Restart the app to reload saved data.");}error(e);return false;}}
 private void error(Exception e){new AlertDialog.Builder(this).setTitle("Please check").setMessage(e.getMessage()==null?e.toString():e.getMessage()).setPositiveButton("OK",null).show();}
 private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private LinearLayout dialogBox(){LinearLayout l=column();l.setPadding(dp(20),dp(12),dp(20),dp(12));return l;}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 private void add(LinearLayout p,View v,int top){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(top);p.addView(v,lp);}
 private LinearLayout card(LinearLayout parent){LinearLayout c=column();c.setPadding(dp(18),dp(18),dp(18),dp(18));GradientDrawable bg=new GradientDrawable();bg.setColor(WHITE);bg.setCornerRadius(dp(18));bg.setStroke(dp(1),0xffdfe7eb);c.setBackground(bg);add(parent,c,16);return c;}
 private void section(LinearLayout p,String s){TextView t=text(s,12,TEAL);t.setLetterSpacing(.09f);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);add(p,t,0);}
 private EditText input(LinearLayout p,String label,String value,boolean multiline){add(p,text(label,13,MUTED),12);EditText e=new EditText(this);e.setText(value);e.setTextColor(INK);e.setTextSize(16);e.setInputType(multiline?131073:1);e.setMinLines(multiline?3:1);e.setSingleLine(!multiline);if(label.toLowerCase(Locale.ROOT).contains("email"))e.setInputType(33);add(p,e,2);return e;}
 private Spinner spinner(LinearLayout p,String label,String[] values,String selected){add(p,text(label,13,MUTED),12);Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);s.setAdapter(a);int at=Arrays.asList(values).indexOf(selected);s.setSelection(Math.max(0,at));s.setMinimumHeight(dp(48));add(p,s,2);return s;}
 private void button(LinearLayout p,String label,View.OnClickListener action,boolean primary){Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(primary?WHITE:TEAL);b.setMinHeight(dp(48));GradientDrawable bg=new GradientDrawable();bg.setColor(primary?TEAL:0xffe8f2f0);bg.setCornerRadius(dp(10));b.setBackground(bg);b.setPadding(dp(12),dp(10),dp(12),dp(10));b.setOnClickListener(action);add(p,b,12);}
}



