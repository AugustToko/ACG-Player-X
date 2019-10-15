/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package top.geek_studio.chenlongcould.musicplayer.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.util.ATHUtil;
import top.geek_studio.chenlongcould.musicplayer.Common.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RetroColorUtil {
    public static int toolbarColor(@NonNull Context context) {
        int color = ThemeStore.primaryColor(context);
        if (ATHUtil.isWindowBackgroundDark(context)) {
            return ATHUtil.resolveColor(context, R.attr.cardBackgroundColor);
        } else {
            return color;
        }
    }

    @Nullable
    public static Palette generatePalette(@Nullable Bitmap bitmap) {
        return bitmap == null ? null : Palette.from(bitmap).clearFilters().generate();
    }

    public static int getTextColor(@Nullable Palette palette) {
        if (palette == null) {
            return -1;
        }

        int inverse = -1;
        if (palette.getVibrantSwatch() != null) {
            inverse = palette.getVibrantSwatch().getRgb();
        } else if (palette.getLightVibrantSwatch() != null) {
            inverse = palette.getLightVibrantSwatch().getRgb();
        } else if (palette.getDarkVibrantSwatch() != null) {
            inverse = palette.getDarkVibrantSwatch().getRgb();
        }

        int background = getSwatch(palette).getRgb();

        if (inverse != -1) {
            return ColorUtilKot.INSTANCE.getReadableText(inverse, background, 150);
        }
        return ColorUtilKot.INSTANCE.stripAlpha(getSwatch(palette).getTitleTextColor());
    }

    @NonNull
    public static Palette.Swatch getSwatch(@Nullable Palette palette) {
        if (palette == null) {
            return new Palette.Swatch(Color.WHITE, 1);
        }
        return getBestPaletteSwatchFrom(palette.getSwatches());

    }

    public static int getMatColor(Context context, String typeColor) {
        int returnColor = Color.BLACK;
        int arrayId = context.getResources().getIdentifier("md_" + typeColor, "array",
                context.getApplicationContext().getPackageName());

        if (arrayId != 0) {
            TypedArray colors = context.getResources().obtainTypedArray(arrayId);
            int index = (int) (Math.random() * colors.length());
            returnColor = colors.getColor(index, Color.BLACK);
            colors.recycle();
        }
        return returnColor;
    }

    @ColorInt
    public static int getColor(@Nullable Palette palette, int fallback) {
        if (palette != null) {
            if (palette.getVibrantSwatch() != null) {
                return palette.getVibrantSwatch().getRgb();
            } else if (palette.getDarkVibrantSwatch() != null) {
                return palette.getDarkVibrantSwatch().getRgb();
            } else if (palette.getLightVibrantSwatch() != null) {
                return palette.getLightVibrantSwatch().getRgb();
            } else if (palette.getMutedSwatch() != null) {
                return palette.getMutedSwatch().getRgb();
            } else if (palette.getLightMutedSwatch() != null) {
                return palette.getLightMutedSwatch().getRgb();
            } else if (palette.getDarkMutedSwatch() != null) {
                return palette.getDarkMutedSwatch().getRgb();
            } else if (!palette.getSwatches().isEmpty()) {
                return Collections.max(palette.getSwatches(), SwatchComparator.getInstance()).getRgb();
            }
        }
        return fallback;
    }

    private static Palette.Swatch getTextSwatch(@Nullable Palette palette) {
        if (palette == null) {
            return new Palette.Swatch(Color.BLACK, 1);
        }
        if (palette.getVibrantSwatch() != null) {
            return palette.getVibrantSwatch();
        } else {
            return new Palette.Swatch(Color.BLACK, 1);
        }
    }

    @ColorInt
    public static int getBackgroundColor(@Nullable Palette palette) {
        return getProperBackgroundSwatch(palette).getRgb();
    }

    private static Palette.Swatch getProperBackgroundSwatch(@Nullable Palette palette) {
        if (palette == null) {
            return new Palette.Swatch(Color.BLACK, 1);
        }
        if (palette.getDarkMutedSwatch() != null) {
            return palette.getDarkMutedSwatch();
        } else if (palette.getMutedSwatch() != null) {
            return palette.getMutedSwatch();
        } else if (palette.getLightMutedSwatch() != null) {
            return palette.getLightMutedSwatch();
        } else {
            return new Palette.Swatch(Color.BLACK, 1);
        }
    }

    private static Palette.Swatch getBestPaletteSwatchFrom(Palette palette) {
        if (palette != null) {
            if (palette.getVibrantSwatch() != null) {
                return palette.getVibrantSwatch();
            } else if (palette.getMutedSwatch() != null) {
                return palette.getMutedSwatch();
            } else if (palette.getDarkVibrantSwatch() != null) {
                return palette.getDarkVibrantSwatch();
            } else if (palette.getDarkMutedSwatch() != null) {
                return palette.getDarkMutedSwatch();
            } else if (palette.getLightVibrantSwatch() != null) {
                return palette.getLightVibrantSwatch();
            } else if (palette.getLightMutedSwatch() != null) {
                return palette.getLightMutedSwatch();
            } else if (!palette.getSwatches().isEmpty()) {
                return getBestPaletteSwatchFrom(palette.getSwatches());
            }
        }
        return null;
    }

    private static Palette.Swatch getBestPaletteSwatchFrom(List<Palette.Swatch> swatches) {
        if (swatches == null) {
            return null;
        }
        return Collections.max(swatches, (opt1, opt2) -> {
            int a = opt1 == null ? 0 : opt1.getPopulation();
            int b = opt2 == null ? 0 : opt2.getPopulation();
            return a - b;
        });
    }


    public static int getDominantColor(Bitmap bitmap, int defaultFooterColor) {
        List<Palette.Swatch> swatchesTemp = Palette.from(bitmap).generate().getSwatches();
        List<Palette.Swatch> swatches = new ArrayList<Palette.Swatch>(swatchesTemp);
        Collections.sort(swatches, (swatch1, swatch2) -> swatch2.getPopulation() - swatch1.getPopulation());
        return swatches.size() > 0 ? swatches.get(0).getRgb() : defaultFooterColor;
    }

    @ColorInt
    public static int shiftBackgroundColorForLightText(@ColorInt int backgroundColor) {
        while (ColorUtilKot.INSTANCE.isColorLight(backgroundColor)) {
            backgroundColor = ColorUtilKot.INSTANCE.darkenColor(backgroundColor);
        }
        return backgroundColor;
    }

    @ColorInt
    public static int shiftBackgroundColorForDarkText(@ColorInt int backgroundColor) {
        while (!ColorUtilKot.INSTANCE.isColorLight(backgroundColor)) {
            backgroundColor = ColorUtilKot.INSTANCE.lightenColor(backgroundColor);
        }
        return backgroundColor;
    }

    private static class SwatchComparator implements Comparator<Palette.Swatch> {

        private static SwatchComparator sInstance;

        static SwatchComparator getInstance() {
            if (sInstance == null) {
                sInstance = new SwatchComparator();
            }
            return sInstance;
        }

        @Override
        public int compare(Palette.Swatch lhs, Palette.Swatch rhs) {
            return lhs.getPopulation() - rhs.getPopulation();
        }
    }
}
