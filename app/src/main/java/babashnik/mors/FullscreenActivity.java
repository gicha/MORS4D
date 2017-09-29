package babashnik.mors;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import eu.davidea.flipview.FlipView;

public class FullscreenActivity extends Activity implements RecognitionListener {

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String DIRECTIONS_SEARCH = "directions";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "system";

    /* Used to handle permission request */
    private static final int SLIDES_COUNT = 5;
    FlipView fv;
    TextView console;
    int curSlide = 0;
    private SpeechRecognizer recognizer;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_fullscreen);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            runRecognizerSetup();
        }
        console = findViewById(R.id.console);
        fv = findViewById(R.id.flipper);
        initImages();
    }

    private void initImages() {
        int imgs[] = {R.drawable.i1, R.drawable.i2, R.drawable.i3, R.drawable.i4, R.drawable.i5};
        for (int i = 0; i < SLIDES_COUNT; i++) {
            LayoutInflater li = getLayoutInflater();
            View v = li.inflate(R.layout.slide, fv, false);
            ImageView iv = v.findViewById(R.id.slide_image);

            iv.setImageDrawable(getResources().getDrawable(imgs[i], null));
            Log.e("MORS", i + "");
            fv.addView(v);
        }

    }

    private void parseResponse(String text) {
        switch (text) {
            case "back":
            case "left":
                if (curSlide > 0) {
                    fv.showPrevious();
                    curSlide--;
                }
                break;
            case "next":
            case "right":
                if (curSlide < SLIDES_COUNT) {
                    fv.showNext();
                    curSlide++;
                }
                break;
            case "blablabla":
                console.setText("AND WAT?");
            case "system":
                return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                console.setText("");
            }
        }.execute();
    }

    private void runRecognizerSetup() {
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(FullscreenActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    Toast.makeText(FullscreenActivity.this, result.toString(), Toast.LENGTH_SHORT).show();
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length == 0 || grantResults[0] != 0) {
                    finish();
                } else {
                    runRecognizerSetup();
                }
                return;
            default:
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;
        String text = hypothesis.getHypstr();
        console.setText(text);
        System.out.println(">>>>>>>>>>>>>>>>   " + text);
        if (text.contains(KEYPHRASE)) {
            switchSearch(DIRECTIONS_SEARCH);
            return;
        }
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;
        String text = hypothesis.getHypstr();
        console.setText(text);
        System.out.println("<<<<<<<<<   " + text);
        parseResponse(text);
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

    }

    private void setupRecognizer(File assetsDir) throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-en"))
                .setDictionary(new File(assetsDir, "main.dict"))
                .setRawLogDir(assetsDir)
                .getRecognizer();
        recognizer.addListener(this);

        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        File directionsGrammar = new File(assetsDir, "directions.gram");
        recognizer.addGrammarSearch(DIRECTIONS_SEARCH, directionsGrammar);

    }

    @Override
    public void onError(Exception error) {
        System.err.println(error.toString());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }
}
