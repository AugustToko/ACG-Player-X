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

package top.geek_studio.chenlongcould.musicplayer.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import top.geek_studio.chenlongcould.musicplayer.adapter.HomeAdapter

/**
 * @param priority 优先级
 * @param title 标题
 * @param subTitle 副标题
 * @param arrayList 数据
 * @param homeSection
 * @param icon 图标
 * */
class Home(val priority: Int,
           @StringRes val title: Int,
           @StringRes val subTitle: Int,
           val arrayList: ArrayList<*>,
           @HomeAdapter.Companion.HomeSection
           val homeSection: Int,
           @DrawableRes
           val icon: Int)