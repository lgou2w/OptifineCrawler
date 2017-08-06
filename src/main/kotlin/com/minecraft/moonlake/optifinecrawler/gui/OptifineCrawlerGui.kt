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
import javafx.beans.binding.Bindings
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
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
    val title = "OptifineCrawler v$version by $author - https://github.com/lgou2w/OptifineCrawler"

    /**************************************************************************
     *
     * Implement Method
     *
     **************************************************************************/

    override fun start(primaryStage: Stage) {
        owner = primaryStage
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

    private var owner: Stage? = null
    private val optifineCrawler = OptifineCrawler(GuiRequestFactory()) // 使用 GUI 的请求工厂
    private val tableView = TableView<OptifineVersion>()
    private val tableDownload = TableColumn<OptifineVersion, String>("Download")
    private val tablePreview = TableColumn<OptifineVersion, Boolean>("Preview")
    private val tableVersion = TableColumn<OptifineVersion, String>("Version")
    private val tableMcVer = TableColumn<OptifineVersion, String>("MC")
    private val tableDate = TableColumn<OptifineVersion, String>("Date")
    private val downloadSelectedInstall = Button(">Download&Install")
    private val downloadSelected = Button(">Download")
    private val requestVerList = Button(">GetVersionList")
    private val downloadLabel = Label("No Download")
    private val downloadProgress = ProgressBar()
    private val menuCopy = MenuItem("Copy")
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
        btnGroup.children.setAll(requestVerList, downloadSelected, downloadSelectedInstall, downloadGroup)
        tableDownload.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.downloadMirror }) }
        tablePreview.cellValueFactory = Callback { it -> Bindings.createBooleanBinding(Callable { it.value.preview }) }
        tableVersion.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.version }) }
        tableMcVer.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.mcVer() }) }
        tableDate.cellValueFactory = Callback { it -> Bindings.createStringBinding(Callable { it.value.date }) }
        tableView.columns.addAll(tableMcVer, tableVersion, tableDate, tableDownload, tablePreview)
        tableView.selectionModel.selectionMode = SelectionMode.SINGLE
        tableView.contextMenu = ContextMenu(menuCopy)
        tableDownload.prefWidth = 230.0
        tablePreview.prefWidth = 70.0
        tableVersion.prefWidth = 200.0
        tableMcVer.prefWidth = 60.0
        tableDate.prefWidth = 80.0
        rootPane.center = tableView
        rootPane.bottom = btnGroup
        rootPane.style = " -fx-font-family: 'Ubuntu', 'Microsoft YaHei'; -fx-font-size: 13px;"
        initListener()
        return rootPane
    }

    private fun initListener() {
        requestVerList.setOnAction { _ -> run {
            val verList = optifineCrawler.requestVersionList()
            println("Optifine Crawler Version List Size: ${verList.size}")
            tableView.items.setAll(verList)
        }}
        downloadSelected.setOnAction { _ -> run {
            val item = getTableViewSelected()
            if(item != null) {
                println("Start Download: " + item.version)
                downloadVer(item)
            }
        }}
        downloadSelectedInstall.setOnAction { _ -> run {
            showMessage(Alert.AlertType.INFORMATION, "This feature has not yet been implemented.", "Info:", ButtonType.OK)
        }}
        menuCopy.setOnAction { _ -> run {
            val item = getTableViewSelected()
            if(item != null) {
                val clipboard = Clipboard.getSystemClipboard()
                val content = ClipboardContent()
                content.putString(item.toString())
                clipboard.setContent(content)
            }
        }}
    }

    private fun getTableViewSelected(message: Boolean = true): OptifineVersion? {
        val index = tableView.selectionModel.selectedIndex
        if(index == -1) {
            if(message)
                showMessage(Alert.AlertType.WARNING, "Please check the selected item and try again.", "Error:", ButtonType.OK)
            return null
        } else {
            return tableView.items[index]
        }
    }

    private fun customSaveDirectory(): File? {
        val directoryChooser = DirectoryChooser()
        directoryChooser.initialDirectory = File(System.getProperty("user.dir"))
        directoryChooser.title = "Choose to save the folder:"
        val file = directoryChooser.showDialog(owner)
        if(file == null || file.isDirectory.not()) {
            showMessage(Alert.AlertType.WARNING, "Please check whether to select a file or a folder.", "Error:", ButtonType.OK)
            return null
        } else {
            return file
        }
    }

    private fun downloadVer(optifineVer: OptifineVersion, disableBtnGroup: Boolean = true) {
        val directory = customSaveDirectory() ?: return
        val finalFile = File(directory, "${optifineVer.version}.jar")
        println("Download to -> ${finalFile.absolutePath}")
        val task = (optifineCrawler.factory as GuiRequestFactory).requestDownloadTask(optifineVer, finalFile)
        task.stateProperty().addListener { _, _, newValue -> run {
            when(newValue) {
                Worker.State.SCHEDULED -> disableBtnGroupButton(disableBtnGroup)
                Worker.State.CANCELLED, Worker.State.FAILED, Worker.State.SUCCEEDED -> disableBtnGroupButton(false)
                else -> { }
            }
        }}
        downloadProgress.progressProperty().bind(task.progressProperty())
        downloadLabel.textProperty().bind(task.messageProperty())
        val thread = Thread(task, "OptifineCrawler Download Task")
        thread.isDaemon = true
        thread.start()
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
                    println("Download Task Done.")
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
