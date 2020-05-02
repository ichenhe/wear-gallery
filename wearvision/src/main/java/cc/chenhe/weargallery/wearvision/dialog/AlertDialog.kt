package cc.chenhe.weargallery.wearvision.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.SparseArray
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.util.set
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.chenhe.weargallery.wearvision.R
import cc.chenhe.weargallery.wearvision.util.TrackSelectionAdapterWrapper
import cc.chenhe.weargallery.wearvision.util.loge
import cc.chenhe.weargallery.wearvision.util.visibleGone
import cc.chenhe.weargallery.wearvision.widget.MinimizableFloatingActionButton
import cc.chenhe.weargallery.wearvision.widget.ObservableScrollView
import kotlin.math.max

private const val TAG = "AlertDialog"

/**
 * An alert dialog using [R.layout.wv_dialog_alert] as the layout.
 *
 * This dialog provides a checkbox to implement custom option, the default text is
 * [R.string.wv_alert_dialog_dont_show_me_again]. This feature is disabled by default, set [showSkipLayout] to `true`
 * to enable it. The text can be changed by setting [skipText]. Generally you should read the value of [isSkipChecked]
 * in callbacks (e.g. [DialogInterface.OnClickListener] or [DialogInterface.OnCancelListener]). However you can also
 * set [isSkipChecked] to a certain value to change the state of checkbox.
 *
 * If you want to change the style of this dialog, there are two ways:
 *
 * 1. Use the constructor with a `themeResId` parameter to pass a custom style.
 * 2. Change the value of `android:alertDialogTheme` in your App theme to a custom style.
 *
 * Anyway, you may want to create a new theme style that inherits from [R.style.Theme_WearVision_Dialog_Alert] and to
 * modify some specific attrs. In addition to some system attributes, there are some WearVision's attributes that can
 * be used:
 *
 * - [R.attr.wv_windowAppBarStyle] - Style of AppBarLayout that contains title and icon.
 * - [R.attr.wv_windowAppBarContentStyle] - Style for the container layout in AppBarLayout.
 * - [R.attr.wv_windowIconStyle] - Style of ImageView for displaying the icon.
 * - [R.attr.wv_dialogContentStyle] - Style of the container(ScrollerView) that wrap the whole content.
 * - [R.attr.wv_dialogMessageStyle] - Style of TextView for displaying the content message.
 * - [R.attr.wv_positiveIconButtonStyle] - Style of the "positive" icon button.
 * - [R.attr.wv_negativeIconButtonStyle] - Style of the "negative" icon button.
 *
 * The following attributes are related to the selection list, note that all item layouts must contain a [TextView]
 * with id [android.R.id.text1] to display the content.
 *
 * - [R.attr.wv_listLayout] RecyclerView showing the list. The root view must be a [RecyclerView] whit id
 * [android.R.id.list].
 * - [R.attr.wv_singleChoiceItemLayout] Item layout in single choice mode.
 * - [R.attr.wv_multiChoiceItemLayout] Item layout in multi choice mode.
 * - [R.attr.wv_listLayout] Item layout in other mode.
 */
class AlertDialog : VisionDialog {

    companion object {

        /**
         * Check whether the given [resId] is valid. If not we try to read the style from
         * [android.R.attr.alertDialogTheme] attr.
         *
         * Copy from [androidx.appcompat.app.AlertDialog].
         */
        private fun resolveDialogTheme(context: Context, @StyleRes resId: Int): Int {
            // Check to see if this resourceId has a valid package ID.
            return if (resId ushr 24 and 0x000000ff >= 0x00000001) {   // start of real resource IDs.
                resId
            } else {
                val outValue = TypedValue()
                context.theme.resolveAttribute(android.R.attr.alertDialogTheme, outValue, true)
                outValue.resourceId
            }
        }
    }

    private lateinit var mIconView: ImageView
    private lateinit var mTitleView: TextView
    private lateinit var mScrollView: ObservableScrollView
    private lateinit var mContentView: LinearLayout
    private lateinit var mMessageView: TextView
    private lateinit var mPositiveButton: MinimizableFloatingActionButton
    private lateinit var mNegativeButton: MinimizableFloatingActionButton
    private var mSkipLayout: ViewGroup? = null
    private var mSkipCheck: CheckBox? = null
    private var mSkipText: TextView? = null
    private var mCustomView: View? = null
    private var mCustomViewLayoutResId = 0

    private var mIconButtonCount = 0
        set(value) {
            field = value
            offsetIconButtons(value)
        }
    private var mPreferredSkipCheck = false
        set(value) {
            field = value
            mSkipCheck?.isChecked = value
        }

    var icon: Drawable? = null
        set(value) {
            field = value
            updateIcon()
        }
    var title: String? = null
        set(value) {
            field = value
            updateTitle()
        }
    var message: String? = null
        set(value) {
            field = value
            updateMessage()
        }
    var positiveButtonIcon: Drawable? = null
        private set(value) {
            field = value
            updatePositiveButtonIcon()
        }
    var negativeButtonIcon: Drawable? = null
        private set(value) {
            field = value
            updateNegativeButtonIcon()
        }
    var positiveIconButtonListener: DialogInterface.OnClickListener? = null
        private set
    var negativeIconButtonListener: DialogInterface.OnClickListener? = null
        private set
    var isSkipChecked
        get() = mSkipCheck?.isChecked ?: false
        set(value) {
            mPreferredSkipCheck = value
        }

    var showSkipLayout: Boolean = false
        set(value) {
            field = value
            updateSkipLayout()
        }
    var skipText: String? = context.getString(R.string.wv_alert_dialog_dont_show_me_again)
        set(value) {
            field = value
            updateSkipText()
        }

    constructor(context: Context) : this(context, 0)

    @SuppressLint("Recycle")
    constructor(context: Context, themeResId: Int) : super(context, resolveDialogTheme(context, themeResId)) {
        context.obtainStyledAttributes(null, R.styleable.AlertDialog, android.R.attr.alertDialogStyle, 0).use { a ->
            mListLayout = a.getResourceId(R.styleable.AlertDialog_wv_listLayout, R.layout.wv_dialog_alert_list)
            mSingleChoiceItemLayout = a.getResourceId(R.styleable.AlertDialog_wv_singleChoiceItemLayout,
                    android.R.layout.select_dialog_singlechoice)
            mMultiChoiceItemLayout = a.getResourceId(R.styleable.AlertDialog_wv_multiChoiceItemLayout,
                    android.R.layout.select_dialog_multichoice)
            mListItemLayout = a.getResourceId(R.styleable.AlertDialog_wv_listItemLayout,
                    android.R.layout.select_dialog_item)
        }
    }

    constructor(context: Context, cancelable: Boolean, cancelListener: DialogInterface.OnCancelListener? = null)
            : this(context, 0) {
        setCancelable(cancelable)
        setOnCancelListener(cancelListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wv_dialog_alert)
        setupView()
        updateView()
    }

    // ---------------------------------------------------------------------------------------------------
    // View
    // ---------------------------------------------------------------------------------------------------

    private fun <T : View> getViewById(@IdRes id: Int, msg: String?): T {
        return if (msg == null) {
            requireNotNull(findViewById(id))
        } else {
            requireNotNull(findViewById(id)) { msg }
        }
    }

    private fun setupView() {
        mIconView = getViewById(android.R.id.icon, "Can not find icon view.")
        mTitleView = getViewById(android.R.id.title, "Can not find title view.")
        mScrollView = getViewById(R.id.wv_scrollView, "Can not find scroll view.")
        mContentView = getViewById(R.id.wv_contentPanel, "Can not find the container layout in the scroll view.")
        mMessageView = getViewById(android.R.id.message, "Can not find message view.")
        mPositiveButton = getViewById(R.id.wv_positiveButton, "Can not find positive button.")
        mNegativeButton = getViewById(R.id.wv_negativeButton, "Can not find negative button.")

        mSkipLayout = findViewById(R.id.wv_skipLayout)
        if (mSkipLayout != null) {
            mSkipCheck = getViewById(R.id.wv_skipCheck, "Can not find skip check box.")
            mSkipText = getViewById(R.id.wv_skipText, "Can not find skip text view.")
        }

        mPositiveButton.setOnClickListener {
            positiveIconButtonListener?.onClick(this, DialogInterface.BUTTON_POSITIVE)
            dismiss()
        }
        mNegativeButton.setOnClickListener {
            negativeIconButtonListener?.onClick(this, DialogInterface.BUTTON_NEGATIVE)
            dismiss()
        }

        mScrollView.onScrollStateChangedListener = object : ObservableScrollView.OnScrollStateChangedListener {
            override fun onScrollStateChanged(scrollView: ObservableScrollView, oldState: Int, newState: Int) {
                when (newState) {
                    ObservableScrollView.SCROLL_STATE_IDLE -> {
                        maximizeButtons()
                    }
                    ObservableScrollView.SCROLL_STATE_DRAGGING -> {
                        minimizeButtons()
                    }
                }
            }
        }

        if (!mItems.isNullOrEmpty()) {
            createListView()
            mListView?.let { listView ->
                val listIndex = mContentView.indexOfChild(mMessageView) + 1
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                mContentView.addView(listView, listIndex, lp)
                listView.adapter = mAdapter
            } ?: kotlin.run {
                loge(TAG, "Can not find the RecyclerView.")
            }
        }

        setupCustomContent(findViewById(android.R.id.custom))
    }

    private fun setupCustomContent(custom: FrameLayout?) {
        val hasCustomView = mCustomView != null || mCustomViewLayoutResId != 0
        if (!hasCustomView) {
            custom?.visibleGone(false)
            return
        }
        requireNotNull(custom) { "Can not find custom container layout." }
        val customView = mCustomView ?: LayoutInflater.from(context).inflate(mCustomViewLayoutResId, custom, false)
        custom.addView(customView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT))
        custom.visibleGone(true)
    }

    private fun updateView() {
        updateIcon()
        updateTitle()
        updateMessage()
        updatePositiveButtonIcon()
        updateNegativeButtonIcon()

        updateSkipLayout()
        updateSkipText()
    }

    private fun updateIcon() {
        if (::mIconView.isInitialized) {
            mIconView.setImageDrawable(icon)
            mIconView.visibleGone(icon != null)
        }
    }

    private fun updateTitle() {
        if (::mTitleView.isInitialized) {
            mTitleView.text = title
            mTitleView.visibleGone(title != null)
        }
    }

    private fun updateMessage() {
        if (::mMessageView.isInitialized) {
            mMessageView.text = message
        }
    }

    private fun updatePositiveButtonIcon() {
        if (::mPositiveButton.isInitialized) {
            mPositiveButton.setImageDrawable(positiveButtonIcon)
            mPositiveButton.visibleGone(positiveButtonIcon != null)
        }
        updateIconButtonCount()
    }

    private fun updateNegativeButtonIcon() {
        if (::mNegativeButton.isInitialized) {
            mNegativeButton.setImageDrawable(negativeButtonIcon)
            mNegativeButton.visibleGone(negativeButtonIcon != null)
        }
        updateIconButtonCount()
    }

    private fun updateIconButtonCount() {
        var c = 0
        if (positiveButtonIcon != null) {
            c++
        }
        if (negativeButtonIcon != null) {
            c++
        }
        mIconButtonCount = c
    }

    private fun offsetIconButtons(iconButtonCount: Int) {
        if (!::mPositiveButton.isInitialized || !::mNegativeButton.isInitialized) {
            return
        }
        // padding
        val paddingBottomUnit = context.resources.getDimensionPixelOffset(R.dimen.wv_alert_dialog_padding_bottom)
        val paddingBottom = if (iconButtonCount > 1) paddingBottomUnit * 2 else paddingBottomUnit
        val paddingHorizontal = when (iconButtonCount) {
            3 -> context.resources.getDimensionPixelOffset(R.dimen.wv_alert_dialog_button_padding_horizontal_full)
            2 -> context.resources.getDimensionPixelOffset(R.dimen.wv_alert_dialog_button_padding_horizontal_pair)
            else -> 0
        }

        mPositiveButton.coordinatorLayoutParams()?.apply {
            if (iconButtonCount == 1) {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            marginStart = paddingHorizontal
            marginEnd = paddingHorizontal
            bottomMargin = paddingBottom
        }
        mNegativeButton.coordinatorLayoutParams()?.apply {
            if (iconButtonCount == 1) {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            marginStart = paddingHorizontal
            marginEnd = paddingHorizontal
            bottomMargin = paddingBottom
        }

        // minimize translation
        val translateY = context.resources.getDimensionPixelOffset(R.dimen.wv_alert_dialog_button_translate_vertical) +
                (paddingBottom - paddingBottomUnit).toFloat()
        val translateX = context.resources.getDimensionPixelOffset(R.dimen.wv_alert_dialog_button_translate_horizontal)
                .toFloat()
        when (iconButtonCount) {
            2, 3 -> {
                mPositiveButton.setMinimizeTranslation(-translateX, translateY)
                mNegativeButton.setMinimizeTranslation(translateX, translateY)
            }
            else -> {
                mPositiveButton.setMinimizeTranslation(0f, translateY)
                mNegativeButton.setMinimizeTranslation(0f, translateY)
            }
        }
        mPositiveButton.post { setScrollPaddingBottom() }
    }

    private fun setScrollPaddingBottom() {
        if (!ViewCompat.isLaidOut(mPositiveButton) && !ViewCompat.isLaidOut(mNegativeButton)) {
            // No buttons
            mScrollView.apply {
                setPadding(
                        paddingLeft, paddingTop, paddingRight,
                        context.resources.getDimensionPixelOffset(R.dimen.wv_page_padding_bottom)
                )
            }
            return
        }
        // set scroll padding bottom to avoid buttons
        val y = max(mPositiveButton.y, mNegativeButton.y).toInt()
        val paddingBottomUnit = context.resources.getDimensionPixelOffset(R.dimen.wv_alert_dialog_padding_bottom)
        mScrollView.apply {
            setPadding(
                    paddingLeft, paddingTop, paddingRight,
                    resources.displayMetrics.heightPixels - y + paddingBottomUnit
            )
        }
    }

    private fun updateSkipLayout() {
        mSkipLayout?.let {
            it.visibleGone(showSkipLayout)
            mSkipCheck!!.isChecked = mPreferredSkipCheck
            if (showSkipLayout) {
                it.setOnClickListener {
                    mSkipCheck!!.toggle()
                }
            }
        }
    }

    private fun updateSkipText() {
        mSkipText?.text = skipText
    }

    private fun View.coordinatorLayoutParams(): CoordinatorLayout.LayoutParams? {
        return layoutParams?.let { lp ->
            if (lp is CoordinatorLayout.LayoutParams) lp
            else null
        }
    }

    private fun minimizeButtons() {
        mPositiveButton.minimize()
        mNegativeButton.minimize()
    }

    private fun maximizeButtons() {
        mPositiveButton.maximize()
        mNegativeButton.maximize()
    }

    // ---------------------------------------------------------------------------------------------------
    // List
    // ---------------------------------------------------------------------------------------------------

    private var mListLayout = 0
    private var mSingleChoiceItemLayout = 0
    private var mMultiChoiceItemLayout = 0
    private var mListItemLayout = 0

    private var mIsSingleChoice = false
        set(value) {
            field = value
            if (value) {
                mIsMultiChoice = false
            }
        }
    private var mIsMultiChoice = false
        set(value) {
            field = value
            if (value) {
                mIsSingleChoice = false
            }
        }
    private var mItems: Array<out CharSequence>? = null
    private var mOnClickListener: DialogInterface.OnClickListener? = null
    private var mOnMultiChoiceListener: DialogInterface.OnMultiChoiceClickListener? = null

    /** Only used for initialization, not synchronized with the adapter. */
    private var mCheckedItems = mutableListOf<Int>()
    private var mAdapter: TrackSelectionAdapterWrapper<*>? = null
    private var mListView: RecyclerView? = null

    var onPrepareListViewListener: OnPrepareListViewListener? = null

    private fun createListView() {
        val listView: RecyclerView = layoutInflater.inflate(mListLayout, null) as RecyclerView
        listView.layoutManager = LinearLayoutManager(context)


        val itemLayout = when {
            mIsSingleChoice -> mSingleChoiceItemLayout
            mIsMultiChoice -> mMultiChoiceItemLayout
            else -> mListItemLayout
        }

        val adapter: RecyclerView.Adapter<*> = CheckedItemAdapter(context, itemLayout, android.R.id.text1, mItems!!)
        onPrepareListViewListener?.onPrepareListView(listView)

        mAdapter = TrackSelectionAdapterWrapper(adapter).also { a ->
            if (mIsSingleChoice) {
                a.choiceMode = TrackSelectionAdapterWrapper.CHOICE_MODE_SINGLE
                if (mCheckedItems.size == 1) {
                    a.setItemChecked(mCheckedItems.first(), true, updateView = false)
                }
                if (mOnClickListener != null) {
                    a.onItemClickListener = object : TrackSelectionAdapterWrapper.OnItemClickListener {
                        override fun onItemClick(holder: RecyclerView.ViewHolder, position: Int) {
                            mOnClickListener?.onClick(this@AlertDialog, position)
                        }
                    }
                }
            } else if (mIsMultiChoice) {
                a.choiceMode = TrackSelectionAdapterWrapper.CHOICE_MODE_MULTIPLE
                mCheckedItems.forEach { a.setItemChecked(it, true, updateView = false) }
                if (mOnMultiChoiceListener != null) {
                    a.onItemClickListener = object : TrackSelectionAdapterWrapper.OnItemClickListener {
                        override fun onItemClick(holder: RecyclerView.ViewHolder, position: Int) {
                            mOnMultiChoiceListener?.onClick(this@AlertDialog, position, a.isItemChecked(position))
                        }
                    }
                }
            }

        }
        mListView = listView
    }

    private class CheckedItemAdapter(
            private val context: Context,
            private val layoutRes: Int,
            private val textViewId: Int,
            private val objects: Array<out CharSequence>
    ) : RecyclerView.Adapter<ViewHolder>() {

        init {
            // The data is constant
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(context).inflate(layoutRes, parent, false)
            return ViewHolder(v)
        }

        override fun getItemCount(): Int = objects.size

        override fun getItemId(position: Int): Long = position.toLong()

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.get<TextView>(textViewId).text = objects[position]
        }
    }


    private class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val views = SparseArray<View>()

        @Suppress("UNCHECKED_CAST")
        fun <T : View> get(id: Int): T {
            return (views.get(id) ?: itemView.findViewById<T>(id)!!.also { views[id] = it }) as T
        }
    }

    /**
     * Interface definition for a callback to be invoked before the ListView  will be bound to an adapter.
     */
    interface OnPrepareListViewListener {
        /**
         * Called before the ListView is bound to an adapter.
         * @param listView The ListView that will be shown in the dialog.
         */
        fun onPrepareListView(listView: RecyclerView?)
    }

    // ---------------------------------------------------------------------------------------------------
    // API
    // ---------------------------------------------------------------------------------------------------

    fun setIcon(@DrawableRes iconRes: Int) {
        icon = ContextCompat.getDrawable(context, iconRes)
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        this.title = title?.toString()
    }

    override fun setTitle(@StringRes titleId: Int) {
        super.setTitle(titleId)
        this.title = context.getString(titleId)
    }

    fun setMessage(@StringRes messageId: Int) {
        this.message = context.getString(messageId)
    }

    fun setPositiveButtonIcon(icon: Drawable?, listener: DialogInterface.OnClickListener?) {
        positiveButtonIcon = icon
        positiveIconButtonListener = listener
    }

    fun setPositiveButtonIcon(@DrawableRes iconRes: Int, listener: DialogInterface.OnClickListener?) {
        setPositiveButtonIcon(context.getDrawable(iconRes), listener)
    }

    fun setNegativeButtonIcon(icon: Drawable?, listener: DialogInterface.OnClickListener?) {
        negativeButtonIcon = icon
        negativeIconButtonListener = listener
    }

    fun setNegativeButtonIcon(@DrawableRes iconRes: Int, listener: DialogInterface.OnClickListener?) {
        setNegativeButtonIcon(context.getDrawable(iconRes), listener)
    }

    /**
     * Set a custom view resource to be the contents of the Dialog. The resource will be inflated, adding all
     * top-level views to the screen.
     *
     * This method muse be called before calling [show].
     *
     * @param layoutResId Resource ID to be inflated.
     */
    fun setView(layoutResId: Int) {
        mCustomView = null
        mCustomViewLayoutResId = layoutResId
    }

    /**
     * Sets a custom view to be the contents of the alert dialog.
     *
     * **Note:** To ensure consistent styling, the custom view should be inflated or constructed using the alert
     * dialog's themed context obtained via [getContext].
     *
     * This method muse be called before calling [show].
     *
     * @param view The view to use as the contents of the alert dialog
     */
    fun setView(view: View?) {
        mCustomView = view
        mCustomViewLayoutResId = 0
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the selected item via
     * the supplied listener. The list will have a check mark displayed to the right of the text for the checked item.
     * Clicking on an item in the list will not dismiss the dialog.
     *
     * This method muse be called before calling [show].
     *
     * @param itemsId The resource id of an array i.e. `R.array.foo`.
     * @param checkedItem Specifies which item is checked. -1  if no items are checked.
     * @param listener Notified when an item on the list is clicked. The dialog will not be dismissed when an item is
     * clicked. It will only be dismissed if clicked on a button, if no buttons are supplied it's up to the user to
     * dismiss the dialog.
     */
    fun setSingleChoiceItems(@ArrayRes itemsId: Int, checkedItem: Int, listener: DialogInterface.OnClickListener?) {
        mItems = context.resources.getTextArray(itemsId)
        setSingleChoiceItems(mItems, checkedItem, listener)
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the selected item via
     * the supplied listener. The list will have a check mark displayed to the right of the text for the checked item.
     * Clicking on an item in the list will not dismiss the dialog.
     *
     * This method muse be called before calling [show].
     *
     * @param items The items to be displayed.
     * @param checkedItem Specifies which item is checked. If -1 no items are checked.
     * @param listener Notified when an item on the list is clicked. The dialog will not be dismissed when an item is
     * clicked. It will only be dismissed if clicked on a button, if no buttons are supplied it's up to the user to
     * dismiss the dialog.
     */
    fun setSingleChoiceItems(items: Array<out CharSequence>?, checkedItem: Int,
                             listener: DialogInterface.OnClickListener?) {
        mItems = items
        mOnClickListener = listener
        mCheckedItems.apply {
            clear()
            if (checkedItem >= 0) {
                add(checkedItem)
            }
        }
        mIsSingleChoice = true
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the selected item via
     * the supplied listener. The list will have a check mark displayed to the right of the text for each checked item.
     * Clicking on an item in the list will not dismiss the dialog.
     *
     * This method muse be called before calling [show].
     *
     * @param itemsId The resource id of an array i.e. R.array.foo
     * @param checkedItems Specifies which position items are checked. It should be null if no items are checked.
     * @param listener Notified when an item on the list is clicked. The dialog will not be dismissed when an item is
     * clicked. It will only be dismissed if clicked on a button, if no buttons are supplied it's up to the user to
     * dismiss the dialog.
     */
    fun setMultiChoiceItems(@ArrayRes itemsId: Int, checkedItems: Collection<Int>?,
                            listener: DialogInterface.OnMultiChoiceClickListener?) {
        mItems = context.resources.getTextArray(itemsId)
        setMultiChoiceItems(mItems, checkedItems, listener)
    }

    /**
     * Set a list of items to be displayed in the dialog as the content, you will be notified of the selected item via
     * the supplied listener. The list will have a check mark displayed to the right of the text for each checked item.
     * Clicking on an item in the list will not dismiss the dialog.
     *
     * This method muse be called before calling [show].
     *
     * @param items The text of the items to be displayed in the list.
     * @param checkedItems Specifies which position items are checked. It should be null if no items are checked.
     * @param listener Notified when an item on the list is clicked. The dialog will not be dismissed when an item is
     * clicked. It will only be dismissed if clicked on a button, if no buttons are supplied it's up to the user to
     * dismiss the dialog.
     */
    fun setMultiChoiceItems(items: Array<out CharSequence>?, checkedItems: Collection<Int>?,
                            listener: DialogInterface.OnMultiChoiceClickListener?) {
        mItems = items
        mOnMultiChoiceListener = listener
        mCheckedItems.apply {
            clear()
            checkedItems?.let { mCheckedItems.addAll(it) }
        }
        mIsMultiChoice = true
    }

    /**
     * Returns the currently checked item. The result is only valid if in the single choice mode. This method must be
     * called after the dialog is shown.
     *
     * @return Position of selected item. [RecyclerView.NO_POSITION] if the dialog has not been displayed or not in
     * single check mode.
     */
    fun getCheckedItemPosition(): Int {
        return mAdapter?.getCheckedItemPosition() ?: RecyclerView.NO_POSITION
    }

    /**
     * This method must be called after the dialog is shown.
     *
     * @return Position of selected items. `null` if the dialog has not been displayed or not in check mode.
     */
    fun getCheckedItemPositions(): IntArray? {
        return mAdapter?.getCheckedItemPositions()
    }

}