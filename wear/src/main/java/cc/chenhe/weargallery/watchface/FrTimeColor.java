package cc.chenhe.weargallery.watchface;

import android.content.Context;
import android.graphics.Color;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.support.wearable.view.CircledImageView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.Arrays;

import cc.chenhe.weargallery.R;
import cc.chenhe.weargallery.fragment.FrSwipeDismiss;
import cc.chenhe.weargallery.ui.recycler.CommonAdapter;
import cc.chenhe.weargallery.ui.recycler.OnItemClickListener;
import cc.chenhe.weargallery.ui.recycler.ViewHolder;
import cc.chenhe.weargallery.uilts.WfSettings;

public class FrTimeColor extends FrSwipeDismiss {
    private static final String[] COLORS = {"#eeeeee", "#ff9800", "#ffeb3b", "#2baf2b",
            "#03a9f4", "#00bcd4", "#5677fc", "#9c27b0", "#e51c23"};

    Context context;
    EditText editText;
    RecyclerView recyclerView;
    CircledImageView ivColor;

    public static FrTimeColor create() {
        return new FrTimeColor();
    }

    @Override
    protected int onGetContentViewId() {
        return R.layout.wf_fr_time_color;
    }

    @Override
    public void onDestroy() {
        String text = editText.getText().toString();
        try {
            Color.parseColor(text);
        } catch (Exception e) {
            text = null;
        }
        if (text != null)
            WfSettings.timeColor(context, text);
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    protected void onLazyLoad() {
        super.onLazyLoad();
        editText = findViewById(R.id.etWfTimeContent);
        ivColor = findViewById(R.id.ivTimeColor);
        recyclerView = findViewById(R.id.rvTimeColor);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 3));
        Adapter adapter = new Adapter(context);
        adapter.setOnItemClickListener(new OnItemClickListener<String>() {
            @Override
            public void onItemClick(ViewHolder holder, String data, int pos) {
                editText.setText(data);
            }
        });
        recyclerView.setAdapter(adapter);
        editText.setText(WfSettings.timeColor(context));
        ivColor.setCircleColor(Color.parseColor(WfSettings.timeColor(context)));

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (s.charAt(0) != '#')
                        s.insert(0, "#");
                    ivColor.setCircleColor(Color.parseColor(s.toString()));
                } catch (Exception ignore) {
                }
            }
        });
    }

    private class Adapter extends CommonAdapter<String> {

        Adapter(Context context) {
            super(context, Arrays.asList(COLORS));
        }

        @Override
        public int getItemLayoutId(int viewType) {
            return R.layout.wf_fr_time_color_item;
        }

        @Override
        public void bindView(ViewHolder holder, final int pos, int viewType) {
            ((CircledImageView) holder.getView(R.id.ivColor))
                    .setCircleColor(Color.parseColor(data.get(pos)));
        }
    }
}
