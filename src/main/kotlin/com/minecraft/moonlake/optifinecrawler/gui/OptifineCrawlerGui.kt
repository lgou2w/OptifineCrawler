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

package com.minecraft.moonlake.optifinecrawler.gui

import com.minecraft.moonlake.optifinecrawler.HttpRequestFactory
import com.minecraft.moonlake.optifinecrawler.OptifineCrawler
import com.minecraft.moonlake.optifinecrawler.OptifineVersion
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.concurrent.ScheduledService
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.Duration
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.Callable

class OptifineCrawlerGui: Application() {

    /**************************************************************************
     *
     * Static
     *
     **************************************************************************/

    companion object {
        fun launch(args: Array<out String>) {
            launch(OptifineCrawlerGui::class.java, *args)
        }
    }

    /**************************************************************************
     *
     * Member
     *
     **************************************************************************/

    val version = "1.0.0"
    val author = "Month_Light"
    val title = "OptifineCrawler $version by $author - https://github.com/lgou2w/OptifineCrawler"

    // private member
    private val optifineCrawler = OptifineCrawler(GuiRequestFactory()) // 使用 GUI 的请求工厂

    /**************************************************************************
     *
     * Implement Method
     *
     **************************************************************************/

    override fun start(primaryStage: Stage) {
        primaryStage.scene = Scene(createUserGui(), 650.0, 400.0)
        primaryStage.isResizable = false
        primaryStage.title = title
        primaryStage.centerOnScreen()
        primaryStage.show()
    }

    /**************************************************************************
     *
     * Private Member
     *
     **************************************************************************/

    private val tableView = TableView<OptifineVersion>()
    private val tableDownload = TableColumn<OptifineVersion, String>("Download")
    private val tablePreview = TableColumn<OptifineVersion, Boolean>("Preview")
    private val tableVersion = TableColumn<OptifineVersion, String>("Version")
    private val tableDate = TableColumn<OptifineVersion, String>("Date")
    private val downloadSelected = Button("> Download Selected Item <")
    private val requestVerList = Button("> Get The Version List <")
    private val downloadLabel = Label("No Download")
    private val downloadProgress = ProgressBar()
    private val downloadGroup = HBox()
    private val rootPane = BorderPane()
    private val btnGroup = HBox()

    /**************************************************************************
     *
     * Private Method
     *
     **************************************************************************/

    private fun createUserGui(): Parent {
        btnGroup.spacing = 10.0
        btnGroup.padding = Insets(10.0, .0, 10.0, 10.0)
        downloadGroup.spacing = 10.0
        downloadProgress.setPrefSize(120.0, downloadProgress.prefHeight)
        downloadGroup.padding = Insets(2.5, .0, 2.5, 5.0)
        downloadGroup.children.setAll(downloadLabel, downloadProgress)
        btnGroup.children.setAll(requestVerList, downloadSelected, downloadGroup)
        tableDownload.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.downloadMirror }) }
        tablePreview.cellValueFactory = Callback { it -> Bindings.createBooleanBinding(Callable { it.value.preview }) }
        tableVersion.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.version }) }
        tableDate.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.date }) }
        tableView.columns.addAll(tableVersion, tableDate, tableDownload, tablePreview)
        tableDownload.prefWidth = 250.0
        tablePreview.prefWidth = 80.0
        tableVersion.prefWidth = 200.0
        tableDate.prefWidth = 80.0
        rootPane.center = tableView
        rootPane.bottom = btnGroup
        initListener()
        return rootPane
    }

    private fun initListener() {
        requestVerList.setOnAction { _ -> run {
            val verList = optifineCrawler.requestVersionList()
            tableView.items.setAll(verList)
            println("Optifine Crawler Version List Size: ${verList.size}")
        }}
        downloadSelected.setOnAction { _ -> run {
            val index = tableView.selectionModel.selectedIndex
            if(index == -1) {
                showMessage(Alert.AlertType.WARNING, "Please check the download and try again.", "Error:", ButtonType.OK)
            } else {
                val item = tableView.items[index]
                println("Start Download: " + item.version)
                downloadVer(item)
            }
        }}
    }

    private fun downloadVer(optifineVer: OptifineVersion, disableBtnGroup: Boolean = true) {
        val finalFile = File(System.getProperty("user.dir"), "${optifineVer.version}.jar")
        val task = (optifineCrawler.factory as GuiRequestFactory).requestDownloadTask(optifineVer, finalFile)
        task.stateProperty().addListener { _, _, newValue -> run {
            when(newValue) {
                Worker.State.SCHEDULED -> disableBtnGroupButton(disableBtnGroup)
                Worker.State.CANCELLED, Worker.State.FAILED, Worker.State.SUCCEEDED -> releaseDownloadGroup()
                else -> { }
            }
        }}
        downloadProgress.progressProperty().bind(task.progressProperty())
        downloadLabel.textProperty().bind(task.messageProperty())
        Thread(task).start()
    }

    private fun releaseDownloadGroup() {
        val service = object: ScheduledService<Unit>() {
            override fun createTask(): Task<Unit> {
                return object: Task<Unit>() {
                    override fun call() {
                        Platform.runLater {
                            disableBtnGroupButton(false)
                            downloadProgress.progressProperty().unbind()
                            downloadProgress.progress = -1.0
                            downloadLabel.textProperty().unbind()
                            downloadLabel.text = "No Download"
                        }
                    }
                }
            }
        }
        service.delay = Duration.seconds(3.0)
        service.start()
    }

    private fun disableBtnGroupButton(state: Boolean) {
        btnGroup.children.filter { it is Button }.forEach { it.isDisable = state }
    }

    private fun showMessage(alertType: Alert.AlertType, message: String, title: String, vararg buttons: ButtonType): Optional<ButtonType> {
        val alert = Alert(alertType, message, *buttons)
        alert.title = title
        alert.graphic = null
        alert.headerText = null
        return alert.showAndWait()
    }

    private inner class GuiRequestFactory: HttpRequestFactory {
        // 持有一个默认的请求工厂
        private val defOptifineFactory = OptifineCrawler.optifineFactory()
        private val rounding = DecimalFormat("#.00")

        @Throws(Exception::class)
        override fun requestGet(url: String): String {
            return defOptifineFactory.requestGet(url)
        }

        @Throws(Exception::class)
        override fun requestDownload(url: String, out: File) {
            defOptifineFactory.requestDownload(url, out)
        }

        fun requestDownloadTask(optifineVer: OptifineVersion, out: File): Task<Unit> {
            // 这个函数的话就是通过自己实现返回一个 task 对象
            // 拥有进度属性和消息属性
            return object: Task<Unit>() {
                override fun call() {
                    updateMessage("Downloading...")
                    val url = optifineCrawler.parseResultFileUrl(optifineVer)
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
                        val contentLength = connection.contentLength.toLong()
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
                        var length = 0; var downloaded = 0L
                        var lastDownloaded = 0L
                        var lastTime = System.currentTimeMillis()
                        while (input.read(buffer).apply { length = this } != -1) {
                            access.write(buffer, 0, length)
                            downloaded += length
                            val now = System.currentTimeMillis()
                            if(now - lastTime >= 1000) {
                                updateProgress(downloaded, contentLength)
                                updateMessage(formatDownloadSpeed(downloaded, lastDownloaded))
                                lastDownloaded = downloaded
                                lastTime = now
                            }
                        }
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

                override fun succeeded() {
                    updateProgress(1.0, 1.0)
                    updateMessage("Download completed.")
                }

                override fun failed() {
                    updateProgress(.0, 1.0)
                    updateMessage("Download failed.")
                }

                override fun done() {
                    println("Task Done.")
                }

                private fun formatDownloadSpeed(downloaded: Long, lastDownloaded: Long): String {
                    val kilobyte = (downloaded - lastDownloaded) / 1024.0
                    if(kilobyte <= 1024.0)
                        return "Speed: ${rounding.format(kilobyte)}KB/s"
                    return "Speed: ${rounding.format(kilobyte / 1024.0)}MB/s"
                }
            }
        }
    }
}
