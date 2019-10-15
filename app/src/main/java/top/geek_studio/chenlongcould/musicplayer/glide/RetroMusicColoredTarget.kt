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

package top.geek_studio.chenlongcould.musicplayer.glide

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.request.animation.GlideAnimation
import com.kabouzeid.appthemehelper.util.ATHUtil
import top.geek_studio.chenlongcould.musicplayer.Common.R
import top.geek_studio.chenlongcould.musicplayer.glide.palette.BitmapPaletteTarget
import top.geek_studio.chenlongcould.musicplayer.glide.palette.BitmapPaletteWrapper
import top.geek_studio.chenlongcould.musicplayer.util.PreferenceUtil
import top.geek_studio.chenlongcould.musicplayer.util.RetroColorUtil.getColor
import top.geek_studio.chenlongcould.musicplayer.util.RetroColorUtil.getDominantColor

abstract class RetroMusicColoredTarget(view: ImageView) : BitmapPaletteTarget(view) {

    protected val defaultFooterColor: Int
        get() = ATHUtil.resolveColor(getView().context, R.attr.defaultFooterColor)

    protected val albumArtistFooterColor: Int
        get() = ATHUtil.resolveColor(getView().context, R.attr.cardBackgroundColor)

    override fun onLoadFailed(e: Exception, errorDrawable: Drawable?) {
        super.onLoadFailed(e, errorDrawable)
        onColorReady(defaultFooterColor)
    }

    override fun onResourceReady(resource: BitmapPaletteWrapper,
                                 glideAnimation: GlideAnimation<in BitmapPaletteWrapper>?) {
        super.onResourceReady(resource, glideAnimation)

        val defaultColor = defaultFooterColor

        onColorReady(if (PreferenceUtil.getInstance(getView().context).isDominantColor)
            getDominantColor(resource.bitmap, defaultColor)
        else
            getColor(resource.palette, defaultColor))
    }

    abstract fun onColorReady(color: Int)
}
