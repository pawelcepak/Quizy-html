package pl.szynolandia.szybkaklawiatura;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FastKeyboardService extends InputMethodService {
    public static final String PROFILE_WORK = "WORK";
    public static final String PROFILE_NORMAL = "NORMAL";
    private static final Locale PL = new Locale("pl", "PL");
    private static final String[][] ROWS = {{"q","w","e","r","t","y","u","i","o","p"},{"a","s","d","f","g","h","j","k","l"},{"z","c","b","n","m","⌫"},{"PROFIL",","," ",".","WKLEJ","⏎"}};
    private static final Map<String,String[]> LONG_PRESS = new HashMap<>();
    static {
        LONG_PRESS.put("a",new String[]{"ą"}); LONG_PRESS.put("c",new String[]{"ć"}); LONG_PRESS.put("e",new String[]{"ę"});
        LONG_PRESS.put("l",new String[]{"ł"}); LONG_PRESS.put("n",new String[]{"ń"}); LONG_PRESS.put("o",new String[]{"ó"});
        LONG_PRESS.put("s",new String[]{"ś"}); LONG_PRESS.put("z",new String[]{"ź","ż"}); LONG_PRESS.put(".",new String[]{"?","!"});
    }

    private LearningStore store;
    private PolishLanguageCorrector polish;
    private SharedPreferences prefs;
    private ClipboardManager clipboard;
    private final StringBuilder token = new StringBuilder();
    private final List<TextView> suggestionViews = new ArrayList<>();
    private Button profileButton;
    private String previousWord="", pendingWord="", pendingPreviousWord="";
    private boolean learningAllowedInField=true, sentenceStart=true, tokenCapitalized=false;
    private final Handler repeatHandler=new Handler(Looper.getMainLooper());
    private boolean repeatingBackspace=false;
    private final Runnable repeatDelete=new Runnable(){ @Override public void run(){ if(!repeatingBackspace)return; handleBackspace(); repeatHandler.postDelayed(this,65); }};

    @Override public void onCreate(){
        super.onCreate(); store=new LearningStore(this); polish=new PolishLanguageCorrector();
        prefs=getSharedPreferences("keyboard_prefs",MODE_PRIVATE); clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override public View onCreateInputView(){
        suggestionViews.clear();
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(1),dp(2),dp(1),dp(3)); root.setBackgroundColor(Color.rgb(29,31,40));
        LinearLayout suggestions=new LinearLayout(this); suggestions.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<3;i++){
            TextView t=new TextView(this); t.setGravity(Gravity.CENTER); t.setTextSize(i==1?21:19); t.setTextColor(Color.WHITE); t.setSingleLine(true); t.setEllipsize(TextUtils.TruncateAt.END); t.setBackgroundColor(Color.rgb(56,57,65));
            t.setOnClickListener(v->{ v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); useSuggestion(((TextView)v).getText().toString()); });
            t.setOnLongClickListener(v->{ String value=((TextView)v).getText().toString(); if(!TextUtils.isEmpty(value)&&PROFILE_WORK.equals(profile())){ store.forgetWord(PROFILE_WORK,value); Toast.makeText(this,"Usunięto z nauki: "+value,Toast.LENGTH_SHORT).show(); refreshSuggestions(); return true;} return false; });
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(50),1f); p.setMargins(dp(1),0,dp(1),dp(2)); suggestions.addView(t,p); suggestionViews.add(t);
        }
        root.addView(suggestions);
        for(int r=0;r<ROWS.length;r++){
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_HORIZONTAL);
            if(r==1)addSpacer(row,.18f); else if(r==2)addSpacer(row,.30f);
            for(String key:ROWS[r]){ Button b=makeKey(key); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(66),keyWeight(key)); p.setMargins(0,dp(1),0,dp(1)); row.addView(b,p); if("PROFIL".equals(key))profileButton=b; }
            if(r==1)addSpacer(row,.18f); else if(r==2)addSpacer(row,.30f);
            root.addView(row,new LinearLayout.LayoutParams(-1,dp(68)));
        }
        refreshSuggestions(); return root;
    }

    private Button makeKey(String key){
        Button b=new Button(this); b.setText("PROFIL".equals(key)?profileLabel():key); b.setGravity(Gravity.CENTER); b.setTextSize(key.length()>3?13:26); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setMinWidth(0); b.setMinimumWidth(0); b.setPadding(0,0,0,0); b.setBackground(keyBackground());
        if("⌫".equals(key)) setupBackspace(b); else {
            b.setOnClickListener(v->{ v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); handleKey(key); });
            if(LONG_PRESS.containsKey(key)) b.setOnLongClickListener(v->{ v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); showAlternatives(v,key,LONG_PRESS.get(key)); return true; });
        }
        return b;
    }
    private GradientDrawable keyBackground(){ GradientDrawable d=new GradientDrawable(); d.setColor(Color.rgb(62,63,72)); d.setCornerRadius(dp(6)); d.setStroke(dp(1),Color.rgb(18,19,24)); return d; }
    private void addSpacer(LinearLayout row,float weight){ row.addView(new View(this),new LinearLayout.LayoutParams(0,1,weight)); }
    private float keyWeight(String k){ if(" ".equals(k))return 3.8f; if("PROFIL".equals(k))return 1.65f; if("WKLEJ".equals(k))return 1.35f; if("⌫".equals(k))return 1.35f; if("⏎".equals(k))return 1.2f; return 1f; }

    private void setupBackspace(Button b){
        b.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);handleBackspace();});
        b.setOnTouchListener((v,e)->{ if(e.getAction()==MotionEvent.ACTION_DOWN){repeatingBackspace=true;repeatHandler.postDelayed(repeatDelete,380);} else if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){repeatingBackspace=false;repeatHandler.removeCallbacks(repeatDelete);} return false; });
    }

    private void showAlternatives(View anchor,String source,String[] alternatives){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.HORIZONTAL); box.setPadding(dp(3),dp(3),dp(3),dp(3)); box.setBackgroundColor(Color.rgb(35,36,44)); final PopupWindow[] holder=new PopupWindow[1];
        for(String alt:alternatives){ Button c=new Button(this); c.setText(alt); c.setTextSize(24); c.setTextColor(Color.WHITE); c.setAllCaps(false); c.setBackground(keyBackground()); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(66),dp(66)); p.setMargins(dp(2),dp(2),dp(2),dp(2)); box.addView(c,p); c.setOnClickListener(v->{insertAlternative(source,alt);if(holder[0]!=null)holder[0].dismiss();}); }
        PopupWindow popup=new PopupWindow(box,ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,true); holder[0]=popup; popup.setOutsideTouchable(true); popup.setElevation(dp(8)); popup.showAsDropDown(anchor,0,-dp(142),Gravity.CENTER_HORIZONTAL);
    }

    private void insertAlternative(String source,String value){
        if(getCurrentInputConnection()==null)return;
        if(".".equals(source)){ finishToken(false);commitPendingLearning();getCurrentInputConnection().commitText(value,1);sentenceStart=true;refreshSuggestions();return; }
        commitPendingLearning(); String out=value; if(token.length()==0&&sentenceStart){out=value.toUpperCase(PL);tokenCapitalized=true;sentenceStart=false;} getCurrentInputConnection().commitText(out,1);token.append(value.toLowerCase(PL));refreshSuggestions();
    }

    @Override public void onStartInput(EditorInfo info,boolean restarting){ super.onStartInput(info,restarting); token.setLength(0);previousWord="";pendingWord="";pendingPreviousWord="";learningAllowedInField=!isSensitiveField(info);sentenceStart=detectSentenceStart();tokenCapitalized=false;refreshSuggestions(); }
    @Override public void onFinishInput(){ repeatingBackspace=false;repeatHandler.removeCallbacks(repeatDelete);commitPendingLearning();super.onFinishInput(); }

    private void handleKey(String key){
        if(getCurrentInputConnection()==null)return;
        if("⏎".equals(key)){finishToken(false);commitPendingLearning();getCurrentInputConnection().commitText("\n",1);previousWord="";sentenceStart=true;tokenCapitalized=false;refreshSuggestions();return;}
        if("PROFIL".equals(key)){commitPendingLearning();toggleProfile();return;}
        if("WKLEJ".equals(key)){commitPendingLearning();pasteClipboard();return;}
        if(" ".equals(key)){finishToken(true);return;}
        if(",".equals(key)||".".equals(key)){finishToken(false);commitPendingLearning();getCurrentInputConnection().commitText(key,1);if(".".equals(key))sentenceStart=true;refreshSuggestions();return;}
        if(key.length()==1&&Character.isLetter(key.charAt(0))){
            commitPendingLearning(); String out=key; if(token.length()==0&&sentenceStart){out=key.toUpperCase(PL);tokenCapitalized=true;sentenceStart=false;} getCurrentInputConnection().commitText(out,1);token.append(key.toLowerCase(PL));refreshSuggestions();
        }
    }

    private void handleBackspace(){
        if(getCurrentInputConnection()==null)return;
        if(token.length()>0){getCurrentInputConnection().deleteSurroundingText(1,0);token.deleteCharAt(token.length()-1);if(token.length()==0&&tokenCapitalized){sentenceStart=true;tokenCapitalized=false;}}
        else if(!pendingWord.isEmpty()){getCurrentInputConnection().deleteSurroundingText(1,0);token.append(pendingWord);previousWord=pendingPreviousWord;pendingWord="";pendingPreviousWord="";}
        else getCurrentInputConnection().deleteSurroundingText(1,0);
        refreshSuggestions();
    }

    private void finishToken(boolean addSpace){
        if(token.length()==0){if(addSpace)getCurrentInputConnection().commitText(" ",1);return;}
        String typed=LearningStore.normalizeWord(token.toString()); String finalWord=typed;
        if(!typed.isEmpty()&&prefs.getBoolean("autocorrect",true)){
            String corrected=polish.correctDiacritics(typed);
            if(corrected.equals(typed)) corrected=PolishAutocorrect.correct(typed);
            if(corrected.equals(typed)&&!store.isKnown(profile(),typed)) corrected=store.bestCorrection(profile(),typed);
            if(!corrected.equals(typed)){
                getCurrentInputConnection().deleteSurroundingText(token.length(),0); String display=tokenCapitalized?capitalize(corrected):corrected; getCurrentInputConnection().commitText(display,1); finalWord=corrected;
            }
        }
        pendingWord=finalWord;pendingPreviousWord=previousWord;if(!finalWord.isEmpty())previousWord=finalWord;token.setLength(0);tokenCapitalized=false;if(addSpace)getCurrentInputConnection().commitText(" ",1);refreshSuggestions();
    }

    private void commitPendingLearning(){ if(pendingWord.isEmpty())return; if(PROFILE_WORK.equals(profile())&&learningAllowedInField)store.learn(PROFILE_WORK,pendingPreviousWord,pendingWord); pendingWord="";pendingPreviousWord=""; }
    private void useSuggestion(String value){ if(TextUtils.isEmpty(value)||getCurrentInputConnection()==null)return;commitPendingLearning();String word=LearningStore.normalizeWord(value);getCurrentInputConnection().deleteSurroundingText(token.length(),0);String display=(token.length()==0&&sentenceStart)?capitalize(word):word;getCurrentInputConnection().commitText(display,1);token.setLength(0);token.append(word);tokenCapitalized=display.length()>0&&Character.isUpperCase(display.charAt(0));sentenceStart=false;refreshSuggestions(); }
    private void pasteClipboard(){ if(clipboard==null||!clipboard.hasPrimaryClip()||getCurrentInputConnection()==null)return;ClipData d=clipboard.getPrimaryClip();if(d==null||d.getItemCount()==0)return;CharSequence text=d.getItemAt(0).coerceToText(this);if(!TextUtils.isEmpty(text)){getCurrentInputConnection().commitText(text,1);token.setLength(0);previousWord="";sentenceStart=endsSentence(text.toString());refreshSuggestions();} }

    private void refreshSuggestions(){
        if(suggestionViews.isEmpty()||store==null)return;List<String> ranked=store.suggest(profile(),token.toString(),previousWord,3);String[] display={"","",""};
        if(ranked.size()==1)display[1]=ranked.get(0); else if(ranked.size()==2){display[0]=ranked.get(1);display[1]=ranked.get(0);} else if(ranked.size()>=3){display[0]=ranked.get(1);display[1]=ranked.get(0);display[2]=ranked.get(2);}
        for(int i=0;i<suggestionViews.size();i++){String s=display[i];if(sentenceStart&&token.length()==0&&!s.isEmpty())s=capitalize(s);suggestionViews.get(i).setText(s);} if(profileButton!=null)profileButton.setText(profileLabel());
    }

    private void toggleProfile(){String next=PROFILE_WORK.equals(profile())?PROFILE_NORMAL:PROFILE_WORK;prefs.edit().putString("profile",next).apply();token.setLength(0);previousWord="";pendingWord="";pendingPreviousWord="";refreshSuggestions();}
    private boolean detectSentenceStart(){try{CharSequence before=getCurrentInputConnection()==null?null:getCurrentInputConnection().getTextBeforeCursor(80,0);if(before==null||before.length()==0)return true;for(int i=before.length()-1;i>=0;i--){char c=before.charAt(i);if(Character.isWhitespace(c))continue;return c=='.'||c=='!'||c=='?'||c=='\n';}}catch(Exception ignored){}return true;}
    private boolean endsSentence(String s){for(int i=s.length()-1;i>=0;i--){char c=s.charAt(i);if(Character.isWhitespace(c))continue;return c=='.'||c=='!'||c=='?'||c=='\n';}return false;}
    private String capitalize(String s){if(s==null||s.isEmpty())return s;return s.substring(0,1).toUpperCase(PL)+s.substring(1);}
    private boolean isSensitiveField(EditorInfo info){if(info==null)return false;int cls=info.inputType&InputType.TYPE_MASK_CLASS,variation=info.inputType&InputType.TYPE_MASK_VARIATION;if(cls==InputType.TYPE_CLASS_TEXT)return variation==InputType.TYPE_TEXT_VARIATION_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD||variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;return cls==InputType.TYPE_CLASS_NUMBER&&variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD;}
    private String profile(){return prefs.getString("profile",PROFILE_WORK);} private String profileLabel(){return PROFILE_WORK.equals(profile())?"PRACA":"NORMAL";} private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
