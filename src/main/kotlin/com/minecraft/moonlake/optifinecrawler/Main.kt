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

/**************************************************************************
 *
 * Main Method
 *
 **************************************************************************/

fun main(args: Array<out String>) {
    val oc = OptifineCrawler()
    oc.requestVersionList().forEach { println(it) }
}

/**************************************************************************
 *
 * Inline Method
 *
 **************************************************************************/

inline fun String.htmlEscape(vararg targets: HtmlEscape): String {
    var final = this
    return targets.forEach { final = final.replace(it.escape, it.value) }.let { final }
}

inline fun String.htmlEscapes(): String
        = htmlEscape(*HtmlEscape.values())

inline fun NodeList.toArrayList(): List<Element>
        = (0..this.length).map { this.item(it) }.filter { it != null && it is Element }.map { it as Element }
