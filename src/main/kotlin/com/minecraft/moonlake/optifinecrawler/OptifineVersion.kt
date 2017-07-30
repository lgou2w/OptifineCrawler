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

package com.minecraft.moonlake.optifinecrawler

data class OptifineVersion(
        var version: String? = null,
        var download: String? = null,
        var downloadMirror: String? = null,
        var changelog: String? = null,
        var date: String? = null,
        var preview: Boolean = false) {

    /**
     * Version: OptiFine 1.12 HD U C4
     * Download: http://adf.ly/404181/optifine.net/adloadx?f=OptiFine_1.12_HD_U_C4.jar
     * DownloadMirror: http://optifine.net/adloadx?f=OptiFine_1.12_HD_U_C4.jar
     * Changelog: http://optifine.net/changelog?f=OptiFine_1.12_HD_U_C4.jar
     * Date: 03.07.2017
     * Preview: Preview Version
     */

    /**
     * 获取此 Optifine 版本是否为空
     */
    fun isEmpty(): Boolean
            = (version == null && download == null && downloadMirror == null && changelog == null && date == null)
}
