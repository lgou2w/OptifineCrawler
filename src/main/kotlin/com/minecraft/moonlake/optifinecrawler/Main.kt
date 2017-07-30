/*
 * Copyright (C) 2017 The MoonLake Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@file:JvmName("Main")

package com.minecraft.moonlake.optifinecrawler

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File

/**************************************************************************
 *
 * Main Method
 *
 **************************************************************************/

fun main(args: Array<out String>) {
    val oc = OptifineCrawler()
    val optifineVer = oc.requestVersionList()[0] // 下载第一个最新的预发布版
    oc.downloadOptifine(optifineVer, File("target\\${optifineVer.version}.jar")) // 保存文件
    println("下载完成.")
}

/**************************************************************************
 *
 * Inline Method
 *
 **************************************************************************/

inline fun String.httpEscape(vararg targets: HttpEscape): String {
    var final = this
    return targets.forEach { final = final.replace(it.escape, it.value) }.let { final }
}

inline fun String.httpEscapes(): String
        = httpEscape(*HttpEscape.values())

inline fun NodeList.toArrayList(): List<Element>
        = (0..this.length).map { this.item(it) }.filter { it != null && it is Element }.map { it as Element }
