package cn.levey.bannerlib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import cn.levey.bannerlib.base.RxBannerConfig;
import cn.levey.bannerlib.base.RxBannerConstants;
import cn.levey.bannerlib.base.RxBannerLogger;
import cn.levey.bannerlib.base.RxBannerTextView;
import cn.levey.bannerlib.base.RxBannerUtil;
import cn.levey.bannerlib.data.RxBannerAdapter;
import cn.levey.bannerlib.impl.RxBannerChangeListener;
import cn.levey.bannerlib.impl.RxBannerClickListener;
import cn.levey.bannerlib.impl.RxBannerLoaderInterface;
import cn.levey.bannerlib.impl.RxBannerTitleChangeListener;
import cn.levey.bannerlib.impl.RxBannerTitleClickListener;
import cn.levey.bannerlib.indicator.RxBannerIndicator;
import cn.levey.bannerlib.indicator.draw.controller.AttributeController;
import cn.levey.bannerlib.indicator.draw.controller.DrawController;
import cn.levey.bannerlib.indicator.draw.data.Indicator;
import cn.levey.bannerlib.manager.AutoPlayRecyclerView;
import cn.levey.bannerlib.manager.ScaleLayoutManager;

/**
 * Created by Levey on 2018/4/2 15:11.
 * e-mail: m@levey.cn
 */

public class RxBanner extends FrameLayout {

    private Context mContext;
    private int parentWidth, parentHeight;
    private RxBannerAdapter mAdapter;
    private AutoPlayRecyclerView mBannerRv;
    private RxBannerTextView mTitleTv;
    private ScaleLayoutManager mLayoutManager;
    private int timeInterval = RxBannerConfig.getInstance().getTimeInterval();
    private RxBannerConfig.OrderType orderType;
    private boolean autoPlay = true;
    private boolean viewPaperMode = true;
    private boolean infinite;
    private float itemScale;
    private boolean titleVisibility;
    private int titleGravity, titleLayoutGravity;
    private int titleMargin, titleMarginTop, titleMarginBottom, titleMarginStart, titleMarginEnd;
    private int titlePadding, titlePaddingTop, titlePaddingBottom, titlePaddingStart, titlePaddingEnd;
    private int titleWidth, titleHeight;
    private int titleSize;
    private int titleColor, titleBackgroundColor, titleBackgroundResource;
    private boolean titleMarquee;
    private int itemPercent;
    private int itemSpace;
    private float itemMoveSpeed;
    private float centerAlpha;
    private float sideAlpha;
    private int orientation;
    private List<Object> mUrls = new ArrayList<>();
    private List<String> mTitles = new ArrayList<>();
    private RxBannerClickListener onBannerClickListener;
    private RxBannerLoaderInterface mLoader;
    private RxBannerTitleClickListener onTitleClickListener;
    private RecyclerView.ItemAnimator itemAnimator;
    private boolean needStart = false;
    private View indicatorView;
    private Indicator indicatorConfig;
    private boolean rb_indicator_visibility;


    public RxBanner(@NonNull Context context) {
        this(context, null);
    }

    public RxBanner(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RxBanner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initView(context, attrs);
    }

    protected void initView(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RxBanner);
        orientation = typedArray.getInt(R.styleable.RxBanner_rb_orientation, LinearLayout.HORIZONTAL);
        viewPaperMode = typedArray.getBoolean(R.styleable.RxBanner_rb_viewPaperMode, true);
        infinite = typedArray.getBoolean(R.styleable.RxBanner_rb_infinite, true);
        autoPlay = typedArray.getBoolean(R.styleable.RxBanner_rb_autoPlay, true);

        RxBannerLogger.i(" infinite = " + infinite);
        RxBannerLogger.i(" autoPlay = " + autoPlay);
        itemPercent = typedArray.getInt(R.styleable.RxBanner_rb_itemPercent, 100);
        itemSpace = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_itemSpace, 0);
        itemScale = typedArray.getFloat(R.styleable.RxBanner_rb_itemScale, 1.0f);
        itemMoveSpeed = typedArray.getFloat(R.styleable.RxBanner_rb_itemMoveSpeed, 1.0f);
        centerAlpha = typedArray.getFloat(R.styleable.RxBanner_rb_centerAlpha, 1.0f);
        sideAlpha = typedArray.getFloat(R.styleable.RxBanner_rb_sideAlpha, 1.0f);
        timeInterval = typedArray.getInt(R.styleable.RxBanner_rb_timeInterval, RxBannerConfig.getInstance().getTimeInterval());
        if (itemMoveSpeed <= 0)
            throw new IllegalArgumentException(RxBannerLogger.LOGGER_TAG + ": itemMoveSpeed should be greater than 0");
        if (itemPercent <= 0)
            throw new IllegalArgumentException(RxBannerLogger.LOGGER_TAG + ": itemPercent should be greater than 0");
        if (timeInterval < 200)
            throw new IllegalArgumentException(RxBannerLogger.LOGGER_TAG + ": for better performance, timeInterval should be greater than 200 millisecond");
        orderType = RxBannerUtil.getOrder(typedArray.getInt(R.styleable.RxBanner_rb_orderType,
                RxBannerUtil.getOrderType(RxBannerConfig.getInstance().getOrderType())));

        //title
        titleVisibility = typedArray.getBoolean(R.styleable.RxBanner_rb_title_visible, true);
        if (titleVisibility) {
            titleGravity = typedArray.getInt(R.styleable.RxBanner_rb_title_gravity, Gravity.START);
            titleLayoutGravity = typedArray.getInt(R.styleable.RxBanner_rb_title_layout_gravity, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            titleMargin = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_margin, 0);
            titleMarginTop = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_marginTop, 0);
            titleMarginBottom = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_marginBottom, 0);
            titleMarginStart = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_marginStart, 0);
            titleMarginEnd = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_marginEnd, 0);
            titlePadding = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_padding, RxBannerUtil.dp2px(3));
            titlePaddingTop = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_paddingTop, 0);
            titlePaddingBottom = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_paddingBottom, 0);
            titlePaddingStart = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_paddingStart, 0);
            titlePaddingEnd = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_paddingEnd, 0);
            try {
                titleWidth = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_width, ViewGroup.LayoutParams.MATCH_PARENT);
            } catch (Exception e) {
                titleWidth = typedArray.getInt(R.styleable.RxBanner_rb_title_width, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            try {
                titleHeight = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_height, ViewGroup.LayoutParams.WRAP_CONTENT);
            } catch (Exception e) {
                titleHeight = typedArray.getInt(R.styleable.RxBanner_rb_title_height, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            titleSize = typedArray.getDimensionPixelSize(R.styleable.RxBanner_rb_title_size, RxBannerUtil.sp2px(14));
            titleColor = typedArray.getColor(R.styleable.RxBanner_rb_title_color, Color.WHITE);
            titleBackgroundColor = typedArray.getColor(R.styleable.RxBanner_rb_title_backgroundColor, RxBannerUtil.DEFAULT_BG_COLOR);
            titleBackgroundResource = typedArray.getResourceId(R.styleable.RxBanner_rb_title_backgroundResource, Integer.MAX_VALUE);
            titleMarquee = typedArray.getBoolean(R.styleable.RxBanner_rb_title_marquee, false);
        }

        rb_indicator_visibility = typedArray.getBoolean(R.styleable.RxBanner_rb_title_marquee, true);
        if (rb_indicator_visibility) {
            AttributeController attributeController = new AttributeController();
            indicatorConfig = attributeController.init(mContext, attrs);
        }
        typedArray.recycle();
        initView();
    }

    protected void initView() {
        mBannerRv = new AutoPlayRecyclerView(mContext);
        LayoutParams layoutParams = new LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        addView(mBannerRv, layoutParams);
        mLayoutManager = new ScaleLayoutManager.Builder(mContext, itemSpace).build();
        mLayoutManager.setOrientation(orientation);
        mLayoutManager.setItemScale(itemScale);
        mLayoutManager.setAutoPlay(autoPlay);
        mLayoutManager.setInfinite(infinite);
        mLayoutManager.setItemMoveSpeed(itemMoveSpeed);
        mLayoutManager.setCenterAlpha(centerAlpha);
        mLayoutManager.setSideAlpha(sideAlpha);
        mLayoutManager.setSmoothListener(new RxBannerTitleChangeListener() {
            @Override
            public void onBannerSelected(int position) {
                RxBannerLogger.i(" setSmoothListener = " + position);
            }
        });
        if (itemAnimator != null) mBannerRv.setItemAnimator(itemAnimator);

        if (titleVisibility) {
            mTitleTv = new RxBannerTextView(mContext);
            LayoutParams titleLayoutParams = new LayoutParams(titleWidth, titleHeight);
            if (titleMarginTop == 0) titleMarginTop = titleMargin;
            if (titleMarginBottom == 0) titleMarginBottom = titleMargin;
            if (titleMarginStart == 0) titleMarginStart = titleMargin;
            if (titleMarginEnd == 0) titleMarginEnd = titleMargin;
            titleLayoutParams.setMargins(titleMarginStart, titleMarginTop, titleMarginEnd, titleMarginBottom);

            titleLayoutParams.gravity = titleLayoutGravity;
            mTitleTv.setLayoutParams(titleLayoutParams);
            mTitleTv.setTextColor(titleColor);
            mTitleTv.setBackgroundColor(titleBackgroundColor);
            mTitleTv.setGravity(titleGravity);
            mTitleTv.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize);

            if (titlePadding != 0)
                mTitleTv.setPadding(titlePadding, titlePadding, titlePadding, titlePadding);
            if (titlePaddingTop == 0) titlePaddingTop = titlePadding;
            if (titlePaddingBottom == 0) titlePaddingBottom = titlePadding;
            if (titlePaddingStart == 0) titlePaddingStart = titlePadding;
            if (titlePaddingEnd == 0) titlePaddingEnd = titlePadding;
            mTitleTv.setPadding(titlePaddingStart, titlePaddingTop, titlePaddingEnd, titlePaddingBottom);
            if (titleBackgroundResource != Integer.MAX_VALUE)
                mTitleTv.setBackgroundResource(titleBackgroundResource);

            if (titleMarquee) {
                mTitleTv.setFocused(true);
                mTitleTv.setSingleLine(true);
                mTitleTv.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                mTitleTv.setMarqueeRepeatLimit(-1);
            } else {
                mTitleTv.setFocused(false);
            }
            mTitleTv.setTag(RxBannerConstants.TAG_TITLE_VIEW + 0);
            addView(mTitleTv, titleLayoutParams);
        }


        mLayoutManager.setRxBannerTitleChangeListener(new RxBannerTitleChangeListener() {
            @Override
            public void onBannerSelected(int position) {
                if (mTitleTv == null) {
                    return;
                }
                if (mTitles.isEmpty()) {
                    if (mTitleTv.getVisibility() == VISIBLE) mTitleTv.setVisibility(GONE);
                    return;
                }
                if (mUrls.size() == mTitles.size()) {
                    if(mTitleTv.getTag().toString().equals(RxBannerConstants.TAG_TITLE_VIEW + position)){
                        return;
                    }
                    mTitleTv.setTag(RxBannerConstants.TAG_TITLE_VIEW + position);
                    mTitleTv.setText(mTitles.get(position));
                    if (mTitleTv.getVisibility() == GONE) mTitleTv.setVisibility(VISIBLE);
                } else {
                    if (mTitleTv.getVisibility() == VISIBLE) mTitleTv.setVisibility(GONE);
                }
            }
        });
    }

    public RxBanner setOnBannerChangeListener(RxBannerChangeListener onBannerChangeListener) {
        if (mLayoutManager != null) {
            mLayoutManager.setRxBannerChangeListener(onBannerChangeListener);
        }
        return this;
    }


    protected void initRvData() {
        if (mBannerRv != null) {
            mBannerRv.setLayoutManager(mLayoutManager);
            RxBannerLogger.i( " viewPaperMode = " + viewPaperMode);
            mBannerRv.setViewPaperMode(viewPaperMode);
            mBannerRv.setDirection(orderType);
            mBannerRv.setTimeInterval(timeInterval);
            mBannerRv.setAutoPlay(autoPlay);
        }
    }

    protected void initAdapter() {
        initRvData();
        if (mAdapter == null) {
            mAdapter = new RxBannerAdapter(mContext, orientation, getPercentSize());
            mAdapter.setLoader(mLoader);
            mAdapter.setDatas(mUrls);
            if (indicatorConfig != null) {
                indicatorConfig.setCount(mUrls.size());
            }
            mAdapter.setRxBannerClickListener(onBannerClickListener);
            mBannerRv.setAdapter(mAdapter);
        }


        if (onTitleClickListener != null && mTitleTv != null) {
            mTitleTv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mLayoutManager != null) {
                        onTitleClickListener.onTitleClick(mLayoutManager.getCurrentPosition());
                    }
                }
            });
        }

        if (rb_indicator_visibility) {
            if (indicatorView != null) {
                indicatorView.setTag(RxBannerConstants.TAG_INDICATOR_CUSTOM);
                addView(indicatorView);
            } else {
                final RxBannerIndicator indicator = new RxBannerIndicator(mContext);
                indicator.setIndicatorConfig(indicatorConfig);
                indicator.setRecyclerView(mBannerRv);
                if (indicator.getConfig().isClickable()) {
                    indicator.setClickListener(new DrawController.ClickListener() {
                        @Override
                        public void onIndicatorClicked(int position) {
                            indicator.setSelection(position);
                            setCurrentPosition(position);
                        }
                    });
                }
                LayoutParams indicatorLp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                indicatorLp.gravity = indicator.getConfig().getGravity();
                indicatorLp.setMargins(
                        indicator.getConfig().getMarginStart(),
                        indicator.getConfig().getMarginTop(),
                        indicator.getConfig().getMarginEnd(),
                        indicator.getConfig().getMarginBottom());
                indicator.setLayoutParams(indicatorLp);
                indicatorView = indicator;
                indicatorView.setTag(RxBannerConstants.TAG_INDICATOR_CUSTOM);
                addView(indicatorView);
            }
        }

        if(mTitleTv != null ){
            try {
                mTitleTv.setText(mTitles.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public View getCustomIndicator(){
        if(indicatorView != null && indicatorView.getTag().toString().equals(RxBannerConstants.TAG_INDICATOR_CUSTOM)){
            return indicatorView;
        }
        throw new NullPointerException("please set a custom indicator view before get it");
    }

    public RxBannerIndicator getDefaultIndicator(){
        if(indicatorView != null && indicatorView.getTag().toString().equals(RxBannerConstants.TAG_INDICATOR_DEFAULT)){
            return (RxBannerIndicator)indicatorView;
        }
        throw new NullPointerException("please check rb_indicator_viable attribute in xml");
    }

    public RxBanner setLoader(RxBannerLoaderInterface mLoader) {
        if (mAdapter != null) {
            mAdapter.setLoader(mLoader);
        } else {
            this.mLoader = mLoader;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public RxBanner setDatas(List<?> urls) {
        if (urls != null && !urls.isEmpty()) {
            this.mUrls.clear();
            this.mUrls.addAll(urls);
            if (mBannerRv != null && mAdapter != null && mAdapter.getDatas() != null) {
                mAdapter.setDatas(mUrls);
                mBannerRv.smoothScrollToPosition(0);
                if(indicatorView != null && indicatorView instanceof RxBannerIndicator){
                    ((RxBannerIndicator) indicatorView).setSelection(0);
                }
                restart();
            }
        }
        return this;
    }

    public RxBanner setDatas(List<?> urls, List<String> titles) {
        if (urls.size() != titles.size()) {
            throw new IllegalStateException("urls size not equal to titles size");
        }
        setDatas(urls);
        setTitles(titles);
        return this;
    }

    public void addDatas(List<?> urls, List<String> titles) {
        if (urls.size() != titles.size()) {
            throw new IllegalStateException("urls size not equal to titles size");
        }
        addDatas(urls);
        addTitles(titles);
    }

    public void addDatas(List<?> urls) {
        if (urls != null && !urls.isEmpty()) {
            this.mUrls.addAll(urls);
        }
    }


    protected void setTitles(List<String> titles) {
        if (titles != null && !titles.isEmpty()) {
            this.mTitles.clear();
            this.mTitles.addAll(titles);
        }
    }

    public RxBanner setOnBannerTitleClickListener(final RxBannerTitleClickListener onTitleClickListener) {
        this.onTitleClickListener = onTitleClickListener;
        if (onTitleClickListener != null && mTitleTv != null) {
            mTitleTv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mLayoutManager != null) {
                        onTitleClickListener.onTitleClick(mLayoutManager.getCurrentPosition());
                    }
                }
            });
        }
        return this;
    }

    protected void addTitles(List<String> titles) {
        if (titles != null && !titles.isEmpty()) {
            this.mTitles.addAll(titles);
        }
    }

    public RxBanner setOrderType(RxBannerConfig.OrderType orderType) {
        if (mBannerRv != null) {
            mBannerRv.setDirection(orderType);
        }
        this.orderType = orderType;
        return this;
    }

    public RxBanner setTimeInterval(int timeInterval) {
        if (mBannerRv != null) {
            mBannerRv.setTimeInterval(timeInterval);
        }
        this.timeInterval = timeInterval;
        return this;
    }

    public int getTimeInterval() {
        return timeInterval;
    }

    public RxBannerConfig.OrderType getOrderType() {
        return orderType;
    }


    protected void initStart() {
        initAdapter();
        if (mBannerRv != null && mAdapter != null && !mAdapter.getDatas().isEmpty() && isAutoPlay() && needStart) {
            mBannerRv.start();
            RxBannerLogger.i("start success");
        } else if (!isAutoPlay()) {
            RxBannerLogger.i("can not start, auto play = false");
        } else {
            RxBannerLogger.i("please call start() to start banner");
        }
    }

    public void start() {
        needStart = true;
        if (isParentCreate() && mBannerRv != null && mAdapter != null && autoPlay) {
            mBannerRv.start();
        }
    }


    //已在 onDetachedFromWindow 实现回收资源，也可根据需求自行调用完成销毁
    public void onDestroy() {
        pause();
        if (mAdapter != null) mAdapter = null;
        if (mTitleTv != null) mTitleTv = null;
        if (mBannerRv != null) {
            mBannerRv.removeAllViews();
            mBannerRv.destroyDrawingCache();
            mBannerRv = null;
        }
    }

    public void onResume() {
        start();
    }

    public void pause() {
        if (mBannerRv != null && autoPlay) {
            mBannerRv.pause();
        }
    }

    public void onPause() {
        pause();
    }

    protected void restart() {
        if (autoPlay) {
            pause();
            start();
        }
    }

    public RxBanner setCustomIndicator(View indicatorView) {
        this.indicatorView = indicatorView;
        return this;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public RxBanner setAutoPlay(boolean autoPlay) {
        if (mBannerRv != null && mAdapter != null) {
            mBannerRv.setAutoPlay(autoPlay);
        }
        this.autoPlay = autoPlay;
        RxBannerLogger.i("setAutoPlay = " + autoPlay);
        return this;
    }

    public void setCurrentPosition(int position) {
        if (mBannerRv != null && mAdapter != null && mLayoutManager != null && !mAdapter.getDatas().isEmpty()) {
            mBannerRv.smoothScrollToPosition(position);
            if (autoPlay && mBannerRv.isRunning()) {
                mBannerRv.pause();
                mBannerRv.start();
            }
        }
    }

    public int getCurrentPosition() {
        if (mBannerRv != null && mAdapter != null && !mAdapter.getDatas().isEmpty()) {
            return mLayoutManager.getCurrentPosition();
        }
        return -1;
    }

    public RxBanner setOnBannerClickListener(RxBannerClickListener onClickListener) {
        if (mAdapter != null) {
            mAdapter.setRxBannerClickListener(onClickListener);
        }
        this.onBannerClickListener = onClickListener;
        return this;
    }

    private int getPercentSize() {
        int percentSize = RelativeLayout.LayoutParams.MATCH_PARENT;
        if (orientation == LinearLayout.VERTICAL) {
            if (parentHeight == RelativeLayout.LayoutParams.MATCH_PARENT)
                return RelativeLayout.LayoutParams.MATCH_PARENT;
            percentSize = RxBannerUtil.getPercentSize(parentHeight, itemPercent);
        } else if (orientation == LinearLayout.HORIZONTAL) {
            if (parentWidth == RelativeLayout.LayoutParams.MATCH_PARENT)
                return RelativeLayout.LayoutParams.MATCH_PARENT;
            percentSize = RxBannerUtil.getPercentSize(parentWidth, itemPercent);
        }
        return percentSize;
    }

    public RxBanner setItemAnimator(RecyclerView.ItemAnimator itemAnimator) {
        if (mBannerRv != null) {
            mBannerRv.setItemAnimator(itemAnimator);
        }
        this.itemAnimator = itemAnimator;
        return this;
    }

    protected boolean isParentCreate() {
        return parentWidth != 0 && parentWidth != -1 && parentHeight != 0 && parentHeight != -1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final View v;
        try {
            // 从第一个已创建的子view获取父布局的宽高，用于下面的百分比计算
            v = getChildAt(0);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (v.getWidth() != 0 && v.getHeight() != 0) {
                    if (parentWidth == 0 || parentWidth == -1) parentWidth = v.getWidth();
                    if (parentHeight == 0 || parentHeight == -1) parentHeight = v.getHeight();
                    if (parentWidth != 0 && parentWidth != -1 && parentHeight != 0 && parentHeight != -1 && needStart)
                        initStart();
                }
            }
        });
    }

    public RxBannerTextView getTitleTextView() {
        if (mTitleTv != null) {
            return mTitleTv;
        }
        return null;
    }

    public String getTitleString() {
        if (mTitleTv != null) {
            return mTitleTv.getText().toString();
        } else {
            return null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        onDestroy();
    }
}
