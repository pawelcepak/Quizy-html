package pl.szynolandia.szybkaklawiatura;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.widget.Button;

public class FastKeyboardService extends InputMethodService {
    @Override
    public View onCreateInputView() {
        Button button = new Button(this);
        button.setText("Szybka Klawiatura");
        button.setOnClickListener(v -> getCurrentInputConnection().commitText("test", 1));
        return button;
    }
}
