package com.jbion.android.lib.list.swipe;

import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.jbion.android.pulltorefresh.R;

/**
 * ListView subclass that provides the swipe functionality
 */
public class SwipeListView extends ListView {

    private static final String LOG_TAG = SwipeListView.class.getSimpleName();

    /**
     * User options container.
     */
    private final SwipeOptions opts = new SwipeOptions(getContext());
    /**
     * Internal listener for common swipe events
     */
    private SwipeListViewListener swipeListViewListener;
    /**
     * Internal touch listener
     */
    private SwipeListViewTouchListener touchListener;

    /**
     * If you create a SwipeListView programmatically you need to specifiy back and
     * front identifier.
     * 
     * @param context
     *            Context
     * @param backViewId
     *            Back view resource identifier
     * @param frontViewId
     *            Front view resource identifier
     */
    public SwipeListView(Context context, int frontViewId, int backViewId) {
        super(context);
        opts.frontViewId = frontViewId;
        opts.backViewId = backViewId;
        init(null);
    }

    /**
     * @see android.widget.ListView#ListView(android.content.Context,
     *      android.util.AttributeSet)
     */
    public SwipeListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    /**
     * @see android.widget.ListView#ListView(android.content.Context,
     *      android.util.AttributeSet, int)
     */
    public SwipeListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    /**
     * Init ListView
     * 
     * @param attrs
     *            AttributeSet
     */
    private void init(AttributeSet attrs) {
        // populate options
        if (attrs != null) {
            TypedArray styled = getContext().obtainStyledAttributes(attrs,
                    R.styleable.SwipeListView);
            opts.set(getContext(), styled);
            styled.recycle();
        }

        touchListener = new SwipeListViewTouchListener(this, opts);
        super.setOnTouchListener(touchListener);
        super.setOnScrollListener(touchListener.makeScrollListener());
    }

    /**
     * Sets the Listener
     * 
     * @param swipeListViewListener
     *            Listener
     */
    public void setSwipeListViewListener(SwipeListViewListener swipeListViewListener) {
        this.swipeListViewListener = swipeListViewListener;
    }

    /**
     * Recycle cell. This method should be called from getView in Adapter when using
     * ACTION_CHOICE
     * 
     * @param convertView
     *            parent view
     * @param position
     *            position in list
     */
    public void recycle(View convertView, int position) {
        touchListener.reloadChoiceStateInView(convertView.findViewById(opts.frontViewId), position);
    }

    /**
     * Get if item is selected
     * 
     * @param position
     *            position in list
     * @return whether the specified position is currently in a swiped state.
     */
    public boolean isChecked(int position) {
        return touchListener.isChecked(position);
    }

    /**
     * Returns a list of the positions that are currently swiped.
     * 
     * @return a list of the swiped positions.
     */
    public List<Integer> getSelectedPositions() {
        return touchListener.getSelectedPositions();
    }

    /**
     * Returns the number of currently swiped items.
     * 
     * @return the number of currently swiped items.
     */
    public int getCountSelected() {
        return touchListener.getCountSelected();
    }

    /**
     * Unselected choice state in item
     */
    public void unselectedChoiceStates() {
        touchListener.unselectedChoiceStates();
    }

    /**
     * Dismiss item
     * 
     * @param position
     *            Position that you want open
     */
    public void dismiss(int position) {
        int height = touchListener.dismiss(position);
        if (height > 0) {
            touchListener.handlerPendingDismisses(height);
        } else {
            int[] dismissPositions = new int[1];
            dismissPositions[0] = position;
            onDismiss(dismissPositions);
            touchListener.resetPendingDismisses();
        }
    }

    /**
     * Dismiss items selected
     */
    public void dismissSelected() {
        List<Integer> list = touchListener.getSelectedPositions();
        int[] dismissPositions = new int[list.size()];
        int height = 0;
        for (int i = 0; i < list.size(); i++) {
            int position = list.get(i);
            dismissPositions[i] = position;
            int auxHeight = touchListener.dismiss(position);
            if (auxHeight > 0) {
                height = auxHeight;
            }
        }
        if (height > 0) {
            touchListener.handlerPendingDismisses(height);
        } else {
            onDismiss(dismissPositions);
            touchListener.resetPendingDismisses();
        }
        touchListener.resetOldActions();
    }

    /**
     * Open ListView's item
     * 
     * @param position
     *            Position that you want open
     */
    public void openAnimate(int position) {
        touchListener.openAnimate(position);
    }

    /**
     * Close ListView's item
     * 
     * @param position
     *            Position that you want open
     */
    public void closeAnimate(int position) {
        touchListener.closeAnimate(position);
    }

    /**
     * @see android.widget.ListView#setAdapter(android.widget.ListAdapter)
     */
    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        touchListener.resetItems();
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                onListChanged();
                touchListener.resetItems();
            }
        });
    }

    /**
     * @see android.widget.ListView#onInterceptTouchEvent(android.view.MotionEvent)
     */

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.i(LOG_TAG, "onInterceptTouchEvent");
        boolean shouldIntercept = false;
        if (isEnabled()) {
            shouldIntercept = touchListener.onInterceptTouchEvent(ev);
        }
        return super.onInterceptTouchEvent(ev) || shouldIntercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.i(LOG_TAG, "onTouchEvent");
        boolean res = touchListener.onTouch(this, ev);
        return super.onTouchEvent(ev) || res;
    }

    /**
     * Close all opened items
     */
    public void closeOpenedItems() {
        touchListener.closeOpenedItems();
    }

    /*
     * OPTIONS SETTERS
     */

    /**
     * Sets the swipe swipeMode
     * 
     * @param swipeMode
     */
    public void setSwipeMode(int swipeMode) {
        opts.swipeMode = swipeMode;
    }

    /**
     * Return action on left
     * 
     * @return Action
     */
    public int getSwipeActionLeft() {
        return opts.swipeActionLeft;
    }

    /**
     * Set action on left
     * 
     * @param swipeActionLeft
     *            Action
     */
    public void setSwipeActionLeft(int swipeActionLeft) {
        opts.swipeActionLeft = swipeActionLeft;
    }

    /**
     * Return action on right
     * 
     * @return Action
     */
    public int getSwipeActionRight() {
        return opts.swipeActionRight;
    }

    /**
     * Set action on right
     * 
     * @param swipeActionRight
     *            Action
     */
    public void setSwipeActionRight(int swipeActionRight) {
        opts.swipeActionRight = swipeActionRight;
    }

    /**
     * Defines how to consider the given offsets.
     * 
     * @param swipeOffsetType
     */
    public void setOffsetType(int swipeOffsetType) {
        opts.swipeOffsetType = swipeOffsetType;
    }

    /**
     * Set the left offset
     * 
     * @param leftOffset
     *            Offset
     */
    public void setLeftOffset(float leftOffset) {
        opts.swipeOffsetLeft = leftOffset;
    }

    /**
     * Sets the right offset
     * 
     * @param rightOffset
     *            Offset
     */
    public void setRightOffset(float rightOffset) {
        opts.swipeOffsetRight = rightOffset;
    }

    /**
     * Set if the user can open an item with long press on cell
     * 
     * @param openOnLongClick
     */
    public void setOpenOnLongClick(boolean openOnLongClick) {
        opts.openOnLongClick = openOnLongClick;
    }

    /**
     * Set if all item opened will be close when the user move ListView
     * 
     * @param multipleSelectEnabled
     */
    public void setMultipleSelectEnabled(boolean multipleSelectEnabled) {
        opts.multipleSelectEnabled = multipleSelectEnabled;
    }

    /**
     * Set if all item opened will be close when the user move ListView
     * 
     * @param closeAllItemsOnScroll
     */
    public void setCloseAllItemsOnScroll(boolean closeAllItemsOnScroll) {
        opts.closeAllItemsOnScroll = closeAllItemsOnScroll;
    }

    /**
     * Sets animation time when the user drops the cell
     * 
     * @param animationTime
     *            milliseconds
     */
    public void setAnimationTime(long animationTime) {
        if (animationTime > 0) {
            opts.animationTime = animationTime;
        }
    }

    /*
     * LISTENER CALLBACKS
     */

    /**
     * Notifies onDismiss
     * 
     * @param reverseSortedPositions
     *            All dismissed positions
     */
    protected void onDismiss(int[] reverseSortedPositions) {
        if (swipeListViewListener != null) {
            swipeListViewListener.onDismiss(reverseSortedPositions);
        }
    }

    /**
     * Start open item
     * 
     * @param position
     *            list item
     * @param action
     *            current action
     * @param right
     *            to right
     */
    protected void onStartOpen(int position, int action, boolean right) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onStartOpen(position, action, right);
        }
    }

    /**
     * Start close item
     * 
     * @param position
     *            list item
     * @param right
     */
    protected void onStartClose(int position, boolean right) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onStartClose(position, right);
        }
    }

    /**
     * Notifies onClickFrontView
     * 
     * @param position
     *            item clicked
     */
    protected void onClickFrontView(int position) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onClickFrontView(position);
        }
    }

    /**
     * Notifies onClickBackView
     * 
     * @param position
     *            back item clicked
     */
    protected void onClickBackView(int position) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onClickBackView(position);
        }
    }

    /**
     * Notifies onOpened
     * 
     * @param position
     *            Item opened
     * @param toRight
     *            If should be opened toward the right
     */
    protected void onOpened(int position, boolean toRight) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onOpened(position, toRight);
        }
    }

    /**
     * Notifies onClosed
     * 
     * @param position
     *            Item closed
     * @param fromRight
     *            If open from right
     */
    protected void onClosed(int position, boolean fromRight) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onClosed(position, fromRight);
        }
    }

    /**
     * Notifies onChoiceChanged
     * 
     * @param position
     *            position that choice
     * @param selected
     *            if item is selected or not
     */
    protected void onChoiceChanged(int position, boolean selected) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onChoiceChanged(position, selected);
        }
    }

    /**
     * User start choice items
     */
    protected void onChoiceStarted() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onChoiceStarted();
        }
    }

    /**
     * User end choice items
     */
    protected void onChoiceEnded() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onChoiceEnded();
        }
    }

    /**
     * User is in first item of list
     */
    protected void onFirstListItem() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onFirstListItem();
        }
    }

    /**
     * User is in last item of list
     */
    protected void onLastListItem() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onLastListItem();
        }
    }

    /**
     * Notifies onListChanged
     */
    protected void onListChanged() {
        if (swipeListViewListener != null) {
            swipeListViewListener.onListChanged();
        }
    }

    /**
     * Notifies onMove
     * 
     * @param position
     *            Item moving
     * @param x
     *            Current position
     */
    protected void onMove(int position, float x) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            swipeListViewListener.onMove(position, x);
        }
    }

    protected int onChangeSwipeMode(int position) {
        if (swipeListViewListener != null && position != ListView.INVALID_POSITION) {
            return swipeListViewListener.onChangeSwipeMode(position);
        }
        return SwipeOptions.SWIPE_MODE_DEFAULT;
    }
}