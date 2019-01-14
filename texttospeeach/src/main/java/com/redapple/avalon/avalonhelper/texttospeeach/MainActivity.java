package com.redapple.avalon.avalonhelper.texttospeeach;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import com.redapple.avalon.avalonhelper.texttospeeach.Config.Option;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnInitListener {

  private Config mConfig;
  private TextToSpeech mTTS;
  private ScriptGenerator mGenerator;
  private ListView listView;
  private EditText editText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mTTS = new TextToSpeech(this, this);

    mConfig = new Config();
    mConfig.load(this);

    // Populate a SimpleAdapter with Option data
    String[] from = {
        "text1",
        "text2",
        "img1",
    };
    int[] to = {
        android.R.id.text1,
        android.R.id.text2,
        R.id.character_image_view,
    };

    List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

    for (Option option : Config.OPTIONS_ORDERED) {
      Map<String, Object> row = new HashMap<String, Object>();
      row.put(from[0], getString(option.getTitleResId()));
      row.put(from[1], getString(option.getDescResId()));
      row.put(from[2], option.getImgResId());
      data.add(row);
    }

    // Configure ListView for multiple choice mode (and set checked items)
    listView = findViewById(R.id.list);

    listView.setItemsCanFocus(false);
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    listView.setAdapter(new SimpleAdapter(this, data, R.layout.row_option, from, to));
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (mIsSpeaking) {
          shutUp();
        }

        SparseBooleanArray checkedItems = ((ListView) adapterView).getCheckedItemPositions();
        mConfig.setOptionEnabled(Config.OPTIONS_ORDERED[position], checkedItems.get(position));
        mConfig.save(MainActivity.this);

        syncConfigToList();
      }
    });

    syncConfigToList();

    editText = findViewById(R.id.number);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    mTTS.shutdown();
  }

  // Syncs the current configuration to the checked items on the ListView
  private void syncConfigToList() {
    for (int a = 0; a < Config.OPTIONS_ORDERED.length; a++) {
      listView.setItemChecked(a, mConfig.isOptionEnabled(Config.OPTIONS_ORDERED[a]));
    }
  }

  //////////////////////////////////////////////////////////////////////////
  // Action bar

  private MenuItem mSpeakMenuItem;
  private MenuItem mShutUpMenuItem;
  private MenuItem mRandomMenuItem;

  // Sometimes starting/stopping is not immediate; use this to determine state
  private boolean mIsSpeaking = false;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    mSpeakMenuItem = menu.findItem(R.id.menu_speak);
    mShutUpMenuItem = menu.findItem(R.id.menu_shut_up);
    mRandomMenuItem = menu.findItem(R.id.menu_random_number);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    mSpeakMenuItem.setVisible(!mIsSpeaking);
    mShutUpMenuItem.setVisible(mIsSpeaking);
    mRandomMenuItem.setVisible(!mIsSpeaking);

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    if (item.getItemId() == R.id.menu_speak) {
      mGenerator.saySpeech(mConfig);
      setIsSpeaking(true);
      return true;
    } else if (item.getItemId() == R.id.menu_shut_up) {
      shutUp();
      return true;
    } else if (item.getItemId() == R.id.menu_random_number) {
      int bound = Integer.parseInt(editText.getText().toString().trim());
      int number = new Random(System.currentTimeMillis()).nextInt(bound) + 1;
      TextView textView = findViewById(R.id.random);
      textView.setText(String.valueOf(number));
      mTTS.speak(getString(R.string.script_first_player, number), TextToSpeech.QUEUE_ADD, null,
          "id");
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  private void shutUp() {
    mTTS.stop();
    setIsSpeaking(false);
  }

  private void setIsSpeaking(boolean isSpeaking) {
    mIsSpeaking = isSpeaking;

    supportInvalidateOptionsMenu();
  }

  //////////////////////////////////////////////////////////////////////////
  // android.speech.tts.TextToSpeech.OnInitListener

  @Override
  public void onInit(int status) {
    if (status == TextToSpeech.SUCCESS) {
      mGenerator = new ScriptGenerator(this, mTTS);
      mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
        @Override
        public void onStart(String s) {
          setIsSpeaking(true);
        }

        @Override
        public void onDone(String s) {
          setIsSpeaking(false);
        }

        @Override
        public void onError(String s) {

        }
      });
    }
  }
}
