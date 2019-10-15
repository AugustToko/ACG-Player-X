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

package top.geek_studio.chenlongcould.musicplayer.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import com.kabouzeid.appthemehelper.util.ColorUtil
import top.geek_studio.chenlongcould.musicplayer.Common.R;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ColorIconsImageView : AppCompatImageView {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context) : super(context) {
        init(context, null)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        // Load the styled attributes and set their properties
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.ColorIconsImageView, 0, 0)
        setIconBackgroundColor(attributes.getColor(R.styleable.ColorIconsImageView_iconBackgroundColor, Color.RED))
        attributes.recycle()
    }

    private fun setIconBackgroundColor(color: Int) {
        setBackgroundResource(R.drawable.color_circle_gradient)
        backgroundTintList = ColorStateList.valueOf(ColorUtil.adjustAlpha(color, 0.22f))
        imageTintList = ColorStateList.valueOf(ColorUtil.withAlpha(color, 0.75f))
        requestLayout()
        invalidate()
    }
}
