package cc.chenhe.weargallery.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import cc.chenhe.weargallery.R;

public class AlertDialog extends Dialog {

    private TextView tvTitle;
    private TextView tvMessage;
    private View viewNegative, viewPositive, viewMiddle;
    private ImageView ivNegative, ivPositive, ivMiddle;

    private OnClickListener positiveListener, negativeListener;
    private String title;
    private String message;
    private boolean showPositiveButton, showNegativeButton;

    public interface OnClickListener {
        void onClick(DialogInterface dialog, int which);
    }

    public AlertDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);
        setCanceledOnTouchOutside(false);
        initView();
        refreshView();
        initEvent();
    }

    private void initEvent() {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.viewDialogMiddleBtn:
                    case R.id.viewDialogPositiveBtn:
                        if (positiveListener != null)
                            positiveListener.onClick(AlertDialog.this, BUTTON_POSITIVE);
                        dismiss();
                        break;
                    case R.id.viewDialogNegativeBtn:
                        if (negativeListener != null)
                            negativeListener.onClick(AlertDialog.this, BUTTON_NEGATIVE);
                        dismiss();
                        break;
                }
            }
        };
        viewPositive.setOnClickListener(clickListener);
        viewNegative.setOnClickListener(clickListener);
        if (viewMiddle != null)
            viewMiddle.setOnClickListener(clickListener);
    }

    private void refreshView() {
        if (!TextUtils.isEmpty(title)) {
            tvTitle.setText(title);
            tvTitle.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setVisibility(View.INVISIBLE);
        }
        if (!TextUtils.isEmpty(message)) {
            tvMessage.setText(message);
        }
        viewNegative.setVisibility(showNegativeButton ? View.VISIBLE : View.GONE);
        ivNegative.setVisibility(showNegativeButton ? View.VISIBLE : View.GONE);
        if (showPositiveButton) {
            if (!useMiddleBtn()) {
                viewPositive.setVisibility(View.VISIBLE);
                ivPositive.setVisibility(View.VISIBLE);
                if (viewMiddle != null) {
                    viewMiddle.setVisibility(View.GONE);
                    ivMiddle.setVisibility(View.GONE);
                }
            } else {
                viewPositive.setVisibility(View.GONE);
                ivPositive.setVisibility(View.GONE);
                viewMiddle.setVisibility(View.VISIBLE);
                ivMiddle.setVisibility(View.VISIBLE);
            }
        }
        setMessageMargin(useMiddleBtn());
    }

    private void setMessageMargin(boolean useMiddleBtn) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) tvMessage.getLayoutParams();
        Resources resources = getContext().getResources();
        params.setMarginStart(resources.getDimensionPixelOffset(useMiddleBtn ?
                R.dimen.dialog_message_margin_horizontal2 : R.dimen.dialog_message_margin_horizontal1));
        params.setMarginEnd(resources.getDimensionPixelOffset(useMiddleBtn ?
                R.dimen.dialog_message_margin_horizontal2 : R.dimen.dialog_message_margin_horizontal1));
        params.setMargins(
                0,
                resources.getDimensionPixelOffset(R.dimen.dialog_message_margin_top),
                0,
                resources.getDimensionPixelOffset(useMiddleBtn ?
                        R.dimen.dialog_message_margin_bottom2 : R.dimen.dialog_message_margin_bottom1)
        );
        tvMessage.setLayoutParams(params);
    }

    /**
     * 是否使用中间按钮作为确认钮。
     */
    private boolean useMiddleBtn() {
        return !showNegativeButton && viewMiddle != null;
    }

    private void initView() {
        tvTitle = findViewById(R.id.tvDialogTitle);
        tvMessage = findViewById(R.id.tvDialogContent);
        tvMessage.setMovementMethod(ScrollingMovementMethod.getInstance());
        viewNegative = findViewById(R.id.viewDialogNegativeBtn);
        viewPositive = findViewById(R.id.viewDialogPositiveBtn);
        viewMiddle = findViewById(R.id.viewDialogMiddleBtn);
        ivPositive = findViewById(R.id.ivDialogPositive);
        ivNegative = findViewById(R.id.ivDialogNegative);
        ivMiddle = findViewById(R.id.ivDialogMiddle);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPositiveButtonListener(OnClickListener listener) {
        this.positiveListener = listener;
        if (listener != null)
            setShowPositiveButton(true);
    }

    public void setNegativeButtonListener(OnClickListener listener) {
        this.negativeListener = listener;
        if (listener != null)
            setShowNegativeButton(true);
    }

    public void setShowPositiveButton(boolean show) {
        this.showPositiveButton = show;
    }

    public void setShowNegativeButton(boolean show) {
        this.showNegativeButton = show;
    }

    public static class Builder {
        Context context;
        AlertDialog dialog;

        public AlertDialog show() {
            dialog.show();
            return dialog;
        }

        public Builder(Context context) {
            this.context = context;
            this.dialog = new AlertDialog(context);
        }

        public void setTitle(String title) {
            dialog.setTitle(title);
        }

        public Builder setTitle(@StringRes int titleId) {
            dialog.setTitle(context.getString(titleId));
            return this;
        }

        public void setMessage(String message) {
            dialog.setMessage(message);
        }

        public Builder setMessage(@StringRes int msgId) {
            dialog.setMessage(context.getString(msgId));
            return this;
        }

        public Builder setShowPositiveButton(boolean show) {
            dialog.setShowPositiveButton(show);
            return this;
        }

        public Builder setShowNegativeButton(boolean show) {
            dialog.setShowNegativeButton(show);
            return this;
        }

        public Builder setPositiveButtonListener(OnClickListener listener) {
            dialog.setPositiveButtonListener(listener);
            return this;
        }

        public Builder setNegativeButtonListener(OnClickListener listener) {
            dialog.setNegativeButtonListener(listener);
            return this;
        }
    }
}
