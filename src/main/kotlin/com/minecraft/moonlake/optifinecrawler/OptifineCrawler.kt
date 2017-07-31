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
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

open class OptifineCrawler {

    /**************************************************************************
     *
     * Member
     *
     **************************************************************************/

    /**
     * 目标 Optifine.net 的下载页面链接
     */
    protected val url: String

    /**
     * Http 请求工厂
     */
    val factory: HttpRequestFactory

    /**************************************************************************
     *
     * Constructor
     *
     **************************************************************************/

    constructor(url: String = "http://optifine.net", factory: HttpRequestFactory = optifineFactory()) {
        this.url = url
        this.factory = factory
    }

    /**************************************************************************
     *
     * Overridable Method
     *
     **************************************************************************/

    /**
     * 将请求到的下载页面完整内容进行解析为 Optifine 版本对象列表
     */
    @Throws(RuntimeException::class)
    open fun requestVersionList(): List<OptifineVersion> {
        val list: MutableList<OptifineVersion> = ArrayList()
        try {
            val content = factory.requestGet(parseDownloadUrl()).httpEscapes().toByteArray()
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
                                        optifineVer.preview = it.textContent.contains("pre", true) // 包含 pre 则说明这个版本为预发布版
                                    }
                                    "downloadLineDownload", "downloadLineDownloadFirst" -> optifineVer.download = (it.getElementsByTagName("a").item(0) as Element).getAttribute("href")
                                    "downloadLineMirror" -> optifineVer.downloadMirror = (it.getElementsByTagName("a").item(0) as Element).getAttribute("href")
                                    "downloadLineChangelog" -> optifineVer.changelog = "$url/" + (it.getElementsByTagName("a").item(0) as Element).getAttribute("href")
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
     * 将指定 Optifine 版本下载到指定输出文件
     */
    @Throws(RuntimeException::class, IllegalStateException::class)
    open fun downloadOptifine(optifineVer: OptifineVersion, out: File) {
        if(optifineVer.isEmpty() || optifineVer.downloadMirror == null)
            throw IllegalStateException("目标 Optifine 版本对象信息为空或下载镜像为 null 值.")

        /**
         * 向版本的镜像链接发送 Http 请求解析到里面的这个最终下载链接即可
         * <a href="downloadx?f=OptiFine_1.12_HD_U_C4.jar&x=7d68d2c63569bad6674f6d4f61ffcf51">Download OptiFine_1.12_HD_U_C4.jar</a>
         * <a href="downloadx?f=preview_OptiFine_1.12_HD_U_C5_pre.jar&x=938f132853697860dc6843c6b5d6de8c">Download preview_OptiFine_1.12_HD_U_C5_pre.jar</a>
         */
        try {
            var target: String = ""
            val content = factory.requestGet(optifineVer.downloadMirror!!).httpEscapes()
            val matcher = Pattern.compile("\"downloadx\\?f=${if(optifineVer.preview) "preview_" else ""}OptiFine(.*)\"").matcher(content)
            while (matcher.find())
                target = "http://optifine.net/downloadx?f=${if(optifineVer.preview) "preview_" else ""}OptiFine${matcher.group(1)}"
            // 解析成功后完成最后的下载任务
            factory.requestDownload(target, out)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * 解析目标 Optifine 的下载链接
     */
    protected open fun parseDownloadUrl(): String
            = "$url/downloads"

    /**************************************************************************
     *
     * Static
     *
     **************************************************************************/

    companion object {
        /**
         * OptifineCrawler 的默认 Http 请求工厂
         */
        private fun optifineFactory(): HttpRequestFactory {
            return object: HttpRequestFactory {
                @Throws(Exception::class)
                override fun requestGet(url: String): String {
                    val result: String?
                    try {
                        val connection = URL(url).openConnection() as HttpURLConnection
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
                        result = byteArrayOutput.toString("utf-8")
                    } catch (e: Exception) {
                        throw e
                    }
                    return result ?: ""
                }
                @Throws(Exception::class)
                override fun requestDownload(url: String, out: File) {
                    var input: InputStream? = null
                    var access: RandomAccessFile? = null
                    try {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.doInput = true
                        connection.connectTimeout = 15000
                        connection.readTimeout = 15000
                        connection.addRequestProperty("User-Agent", "MoonLake OptifineCrawler by lgou2w")
                        connection.connect()
                        if(connection.responseCode / 100 != 2)
                            throw IOException("请求的目标响应码不为 200, 当前: ${connection.responseCode}.")
                        val contentLength = connection.contentLength
                        if(contentLength < 1)
                            throw IOException("请求的目标内容长度无效.")
                        if(!(out.parentFile.isDirectory || out.mkdirs()))
                            throw IOException("无法创建目录文件.")
                        val tmp = File(out.absolutePath + ".mldltmp")
                        if(!tmp.exists())
                            tmp.createNewFile()
                        else if(!tmp.renameTo(tmp))
                            throw IllegalStateException("临时文件处于锁状态, 请检测是否被其他进程占用.")
                        input = connection.inputStream
                        access = RandomAccessFile(tmp, "rw")
                        val buffer: ByteArray = ByteArray(1024)
                        var length = 0
                        while (input.read(buffer).apply { length = this } != -1)
                            access.write(buffer, 0, length)
                        if(input != null) try {
                            input.close()
                            input = null
                        } catch (e: Exception) {
                        }
                        if(access != null) try {
                            access.close()
                            access = null
                        } catch (e: Exception) {
                        }
                        if(out.exists())
                            out.delete()
                        tmp.renameTo(out)
                    } catch (e: Exception) {
                        out.delete()
                        throw e
                    } finally {
                        if(input != null) try {
                            input.close()
                        } catch (e: Exception) {
                        }
                        if(access != null) try {
                            access.close()
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }
}
