/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import java.text.DecimalFormatSymbols;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.calculator2.view.CalculatorViewPager;
import com.android.calculator2.view.GraphView;
import com.xlythe.engine.theme.App;

public class Page {
    private final String mName;
    private final String mKey;
    private final boolean mDefaultValue;
    private final boolean mHasTutorial;
    private final Panel mPanel;
    private final boolean mIsSmall;
    private View mView;

    public Page(Context context, Panel panel) {
        mName = context.getString(panel.getName());
        mKey = panel.name();
        mDefaultValue = context.getResources().getBoolean(panel.getDefaultValue());
        mHasTutorial = panel.hasTutorial();
        mPanel = panel;
        mIsSmall = panel.getClass().isAssignableFrom(SmallPanel.class);
    }

    Page(App app) {
        mName = app.getName();
        mKey = app.getPackageName();
        mDefaultValue = true;
        mHasTutorial = false;
        mPanel = null;
        mIsSmall = false;
    }

    Page(Page page) {
        mName = page.getName();
        mKey = page.getKey();
        mDefaultValue = page.getDefaultValue();
        mHasTutorial = page.hasTutorial();
        mPanel = page.mPanel;
        mIsSmall = page.isSmall();
    }

    public static List<Page> getAllPages(Context context) {
        var list = new ArrayList<Page>();
        for (Panel p : NormalPanel.values()) {
            list.add(new Page(context, p));
        }

        /* TODO: Readd when extensions are added
        List<App> extensions = Extension.getApps(context);
        for (App a : extensions) {
            Page page = new Page(a);
            list.add(page);
        } */

        sortPages(context, list);
        return list;
    }

    public static List<Page> getPages(Context context) {
        var list = new ArrayList<Page>();
        for (Panel p : NormalPanel.values()) {
            var page = new Page(context, p);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }

        /* TODO: Readd when extensions are added
        List<App> extensions = Extension.getApps(context);
        for (App a : extensions) {
            Page page = new Page(a);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        } */

        sortPages(context, list);
        while (!list.isEmpty() && list.size() < 4
                && CalculatorSettings.useInfiniteScrolling(context)) {
            // Double the records to avoid using the same view twice
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.add(new Page(list.get(i)));
            }
        }

        return list;
    }

    public static List<Page> getSmallPages(Context context) {
        var list = new ArrayList<Page>();
        for (Panel p : SmallPanel.values()) {
            var page = new Page(context, p);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }

        sortPages(context, list);
        while (!list.isEmpty() && list.size() < 4
                && CalculatorSettings.useInfiniteScrolling(context)) {
            // Double the records to avoid using the same view twice
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.add(new Page(list.get(i)));
            }
        }

        return list;
    }

    public static List<Page> getLargePages(Context context) {
        var list = new ArrayList<Page>();
        for (Panel p : LargePanel.values()) {
            var page = new Page(context, p);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        }

        /* TODO Readd when extensions are added
        List<App> extensions = Extension.getApps(context);
        for (App a : extensions) {
            Page page = new Page(a);
            if (CalculatorSettings.isPageEnabled(context, page)) {
                list.add(page);
            }
        } */

        sortPages(context, list);
        while (!list.isEmpty() && list.size() < 4
                && CalculatorSettings.useInfiniteScrolling(context)) {
            // Double the records to avoid using the same view twice
            int size = list.size();
            for (int i = 0; i < size; i++) {
                list.add(new Page(list.get(i)));
            }
        }

        return list;
    }

    private static void sortPages(Context context, List<Page> list) {
        Collections.sort(list, (lhs, rhs) -> Integer.compare(
                CalculatorSettings.getPageOrder(context, lhs),
                CalculatorSettings.getPageOrder(context, rhs)));
    }

    public static Page getCurrentPage(CalculatorViewPager pager) {
        List<Page> pages = ((CalculatorPageAdapter) pager.getAdapter()).getPages();
        if (!pages.isEmpty()) {
            return pages.get(pager.getCurrentItem() % pages.size());
        } else {
            return null;
        }
    }

    public static Page getPage(List<Page> pages, String name) {
        for (Page p : pages) {
            if (p.getName().equals(name)) {
                return p;
            }
        }

        return null;
    }

    public static int getOrder(List<Page> pages, Page page) {
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            if (p.equals(page)) {
                return i;
            }
        }

        return -1;
    }

    public static List<Page> removeDuplicates(List<Page> pages) {
        var clean = new ArrayList<Page>();
        for (Page p : pages) {
            if (clean.contains(p)) {
                continue;
            }

            clean.add(p);
        }

        return clean;
    }

    public String getName() {
        return mName;
    }

    public String getKey() {
        return mKey;
    }

    public boolean getDefaultValue() {
        return mDefaultValue;
    }

    public boolean hasTutorial() {
        return mHasTutorial;
    }

    public boolean isGraph() {
        return (mPanel != null)
                && (NormalPanel.GRAPH.equals(mPanel) || LargePanel.GRAPH.equals(mPanel));
    }

    public boolean isBasic() {
        return (mPanel != null)
                && (NormalPanel.BASIC.equals(mPanel) || LargePanel.BASIC.equals(mPanel));
    }

    public boolean isAdvanced() {
        return (mPanel != null)
                && (NormalPanel.ADVANCED.equals(mPanel) || SmallPanel.ADVANCED.equals(mPanel));
    }

    public boolean isSmall() {
        return mIsSmall;
    }

    public boolean isLarge() {
        return !mIsSmall;
    }

    public void showTutorial(Calculator calc, boolean animate) {
        if (mPanel != null && mPanel.hasTutorial()) {
            mPanel.showTutorial(calc, animate);
        }
    }

    public View getView(Context context) {
        return getView(context, null, null, null);
    }

    public View getView(Context context, EventListener listener, Graph graph, Logic logic) {
        if (mPanel != null) {
            if (mView == null) {
                mView = mPanel.getView(context);
                mView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        mView = null;
                    }

                    @Override
                    public void onViewAttachedToWindow(View v) {
                        // Do nothing here
                    }
                });
            }

            if (logic != null) {
                mPanel.refresh(context, mView, listener, graph, logic);
            }
        } else {
            mView = null;
        }

        return mView;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Page p) {
            return Objects.equals(getKey(), p.getKey());
        }

        return false;
    }

    private static void setDecimalButton(View view) {
        final var dfs = new DecimalFormatSymbols();
        if (view.findViewById(R.id.dot) != null) {
            var dotBtn = (Button) view.findViewById(R.id.dot);
            dotBtn.setText(String.valueOf(dfs.getDecimalSeparator()));
        }
    }

    public enum NormalPanel implements Panel {
        GRAPH(R.string.graph, R.bool.GRAPH, true),
        HEX(R.string.hexPanel, R.bool.HEX, true),
        BASIC(R.string.basic, R.bool.BASIC, true),
        ADVANCED(R.string.advanced, R.bool.ADVANCED, false),
        MATRIX(R.string.matrix, R.bool.MATRIX, true);
        final int mName;
        final int mDefaultValue;
        final boolean mHasTutorial;
        private View mMatrixView;
        private boolean mLaunchMatrixCling = false;
        private final Map<View, GraphView> mGraphHolder = new HashMap<>();

        NormalPanel(int name, int defaultValue, boolean hasTutorial) {
            mName = name;
            mDefaultValue = defaultValue;
            mHasTutorial = hasTutorial;
        }

        public int getName() {
            return mName;
        }

        public int getDefaultValue() {
            return mDefaultValue;
        }

        public boolean hasTutorial() {
            return mHasTutorial;
        }

        public View getView(Context context) {
            return switch (this) {
                case BASIC -> View.inflate(context, R.layout.simple_pad, null);
                case GRAPH -> View.inflate(context, R.layout.graph_pad, null);
                case MATRIX -> View.inflate(context, R.layout.matrix_pad, null);
                case ADVANCED -> View.inflate(context, R.layout.advanced_pad, null);
                case HEX -> View.inflate(context, R.layout.hex_pad, null);
                default -> null;
            };
        }

        public void showTutorial(final Calculator calc, boolean animate) {
            switch (this) {
                case BASIC -> calc.showFirstRunSimpleCling(animate);
                case GRAPH -> calc.showFirstRunGraphCling(animate);
                case MATRIX -> {
                    if (mMatrixView == null || !mMatrixView.isEnabled()) {
                        mLaunchMatrixCling = true;
                    } else {
                        calc.showFirstRunMatrixCling(animate, mMatrixView);
                    }
                }
                case HEX -> calc.showFirstRunHexCling(animate);
                default -> {}
            }
        }

        @Override
        public void refresh(Context context, final View view, EventListener listener,
                final Graph graph, final Logic logic) {
            setDecimalButton(view);
            if (NormalPanel.GRAPH.equals(this)) {
                if (!mGraphHolder.containsKey(view)) {
                    final GraphView graphView = logic.mGraphView = graph.createGraph(context);
                    mGraphHolder.put(view, graphView);
                    graphView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            mGraphHolder.remove(view);
                        }

                        @Override
                        public void onViewAttachedToWindow(View v) {
                            // Do nothing here
                        }
                    });

                    graphView.getViewTreeObserver().addOnGlobalLayoutListener(
                            () -> logic.getGraphModule().updateGraph(graph));

                    logic.setGraphDisplay(graphView);
                    var l = (LinearLayout) view.findViewById(R.id.graph);
                    l.addView(graphView, new LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT));

                    listener.setGraphDisplay(graphView);
                    View zoomIn = view.findViewById(R.id.zoomIn);
                    zoomIn.setOnClickListener(listener);

                    View zoomOut = view.findViewById(R.id.zoomOut);
                    zoomOut.setOnClickListener(listener);

                    View zoomReset = view.findViewById(R.id.zoomReset);
                    zoomReset.setOnClickListener(listener);

                    logic.getGraphModule().updateGraph(graph);
                } else {
                    mGraphHolder.get(view).invalidate();
                }
            } else if (NormalPanel.HEX.equals(this)) {
                if (logic != null) {
                    switch (logic.getBaseModule().getMode()) {
                        case BINARY -> view.findViewById(R.id.bin).setSelected(true);
                        case DECIMAL -> view.findViewById(R.id.dec).setSelected(true);
                        case HEXADECIMAL -> view.findViewById(R.id.hex).setSelected(true);
                        default -> {}
                    }
                }
            } else if (NormalPanel.MATRIX.equals(this)) {
                mMatrixView = view;
                mMatrixView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        v.setEnabled(true);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        v.setEnabled(false);
                    }
                });

                if (mLaunchMatrixCling) {
                    mMatrixView.getViewTreeObserver().addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @SuppressLint("NewApi")
                                @SuppressWarnings("deprecation")
                                @Override
                                public void onGlobalLayout() {
                                    if (android.os.Build.VERSION.SDK_INT < 16) {
                                        mMatrixView.getViewTreeObserver()
                                                .removeGlobalOnLayoutListener(this);
                                    } else {
                                        mMatrixView.getViewTreeObserver()
                                                .removeOnGlobalLayoutListener(this);
                                    }

                                    ((Calculator) mMatrixView.getContext())
                                            .showFirstRunMatrixCling(false, mMatrixView);
                                }
                            });
                }

                View easterEgg = view.findViewById(R.id.easter);
                if (easterEgg != null && listener != null) {
                    easterEgg.setOnClickListener(listener);
                    easterEgg.setOnLongClickListener(listener);
                }
            }
        }
    }

    public enum SmallPanel implements Panel {
        HEX(R.string.hexPanel, R.bool.HEX, true),
        ADVANCED(R.string.advanced, R.bool.ADVANCED, false);
        final int mName;
        final int mDefaultValue;
        final boolean mHasTutorial;

        SmallPanel(int name, int defaultValue, boolean hasTutorial) {
            mName = name;
            mDefaultValue = defaultValue;
            mHasTutorial = hasTutorial;
        }

        public int getName() {
            return mName;
        }

        public int getDefaultValue() {
            return mDefaultValue;
        }

        public boolean hasTutorial() {
            return mHasTutorial;
        }

        public View getView(Context context) {
            return switch (this) {
                case ADVANCED -> View.inflate(context, R.layout.advanced_pad, null);
                case HEX -> View.inflate(context, R.layout.hex_pad, null);
                default -> null;
            };
        }

        public void showTutorial(Calculator calc, boolean animate) {
            if (this == HEX) {
                calc.showFirstRunHexCling(animate);
            }
        }

        @Override
        public void refresh(Context context, View view, EventListener listener, Graph graph,
                Logic logic) {
            setDecimalButton(view);
            if (SmallPanel.HEX.equals(this)) {
                if (logic != null) {
                    switch (logic.getBaseModule().getMode()) {
                        case BINARY -> view.findViewById(R.id.bin).setSelected(true);
                        case DECIMAL -> view.findViewById(R.id.dec).setSelected(true);
                        case HEXADECIMAL -> view.findViewById(R.id.hex).setSelected(true);
                        default -> {}
                    }
                }
            }
        }
    }

    public enum LargePanel implements Panel {
        GRAPH(R.string.graph, R.bool.GRAPH, true),
        BASIC(R.string.basic, R.bool.BASIC, true),
        MATRIX(R.string.matrix, R.bool.MATRIX, true);
        final int mName;
        final int mDefaultValue;
        final boolean mHasTutorial;
        private View mMatrixView;
        private boolean mLaunchMatrixCling = false;
        private final Map<View, GraphView> mGraphHolder = new HashMap<>();

        LargePanel(int name, int defaultValue, boolean hasTutorial) {
            mName = name;
            mDefaultValue = defaultValue;
            mHasTutorial = hasTutorial;
        }

        public int getName() {
            return mName;
        }

        public int getDefaultValue() {
            return mDefaultValue;
        }

        public boolean hasTutorial() {
            return mHasTutorial;
        }

        public View getView(Context context) {
            return switch (this) {
                case BASIC -> View.inflate(context, R.layout.simple_pad, null);
                case GRAPH -> View.inflate(context, R.layout.graph_pad, null);
                case MATRIX -> View.inflate(context, R.layout.matrix_pad, null);
                default -> null;
            };
        }

        public void showTutorial(Calculator calc, boolean animate) {
            switch (this) {
                case BASIC -> calc.showFirstRunSimpleCling(animate);
                case GRAPH -> calc.showFirstRunGraphCling(animate);
                case MATRIX -> {
                    if (mMatrixView == null || !mMatrixView.isEnabled()) {
                        mLaunchMatrixCling = true;
                    } else {
                        calc.showFirstRunMatrixCling(animate, mMatrixView);
                    }
                }
                default -> {}
            }
        }

        @Override
        public void refresh(Context context, final View view, EventListener listener,
                final Graph graph, final Logic logic) {
            setDecimalButton(view);
            if (LargePanel.GRAPH.equals(this)) {
                if (!mGraphHolder.containsKey(view)) {
                    final GraphView graphView = logic.mGraphView = graph.createGraph(context);
                    mGraphHolder.put(view, graphView);
                    graphView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            mGraphHolder.remove(view);
                        }

                        @Override
                        public void onViewAttachedToWindow(View v) {
                            // Do nothing here
                        }
                    });

                    graphView.getViewTreeObserver().addOnGlobalLayoutListener(
                            () -> logic.getGraphModule().updateGraph(graph));

                    logic.setGraphDisplay(graphView);
                    var l = (LinearLayout) view.findViewById(R.id.graph);
                    l.addView(graphView, new LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT));

                    listener.setGraphDisplay(graphView);
                    View zoomIn = view.findViewById(R.id.zoomIn);
                    zoomIn.setOnClickListener(listener);

                    View zoomOut = view.findViewById(R.id.zoomOut);
                    zoomOut.setOnClickListener(listener);

                    View zoomReset = view.findViewById(R.id.zoomReset);
                    zoomReset.setOnClickListener(listener);

                    logic.getGraphModule().updateGraph(graph);
                } else {
                    mGraphHolder.get(view).invalidate();
                }
            } else if (LargePanel.MATRIX.equals(this)) {
                mMatrixView = view;
                mMatrixView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        v.setEnabled(true);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        v.setEnabled(false);
                    }
                });

                if (mLaunchMatrixCling) {
                    mMatrixView.getViewTreeObserver().addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @SuppressLint("NewApi")
                                @SuppressWarnings("deprecation")
                                @Override
                                public void onGlobalLayout() {
                                    if (android.os.Build.VERSION.SDK_INT < 16) {
                                        mMatrixView.getViewTreeObserver()
                                                .removeGlobalOnLayoutListener(this);
                                    } else {
                                        mMatrixView.getViewTreeObserver()
                                                .removeOnGlobalLayoutListener(this);
                                    }

                                    ((Calculator) mMatrixView.getContext())
                                            .showFirstRunMatrixCling(false, mMatrixView);
                                }
                            });
                }

                View easterEgg = view.findViewById(R.id.easter);
                if (easterEgg != null && listener != null) {
                    easterEgg.setOnClickListener(listener);
                    easterEgg.setOnLongClickListener(listener);
                }
            }
        }
    }

    public interface Panel {
        String name();

        int getName();

        int getDefaultValue();

        boolean hasTutorial();

        View getView(Context context);

        void showTutorial(Calculator calc, boolean animate);

        void refresh(Context context, View view, EventListener listener,
                Graph graph, Logic logic);
    }
}
