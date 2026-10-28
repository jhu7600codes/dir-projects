package ssog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

// opened from the long-press menu on the game's icon. runs in its own process so the
// game can't take it down, and only writes text files: SsogCheats in Assembly-CSharp.dll
// reads them when the game starts or comes back to the front
public class CheatActivity extends Activity {
    private static final String ACTIONS = "ssog_actions.txt";
    private static final String SETTINGS = "ssog_settings.txt";

    private static final String[][] BUTTONS = {
        {"gold", "max gold now"},
        {"characters", "unlock all characters"},
        {"upgrades", "max all upgrades"},
        {"hoverboards", "+10 hoverboards"},
        {"headstarts", "+5 of each headstart"},
        {"boxes", "+5 mystery boxes"},
        {"missions", "finish the current missions"},
        {"daily", "finish today's daily challenge"},
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = dp(16);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("subway surfers og cheats");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        list.addView(title);

        TextView hint = new TextView(this);
        hint.setText("everything kicks in when you go back to the game");
        hint.setPadding(0, dp(4), 0, dp(12));
        list.addView(hint);

        Switch gold = new Switch(this);
        gold.setText("infinite gold (refills after every purchase)");
        gold.setTextSize(16);
        gold.setPadding(0, dp(8), 0, dp(8));
        gold.setChecked(infiniteGold());
        gold.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean on) {
                write(SETTINGS, "infinite_gold=" + (on ? "1" : "0") + "\n", false);
            }
        });
        list.addView(gold);

        for (final String[] b : BUTTONS) {
            Button button = new Button(this);
            button.setText(b[1]);
            button.setAllCaps(false);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    write(ACTIONS, b[0] + "\n", true);
                    Toast.makeText(CheatActivity.this, b[1] + ": queued", Toast.LENGTH_SHORT).show();
                }
            });
            list.addView(button);
        }

        list.addView(numberRow("multiplier", "multiplier (1-30)", "set multiplier"));
        list.addView(numberRow("highscore", "high score", "set high score"));

        Button play = new Button(this);
        play.setText("back to the game");
        play.setAllCaps(false);
        play.setTypeface(Typeface.DEFAULT_BOLD);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent game = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (game != null) {
                    game.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(game);
                }
                finish();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        list.addView(play, lp);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        setContentView(scroll);
    }

    // a number box with a button next to it, queues "<action>:<number>"
    private View numberRow(final String action, String hint, final String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        final EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        row.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = input.getText().toString().trim();
                if (number.length() == 0 || number.length() > 9) {
                    Toast.makeText(CheatActivity.this, "type a number first", Toast.LENGTH_SHORT).show();
                    return;
                }
                write(ACTIONS, action + ":" + number + "\n", true);
                Toast.makeText(CheatActivity.this, label + " to " + number + ": queued", Toast.LENGTH_SHORT).show();
            }
        });
        row.addView(button);
        return row;
    }

    // unity 3.5 picks external or internal storage for persistentDataPath, so write both
    private List<File> dirs() {
        List<File> dirs = new ArrayList<File>();
        dirs.add(getFilesDir());
        File external = getExternalFilesDir(null);
        if (external != null) {
            dirs.add(external);
        }
        return dirs;
    }

    private void write(String name, String text, boolean append) {
        for (File dir : dirs()) {
            try {
                dir.mkdirs();
                FileOutputStream out = new FileOutputStream(new File(dir, name), append);
                out.write(text.getBytes("UTF-8"));
                out.close();
            } catch (Exception e) {
                // the other folder still has it
            }
        }
    }

    private boolean infiniteGold() {
        try {
            File file = new File(getFilesDir(), SETTINGS);
            byte[] data = new byte[(int) file.length()];
            FileInputStream in = new FileInputStream(file);
            in.read(data);
            in.close();
            return new String(data, "UTF-8").contains("infinite_gold=1");
        } catch (Exception e) {
            return false;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
