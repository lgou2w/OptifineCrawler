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

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import javax.xml.parsers.DocumentBuilderFactory

open class OptifineCrawler {

    /**************************************************************************
     *
     * Protocol Member
     *
     **************************************************************************/

    /**
     * 目标 Optifine.net 的下载页面链接
     */
    protected val url: String

    /**************************************************************************
     *
     * Constructor
     *
     **************************************************************************/

    constructor(url: String = "http://optifine.net") {
        this.url = url
    }

    /**************************************************************************
     *
     * Overridable Method
     *
     **************************************************************************/

    /**
     * 将请求到的下载页面完整内容进行解析为 Optifine 版本对象列表
     */
    open fun requestVersionList(): List<OptifineVersion> {
        val list: MutableList<OptifineVersion> = ArrayList()
        try {
            val connection = URL(parseDownloadUrl()).openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.useCaches = false
            connection.addRequestProperty("User-Agent", "MoonLake OptifineCrawler by lgou2w")
            connection.connect()
            val input = connection.inputStream
            val byteArrayOutput = ByteArrayOutputStream()
            val buffer: ByteArray = ByteArray(1024)
            var length = 0
            while(input.read(buffer).apply { length = this } != -1)
                byteArrayOutput.write(buffer, 0, length)

            val content = byteArrayOutput.toString("utf-8").htmlEscapes().toByteArray()
            val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val element = builder.parse(ByteArrayInputStream(content)).documentElement
            element
                    .getElementsByTagName("table").toArrayList()
                    .filter { it.getAttribute("class") == "downloadTable" }
                    .map { it.getElementsByTagName("tr").toArrayList() }
                    .forEach {
                        it.map { it.getElementsByTagName("td").toArrayList() }.forEach {
                            val optifineVer = OptifineVersion()
                            it.forEach {
                                val className: String? = it.getAttribute("class")
                                when(className) {
                                    "downloadLineFile", "downloadLineFileFirst" -> {
                                        optifineVer.version = it.textContent
                                        optifineVer.pre = it.textContent.contains("pre", true) // 包含 pre 则说明这个版本为预发布版
                                    }
                                    "downloadLineDownload", "downloadLineDownloadFirst" -> optifineVer.download = (it.getElementsByTagName("a").item(0) as Element).getAttribute("href")
                                    "downloadLineMirror" -> optifineVer.downloadMirror = (it.getElementsByTagName("a").item(0) as Element).getAttribute("href")
                                    "downloadLineChangelog" -> optifineVer.changelog = url + (it.getElementsByTagName("a").item(0) as Element).getAttribute("href")
                                    "downloadLineDate" -> optifineVer.date = it.textContent
                                }
                            }
                            list.add(optifineVer)
                        }
                    }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return list
    }

    /**
     * 解析目标 Optifine 的下载链接
     */
    protected open fun parseDownloadUrl(): String
            = "$url/downloads"
}
